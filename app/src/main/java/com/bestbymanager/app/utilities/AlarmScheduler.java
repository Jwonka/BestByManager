package com.bestbymanager.app.utilities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.time.LocalDate;
import java.time.ZoneId;

public class AlarmScheduler {
    private static final String EXTRA_PRODUCT_ID = "productID";
    private static final String EXTRA_MESSAGE    = "message";

    // Distinguish alarm types (so PendingIntents can never collide)
    private static final String ACTION_EXPIRY = "com.bestbymanager.app.ALARM_EXPIRY";
    private static final String ACTION_EARLY  = "com.bestbymanager.app.ALARM_EARLY_WARNING";

    // Keep requestCodes collision-free even if actions get reused
    private static final int EARLY_WARNING_OFFSET = 1_000_000;
    public static void scheduleAlarm(Context context, LocalDate date, long productID, String message) {
        long alarmStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmReceiver.class)
                .setAction(ACTION_EXPIRY)
                .putExtra(EXTRA_PRODUCT_ID, productID)
                .putExtra(EXTRA_MESSAGE, message);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                Math.toIntExact(productID),
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );
        am.set(AlarmManager.RTC_WAKEUP, alarmStart, pi);
    }

    public static void cancelAlarm(Context context, long productID) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmReceiver.class)
                .setAction(ACTION_EXPIRY)
                .putExtra(EXTRA_PRODUCT_ID, productID);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                Math.toIntExact(productID),
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );
        am.cancel(pi);
        pi.cancel();
    }

    public static void scheduleEarlyWarning(Context context, LocalDate date, long productID, String message) {
        long alarmStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmReceiver.class)
                .setAction(ACTION_EARLY)
                .putExtra(EXTRA_PRODUCT_ID, productID)
                .putExtra(EXTRA_MESSAGE, message);

        int reqCode = Math.toIntExact(productID) + EARLY_WARNING_OFFSET;

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );
        am.set(AlarmManager.RTC_WAKEUP, alarmStart, pi);
    }

    public static void cancelEarlyWarning(Context context, long productID) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmReceiver.class)
                .setAction(ACTION_EARLY)
                .putExtra(EXTRA_PRODUCT_ID, productID);

        int reqCode = Math.toIntExact(productID) + EARLY_WARNING_OFFSET;

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );
        am.cancel(pi);
        pi.cancel();
    }
}