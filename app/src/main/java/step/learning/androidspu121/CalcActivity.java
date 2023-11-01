package step.learning.androidspu121;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

// https://github.com/pH-7/Simple-Java-Calculator/blob/master/src/simplejavacalculator/Calculator.java

public class CalcActivity extends AppCompatActivity {
    private enum CalcBiOperators {
        ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION
    }
    private enum CalcMonoOperators {
        SQUARE, SQUARE_ROOT, INVERSE
    }

    private TextView tvExpression;
    private TextView tvResult;

    private Map<Character, Character> viewCharsToNormalDictionary;
    private Map<Character, Character> normalCharsToViewDictionary;

    private double numLeft;     // Expression number
    private double numRight;    // Result number

    boolean biOperatorIsPlaced;
    CalcBiOperators currentBiOperator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc);

        viewCharsToNormalDictionary = new HashMap<>();
        viewCharsToNormalDictionary.put(getString(R.string.calc_btn_0).charAt(0), '0');
        viewCharsToNormalDictionary.put(getString(R.string.calc_btn_comma).charAt(0), '.');
        viewCharsToNormalDictionary.put(getString(R.string.calc_btn_minus).charAt(0), '-');
        viewCharsToNormalDictionary.put(getString(R.string.calc_btn_plus).charAt(0), '+');
        viewCharsToNormalDictionary.put(getString(R.string.calc_btn_divide).charAt(0), '/');
        viewCharsToNormalDictionary.put(getString(R.string.calc_btn_multiplication).charAt(0), '*');

        normalCharsToViewDictionary = new HashMap<>();
        for (Map.Entry<Character, Character> entry : viewCharsToNormalDictionary.entrySet()) {
            normalCharsToViewDictionary.put(entry.getValue(), entry.getKey());
        }

        biOperatorIsPlaced = false;

        numLeft = 0;
        numRight = 0;

        tvExpression = findViewById(R.id.calc_tv_expression);
        tvResult = findViewById(R.id.calc_tv_result);

        clearClick(null);

        findViewById(R.id.calc_btn_ce).setOnClickListener(this::clearEntryClick);
        findViewById(R.id.calc_btn_c).setOnClickListener(this::clearClick);
        findViewById(R.id.calc_btn_backspace).setOnClickListener(this::backspaceClick);
        findViewById(R.id.calc_btn_comma).setOnClickListener(this::commaClick);
        findViewById(R.id.calc_btn_plus_minus).setOnClickListener(this::plusMinusClick);
        findViewById(R.id.calc_btn_plus).setOnClickListener(this::plusClick);
        findViewById(R.id.calc_btn_minus).setOnClickListener(this::minusClick);
        findViewById(R.id.calc_btn_multiplication).setOnClickListener(this::multiplicationClick);
        findViewById(R.id.calc_btn_divide).setOnClickListener(this::divisionClick);
        findViewById(R.id.calc_btn_equal).setOnClickListener(this::equalsClick);
        findViewById(R.id.calc_btn_inverse).setOnClickListener(view -> calculateMonoOperation(view, CalcMonoOperators.INVERSE));
        findViewById(R.id.calc_btn_square).setOnClickListener(view -> calculateMonoOperation(view, CalcMonoOperators.SQUARE));
        findViewById(R.id.calc_btn_sqrt).setOnClickListener(view -> calculateMonoOperation(view, CalcMonoOperators.SQUARE_ROOT));


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



    //region Logic

    private void digitClick(View view) {
        String result = tvResult.getText().toString();
        if (result.equals( getString( R.string.calc_btn_0 ))) {
            result = ( (Button) view ).getText().toString();
        }
        else {
            result += ( (Button) view).getText();
        }
        numRight = Double.parseDouble( viewCharsConversion(result, true) );
        tvResult.setText( viewCharsConversion(result, false) );
    }

    private void backspaceClick(View view) {
        StringBuilder num = new StringBuilder(tvResult.getText());
        char minus = getString(R.string.calc_btn_minus).charAt(0);
        char zero = getString(R.string.calc_btn_0).charAt(0);
        char comma = getString(R.string.calc_btn_comma).charAt(0);

        if (num.length() == 1) {
            clearEntryClick(view);
            return;
        }
        else if (num.length() == 2 && num.charAt(0) == minus) {
            // -N
            clearEntryClick(view);
            return;
        }
        else if (num.length() == 3 && num.charAt(0) == minus && num.charAt(1) == zero && num.charAt(2) == comma) {
            // -0,
            clearEntryClick(view);
            return;
        }
        else {
            num.deleteCharAt(num.length() - 1);
        }

        numRight = Double.parseDouble( viewCharsConversion(num.toString(), true) );
        tvResult.setText(num.toString());
    }

    private void commaClick(View view) {
        StringBuilder num = new StringBuilder(tvResult.getText());
        char comma = getString(R.string.calc_btn_comma).charAt(0);

        for (int i = 0; i < num.length(); i++) {
            if (num.charAt(i) == comma) {
                return;
            }
        }

        num.append(comma);
        numRight = Double.parseDouble( viewCharsConversion(num.toString(), true) );
        tvResult.setText(num.toString());
    }

    private void clearClick(View view) {
        numLeft = 0;
        numRight = 0;
        biOperatorIsPlaced = false;
        tvExpression.setText("");
        tvResult.setText(R.string.calc_btn_0);
    }

    private void clearEntryClick(View view) {
        numRight = 0;
        tvResult.setText(R.string.calc_btn_0);
    }

    private void plusMinusClick(View view) {
        StringBuilder num = new StringBuilder(tvResult.getText());
        char minus = getString(R.string.calc_btn_minus).charAt(0);
        char zero = getString(R.string.calc_btn_0).charAt(0);
        char comma = getString(R.string.calc_btn_comma).charAt(0);

        if (num.charAt(0) == minus) {
            num.deleteCharAt(0);
        }
        else if (num.length() == 2 && num.charAt(0) == zero && num.charAt(1) == comma) {
            // 0,
            num.insert(0, minus);
        }
        else if ((num.length() >= 1 && num.charAt(0) != zero) || (num.length() >= 2)) {
            num.insert(0, minus);
        }

        numRight = Double.parseDouble( viewCharsConversion(num.toString(), true) );
        tvResult.setText(num.toString());
    }

    private void plusClick(View view) {
        operatorClick(view, CalcBiOperators.ADDITION);
    }

    private void minusClick(View view) {
        operatorClick(view, CalcBiOperators.SUBTRACTION);
    }

    private void divisionClick(View view) {
        operatorClick(view, CalcBiOperators.DIVISION);
    }

    private void multiplicationClick(View view) {
        operatorClick(view, CalcBiOperators.MULTIPLICATION);
    }

    private void equalsClick(View view) {
        if (!biOperatorIsPlaced) {
            return;
        }
        biOperatorIsPlaced = false;
        double result;

        tvExpression.setText(viewCharsConversion(
                tvExpression.getText().toString() + formatDouble(numRight)
                , false));

        numRight = Double.parseDouble(
                viewCharsConversion(
                        tvResult.getText().toString(),
                        true));

        result = calculateBiOperation(view, currentBiOperator);
        if (tvExpression.getText().equals("Error")) {
            numLeft = 0;
            numRight = 0;
            clearEntryClick(view);
            return;
        }

        numRight = result;

        tvResult.setText(viewCharsConversion( formatDouble(numRight), false) );
    }


    private void operatorClick(View view, CalcBiOperators operator) {
        String newExpression = null;

        if (!biOperatorIsPlaced) {
            numLeft = numRight;
            numRight = 0;
            clearEntryClick(view);
        }

        biOperatorIsPlaced = true;

        switch (operator) {
            case ADDITION:
                newExpression = formatDouble(numLeft) + " + ";
                currentBiOperator = CalcBiOperators.ADDITION;
                break;
            case SUBTRACTION:
                newExpression = formatDouble(numLeft) + " - ";
                currentBiOperator = CalcBiOperators.SUBTRACTION;
                break;
            case MULTIPLICATION:
                newExpression = formatDouble(numLeft) + " * ";
                currentBiOperator = CalcBiOperators.MULTIPLICATION;
                break;
            case DIVISION:
                newExpression = formatDouble(numLeft) + " / ";
                currentBiOperator = CalcBiOperators.DIVISION;
                break;
        }

        tvExpression.setText(viewCharsConversion(newExpression, false));
    }

    private void calculateMonoOperation(View view, CalcMonoOperators operator) {
        double tempNum;
        double originalNum;
        String expressionTemplate = null;

        try {
            tempNum = Double.parseDouble(
                    viewCharsConversion(
                            tvResult.getText().toString(),
                            true));
            originalNum = tempNum;

            switch (operator) {
                case SQUARE:
                    tempNum = Math.pow(tempNum, 2);
                    expressionTemplate = "N²";
                    break;
                case SQUARE_ROOT:
                    tempNum = Math.sqrt(tempNum);
                    expressionTemplate = "√N";
                    break;
                case INVERSE:
                    tempNum = 1 / tempNum;
                    expressionTemplate = getString(R.string.calc_btn_inverse) + "(N)";
                    break;
            }
        }
        catch (Exception ex) {
            clearEntryClick(view);
            return;
        }

        numRight = tempNum;
        numLeft = 0;

        tvResult.setText( viewCharsConversion(
                formatDouble(numRight), false ) );

        tvExpression.setText(
                viewCharsConversion(
                        expressionTemplate.replaceAll(
                                "N", String.valueOf(formatDouble(originalNum))
                        ), false)
        );
    }

    private double calculateBiOperation(View view, CalcBiOperators operator) {
        try {
            switch (operator) {
                case ADDITION:
                    return numLeft + numRight;
                case SUBTRACTION:
                    return numLeft - numRight;
                case MULTIPLICATION:
                    return numLeft * numRight;
                case DIVISION:
                    return numLeft / numRight;
            }
        }
        catch (Exception ex) {
            clearClick(view);
            tvExpression.setText("Error");
            return 0;
        }
        return 0;
    }

    //endregion



    //region Utility
    private String formatDouble(double number) {
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.0#");
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);

        if (number % 1 == 0) {
            return Integer.toString((int)number);
        }
        else {
            return decimalFormat.format(number);
        }
    }

    private String viewCharsConversion(String originalString, boolean viewCharsToNormal) {
        StringBuilder result = new StringBuilder();

        if (viewCharsToNormal) {
            for (char c : originalString.toCharArray()) {
                if (viewCharsToNormalDictionary.containsKey(c)) {
                    result.append(viewCharsToNormalDictionary.get(c));
                }
                else {
                    result.append(c);
                }
            }
        }
        else {
            for (char c : originalString.toCharArray()) {
                if (normalCharsToViewDictionary.containsKey(c)) {
                    result.append(normalCharsToViewDictionary.get(c));
                }
                else {
                    result.append(c);
                }
            }
        }

        return result.toString();
    }
    //endregion
}


