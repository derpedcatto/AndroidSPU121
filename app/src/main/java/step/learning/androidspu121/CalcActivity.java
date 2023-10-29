package step.learning.androidspu121;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

// https://github.com/pH-7/Simple-Java-Calculator/blob/master/src/simplejavacalculator/Calculator.java

public class CalcActivity extends AppCompatActivity {
    //region Variables
    private TextView tvExpression;
    private TextView tvResult;

    private double num1;
    private double num2;
    //endregion


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc);

        num1 = 0;
        num2 = 0;

        tvExpression = findViewById(R.id.calc_tv_expression);
        tvResult = findViewById(R.id.calc_tv_result);


        // clearClick(null);
        // findViewById(R.id.calc_btn_backspace).setOnClickListener(this::backspaceClick);

        // Пройти циклом по ідентифікаторах calc_btn_[i], всім вказати один обробник
        // for (int i = 0; i < 10; i++) {
        //     findViewById(
        //             getResources()
        //                 .getIdentifier( // R.
        //                     "calc_btn_" + i,
        //                     "id",    // R.id.calc_btn_[i]
        //                     getPackageName()
        //             )
        //     ).setOnClickListener(this::digitClick);
        // }
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









    //region Utility
    private String formatDouble(double number) {
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.0#");
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
        return decimalFormat.format(number);
    }
    //endregion
}


