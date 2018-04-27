package com.keyshon.ad_001;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity {
    // Инициализируем переменные
    AlarmManager alarmManager;
    private PendingIntent pending_intent;
    private AlarmReceiver alarm;
    private TimePicker alarmTimePicker;
    private TextView alarmTextView;
    MainActivity inst;
    Context context;
    String[] lisRes = {"Случайно", "Звук 1", "Звук 2", "Звук 3", "Звук 4", "Звук 5", "Звук 6", "Звук 7", "Звук 8", "Звук 9"};
    Integer pos = 0;

    // Действия по инициализации активити
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Находим элементы
        setContentView(R.layout.activity_main);
        alarmTextView = (TextView) findViewById(R.id.alarmText);
        alarmTimePicker = (TimePicker) findViewById(R.id.alarmTimePicker);
        Button start_alarm = (Button) findViewById(R.id.start_alarm);
        Button stop_alarm = (Button) findViewById(R.id.stop_alarm);
        Spinner spinner = (Spinner) findViewById(R.id.spinner);

        // Задаём контекст, спиннер
        this.context = this;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, lisRes);
        spinner.setAdapter(adapter);

        final Intent myIntent = new Intent(this.context, AlarmReceiver.class);

        // Создаём будильник
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        // Задаём будильниу выбранное время
        final Calendar calendar = Calendar.getInstance();

        // Листенер на кнопку "Запуска будильника"
        start_alarm.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                // Считываем значения в необходимом формате
                final int hour = alarmTimePicker.getCurrentHour();
                final int minute = alarmTimePicker.getCurrentMinute();;
                String s_hour = getCorrectString(hour);
                String s_minute = getCorrectString(minute);
                // Создаём задачу в календаре
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                // Передаём в наше Намерение
                myIntent.putExtra("extra", "yes");
                myIntent.putExtra("song", String.valueOf(pos));
                pending_intent = PendingIntent.getBroadcast(MainActivity.this, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pending_intent);
                // Выводим результат
                setAlarmText("Время будильника " + s_hour + ":" + s_minute);
            }
        });
        stop_alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Отправляем выключение будильника
                myIntent.putExtra("extra", "no");
                sendBroadcast(myIntent);
                alarmManager.cancel(pending_intent);
                // Обновляем текст
                setAlarmText("Будильник отменён");
            }
        });
        // Листенер на выпадающий список
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onNothingSelected(AdapterView<?> p0) {
                pos = 0;
            }

            @Override
            public void onItemSelected(AdapterView<?> p0, View p1, int p2, long p3) {
                pos = p2;
            }
        });
    }
    // Установить текст в поле
    public void setAlarmText(String alarmText) {
        alarmTextView.setText(alarmText);
    }
    // Приводим число к корректному строковому представлению
    private String getCorrectString(Integer num) {
        if (num < 10)
            return "0" + String.valueOf(num);
        else
            return String.valueOf(num);
    }
    @Override
    public void onStart() {
        super.onStart();
        inst = this;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
