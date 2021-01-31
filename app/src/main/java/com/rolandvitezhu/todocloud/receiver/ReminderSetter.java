package com.rolandvitezhu.todocloud.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;

import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.service.ReminderService;

public class ReminderSetter extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    // Create notification services on boot completed
    createReminderServices(context);
  }

  public static void createReminderServices(Context context) {
    Intent serviceIntent = new Intent(context, ReminderService.class);
    serviceIntent.setAction(ReminderService.CREATE);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startService(serviceIntent);
      context.getApplicationContext().bindService(
          serviceIntent,
          new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
          //retrieve an instance of the service here from the IBinder returned
          //from the onBind method to communicate with
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
      },
          Context.BIND_AUTO_CREATE
      );
    } else
      context.startService(serviceIntent);
  }

  public static void createReminderService(Todo todo) {
    Context applicationContext = AppController.Companion.getAppContext();
    Intent serviceIntent = new Intent(applicationContext, ReminderService.class);
    serviceIntent.putExtra("todo", todo);
    serviceIntent.setAction(ReminderService.CREATE);
    if (applicationContext != null) {
      applicationContext.startService(serviceIntent);
    }
  }

  public static void cancelReminderService(Todo todo) {
    Context applicationContext = AppController.Companion.getAppContext();
    Intent serviceIntent = new Intent(applicationContext, ReminderService.class);
    serviceIntent.putExtra("todo", todo);
    serviceIntent.setAction(ReminderService.CANCEL);
    if (applicationContext != null) {
      applicationContext.startService(serviceIntent);
    }
  }

}
