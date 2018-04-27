package com.keyshon.ad_001;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;


public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Получаем Экстра из файла
        String state = intent.getExtras().getString("extra");
        String song = intent.getExtras().getString("song");
        // Передача музыкальному проигрывателю
        //Bundle extras = new Bundle();
        Intent serviceIntent = new Intent(context, RingtonePlayingService.class);
        serviceIntent.putExtra("extra", state);
        serviceIntent.putExtra("song", song);
        context.startService(serviceIntent);
    }
}