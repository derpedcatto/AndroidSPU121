package step.learning.androidspu121;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class CalcActivity extends AppCompatActivity {
    private TextView tvExpression;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc);

        tvExpression = findViewById(R.id.calc_tv_expression);
        tvResult = findViewById(R.id.calc_tv_result);
        clearClick(null);

        findViewById(R.id.calc_btn_backspace).setOnClickListener(this::backspaceClick);
        findViewById(R.id.calc_btn_c).setOnClickListener(this::clearClick);
        findViewById(R.id.calc_btn_ce).setOnClickListener(this::clearEntryClick);
        findViewById(R.id.calc_btn_plus_minus).setOnClickListener(this::plusMinusClick);
        findViewById(R.id.calc_btn_comma).setOnClickListener(this::commaClick);

        // Пройти циклом по ідентифікаторах calc_btn_[i], всім вказати один обробник
        for (int i = 0; i < 10; i++) {
            findViewById(
                    getResources()
                        .getIdentifier( // R.
                            "calc_btn_" + i,
                            "id",    // R.id.calc_btn_[i]
                            getPackageName()
                    )
            ).setOnClickListener(this::digitClick);
        }
    }

    /**
     * Click on '0' - '9'
     */
    private void digitClick(View view) {
        String result = tvResult.getText().toString();
        if (result.equals( getString( R.string.calc_btn_0 ))) {
            result = ( (Button) view ).getText().toString();
        }
        else {
            result += ( (Button) view).getText();
        }
        tvResult.setText(result);
    }

    /**
     * Click on 'C'
     */
    private void clearClick(View view) {
        tvExpression.setText("");
        tvResult.setText(R.string.calc_btn_0);
    }

    /**
     * Click on 'CE'
     */
    private void clearEntryClick(View view) {
        tvResult.setText(R.string.calc_btn_0);
    }

    /**
     * Click on Backspace
     */
    private void backspaceClick(View view) {
        CharSequence result = tvResult.getText();

        if (result.length() == 2 && result.charAt(0) == '-') {
            result = new StringBuilder(getString(R.string.calc_btn_0));
        }
        else if (result.length() > 1) {
            result = result.subSequence(0, result.length() - 1);
        }
        else {
            result = new StringBuilder(getString(R.string.calc_btn_0));
        }

        tvResult.setText(result);
    }

    /**
     * Click on ','
     */
    private void commaClick(View view) {
        boolean commaAbsent = true;
        CharSequence text = tvResult.getText();
        char commaChar = getString( R.string.calc_btn_comma ).charAt(0);

        for (int i = 0; i < tvResult.length(); i++) {
            if (text.charAt(i) == commaChar) {
                commaAbsent = false;
                break;
            }
        }

        if (commaAbsent) {
            tvResult.setText(new StringBuilder().append(text).append(commaChar));
        }
    }

    /**
     * Click on '+/-'
     */
    private void plusMinusClick(View view) {
        if (tvResult.getText().charAt(0) == getString(R.string.calc_btn_0).charAt(0)) {
            return;
        }

        CharSequence result = tvResult.getText();

        if (result.charAt(0) == '-') {
            result = new StringBuilder(result).deleteCharAt(0);
        }
        else {
            result = new StringBuilder().append('-').append(result);
        }

        tvResult.setText(result);
    }





    /*
    Зміна конфігурації
    "+" автоматично визначається потрібний ресурс
    "-" активність перестворюється і втрачаються напрацьовані дані
    Для того щоб мати можливість збереження/відновлення цих даних
    задаються наступні обробники:
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence("expression", tvExpression.getText());
        outState.putCharSequence("result", tvResult.getText());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        tvExpression.setText(savedInstanceState.getCharSequence("expression"));
        tvResult.setText(savedInstanceState.getCharSequence("result"));
    }
}