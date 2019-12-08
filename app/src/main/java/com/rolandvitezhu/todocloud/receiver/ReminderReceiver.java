package com.rolandvitezhu.todocloud.receiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import static com.rolandvitezhu.todocloud.app.Constant.NOTIFICATION_CHANNEL_REMINDER;

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

    notificationBuilder
        .setSmallIcon(R.mipmap.ic_launcher)
        .setTicker(notificationText)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(pendingIntent)
        .setContentTitle(context.getString(R.string.all_reminder))
        .setContentText(notificationText)
        .setAutoCancel(true);

    NotificationManager notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      String notificationChannelName =
          AppController.getAppContext().getString(R.string.all_reminder);

      NotificationChannel channel = new NotificationChannel(
          NOTIFICATION_CHANNEL_REMINDER,
          notificationChannelName,
          NotificationManager.IMPORTANCE_DEFAULT);
      notificationManager.createNotificationChannel(channel);
      notificationBuilder.setChannelId(NOTIFICATION_CHANNEL_REMINDER);
    }

    Notification notification = notificationBuilder.build();

    notificationManager.notify((int) id, notification);
  }

  private NotificationCompat.Builder setDefaultNotificationOptions(Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    boolean sound = sharedPreferences.getBoolean("reminder_sound_preference", true);
    boolean vibrate = sharedPreferences.getBoolean("vibration_preference", true);
    boolean lights = sharedPreferences.getBoolean("led_light_preference", true);
    NotificationCompat.Builder notificationBuilder =
        new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_REMINDER);

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
