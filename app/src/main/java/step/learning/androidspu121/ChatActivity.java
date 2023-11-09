package step.learning.androidspu121;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import android.content.Context;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import step.learning.androidspu121.orm.ChatMessage;
import step.learning.androidspu121.orm.ChatResponse;

public class ChatActivity extends AppCompatActivity {
    private final static String chatHost = "https://chat.momentfor.fun";
    private final byte[] buffer = new byte[8192];
    private final static String savesFilename = "saves.chat";
    private final Gson gson = new Gson();
    private final List<ChatMessage> chatMessages = new ArrayList<>();
    private EditText etNick;
    private EditText etMessage;
    private ScrollView svContainer;
    private LinearLayout llContainer;
    private Animation newMsgNotifAnimation;
    private MediaPlayer newMsgNotifSound;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // loadChatMessages();
        new Thread(this::loadChatMessages).start();

        etNick = findViewById(R.id.chat_et_nick);
        etMessage = findViewById(R.id.chat_et_message);
        svContainer = findViewById(R.id.chat_sv_container);
        llContainer = findViewById(R.id.chat_ll_container);
        findViewById(R.id.chat_btn_send).setOnClickListener(this::sendButtonClick);
        findViewById(R.id.chat_btn_savenick).setOnClickListener(this::saveNickClick);

        handler = new Handler();
        handler.post(this::updateChat);
        
        if (!loadNick()) {
            etNick.requestFocus();
            Toast.makeText(this, "Виберіть собі нік", Toast.LENGTH_LONG).show();
        }

        /*
        ??????
         */
        findViewById(R.id.tvMsgNotifUnreadCount).setVisibility(View.INVISIBLE);

        newMsgNotifSound = MediaPlayer.create(
                ChatActivity.this,
                R.raw.newmsg_notif
        );
        newMsgNotifAnimation = AnimationUtils.loadAnimation(
                ChatActivity.this,
                R.anim.chat_notif_unread_pop
        );
    }


    private void updateChat() {
        new Thread(this::loadChatMessages).start();
        handler.postDelayed(this::updateChat, 3000);
    }


    private void saveNickClick(View view) {
        String nick = etNick.getText().toString();
        if (nick.isEmpty()) {
            Toast.makeText(this, "Заповніть поле нікнейму", Toast.LENGTH_SHORT).show();
            etNick.requestFocus();
            return;
        }
        try (FileOutputStream writer = openFileOutput(savesFilename, Context.MODE_PRIVATE)) {
            writer.write( nick.getBytes(StandardCharsets.UTF_8) );
        }
        catch (IOException ex) {
            Log.e("saveNickClick", ex.getMessage());
        }
    }

    private boolean loadNick() {
        try (FileInputStream reader = openFileInput(savesFilename)) {
            String nick = readString(reader);
            if (nick.isEmpty()) {
                return false;
            }
            etNick.setText(nick);
            return true;
        }
        catch (IOException ex) {
            Log.e("loadNickClick", ex.getMessage());
            return false;
        }
    }

    // TODO: merge with sendMessage()
    private void sendButtonClick(View view) {
        String nick = etNick.getText().toString();
        String message = etMessage.getText().toString();

        if (nick.isEmpty()) {
            Toast.makeText(this, "Введіть нік", Toast.LENGTH_SHORT).show();
            return;
        }
        if (message.isEmpty()) {
            Toast.makeText(this, "Введіть повідомлення", Toast.LENGTH_SHORT).show();
            return;
        }

        final ChatMessage chatMessage = new ChatMessage();
        chatMessage.setAuthor(nick);
        chatMessage.setText(message);

        new Thread( () -> postChatMessage(chatMessage) ).start();
    }

    private void postChatMessage(ChatMessage chatMessage) {
        try {
            // POST повідомлення надсилається у декілька етапів
            // 1. Налаштування з'єднання
            URL url = new URL(chatHost);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);   // У з'єднання можна писати (Output) - формувати тіло
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            // Заголовки HTTP встановлюються як RequestProperty
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Accept", "*/*");
            connection.setChunkedStreamingMode(0); // Не ділити на блоки - надсилати одним пакетом

            // 2. Формуємо тіло запиту (пишемо Output)
            OutputStream outputStream = connection.getOutputStream();

            // author=Nick&msg=Message
            // !! 2+2 -> &msg=2+2 --> 2 2 (+ в url це код пробіла)
            // 2&2 --> author=Nick&msg=2&2 --> author=nick, msg=2, 2=null
            // My %20 --> My [space] -- %20 код пробіла ==> перед надсиланням дані треба кодувати
            String body = String.format("author=%s&msg=%s",
                    URLEncoder.encode(chatMessage.getAuthor(), StandardCharsets.UTF_8.name()),
                    URLEncoder.encode(chatMessage.getText()), StandardCharsets.UTF_8.name()
            );
            outputStream.write( body.getBytes(StandardCharsets.UTF_8) );
            outputStream.flush();
            outputStream.close();

            // 3. Одержуємо відповідь, перевіряємо статус, за потреби читаємо тіло
            int statusCode = connection.getResponseCode();
            if (statusCode == 201) {    // у разі успіху приходить лише статус, тіла нема
                Log.d("postChatMessage", "SENT OK");
                new Thread(this::loadChatMessages).start();
                runOnUiThread( () -> {
                    etMessage.setText("");
                    etMessage.requestFocus();
                });
            }
            else {  // якщо не успіх, то повідомлення про помилку - у тілі
                InputStream inputStream = connection.getInputStream();
                String responseBody = readString( inputStream );
                inputStream.close();
                Log.e("postChatMessage", statusCode + " " + responseBody);
            }

            // 4. Закриваємо підключення, звільняємо ресурс
            connection.disconnect();
        }
        catch (Exception ex) {
            Log.e("postChatMessage", ex.getMessage());
        }
    }


    private String readString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) > 0) {
            builder.write(buffer, 0, bytesRead);
        }

        String result = builder.toString(StandardCharsets.UTF_8.name());
        builder.close();
        return result;
    }


    private void loadChatMessages() {
        try {
            URL chatUrl = new URL(chatHost);
            InputStream chatStream = chatUrl.openStream();
            String data = readString(chatStream);

            ChatResponse chatResponse = gson.fromJson(data, ChatResponse.class);
            // Впорядковуємо відповідь - останні повідомлення "знизу", тобто за зростанням дати-часу
            chatResponse.getData().sort(Comparator.comparing(ChatMessage::getMomentAsDate));

            boolean wasNewMessage = false;
            for (ChatMessage chatMessage : chatResponse.getData()) {
                if (chatMessages.stream().noneMatch(m ->
                        m.getId().equals(chatMessage.getId()))
                ) {
                    chatMessages.add(chatMessage);
                    wasNewMessage = true;
                }
            }
            if (wasNewMessage) {
                runOnUiThread(this::showChatMessages);
            }

            chatStream.close();
        }
        catch (MalformedURLException e) {
            Log.d("loadChatMessages -> chatUrl", "MalformedURLException" + e.getMessage());
        }
        catch (IOException e) {
            Log.d("loadChatMessages -> chatStream", "IOException" + e.getMessage());
        }
        catch (android.os.NetworkOnMainThreadException ex) {
            Log.d("loadChatMessages", "NetworkOnMainThreadException " + ex.getMessage());
        }
        catch (java.lang.SecurityException ex) {
            Log.d("loadChatMessages", "SecurityException " + ex.getMessage());
        }
    }

    private void showChatMessages() {
        // додавання View вниз вимагає прокрутки контейнера
        boolean needScroll = false;

        for (ChatMessage chatMessage : this.chatMessages) {
            if (chatMessage.getView() == null) {
                View messageView;
                boolean isUserMsg;

                if (Objects.equals(chatMessage.getAuthor(), etNick.getText().toString())) {
                    isUserMsg = true;
                }
                else {
                    isUserMsg = false;
                }
                messageView = createChatMessageView(chatMessage, isUserMsg);
                chatMessage.setView(messageView);
                llContainer.addView(messageView);

                needScroll = true;
                chatMessage.setView(messageView);
            }
        }

        if (needScroll) {
            // Прокрутка контейнера (ScrollView) до самого нижнього положення.
            // !! Але додавання елементів до контейнера у попередніх операціях (addView)
            // ще не "відпрацьована" на UI - як елементи вони є, але їх розміри ще не прораховані.
            // Команду прокрутки треба ставити у !чергу! за відображенням
            svContainer.post( () -> svContainer.fullScroll(View.FOCUS_DOWN) );
        }
    }

    private View createChatMessageView(ChatMessage chatMessage, boolean isUserMessage) {
        /*
        Створення елемента програмно складається з кількох дій:
        1. "Внутрішні" налаштування
        2. "Зовнішні" - правила "вбудови" елемента в інші елементи (Layouts)
         */
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        layoutParams.setMargins(5, 5, 5, 5);

        if (isUserMessage) {
            layoutParams.gravity = Gravity.END;
        }
        else {
            layoutParams.gravity = Gravity.START;
        }


        LinearLayout messageLayout = new LinearLayout(this);
        /*
        margin
        --messageLayout-- {
            textView {Author}
            textView {Text}
        }
         */
        messageLayout.setLayoutParams(layoutParams);
        messageLayout.setPadding(50, 25, 50, 25);
        messageLayout.setOrientation(LinearLayout.VERTICAL);

        if (isUserMessage) {
            messageLayout.setBackground(AppCompatResources.getDrawable(ChatActivity.this, R.drawable.chat_message_mine));
        }
        else {
            messageLayout.setBackground(AppCompatResources.getDrawable(ChatActivity.this, R.drawable.chat_message_other));
        }

        TextView textView = new TextView(this);
        textView.setText(chatMessage.getMoment());
        textView.setTextSize(10);
        textView.setTypeface(null, Typeface.ITALIC);
        messageLayout.addView(textView);

        textView = new TextView(this);
        textView.setText(chatMessage.getAuthor());
        textView.setTypeface(null, Typeface.BOLD);
        messageLayout.addView(textView);

        textView = new TextView(this);
        textView.setText(chatMessage.getText());
        messageLayout.addView(textView);

        return messageLayout;
    }



    private void setNotificationState() {
        TextView notifBubble = findViewById(R.id.tvMsgNotifUnreadCount);
        int unreadMsgs = 0;



        if (unreadMsgs == 0) {
            notifBubble.setVisibility(View.INVISIBLE);
            return;
        }

        //runOnUiThread( () -> notifBubble.setText( ) );

        notifBubble.startAnimation(newMsgNotifAnimation);
        newMsgNotifSound.start();
        notifBubble.setVisibility(View.VISIBLE);
    }
}

/*
    private void sendMessage(View view) {
        TextView notifBubble = (TextView) findViewById(R.id.tvMsgNotifUnreadCount);
        int notifCounter = Integer.parseInt(notifBubble.getText().toString()) + 1;
        runOnUiThread(() -> notifBubble.setText(String.valueOf(notifCounter)));
        notifBubble.startAnimation(newMsgNotifAnimation);
        newMsgNotifSound.start();
    }
 */


/*
Робота з мережею Інтернет
Основний об'єкт для роботи з мережею - URL. Він дещо нагадує File у контексті
надання доступу до даних, а також у тому, що створення об'єкту не спричинює
мережної активності. Звернення до мережі починається при зверненні до
методів цього об'єкту.

Особливості:
1. android.os.NetworkOnMainThreadException
     Звертатись до мережи не можна з UI потоку, необхідно створювати новий потік

2. java.lang.SecurityException: Permission denied (missing INTERNET permission?)
    Для роботи з мережею необхідно зазначити дозвіл у маніфесті
    (app.manifests.AndroidManifest.xml -> add string <uses-permission android:name="android.permission.INTERNET" />)

3. android.view.ViewRootImpl$CalledFromWrongThreadException: Only the original thread that created a view hierarchy can touch its views.
    Оскільки робота з мережею ведеться з окремого потоку, прямі звернення до
    елементів UI не дозволяються. Делегування запуску здійснюється методом 'runOnUiThread(...)'
 */

/*
Періодичний запуск - особливості

Є дві поширені схеми періодичного запуску - Таймер та Подійний цикл
Таймер, як правило, створюється як окремий потік, який подає хроно-імпульси
Подійний цикл використовує внутрішній цикл застосунку, плануючи відтермінований запуск.
Для управління внутрішнім циклом використовують об'єкти типу Handler

 */