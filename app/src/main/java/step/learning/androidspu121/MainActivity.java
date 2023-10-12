package step.learning.androidspu121;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private TextView tvHello;
    private Button btnHello;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // підключення розмітки - робота з UI тільки після цієї команди
        setContentView(R.layout.activity_main);

        // View - пробатько всіх UI елементів
        tvHello = findViewById(R.id.main_tv_hello);
        tvHello.setText(R.string.main_tv_hello_text);

        btnHello = findViewById(R.id.main_button_hello);
        btnHello.setOnClickListener(this::helloClick);
    }

    private void helloClick(View view) {    // Всі обробники подій повинні мати такий прототип, view - sender
        tvHello.setText(tvHello.getText() + "!");
        Intent calcIntent = new Intent(this.getApplicationContext(), CalcActivity.class);
        startActivity(calcIntent);
    }
}