package com.example.todocloud.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

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
    dbLoader = new DbLoader(this);
  }

  /**
   * Feldolgozza az intent-eket a megfelelő módon.
   * @param intent A feldolgozandó Intent.
   */
  @Override
  protected void onHandleIntent(Intent intent) {
    String action = intent.getAction();

    // null esetén a ReminderSetter-től jött az Intent, ami a BOOT_COMPLETED-re reagált.
    Todo todo = intent.getParcelableExtra("todo");

    // Ha az intent action-je passzol az IntentFilter-ünk (matcher) valamelyik action-jével, akkor
    // azt az execute metódussal megfelelő módon feldolgozzuk.
    if (matcher.matchAction(action)) {
      execute(action, todo);
    }
  }

  /**
   * Feldolgozza az emlékeztetőt a megfelelő módon.
   * @param action Az emlékeztetőhöz tartozó action.
   * @param todo Az emlékeztetőhöz tartozó Todo.
   */
  private void execute(String action, Todo todo) {
    // Ha a todo != null, akkor adott todo-t kérdezünk le, egyébként pedig az összeset.
    if (todo != null) {
      handleReminder(todo, action);
    } else {
      ArrayList<Todo> todos = dbLoader.getTodosWithReminder();
      handleReminders(todos, action);
    }
  }

  private void handleReminder(Todo todo, String action) {
    Intent reminderIntent = new Intent(this, ReminderReceiver.class);
    reminderIntent.putExtra("id", todo.get_id());
    reminderIntent.putExtra("msg", todo.getTitle());

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

  private boolean isNotPastReminderDateTime(Todo todo) {
    return todo.getReminderDateTimeInLong() >= new Date().getTime();
  }

  private void handleReminders(ArrayList<Todo> todos, String action) {
    for (Todo todo:todos) {
      Intent reminderIntent = new Intent(this, ReminderReceiver.class);
      reminderIntent.putExtra("id", todo.get_id());
      reminderIntent.putExtra("msg", todo.getTitle());

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
