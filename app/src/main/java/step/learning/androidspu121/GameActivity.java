package step.learning.androidspu121;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class GameActivity extends AppCompatActivity {
    //region Variables
    private final Random random = new Random();
    private static final int N = 4;     // Розмір поля
    private AlertDialog activeDialog = null;
    private SharedPreferences sharedPref;
    SharedPreferences.Editor sharedPrefEditor;

    private Stack<int[][]> cellsStack;
    private TextView[][] tvCells = new TextView[N][N];

    private Stack<Integer> scoreHistory;
    private TextView tvScore;
    private int bestScore;
    private TextView tvBestScore;

    private Button btnUndo;
    private ImageButton btnMuteSound;
    private boolean soundIsMuted;

    private String gameOverMessage;

    private Animation spawnCellAnimation;
    private Animation collapseCellAnimation;
    private MediaPlayer spawnSound;
    private MediaPlayer mergeSound;
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
                R.raw.pop_strong
        );
        mergeSound = MediaPlayer.create(
                GameActivity.this,
                R.raw.pop_low
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

        soundIsMuted = false;
        btnMuteSound = findViewById(R.id.game_btn_muteSound);
        btnMuteSound.setOnClickListener(this::btnMuteSoundClick);

        btnUndo = findViewById(R.id.game_btn_undo);
        btnUndo.setOnClickListener(this::btnUndoClick);
        tvScore = findViewById(R.id.game_tv_score);
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


        findViewById(R.id.game_btn_new).setOnClickListener(this::btnNewGameClick);
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


    private void btnMuteSoundClick(View view) {
        if (soundIsMuted) {
            soundIsMuted = false;
            btnMuteSound.setImageResource(android.R.drawable.btn_star_big_off);
        }
        else {
            soundIsMuted = true;
            btnMuteSound.setImageResource(android.R.drawable.btn_star_big_on);
        }
    }


    private void newGame() {
        scoreHistory = new Stack<>();
        scoreHistory.add(0);
        tvScore.setText("0");

        cellsStack = new Stack<>();
        int[][] cells = new int[N][N];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                cells[i][j] = 0;
            }
        }
        spawnCell(cells);
        spawnCell(cells);

        cellsStack.push(cells);
        showField();
    }

    private void btnNewGameClick(View view) {
        gameOverMessage = null;
        gameOver();
        newGame();
    }


    /**
     * Поставити нове число у випадкову вільну комірку
     * Значення: 2 (з імовірністю 0.9) або 4
     */
    private void spawnCell(int[][] cells) {
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
        if (!soundIsMuted) {
            spawnSound.start();
        }
    }


    /**
     * Показ усіх комірок - відображення масиву чисел на ігрове поле
     */
    @SuppressLint("DiscouragedApi")
    private void showField() {
        // для кожної комірки визначаємо стиль у відповідності до
        // її значення та застосовуємо його
        Resources resources = getResources();
        int[][] cells = cellsStack.lastElement();

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
        tvScore.setText(getString(R.string.game_tv_score, scoreHistory.lastElement()));
        tvBestScore.setText(getString(R.string.game_tv_best, bestScore));
    }


    private void gameOver() {
        checkAndSetHighScore();

        // Dialog
        if (activeDialog != null && activeDialog.isShowing()) {
            return;
        }

        if (gameOverMessage != null) {
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
    }

    private boolean isGameOver() {
        if (!canMove(cellsStack.lastElement())) {
            gameOverMessage = "Кінець гри! Рухи неможливі!";
            return true;
        }

        for (int row = 0; row < N; row++) {
            for (int col = 0; col < N; col++) {
                if (cellsStack.lastElement()[row][col] == 2048) {
                    gameOverMessage = "Кінець гри! Виграш!";
                    return true;
                }
            }
        }

        return false;
    }


    private void checkAndSetHighScore() {
        int score = scoreHistory.lastElement();
        if (score > bestScore) {
            bestScore = score;

            sharedPrefEditor.putInt("highScore", bestScore);
            sharedPrefEditor.apply();

            tvBestScore.setText(getString(R.string.game_tv_best, bestScore));
        }
    }


    private void btnUndoClick(View view) {
        if (cellsStack.size() > 1) {
            cellsStack.pop();
        }
        if (scoreHistory.size() > 1) {
            scoreHistory.pop();
        }
        tvScore.setText( getString( R.string.game_tv_score, scoreHistory.lastElement() ) );
        showField();
    }


    //region Movement

    private void processMove(MoveDirection direction) {
        // Game Over
        if (isGameOver()) {
            gameOver();
        }

        int[][] cells;
        // int[][] cells = new int[N][N];
        // copyCellsArray(cellsStack.lastElement(), cells);

        if (canMove(direction)) {
            cells = move(direction);
            spawnCell(cells);
            cellsStack.push(cells);
            showField();
        }
        else {
            Toast.makeText(GameActivity.this, "Ходу немає", Toast.LENGTH_SHORT).show();
        }
    }

    private int[][] move(MoveDirection direction) {
        int[][] newCells = new int[N][N];
        switch (direction) {
            case BOTTOM:
                copyCellsArray( moveDown( cellsStack.lastElement() ), newCells );
                break;
            case LEFT:
                copyCellsArray( moveLeft( cellsStack.lastElement() ), newCells );
                break;
            case RIGHT:
                copyCellsArray( moveRight( cellsStack.lastElement() ), newCells );
                break;
            case TOP:
                copyCellsArray( moveUp( cellsStack.lastElement() ), newCells );
                break;
        }

        return newCells;
    }

    private boolean canMove(MoveDirection direction) {
        switch (direction) {
            case BOTTOM:
                return canMoveDown(cellsStack.lastElement());
            case LEFT:
                return canMoveLeft(cellsStack.lastElement());
            case RIGHT:
                return canMoveRight(cellsStack.lastElement());
            case TOP:
                return canMoveUp(cellsStack.lastElement());
        }
        return false;
    }

    private boolean canMove(int[][] cells) {
        if (!canMoveUp(cells) && !canMoveDown(cells) && !canMoveLeft(cells) && !canMoveRight(cells)) {
            return false;
        }
        return true;
    }


    private boolean canMoveLeft(int[][] cells) {
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

    private int[][] moveLeft(int[][] cells) {
        // 1. Переміщуємо всі значення ліворуч (без "пробілів")
        // 2. Перевіряємо коллапси (злиття), зливаємо
        // 3. Повторюємо п. 1 після злиття
        // 4. Повторюємо пункти для кожного рядка
        boolean isMerge = false;
        int tempScore = scoreHistory.lastElement();

        int[][] newCells = new int[N][N];
        copyCellsArray(cells, newCells);

        for (int i = 0; i < N; i++) {
            // 1. ліворуч
            boolean repeat;
            do {
                repeat = false;
                for (int j = 0; j < N - 1; j++) {
                    if (newCells[i][j] == 0 && newCells[i][j + 1] != 0) {
                        newCells[i][j] = newCells[i][j + 1];
                        newCells[i][j + 1] = 0;
                        repeat = true;
                    }
                }
            } while (repeat);

            // 2. collapse
            for (int j = 0; j < N - 1; j++) {
                if (newCells[i][j] != 0 && newCells[i][j] == newCells[i][j + 1]) {   // [2284]
                    newCells[i][j] += newCells[i][j + 1];                         // [4284]
                    tempScore += newCells[i][j];
                    tvCells[i][j].startAnimation(collapseCellAnimation);

                    // 3. Переміщуємо на "злите" місце
                    for (int k = j + 1; k < N - 1; k++) {                   // [4844]
                        newCells[i][k] = newCells[i][k + 1];
                    }

                    // занулюємо останню                                    // [4840]
                    newCells[i][N - 1] = 0;

                    isMerge = true;
                }
            }
        }

        if (isMerge && !soundIsMuted) {
            mergeSound.start();
        }

        scoreHistory.push(tempScore);
        return newCells;
    }


    private boolean canMoveRight(int[][] cells) {
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

    private int[][] moveRight(int[][] cells) {
        boolean isMerge = false;
        int tempScore = scoreHistory.lastElement();

        int[][] newCells = new int[N][N];
        copyCellsArray(cells, newCells);

        for (int i = 0; i < N; i++) {
            // 1. праворуч
            boolean repeat;
            do {
                repeat = false;
                for (int j = N - 1; j > 0; j--) {
                    if (newCells[i][j] == 0 && newCells[i][j - 1] != 0) {
                        newCells[i][j] = newCells[i][j - 1];
                        newCells[i][j - 1] = 0;
                        repeat = true;
                    }
                }
            } while (repeat);

            // 2. коллапс
            for (int j = N - 1; j > 0; j--) {
                if (newCells[i][j] != 0 && newCells[i][j] == newCells[i][j - 1]) {
                    newCells[i][j] += newCells[i][j - 1];
                    tempScore += newCells[i][j];
                    tvCells[i][j].startAnimation(collapseCellAnimation);

                    // 3. Переміщуємо на "злите" місце
                    for (int k = j - 1; k > 0; k--) {
                        newCells[i][k] = newCells[i][k - 1];
                    }

                    // занулюємо останню
                    newCells[i][0] = 0;

                    isMerge = true;
                }
            }
        }
        if (isMerge && !soundIsMuted) {
            mergeSound.start();
        }

        scoreHistory.push(tempScore);
        return newCells;
    }


    private boolean canMoveUp(int[][] cells) {
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

    private int[][] moveUp(int[][] cells) {
        boolean isMerge = false;
        int tempScore = scoreHistory.lastElement();

        int[][] newCells = new int[N][N];
        copyCellsArray(cells, newCells);

        for (int col = 0; col < N; col++) {
            // 1. Up
            boolean repeat;
            do {
                repeat = false;
                for (int row = 0; row < N - 1; row++) {
                    if (newCells[row][col] == 0 && newCells[row + 1][col] != 0) {
                        newCells[row][col] = newCells[row + 1][col];
                        newCells[row + 1][col] = 0;
                        repeat = true;
                    }
                }
            } while (repeat);

            // 2. Collapse
            for (int row = 0; row < N - 1; row++) {
                if (newCells[row][col] != 0 && newCells[row][col] == newCells[row + 1][col]) {
                    newCells[row][col] += newCells[row + 1][col];
                    tempScore += newCells[row][col];
                    tvCells[row][col].startAnimation(collapseCellAnimation);

                    // 3. Переміщуємо на "злите" місце
                    for (int k = row + 1; k < N - 1; k++) {
                        newCells[k][col] = newCells[k + 1][col];
                    }

                    // занулюємо останню
                    newCells[N - 1][col] = 0;

                    isMerge = true;
                }
            }
        }
        if (isMerge && !soundIsMuted) {
            mergeSound.start();
        }

        scoreHistory.push(tempScore);
        return newCells;
    }


    private boolean canMoveDown(int[][] cells) {
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

    private int[][] moveDown(int[][] cells) {
        boolean isMerge = false;
        int tempScore = scoreHistory.lastElement();

        int[][] newCells = new int[N][N];
        copyCellsArray(cells, newCells);

        for (int col = 0; col < N; col++) {
            // 1. Down
            boolean repeat;
            do {
                repeat = false;
                for (int row = N - 1; row > 0; row--) {
                    if (newCells[row][col] == 0 && newCells[row - 1][col] != 0) {
                        newCells[row][col] = newCells[row - 1][col];
                        newCells[row - 1][col] = 0;
                        repeat = true;
                    }
                }
            } while (repeat);

            // 2. Collapse
            for (int row = N - 1; row > 0; row--) {
                if (newCells[row][col] != 0 && newCells[row][col] == newCells[row - 1][col]) {
                    newCells[row][col] += newCells[row - 1][col];
                    tempScore += newCells[row][col];
                    tvCells[row][col].startAnimation(collapseCellAnimation);

                    // 3. Move to the "collapsed" position
                    for (int k = row - 1; k > 0; k--) {
                        newCells[k][col] = newCells[k - 1][col];
                    }

                    // Set the top cell to zero
                    newCells[0][col] = 0;

                    isMerge = true;
                }
            }
        }
        if (isMerge && !soundIsMuted) {
            mergeSound.start();
        }

        scoreHistory.push(tempScore);
        return newCells;
    }


    private enum MoveDirection {
        BOTTOM, LEFT, RIGHT, TOP
    }

    //endregion

    //region Utility
    private void copyCellsArray(int[][] source, int[][] destination) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                destination[i][j] = source[i][j];
            }
        }
    }
    //endregion
}