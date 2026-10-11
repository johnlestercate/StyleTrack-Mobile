package com.styletrack.customer.notify;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.styletrack.customer.MainActivity;
import com.styletrack.customer.R;
import com.styletrack.customer.data.AppNotification;

public final class Notifier {
    private static final String CHANNEL_ID = "bookings";

    private Notifier() {}

    public static void createChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(
                new NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_bookings), NotificationManager.IMPORTANCE_DEFAULT));
    }

    public static boolean hasPermission(Context context) {
        return Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    /** Shows a system notification; does nothing if the customer has not allowed notifications. */
    public static void show(Context context, AppNotification n) {
        if (!hasPermission(context)) return;
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(
                context, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        android.app.Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(n.title)
                .setContentText(n.message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(n.message))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build();
        NotificationManagerCompat.from(context).notify((int) n.id, notification);
    }
}
