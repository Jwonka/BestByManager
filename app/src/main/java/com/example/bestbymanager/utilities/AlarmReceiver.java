package com.example.bestbymanager.utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        long productID = intent.getLongExtra("productID", -1L);
        String message    = intent.getStringExtra("message");
        NotificationHelper.postExpirationNotification(context, productID, "Best By Manager", message);
    }
}
