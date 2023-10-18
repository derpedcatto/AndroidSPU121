package step.learning.androidspu121;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    // private TextView tvHeading;
    private Button btnCalc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // підключення розмітки - робота з UI тільки після цієї команди
        setContentView(R.layout.activity_main);

        // View - пробатько всіх UI елементів
        // tvHeading = findViewById(R.id.main_tv_hello);
        // tvHeading.setText(R.string.main_tv_heading);

        btnCalc = findViewById(R.id.main_button_calc);
        btnCalc.setOnClickListener(this::calcClick);

        findViewById(R.id.main_button_game).setOnClickListener(this::startGame);
    }

    private void calcClick(View view) {    // Всі обробники подій повинні мати такий прототип, view - sender
        // tvHeading.setText(tvHeading.getText() + "!");
        startActivity(new Intent(this.getApplicationContext(), CalcActivity.class));
    }

    private void startGame(View view) {
        startActivity(new Intent(this.getApplicationContext(), GameActivity.class));
    }
}