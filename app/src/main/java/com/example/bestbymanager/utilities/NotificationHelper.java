package com.example.bestbymanager.utilities;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.example.bestbymanager.R;
import com.example.bestbymanager.UI.activities.ProductDetails;

public class NotificationHelper {
    public static final String CHANNEL_ID = "best_by_expiration";

    public static void ensureChannelExists(Context context) {
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Product expiration alerts", NotificationManager.IMPORTANCE_DEFAULT);

        ch.setDescription("Notifies you when a product is near or past its best-by date");

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
    }

    public static void postExpirationNotification(Context context, long productID, String title, String text) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent open = new Intent(context, ProductDetails.class)
                .putExtra("productID", productID)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent content = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(open)
                .getPendingIntent((int) productID, PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_expire)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(content)
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat.from(context).notify((int) productID, n);
    }
}
