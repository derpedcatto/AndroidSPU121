package step.learning.androidspu121;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {
    //region Variables
    private final Random random = new Random();
    private static final int N = 4;     // Розмір поля
    private AlertDialog activeDialog = null;
    private SharedPreferences sharedPref;
    SharedPreferences.Editor sharedPrefEditor;

    private int[][] cells = new int[N][N];
    private List<int[][]> cellsHistory;
    private TextView[][] tvCells = new TextView[N][N];

    private List<Integer> scoreHistory;
    private int score;
    private TextView tvScore;
    private int bestScore;
    private TextView tvBestScore;

    private Button btnUndo;

    private String gameOverMessage;

    private Animation spawnCellAnimation;
    private Animation collapseCellAnimation;
    private MediaPlayer spawnSound;
    //endregion

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_game );
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        sharedPref = getApplicationContext().getSharedPreferences("HighScorePref", MODE_PRIVATE);
        sharedPrefEditor = sharedPref.edit();

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


        btnUndo = findViewById(R.id.game_btn_undo);
        btnUndo.setOnClickListener(this::btnUndoClick);
        tvScore = findViewById(R.id.game_tv_score);
        tvScore.setOnClickListener(v -> {moveLeft();spawnCell();showField();});
        tvBestScore = findViewById(R.id.game_tv_best);

        if (sharedPref.contains("highScore")) {
            bestScore = sharedPref.getInt("highScore", -1);
        }
        else {
            bestScore = 0;
        }

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


        findViewById(R.id.game_btn_swipeUp).setOnClickListener(view -> processMove(MoveDirection.TOP));
        findViewById(R.id.game_btn_swipeDown).setOnClickListener(view -> processMove(MoveDirection.BOTTOM));
        findViewById(R.id.game_btn_swipeLeft).setOnClickListener(view -> processMove(MoveDirection.LEFT));
        findViewById(R.id.game_btn_swipeRight).setOnClickListener(view -> processMove(MoveDirection.RIGHT));

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
                    }
                    @Override public void onSwipeTop() {
                        processMove(MoveDirection.TOP);
                    }
                } ) ;

        newGame();
    }


    private void newGame() {
        score = 0;
        tvScore.setText("0");

        cellsHistory = new ArrayList<>();

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                cells[i][j] = 0;
            }
        }
        spawnCell();
        spawnCell();

        cellsHistory.add(cells);
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


    private void gameOver() {
        checkAndSetHighScore();

        // Dialog
        if (activeDialog != null && activeDialog.isShowing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(gameOverMessage)
                .setPositiveButton("Заново", (dialog, id) -> {
                    newGame();
                    dialog.dismiss();
                })
                .setNegativeButton("Вийти", (dialog, id) -> {
                    finish();
                });

        builder.setCancelable(false);
        activeDialog = builder.create();
        activeDialog.show();
    }

    private boolean isGameOver() {
        if (!canMove()) {
            gameOverMessage = "Кінець гри! Рухи неможливі!";
            return true;
        }

        for (int row = 0; row < N; row++) {
            for (int col = 0; col < N; col++) {
                if (cells[row][col] == 2048) {
                    gameOverMessage = "Кінець гри! Виграш!";
                    return true;
                }
            }
        }

        return false;
    }


    private void checkAndSetHighScore() {
        if (score > bestScore) {
            bestScore = score;

            sharedPrefEditor.putInt("highScore", bestScore);
            sharedPrefEditor.apply();

            tvBestScore.setText(getString(R.string.game_tv_best, bestScore));
        }
    }


    private void btnUndoClick(View view) {
        showField();
    }

    //region Movement
    private void processMove(MoveDirection direction) {
        // Game Over
        if (isGameOver()) {
            gameOver();
        }

        if (canMove(direction)) {
            move(direction);
            spawnCell();
            showField();
        }
        else {
            Toast.makeText(GameActivity.this, "Ходу немає", Toast.LENGTH_SHORT).show();
        }
    }

    private void move(MoveDirection direction) {
        switch (direction) {
            case BOTTOM:
                moveDown();
                break;
            case LEFT:
                moveLeft();
                break;
            case RIGHT:
                moveRight();
                break;
            case TOP:
                moveUp();
                break;
        }
    }

    private boolean canMove(MoveDirection direction) {
        switch (direction) {
            case BOTTOM:
                return canMoveDown();
            case LEFT:
                return canMoveLeft();
            case RIGHT:
                return canMoveRight();
            case TOP:
                return canMoveUp();
        }
        return false;
    }

    private boolean canMove() {
        if (!canMoveUp() && !canMoveDown() && !canMoveLeft() && !canMoveRight()) {
            return false;
        }
        return true;
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


    private boolean canMoveUp() {
        for (int col = 0; col < N; col++) {
            for (int row = 0; row < N - 1; row++) {
                if ((cells[row][col] == 0 && cells[row + 1][col] != 0) ||
                    (cells[row][col] != 0 && cells[row][col] == cells[row + 1][col])) {
                    return true;
                }
            }
        }
        return false;
    }

    private void moveUp() {
        for (int col = 0; col < N; col++) {
            // 1. Up
            boolean repeat;
            do {
                repeat = false;
                for (int row = 0; row < N - 1; row++) {
                    if (cells[row][col] == 0 && cells[row + 1][col] != 0) {
                        cells[row][col] = cells[row + 1][col];
                        cells[row + 1][col] = 0;
                        repeat = true;
                    }
                }
            } while (repeat);

            // 2. Collapse
            for (int row = 0; row < N - 1; row++) {
                if (cells[row][col] != 0 && cells[row][col] == cells[row + 1][col]) {
                    cells[row][col] += cells[row + 1][col];
                    score += cells[row][col];
                    tvCells[row][col].startAnimation(collapseCellAnimation);

                    // 3. Переміщуємо на "злите" місце
                    for (int k = row + 1; k < N - 1; k++) {
                        cells[k][col] = cells[k + 1][col];
                    }

                    // занулюємо останню
                    cells[N - 1][col] = 0;
                }
            }
        }
    }


    private boolean canMoveDown() {
        for (int col = 0; col < N; col++) {
            for (int row = N - 1; row > 0; row--) {
                if ((cells[row][col] == 0 && cells[row - 1][col] != 0) ||
                    (cells[row][col] != 0 && cells[row][col] == cells[row - 1][col])) {
                    return true;
                }
            }
        }
        return false;
    }

    private void moveDown() {
        for (int col = 0; col < N; col++) {
            // 1. Down
            boolean repeat;
            do {
                repeat = false;
                for (int row = N - 1; row > 0; row--) {
                    if (cells[row][col] == 0 && cells[row - 1][col] != 0) {
                        cells[row][col] = cells[row - 1][col];
                        cells[row - 1][col] = 0;
                        repeat = true;
                    }
                }
            } while (repeat);

            // 2. Collapse
            for (int row = N - 1; row > 0; row--) {
                if (cells[row][col] != 0 && cells[row][col] == cells[row - 1][col]) {
                    cells[row][col] += cells[row - 1][col];
                    score += cells[row][col];
                    tvCells[row][col].startAnimation(collapseCellAnimation);

                    // 3. Move to the "collapsed" position
                    for (int k = row - 1; k > 0; k--) {
                        cells[k][col] = cells[k - 1][col];
                    }

                    // Set the top cell to zero
                    cells[0][col] = 0;
                }
            }
        }
    }


    private enum MoveDirection {
        BOTTOM, LEFT, RIGHT, TOP
    }
    //endregion
}