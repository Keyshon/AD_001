package com.keyshon.ad_001;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Получаем Экстра из файла
        String state = intent.getExtras().getString("extra");
        // Передача музыкальному проигрывателю
        Intent serviceIntent = new Intent(context,RingtonePlayingService.class);
        serviceIntent.putExtra("extra", state);
        context.startService(serviceIntent);
    }
}