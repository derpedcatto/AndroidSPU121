package step.learning.androidspu121;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {
    private static final int N = 4;     // Розмір поля
    private int[][] cells = new int[N][N];
    private TextView[][] tvCells = new TextView[N][N];
    private int score;
    private TextView tvScore;
    private int bestScore;
    private TextView tvBestScore;
    private final Random random = new Random();
    private Animation spawnCellAnimation;
    private Animation collapseCellAnimation;
    private MediaPlayer spawnSound;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_game );

        spawnSound = MediaPlayer.create(
                GameActivity.this,
                R.raw.pickup_00
        );
        spawnCellAnimation = AnimationUtils.loadAnimation(
                GameActivity.this,
                R.anim.game_spawn_cell
        );
        spawnCellAnimation.reset();
        collapseCellAnimation = AnimationUtils.loadAnimation(
                GameActivity.this,
                R.anim.game_collapse_cells
        );
        collapseCellAnimation.reset();


        tvScore = findViewById(R.id.game_tv_score);
        tvScore.setOnClickListener(v -> {moveLeft();spawnCell();showField();});
        tvBestScore = findViewById(R.id.game_tv_best);


        // Пошук ідентифікаторів за іменем (String) та ресурсів через них
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                tvCells[i][j] = findViewById(
                        getResources()     // R.
                            .getIdentifier(
                                    "game_cell_" + i + j,
                                    "id",
                                    getPackageName()
                            )
                );
            }
        }

        // Задати ігровому полю висоту таку ж як ширину
        // Проблема: на етапі onCreate розміри ще не відомі
        TableLayout gameField = findViewById( R.id.game_field ) ;
        gameField.post(   // поставити задачу у чергу, вона буде виконана
                // коли gameField виконає усі попередні задачі, у т.ч.
                // розрахунок розміру та рисування.
                () -> {
                    int windowWidth = this.getWindow().getDecorView().getWidth() ;
                    int margin =
                            ((LinearLayout.LayoutParams)gameField.getLayoutParams()).leftMargin;
                    int fieldSize = windowWidth - 2 * margin ;
                    LinearLayout.LayoutParams layoutParams =
                            new LinearLayout.LayoutParams( fieldSize, fieldSize ) ;
                    layoutParams.setMargins( margin, margin, margin, margin );
                    gameField.setLayoutParams( layoutParams ) ;
                }
        ) ;


        findViewById( R.id.game_layout ).setOnTouchListener(
                new OnSwipeListener( GameActivity.this ) {
                    @Override public void onSwipeBottom() {
                        processMove(MoveDirection.BOTTOM);
                    }
                    @Override public void onSwipeLeft() {
                        processMove(MoveDirection.LEFT);
                    }
                    @Override public void onSwipeRight() {
                        processMove(MoveDirection.RIGHT);
                        // Toast.makeText( GameActivity.this, "Ходу немає (->)", Toast.LENGTH_SHORT ).show();
                    }
                    @Override public void onSwipeTop() {
                        processMove(MoveDirection.TOP);
                    }
                } ) ;

        newGame();
    }

    private void newGame() {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                cells[i][j] = 0;
            }
        }
        spawnCell();
        spawnCell();
        showField();
    }

    /**
     * Поставити нове число у випадкову вільну комірку
     * Значення: 2 (з імовірністю 0.9) або 4
     */
    private void spawnCell() {
        // Шукаємо всі вільні комірки
        List<Integer> freeCellsIndexes = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (cells[i][j] == 0) {
                    freeCellsIndexes.add(i * N + j);
                }
            }
        }
        // Визначаємо випадкову з вільних
        int randIndex = freeCellsIndexes.get(random.nextInt(freeCellsIndexes.size()));
        int x = randIndex / N;
        int y = randIndex % N;

        cells[x][y] = random.nextInt(10) == 0   // Умова з імов. 0.1
                        ? 4
                        : 2;

        // призначаємо анімацію для її представлення
        tvCells[x][y].startAnimation(spawnCellAnimation);
        // програємо звук
        spawnSound.start();
    }

    /**
     * Показ усіх комірок - відображення масиву чисел на ігрове поле
     */
    private void showField() {
        // для кожної комірки визначаємо стиль у відповідності до
        // її значення та застосовуємо його
        Resources resources = getResources();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                // сам текст комірки
                tvCells[i][j].setText(String.valueOf(cells[i][j]));
                // стиль !! але зі стилем заст. не всі атрибути
                tvCells[i][j].setTextAppearance(
                        resources.getIdentifier(
                                "game_cell_" + cells[i][j],
                                "style",
                                getPackageName()
                        )
                );
                // фонові атрибути потрібно додавати окремо (до стилів)
                tvCells[i][j].setBackgroundColor(
                        resources.getColor(
                                resources.getIdentifier(
                                        "game_cell_background_" + cells[i][j],
                                        "color",
                                        getPackageName()
                                ),
                                getTheme()
                        )
                );
            }
        }

        // виводимо текст та кращий (заповнюючи плейсхолдер ресурсу)
        tvScore.setText(getString(R.string.game_tv_score, score));
        tvBestScore.setText(getString(R.string.game_tv_best, bestScore));
    }

    private boolean canMoveLeft() {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N - 1; j++) {
                if (cells[i][j] == 0 && cells[i][j + 1] != 0 ||
                    cells[i][j] != 0 && cells[i][j] == cells[i][j + 1]) {
                    return true;
                }
            }
        }
        return false;
    }

    private void moveLeft() {
        // 1. Переміщуємо всі значення ліворуч (без "пробілів")
        // 2. Перевіряємо коллапси (злиття), зливаємо
        // 3. Повторюємо п. 1 після злиття
        // 4. Повторюємо пункти для кожного рядка
        for (int i = 0; i < N; i++) {
            // 1. ліворуч
            boolean repeat;
            do {
                repeat = false;
                for (int j = 0; j < N - 1; j++) {
                    if (cells[i][j] == 0 && cells[i][j + 1] != 0) {
                        cells[i][j] = cells[i][j + 1];
                        cells[i][j + 1] = 0;
                        repeat = true;
                    }
                }
            } while (repeat);

            // 2. collapse
            for (int j = 0; j < N - 1; j++) {
                if (cells[i][j] != 0 && cells[i][j] == cells[i][j + 1]) {   // [2284]
                    cells[i][j] += cells[i][j + 1];                         // [4284]
                    score += cells[i][j];
                    tvCells[i][j].startAnimation(collapseCellAnimation);

                    // 3. Переміщуємо на "злите" місце
                    for (int k = j + 1; k < N - 1; k++) {                   // [4844]
                        cells[i][k] = cells[i][k + 1];
                    }

                    // занулюємо останню                                    // [4840]
                    cells[i][N - 1] = 0;
                }
            }

        }
    }

    private boolean canMoveRight() {
        for (int i = 0; i < N; i++) {
            for (int j = 1; j < N; j++) {
                if (cells[i][j] == 0 && cells[i][j - 1] != 0 ||
                        cells[i][j] != 0 && cells[i][j] == cells[i][j - 1]) {
                    return true;
                }
            }
        }
        return false;
    }

    private void moveRight() {
        for (int i = 0; i < N; i++) {
            // 1. праворуч
            boolean repeat;
            do {
                repeat = false;
                for (int j = N - 1; j > 0; j--) {
                    if (cells[i][j] == 0 && cells[i][j - 1] != 0) {
                        cells[i][j] = cells[i][j - 1];
                        cells[i][j - 1] = 0;
                        repeat = true;
                    }
                }
            } while (repeat);

            // 2. коллапс
            for (int j = N - 1; j > 0; j--) {
                if (cells[i][j] != 0 && cells[i][j] == cells[i][j - 1]) {
                    cells[i][j] += cells[i][j - 1];
                    score += cells[i][j];
                    tvCells[i][j].startAnimation(collapseCellAnimation);

                    // 3. Переміщуємо на "злите" місце
                    for (int k = j - 1; k > 0; k--) {
                        cells[i][k] = cells[i][k - 1];
                    }

                    // занулюємо останню
                    cells[i][0] = 0;
                }
            }

        }
    }

    private boolean canMove(MoveDirection direction) {
        switch (direction) {
            case BOTTOM:
                return false;
            case LEFT:
                return canMoveLeft();
            case RIGHT:
                return canMoveRight();
            case TOP:
                return false;
        }
        return false;
    }

    private void move(MoveDirection direction) {
        switch (direction) {
            case BOTTOM:
                break;
            case LEFT:
                moveLeft();
                break;
            case RIGHT:
                moveRight();
                break;
            case TOP:
                break;
        }
    }

    private void processMove(MoveDirection direction) {
        if (canMove(direction)) {
            move(direction);
            spawnCell();
            showField();
        }
        else {
            Toast.makeText(GameActivity.this, "Ходу немає", Toast.LENGTH_SHORT).show();
        }
    }

    private enum MoveDirection {
        BOTTOM, LEFT, RIGHT, TOP
    }
}