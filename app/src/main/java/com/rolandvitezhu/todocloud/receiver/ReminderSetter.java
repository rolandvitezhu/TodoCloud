package com.rolandvitezhu.todocloud.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.service.ReminderService;

public class ReminderSetter extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    createReminderServicesOnBootCompleted(context);
  }

  private void createReminderServicesOnBootCompleted(Context context) {
    Intent serviceIntent = new Intent(context, ReminderService.class);
    serviceIntent.setAction(ReminderService.CREATE);
    context.startService(serviceIntent);
  }

  public static void createReminderService(Todo todo) {
    Context applicationContext = AppController.getAppContext();
    Intent serviceIntent = new Intent(applicationContext, ReminderService.class);
    serviceIntent.putExtra("todo", todo);
    serviceIntent.setAction(ReminderService.CREATE);
    applicationContext.startService(serviceIntent);
  }

  public static void cancelReminderService(Todo todo) {
    Context applicationContext = AppController.getAppContext();
    Intent serviceIntent = new Intent(applicationContext, ReminderService.class);
    serviceIntent.putExtra("todo", todo);
    serviceIntent.setAction(ReminderService.CANCEL);
    applicationContext.startService(serviceIntent);
  }

}
