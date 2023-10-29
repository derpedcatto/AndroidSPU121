package step.learning.androidspu121;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import step.learning.androidspu121.orm.ChatMessage;
import step.learning.androidspu121.orm.ChatResponse;

public class ChatActivity extends AppCompatActivity {
    private final static String chatHost = "https://chat.momentfor.fun";
    private final byte[] buffer = new byte[8192];
    private final Gson gson = new Gson();
    private final List<ChatMessage> chatMessages = new ArrayList<>();
    private EditText etNick;
    private EditText etMessage;
    private ScrollView svContainer;
    private LinearLayout llContainer;
    private Animation newMsgNotifAnimation;
    private MediaPlayer newMsgNotifSound;

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
        findViewById(R.id.chat_btn_send).setOnClickListener(this::sendMessage);

        newMsgNotifSound = MediaPlayer.create(
                ChatActivity.this,
                R.raw.newmsg_notif
        );
        newMsgNotifAnimation = AnimationUtils.loadAnimation(
                ChatActivity.this,
                R.anim.chat_notif_unread_pop
        );
    }

    private void loadChatMessages() {
        try {
            URL chatUrl = new URL(chatHost);
            InputStream chatStream = chatUrl.openStream();

            ByteArrayOutputStream builder = new ByteArrayOutputStream();
            int bytesRead;
            while ((bytesRead = chatStream.read(buffer)) > 0) {
                builder.write(buffer, 0, bytesRead);
            }

            String data = builder.toString(StandardCharsets.UTF_8.name());
            ChatResponse chatResponse = gson.fromJson(data, ChatResponse.class);
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

            builder.close();
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
        for (ChatMessage chatMessage : this.chatMessages) {
            if (Objects.equals(chatMessage.getAuthor(), etNick.getText().toString())) {
                llContainer.addView(createChatMessageView(chatMessage, true));
            }
            else {
                llContainer.addView(createChatMessageView(chatMessage, false));
            }
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
        textView.setText(chatMessage.getAuthor());
        messageLayout.addView(textView);

        textView = new TextView(this);
        textView.setText(chatMessage.getText());
        messageLayout.addView(textView);

        return messageLayout;
    }

    private void sendMessage(View view) {
        TextView notifBubble = (TextView) findViewById(R.id.tvMsgNotifUnreadCount);
        int notifCounter = Integer.parseInt(notifBubble.getText().toString()) + 1;
        runOnUiThread(() -> notifBubble.setText(String.valueOf(notifCounter)));
        notifBubble.startAnimation(newMsgNotifAnimation);
        newMsgNotifSound.start();
    }
}

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