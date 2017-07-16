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

    // null esetén az ReminderSetter-től jött az Intent, ami a BOOT_COMPLETED-re reagált.
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
    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    // Ha a todo != null, akkor adott todo-t kérdezünk le, egyébként pedig az összeset.
    if (todo != null) {
      handleReminder(todo, action, alarmManager);
    } else {
      ArrayList<Todo> todos = dbLoader.getTodosWithReminder();
      handleReminders(todos, action, alarmManager);
    }
  }

  /**
   * Végrehajtja az emlékeztető beállításához szükséges műveleteket (egy emlékeztető esetén).
   * @param todo Az emlékeztetőhöz tartozó Todo.
   * @param action Az emlékeztetőhöz tartozó action.
   * @param alarmManager Az emlékeztető beállításához szükséges AlarmManager.
   */
  private void handleReminder(Todo todo, String action, AlarmManager alarmManager) {
    //Az értesítőt csak akkor vesszük figyelembe, ha még nem járt le.
    if (todo.getReminderDateTimeInLong() >= new Date().getTime()) {
      Intent intent = new Intent(this, ReminderReceiver.class);
      intent.putExtra("id", todo.get_id());
      intent.putExtra("msg", todo.getTitle());

      PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) todo.get_id(), intent,
          PendingIntent.FLAG_ONE_SHOT);
      if (CREATE.equals(action)) {
        alarmManager.set(AlarmManager.RTC_WAKEUP, todo.getReminderDateTimeInLong(), pendingIntent);
      } else {
        alarmManager.cancel(pendingIntent);
      }
    }
  }

  /**
   * Végrehajtja az emlékeztető beállításához szükséges műveleteket (egy vagy több emlékeztető
   * esetén).
   * @param todos Az emlékeztető(k)-höz tartozó Todo(k).
   * @param action Az emlékeztető(k)-höz tartozó action.
   * @param alarmManager Az emlékeztető beállításához szükséges AlarmManager.
   */
  private void handleReminders(ArrayList<Todo> todos, String action, AlarmManager alarmManager) {
    for (Todo todo:todos) {
      if (todo.getReminderDateTimeInLong() >= new Date().getTime()) {
        // Az értesítőt csak akkor vesszük figyelembe, ha még nem járt le.

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("id", todo.get_id());
        intent.putExtra("msg", todo.getTitle());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) todo.get_id(), intent,
            PendingIntent.FLAG_ONE_SHOT);
        if (CREATE.equals(action)) {
          alarmManager.set(AlarmManager.RTC_WAKEUP, todo.getReminderDateTimeInLong(), pendingIntent);
        } else {
          alarmManager.cancel(pendingIntent);
        }
      }
    }
  }

}
