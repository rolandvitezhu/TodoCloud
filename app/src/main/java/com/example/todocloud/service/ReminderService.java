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

  private IntentFilter matcher;
  private DbLoader dbLoader;

  public ReminderService() {
    super(TAG);
    matcher = new IntentFilter();
    matcher.addAction(CREATE);
    matcher.addAction(CANCEL);
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

    if (matcher.matchAction(action)) {
      execute(action, todo);
    }
  }

  private void execute(String action, Todo todo) {
    if (isSingleReminder(todo)) {
      handleReminder(todo, action);
    } else {
      handleReminders(action);
    }
  }

  private boolean isSingleReminder(Todo todo) {
    return todo != null;
  }

  private void handleReminder(Todo todo, String action) {
    Intent reminderIntent = prepareReminderIntent(todo);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(
        this,
        (int) todo.get_id(),
        reminderIntent,
        PendingIntent.FLAG_ONE_SHOT
    );
    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

    if (isNotPastReminderDateTime(todo)) {
      if (CREATE.equals(action)) {
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            todo.getReminderDateTimeInLong(),
            pendingIntent
        );
      } else {
        alarmManager.cancel(pendingIntent);
      }
    } else {
      alarmManager.cancel(pendingIntent);
    }
  }

  @NonNull
  private Intent prepareReminderIntent(Todo todo) {
    Intent reminderIntent = new Intent(this, ReminderReceiver.class);
    reminderIntent.putExtra("id", todo.get_id());
    reminderIntent.putExtra("msg", todo.getTitle());
    return reminderIntent;
  }

  private boolean isNotPastReminderDateTime(Todo todo) {
    return todo.getReminderDateTimeInLong() >= new Date().getTime();
  }

  private void handleReminders(String action) {
    ArrayList<Todo> todos = dbLoader.getTodosWithReminder();
    for (Todo todo:todos) {
      Intent reminderIntent = prepareReminderIntent(todo);
      PendingIntent pendingIntent = PendingIntent.getBroadcast(
          this, (int) todo.get_id(),
          reminderIntent,
          PendingIntent.FLAG_ONE_SHOT
      );
      AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

      if (isNotPastReminderDateTime(todo)) {
        if (CREATE.equals(action)) {
          alarmManager.set(
              AlarmManager.RTC_WAKEUP,
              todo.getReminderDateTimeInLong(),
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

}
