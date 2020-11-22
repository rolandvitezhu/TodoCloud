package com.rolandvitezhu.todocloud.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.database.TodoCloudDatabaseDao;
import com.rolandvitezhu.todocloud.receiver.ReminderReceiver;

import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import androidx.annotation.NonNull;

public class ReminderService extends IntentService {

  private static final String TAG = ReminderService.class.getSimpleName();

  public static final String CREATE = "CREATE";
  public static final String CANCEL = "CANCEL";

  @Inject
  TodoCloudDatabaseDao todoCloudDatabaseDao;

  private IntentFilter intentFilter;

  public ReminderService() {
    super(TAG);
    intentFilter = new IntentFilter();
    intentFilter.addAction(CREATE);
    intentFilter.addAction(CANCEL);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Objects.requireNonNull(AppController.Companion.getInstance()).getAppComponent().inject(this);
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
    int id = Objects.requireNonNull(todo.get_id()).intValue();
    PendingIntent pendingIntent = PendingIntent.getBroadcast(
        this,
        id,
        receiverIntent,
        PendingIntent.FLAG_ONE_SHOT
    );
    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    long reminderDateTimeLong = todo.getReminderDateTime();

    if (isNotPastReminderDateTime(reminderDateTimeLong)) {
      if (CREATE.equals(action)) {
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            reminderDateTimeLong,
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
    ArrayList<Todo> todos = null;
    try {
      todos = todoCloudDatabaseDao.getTodosWithReminderJava().get();
    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    for (Todo todo:todos) {
      Intent receiverIntent = prepareReceiverIntent(todo);
      int id = Objects.requireNonNull(todo.get_id()).intValue();
      PendingIntent pendingIntent = PendingIntent.getBroadcast(
          this,
          id,
          receiverIntent,
          PendingIntent.FLAG_ONE_SHOT
      );
      AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
      long reminderDateTimeLong = todo.getReminderDateTime();

      if (isNotPastReminderDateTime(reminderDateTimeLong)) {
        if (CREATE.equals(action)) {
          alarmManager.set(
              AlarmManager.RTC_WAKEUP,
              reminderDateTimeLong,
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
