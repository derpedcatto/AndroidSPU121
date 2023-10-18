package step.learning.androidspu121;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

public class OnSwipeListener implements View.OnTouchListener {
    private final GestureDetector gestureDetector;

    // інтерфейс - набір методів для переозначення в інших класах (для використання)
    public void onSwipeBottom() { }      // абстрактні або порожні?
    public void onSwipeLeft()   { }      // якщо зробити абстрактними, то кожен клас має
    public void onSwipeRight()  { }      // реализівати УСІ методи. Порожні тіла дозволяють
    public void onSwipeTop()    { }      // переозначити ОКРЕМІ з них

    // Конструктор - має приймати контекст (область) у якому діють жести
    public OnSwipeListener(Context context) {
        // передаємо детектору наш обробник (див. нижче)
        this.gestureDetector = new GestureDetector(context, new SwipeGestureListener());
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        // делегуємо оброблення подій до нашого детектора
        return this.gestureDetector.onTouchEvent(motionEvent);
    }

    // Створюємо клас, який буде аналізувати повідомлення детектора жестів
    private final class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int MIN_SWIPE_DISTANCE = 100;
        private static final int MIN_SWIPE_VELOCITY = 100;

        // База для свайпів - Fling
        // торкання->проведення->відпускання(e2)
        @Override
        public boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            // обробники подій повертають true, якщо це 'їх подія' і вона буде оброблена,
            // інакше false, що свідчить про потребу передавати подію іншим обробникам
            boolean result = false;

            // задача - визначити відстань, швидкість та напрям жесту; накласти обмеження
            // на те, щоб вважати жест свайпом

            float dx = e2.getX() - e1.getX(); // відстань по горизонталі (зі знаком)
            float dy = e2.getY() - e1.getY(); // відстань по вертикалі (зі знаком)

            // Сучасні екрани з великою роздільною здатністю можуть навіть жест
            // дотику врахувати як проведення на невелику відстань
            // Відповідно, слід ввести обмеження на мінімальну довжину
            // жесту для вважаня його свайпом. Також бажано обмежити
            // мінімальну швидкість руху для відрізнення йього жесту від scroll

            if (Math.abs(dx) > Math.abs(dy)) {
                // відстань (без знаку) по Х більше ніж по Y -- вважаємо жест горизонтальним
                // а значить далі перевіряємо обмеження на горизонтальну швидкість
                if (Math.abs(dx) >= MIN_SWIPE_DISTANCE && velocityX >= MIN_SWIPE_VELOCITY) {
                    if (dx < 0) {   // e2 <-- e1
                        onSwipeLeft();
                    }
                    else {          // e1 --> e2
                        onSwipeRight();
                    }
                    result = true;
                }
            }
            else {  // dx < dy -- жест вертикальний
                if (Math.abs(dy) >= MIN_SWIPE_DISTANCE && velocityY >= MIN_SWIPE_VELOCITY) {
                    // e1.Y         e2.Y
                    //  v   dy > 0   ^   dy < 0
                    // e2.Y         e1.Y
                    if (dy < 0) {
                        onSwipeTop();
                    }
                    else {
                        onSwipeBottom();
                    }
                    result = true;
                }
            }

            return result;
        }

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }
    }
}

/*
Детектор жестів, свайпи
Пристрої з сенсорним екраном мають ряд особливостей у контексті подій від UI
Замість традиційних для десктопів подій миши з'являються нові:
onTouch (торкання), onFling (проведення), onLongTouch
 */
