package com.keyshon.ad_001;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class StatisticActivity extends Activity {
    private TextView qualityValue;
    private TextView fallValue;
    private TextView sleepValue;
    private TextView phaseValue;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistic);
        qualityValue = (TextView) findViewById(R.id.qualityValue);
        fallValue = (TextView) findViewById(R.id.fallValue);
        sleepValue = (TextView) findViewById(R.id.sleepValue);
        phaseValue = (TextView) findViewById(R.id.phaseValue);
        Button statisticMain = (Button) findViewById(R.id.statisticMain);
        Intent intent = getIntent();
        qualityValue.setText(getCorrectText(intent.getStringExtra("qualityValue"), "qualityValue"));
        fallValue.setText(getCorrectText(intent.getStringExtra("fallValue"), "fallValue"));
        phaseValue.setText(getCorrectText(intent.getStringExtra("phaseValue"), "phaseValue"));

        statisticMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private String getCorrectText(String VALUE, String fieldName) {
        String res = "";
        if (fieldName.equals("qualityValue")) {
            return VALUE + " %";
        }
        else if (fieldName.equals("sleepValue")) {
            return getFormattedText(VALUE) + " в день";
        }
        else {
            return getFormattedText(VALUE);
        }
    }

    private String getFormattedText(String VALUE) {
        int minutes = (int)(Long.valueOf(VALUE).longValue() / 60000);
        int hours = 0;
        while (minutes > 59) {
            minutes -= 60;
            hours++;
        }
        String s_hours = "";
        if (hours == 0)
            s_hours = "";
        else if (hours % 10 == 1 && hours != 11)
            s_hours = String.valueOf(hours) + " час";
        else if (hours % 10 < 5 && (hours / 10) != 1)
            s_hours = String.valueOf(hours) + " часа";
        else
            s_hours = String.valueOf(hours) + " часов";
        String s_minutes = "";
        if (minutes == 0)
            s_minutes = "";
        else if (minutes % 10 == 1 && minutes != 11)
            s_minutes = String.valueOf(minutes) + " минуту";
        else if (minutes % 10 < 5 && minutes % 10 > 1 && (minutes / 10) != 1)
            s_minutes = String.valueOf(minutes) + " минуты";
        else
            s_minutes = String.valueOf(minutes) + " минут";
        return s_hours + " " + s_minutes;
    }
}
