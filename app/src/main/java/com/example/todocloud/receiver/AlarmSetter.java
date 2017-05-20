package com.example.todocloud.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.todocloud.service.AlarmService;

public class AlarmSetter extends BroadcastReceiver {

  /**
   * BOOT_COMPLETED-re reagálva meghívja az AlarmService-t, ami élesíti a még aktuális
   * emlékeztetőket.
   */
  @Override
  public void onReceive(Context context, Intent intent) {
    Intent service = new Intent(context, AlarmService.class);
    service.setAction(AlarmService.CREATE);
    context.startService(service);
  }

}
