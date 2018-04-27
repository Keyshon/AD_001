package com.keyshon.ad_001;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import java.util.Random;


public class RingtonePlayingService extends Service {
    // Инициализируем переменные
    private boolean isRunning;
    private Context context;
    MediaPlayer mMediaPlayer;
    private int startId;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int onStartCommand(Intent intent, int flags, int id)
    {
        // ID - состояние будильника
        int startId = id;
        // Менеджер оповещаний
        final NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Намерение с MainActivity
        Intent intent1 = new Intent(this.getApplicationContext(), MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent1, 0);
        // Вызываем оповещение
        Notification mNotify  = new Notification.Builder(this)
                .setContentTitle("Пора вставать, засоня" + "!")
                .setContentText("Нажмите, чтобы открыть Sleep Well")
                .setSmallIcon(R.drawable.ban)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .build();
        // Получаем команду от Мэйн Активити
        String state = intent.getExtras().getString("extra");
        // Получаем состояние будильника
        assert state != null;
        switch (state) {
            case "no":
                startId = 0;
                break;
            case "yes":
                startId = 1;
                break;
            default:
                startId = 0;
                break;
        }
        // Состояние:
        if (!this.isRunning && startId == 1) {      // Будильник ещё не запущен, а пришла команда запуска
            // Выбран рандом
            String song = intent.getExtras().getString("song");
            if (song == "0") {
                Integer min = 1;
                Integer max = 9;
                Random r = new Random();
                song = "" + (r.nextInt(max - min + 1) + min);
            }
            switch (song) {
                case "1":
                    mMediaPlayer = MediaPlayer.create(this, R.raw.song1);
                    break;
                case "2":
                    mMediaPlayer = MediaPlayer.create(this, R.raw.song2);
                    break;
                case "3":
                    mMediaPlayer = MediaPlayer.create(this, R.raw.song3);
                    break;
                case "4":
                    mMediaPlayer = MediaPlayer.create(this, R.raw.song4);
                    break;
                case "5":
                    mMediaPlayer = MediaPlayer.create(this, R.raw.song5);
                    break;
                case "6":
                    mMediaPlayer = MediaPlayer.create(this, R.raw.song6);
                    break;
                case "7":
                    mMediaPlayer = MediaPlayer.create(this, R.raw.song7);
                    break;
                case "8":
                    mMediaPlayer = MediaPlayer.create(this, R.raw.song8);
                    break;
                case "9":
                    mMediaPlayer = MediaPlayer.create(this, R.raw.song9);
                    break;
            }
            // Запуск проигрывания
            mMediaPlayer.start();
            // Вывод оповещения
            mNM.notify(0, mNotify);
            // Обновление параметров будильника
            this.isRunning = true;
            this.startId = 0;
        }
        else if (!this.isRunning && startId == 0) { // Будильник не запущен и пришла команда остановки
            // Обновление параметров будильника
            this.isRunning = false;
            this.startId = 0;
        }
        else if (this.isRunning && startId == 1) {  // Будильник запущен и пришла команда запуска
            // Обновление параметров будильника
            this.isRunning = true;
            this.startId = 0;
        }
        else {                                      // Будильник запущен и пришла команда остановки
            // Останавливаем проигрывание
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            // Обновление параметров будильника
            this.isRunning = false;
            this.startId = 0;
        }
        return Service.START_NOT_STICKY;
    }
    // Если приложение закрывается - проигрывание заканчивается
    @Override
    public void onDestroy() {
        super.onDestroy();
        this.isRunning = false;
    }
}
