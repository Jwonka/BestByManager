package com.example.bestbymanager.utilities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.time.LocalDate;
import java.time.ZoneId;

public class AlarmScheduler {
    public static void scheduleAlarm(Context context, LocalDate date, long productID, String message) {
        long alarmStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmReceiver.class).putExtra("productID", productID).putExtra("message", message);
        PendingIntent pi = PendingIntent.getBroadcast(context, Math.toIntExact(productID), intent, PendingIntent.FLAG_IMMUTABLE);
        am.set(AlarmManager.RTC_WAKEUP, alarmStart, pi);
    }

    public static void cancelAlarm(Context context, long productID) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, Math.toIntExact(productID), intent, PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
        pi.cancel();
    }
}