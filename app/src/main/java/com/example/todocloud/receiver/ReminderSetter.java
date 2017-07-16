package com.example.todocloud.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.todocloud.app.AppController;
import com.example.todocloud.data.Todo;
import com.example.todocloud.service.ReminderService;

public class ReminderSetter extends BroadcastReceiver {

  /**
   * BOOT_COMPLETED-re reagálva meghívja az ReminderService-t, ami élesíti a még aktuális
   * emlékeztetőket.
   */
  @Override
  public void onReceive(Context context, Intent intent) {
    Intent service = new Intent(context, ReminderService.class);
    service.setAction(ReminderService.CREATE);
    context.startService(service);
  }

  public static void createReminderService(Todo todo) {
    Context applicationContext = AppController.getAppContext();
    Intent reminderService = new Intent(applicationContext, ReminderService.class);
    reminderService.putExtra("todo", todo);
    reminderService.setAction(ReminderService.CREATE);
    applicationContext.startService(reminderService);
  }

  public static void cancelReminderService(Todo todo) {
    Context applicationContext = AppController.getAppContext();
    Intent reminderService = new Intent(applicationContext, ReminderService.class);
    reminderService.putExtra("todo", todo);
    reminderService.setAction(ReminderService.CANCEL);
    applicationContext.startService(reminderService);
  }

}
