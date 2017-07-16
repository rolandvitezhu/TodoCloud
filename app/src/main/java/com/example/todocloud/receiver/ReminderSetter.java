package com.example.todocloud.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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

}
