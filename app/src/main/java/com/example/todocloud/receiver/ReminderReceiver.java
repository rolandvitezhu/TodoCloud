package com.example.todocloud.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v7.preference.PreferenceManager;

import com.example.todocloud.MainActivity;
import com.example.todocloud.R;

public class ReminderReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    long id = intent.getLongExtra("id", 0);
    String notificationText = intent.getStringExtra("notificationText");
    Intent activityIntent = new Intent(context, MainActivity.class);
    activityIntent.putExtra("id", id);
    // Prevent back navigation to the previous Activity by closing it
    activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

    PendingIntent pendingIntent = PendingIntent.getActivity(
        context,
        (int) id,
        activityIntent,
        PendingIntent.FLAG_ONE_SHOT
    );
    NotificationCompat.Builder notificationBuilder = setDefaultNotificationOptions(context);
    Notification notification = notificationBuilder
        .setSmallIcon(R.drawable.ic_launcher)
        .setTicker(notificationText)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(pendingIntent)
        .setContentTitle(context.getString(R.string.all_reminder))
        .setContentText(notificationText)
        .setAutoCancel(true)
        .build();

    NotificationManager notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.notify((int) id, notification);
  }

  private NotificationCompat.Builder setDefaultNotificationOptions(Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    boolean sound = sharedPreferences.getBoolean("reminder_sound_preference", true);
    boolean vibrate = sharedPreferences.getBoolean("vibration_preference", true);
    boolean lights = sharedPreferences.getBoolean("led_light_preference", true);
    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);

    if (sound && vibrate && lights) {
      return notificationBuilder.setDefaults(Notification.DEFAULT_ALL);
    } else if (!sound && vibrate && lights) {
      return notificationBuilder.setDefaults(Notification.DEFAULT_VIBRATE |
          Notification.DEFAULT_LIGHTS);
    } else if (sound && !vibrate && lights) {
      return notificationBuilder.setDefaults(Notification.DEFAULT_SOUND |
          Notification.DEFAULT_LIGHTS);
    } else if (sound && vibrate && !lights) {
      return notificationBuilder.setDefaults(Notification.DEFAULT_SOUND |
          Notification.DEFAULT_VIBRATE);
    } else if (!sound && !vibrate && lights) {
      return notificationBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
    } else if (!sound && vibrate && !lights) {
      return notificationBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
    } else if (sound && !vibrate && !lights) {
      return notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
    } else /*if (!sound && !vibrate && !lights)*/ {
      return notificationBuilder;
    }
  }

}
