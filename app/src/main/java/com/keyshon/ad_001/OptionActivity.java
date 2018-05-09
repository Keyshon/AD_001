package com.keyshon.ad_001;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import org.w3c.dom.Text;

public class OptionActivity extends Activity {
    Integer delayPos = 0;
    Integer lowerLimitPos = 0;
    Integer upperLimitPos = 0;
    Integer lowerPhasePos = 0;
    Integer upperPhasePos = 0;

    String[] delayRes = {"10 мин.", "15 мин.", "20 мин.", "25 мин.", "30 мин."};
    String[] lowerLimitRes = {"5 мин.", "10 мин.", "15 мин.", "20 мин."};
    String[] upperLimitRes = {"5 мин.", "10 мин.", "15 мин.", "20 мин."};
    String[] lowerPhaseRes = {"55 мин.", "60 мин.", "65 мин.", "70 мин.", "75 мин."};
    String[] upperPhaseRes = {"80 мин.", "85 мин.", "90 мин.", "95 мин.", "100 мин."};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_option);
        Button optionMain = (Button) findViewById(R.id.optionMain);
        final Intent dataIntent = getIntent();

        delayPos = getPosition(Long.valueOf(dataIntent.getStringExtra("delayValue")).longValue(), "delayValue");
        lowerLimitPos = getPosition(Long.valueOf(dataIntent.getStringExtra("lowerLimitValue")).longValue(), "lowerLimitValue");
        upperLimitPos = getPosition(Long.valueOf(dataIntent.getStringExtra("upperLimitValue")).longValue(), "upperLimitValue");
        lowerPhasePos = getPosition(Long.valueOf(dataIntent.getStringExtra("lowerPhaseValue")).longValue(), "lowerPhaseValue");
        upperPhasePos = getPosition(Long.valueOf(dataIntent.getStringExtra("upperPhaseValue")).longValue(), "upperPhaseValue");

        Spinner delayValue = (Spinner) findViewById(R.id.delayValue);
        ArrayAdapter<String> delayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, delayRes);
        delayValue.setAdapter(delayAdapter);
        delayValue.setSelection(delayPos);

        Spinner lowerLimitValue = (Spinner) findViewById(R.id.lowerLimitValue);
        ArrayAdapter<String> lowerLimitAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, lowerLimitRes);
        lowerLimitValue.setAdapter(lowerLimitAdapter);
        lowerLimitValue.setSelection(lowerLimitPos);

        Spinner upperLimitValue = (Spinner) findViewById(R.id.upperLimitValue);
        ArrayAdapter<String> upperLimitAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, upperLimitRes);
        upperLimitValue.setAdapter(upperLimitAdapter);
        upperLimitValue.setSelection(upperLimitPos);

        Spinner lowerPhaseValue = (Spinner) findViewById(R.id.lowerPhaseValue);
        ArrayAdapter<String> lowerPhaseAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, lowerPhaseRes);
        lowerPhaseValue.setAdapter(lowerPhaseAdapter);
        lowerPhaseValue.setSelection(lowerPhasePos);

        Spinner upperPhaseValue = (Spinner) findViewById(R.id.upperPhaseValue);
        ArrayAdapter<String> upperPhaseAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, upperPhaseRes);
        upperPhaseValue.setAdapter(upperPhaseAdapter);
        upperPhaseValue.setSelection(upperPhasePos);

        optionMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("delayValue", String.valueOf(getValue(delayPos, "delayValue")));
                intent.putExtra("lowerLimitValue", String.valueOf(getValue(lowerLimitPos, "lowerLimitValue")));
                intent.putExtra("upperLimitValue", String.valueOf(getValue(upperLimitPos, "upperLimitValue")));
                intent.putExtra("lowerPhaseValue", String.valueOf(getValue(lowerPhasePos, "lowerPhaseValue")));
                intent.putExtra("upperPhaseValue", String.valueOf(getValue(upperPhasePos, "upperPhaseValue")));
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        // Листенер на выпадающий список
        delayValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onNothingSelected(AdapterView<?> p0) {
                delayPos = getPosition(Long.valueOf(dataIntent.getStringExtra("delayValue")).longValue(), "delayValue");
            }

            @Override
            public void onItemSelected(AdapterView<?> p0, View p1, int p2, long p3) {
                delayPos = p2;
            }
        });

        // Листенер на выпадающий список
        lowerLimitValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onNothingSelected(AdapterView<?> p0) {
                lowerLimitPos = getPosition(Long.valueOf(dataIntent.getStringExtra("lowerLimitValue")).longValue(), "lowerLimitValue");
            }

            @Override
            public void onItemSelected(AdapterView<?> p0, View p1, int p2, long p3) {
                lowerLimitPos = p2;
            }
        });

        // Листенер на выпадающий список
        upperLimitValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onNothingSelected(AdapterView<?> p0) {
                upperLimitPos = getPosition(Long.valueOf(dataIntent.getStringExtra("upperLimitValue")).longValue(), "upperLimitValue");
            }

            @Override
            public void onItemSelected(AdapterView<?> p0, View p1, int p2, long p3) {
                upperLimitPos = p2;
            }
        });

        // Листенер на выпадающий список
        lowerPhaseValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onNothingSelected(AdapterView<?> p0) {
                lowerPhasePos = getPosition(Long.valueOf(dataIntent.getStringExtra("lowerPhaseValue")).longValue(), "lowerPhaseValue");
            }

            @Override
            public void onItemSelected(AdapterView<?> p0, View p1, int p2, long p3) {
                lowerPhasePos = p2;
            }
        });

        // Листенер на выпадающий список
        upperPhaseValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onNothingSelected(AdapterView<?> p0) {
                upperPhasePos = getPosition(Long.valueOf(dataIntent.getStringExtra("upperPhaseValue")).longValue(), "upperPhaseValue");
            }

            @Override
            public void onItemSelected(AdapterView<?> p0, View p1, int p2, long p3) {
                upperPhasePos = p2;
            }
        });
    }


    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("delayValue", String.valueOf(getValue(delayPos, "delayValue")));
        intent.putExtra("lowerLimitValue", String.valueOf(getValue(lowerLimitPos, "lowerLimitValue")));
        intent.putExtra("upperLimitValue", String.valueOf(getValue(upperLimitPos, "upperLimitValue")));
        intent.putExtra("lowerPhaseValue", String.valueOf(getValue(lowerPhasePos, "lowerPhaseValue")));
        intent.putExtra("upperPhaseValue", String.valueOf(getValue(upperPhasePos, "upperPhaseValue")));
        setResult(RESULT_OK, intent);
        finish();
    }

    private int getPosition(long VALUE, String fieldName) {
        if (fieldName.equals("delayValue")) {
            if (VALUE == 600000L)
                return 0;
            else if (VALUE == 900000L)
                return 1;
            else if (VALUE == 1200000L)
                return 2;
            else if (VALUE == 1500000L)
                return 3;
            else if (VALUE == 1800000L)
                return 4;
        }
        else if (fieldName.equals("lowerLimitValue") || fieldName.equals("upperLimitValue")) {
            if (VALUE == 300000L)
                return 0;
            else if (VALUE == 600000L)
                return 1;
            else if (VALUE == 900000L)
                return 2;
            else if (VALUE == 1200000L)
                return 3;
        }
        else if (fieldName.equals("lowerPhaseValue")) {
            if (VALUE == 3300000L)
                return 0;
            else if (VALUE == 3600000L)
                return 1;
            else if (VALUE == 3900000L)
                return 2;
            else if (VALUE == 4200000L)
                return 3;
            else if (VALUE == 4500000L)
                return 4;
        }
        else if (fieldName.equals("upperPhaseValue")) {
            if (VALUE == 4800000L)
                return 0;
            else if (VALUE == 5100000L)
                return 1;
            else if (VALUE == 5400000L)
                return 2;
            else if (VALUE == 5700000L)
                return 3;
            else if (VALUE == 6000000L)
                return 4;
        }
        return 0;
    }

    private long getValue(Integer POSITION, String fieldName) {
        if (fieldName.equals("delayValue")) {
            if (POSITION == 0)
                return 600000L;
            else if (POSITION == 1)
                return 900000L;
            else if (POSITION == 2)
                return 1200000L;
            else if (POSITION == 3)
                return 1500000L;
            else if (POSITION == 4)
                return 1800000L;
        }
        else if (fieldName.equals("lowerLimitValue") || fieldName.equals("upperLimitValue")) {
            if (POSITION == 0)
                return 300000L;
            else if (POSITION == 1)
                return 600000L;
            else if (POSITION == 2)
                return 900000L;
            else if (POSITION == 3)
                return 1200000L;
        }
        else if (fieldName.equals("lowerPhaseValue")) {
            if (POSITION == 0)
                return 3300000L;
            else if (POSITION == 1)
                return 3600000L;
            else if (POSITION == 2)
                return 3900000L;
            else if (POSITION == 3)
                return 4200000L;
            else if (POSITION == 4)
                return 4500000L;
        }
        else if (fieldName.equals("upperPhaseValue")) {
            if (POSITION == 0)
                return 4800000L;
            else if (POSITION == 1)
                return 5100000L;
            else if (POSITION == 2)
                return 5400000L;
            else if (POSITION == 3)
                return 5700000L;
            else if (POSITION == 4)
                return 6000000L;
        }
        return 0L;
    }
}
