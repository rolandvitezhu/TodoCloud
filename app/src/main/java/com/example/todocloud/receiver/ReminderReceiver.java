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

  /**
   * Notification-t küld a tennivaló reminderDateTime-ja szerinti időpontban.
   */
  @Override
  public void onReceive(Context context, Intent intent) {
    long id = intent.getLongExtra("id", 0);
    String msg = intent.getStringExtra("msg");
    Intent launchIntent = new Intent(context, MainActivity.class);

    launchIntent.putExtra("id", id);

    // Bezárja az előző Activity-t, így a navigálás rendben lesz (hátrafelé navigálva nem je-
    // lennek majd meg nem kívánatos Fragment-ek az előző Activity-ből).
    launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

    PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) id, launchIntent,
        PendingIntent.FLAG_ONE_SHOT);
    NotificationCompat.Builder builder = setDefaultsFromSharedPreferences(context);
    Notification notification = builder.setSmallIcon(R.drawable.ic_launcher)
        .setTicker(msg)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(pendingIntent)
        .setContentTitle(context.getString(R.string.titleReminder))
        .setContentText(msg)
        .setAutoCancel(true).build();

    NotificationManager notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.notify((int) id, notification);
  }

  /**
   * Beállítja a NotificationCompat.Builder hanggal, rezgéssel és fényjelzéssel kapcsolatos tulaj-
   * donságait a SettingsFragment-en beállítottak alapján, a SharedPreferences-ből.
   * @param context Az ReminderReceiver osztály Context-je.
   * @return NotificationCompat.Builder objektum a SharedPreferences szerinti megfelelő beállítá-
   * sokkal.
   */
  private NotificationCompat.Builder setDefaultsFromSharedPreferences(Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    boolean sound = sharedPreferences.getBoolean("reminder_sound_preference", true);
    boolean vibrate = sharedPreferences.getBoolean("vibration_preference", true);
    boolean lights = sharedPreferences.getBoolean("led_light_preference", true);

    if (sound && vibrate && lights) {
      return new NotificationCompat.Builder(context).setDefaults(Notification.DEFAULT_ALL);
    } else if (!sound && vibrate && lights) {
      return new NotificationCompat.Builder(context).setDefaults(Notification.DEFAULT_VIBRATE |
          Notification.DEFAULT_LIGHTS);
    } else if (sound && !vibrate && lights) {
      return new NotificationCompat.Builder(context).setDefaults(Notification.DEFAULT_SOUND |
          Notification.DEFAULT_LIGHTS);
    } else if (sound && vibrate && !lights) {
      return new NotificationCompat.Builder(context).setDefaults(Notification.DEFAULT_SOUND |
          Notification.DEFAULT_VIBRATE);
    } else if (!sound && !vibrate && lights) {
      return new NotificationCompat.Builder(context).setDefaults(Notification.DEFAULT_LIGHTS);
    } else if (!sound && vibrate && !lights) {
      return new NotificationCompat.Builder(context).setDefaults(Notification.DEFAULT_VIBRATE);
    } else if (sound && !vibrate && !lights) {
      return new NotificationCompat.Builder(context).setDefaults(Notification.DEFAULT_SOUND);
    } else /*if (!sound && !vibrate && !lights)*/ {
      return new NotificationCompat.Builder(context);
    }
  }

}
