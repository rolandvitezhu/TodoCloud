package com.example.todocloud.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import com.example.todocloud.data.Todo;
import com.example.todocloud.datastorage.DbLoader;
import com.example.todocloud.receiver.ReminderReceiver;

import java.util.ArrayList;
import java.util.Date;

public class ReminderService extends IntentService {

  private static final String TAG = ReminderService.class.getSimpleName();

  public static final String CREATE = "CREATE";
  public static final String CANCEL = "CANCEL";

  private IntentFilter intentFilter;
  private DbLoader dbLoader;

  public ReminderService() {
    super(TAG);
    intentFilter = new IntentFilter();
    intentFilter.addAction(CREATE);
    intentFilter.addAction(CANCEL);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    dbLoader = new DbLoader();
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    String action = intent.getAction();
    Todo todo = intent.getParcelableExtra("todo");

    if (intentFilter.matchAction(action)) {
      execute(action, todo);
    }
  }

  private void execute(String action, Todo todo) {
    boolean isSingleReminder = todo != null;
    if (isSingleReminder) {
      handleReminder(todo, action);
    } else {
      handleReminders(action);
    }
  }

  private void handleReminder(Todo todo, String action) {
    Intent receiverIntent = prepareReceiverIntent(todo);
    int id = (int) todo.get_id();
    PendingIntent pendingIntent = PendingIntent.getBroadcast(
        this,
        id,
        receiverIntent,
        PendingIntent.FLAG_ONE_SHOT
    );
    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    long reminderDateTime = todo.getReminderDateTimeInLong();

    if (isNotPastReminderDateTime(reminderDateTime)) {
      if (CREATE.equals(action)) {
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            reminderDateTime,
            pendingIntent
        );
      } else {
        alarmManager.cancel(pendingIntent);
      }
    } else {
      alarmManager.cancel(pendingIntent);
    }
  }

  private void handleReminders(String action) {
    ArrayList<Todo> todos = dbLoader.getTodosWithReminder();
    for (Todo todo:todos) {
      Intent receiverIntent = prepareReceiverIntent(todo);
      int id = (int) todo.get_id();
      PendingIntent pendingIntent = PendingIntent.getBroadcast(
          this,
          id,
          receiverIntent,
          PendingIntent.FLAG_ONE_SHOT
      );
      AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
      long reminderDateTime = todo.getReminderDateTimeInLong();

      if (isNotPastReminderDateTime(reminderDateTime)) {
        if (CREATE.equals(action)) {
          alarmManager.set(
              AlarmManager.RTC_WAKEUP,
              reminderDateTime,
              pendingIntent
          );
        } else {
          alarmManager.cancel(pendingIntent);
        }
      } else {
        alarmManager.cancel(pendingIntent);
      }
    }
  }

  @NonNull
  private Intent prepareReceiverIntent(Todo todo) {
    Intent receiverIntent = new Intent(this, ReminderReceiver.class);
    receiverIntent.putExtra("id", todo.get_id());
    receiverIntent.putExtra("notificationText", todo.getTitle());
    return receiverIntent;
  }

  private boolean isNotPastReminderDateTime(long reminderDateTime) {
    return reminderDateTime >= new Date().getTime();
  }

}
