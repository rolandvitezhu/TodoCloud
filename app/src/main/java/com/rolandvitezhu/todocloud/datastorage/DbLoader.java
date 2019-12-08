package com.rolandvitezhu.todocloud.datastorage;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.rolandvitezhu.todocloud.data.Category;
import com.rolandvitezhu.todocloud.data.List;
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.data.User;
import com.rolandvitezhu.todocloud.helper.DateTimeRange;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

public class DbLoader {

  private static final String TAG = DbLoader.class.getSimpleName();

	private static DbLoader instance;

  private SQLiteDatabase sqLiteDatabase;

  private final String[] todoColumns = new String[]{
      DbConstants.Todo.KEY_ROW_ID,
      DbConstants.Todo.KEY_TODO_ONLINE_ID,
      DbConstants.Todo.KEY_USER_ONLINE_ID,
      DbConstants.Todo.KEY_LIST_ONLINE_ID,
      DbConstants.Todo.KEY_TITLE,
      DbConstants.Todo.KEY_PRIORITY,
      DbConstants.Todo.KEY_DUE_DATE,
      DbConstants.Todo.KEY_REMINDER_DATE_TIME,
      DbConstants.Todo.KEY_DESCRIPTION,
      DbConstants.Todo.KEY_COMPLETED,
      DbConstants.Todo.KEY_ROW_VERSION,
      DbConstants.Todo.KEY_DELETED,
      DbConstants.Todo.KEY_DIRTY,
      DbConstants.Todo.KEY_POSITION
  };
  private final String[] listColumns = new String[]{
      DbConstants.List.KEY_ROW_ID,
      DbConstants.List.KEY_LIST_ONLINE_ID,
      DbConstants.List.KEY_USER_ONLINE_ID,
      DbConstants.List.KEY_CATEGORY_ONLINE_ID,
      DbConstants.List.KEY_TITLE,
      DbConstants.List.KEY_ROW_VERSION,
      DbConstants.List.KEY_DELETED,
      DbConstants.List.KEY_DIRTY,
      DbConstants.List.KEY_POSITION
  };
  private final String[] categoryColumns = new String[]{
      DbConstants.Category.KEY_ROW_ID,
      DbConstants.Category.KEY_CATEGORY_ONLINE_ID,
      DbConstants.Category.KEY_USER_ONLINE_ID,
      DbConstants.Category.KEY_TITLE,
      DbConstants.Category.KEY_ROW_VERSION,
      DbConstants.Category.KEY_DELETED,
      DbConstants.Category.KEY_DIRTY,
      DbConstants.Category.KEY_POSITION
  };
  private final String todoOrderBy = DbConstants.Todo.KEY_DUE_DATE
      + ", "
      + DbConstants.Todo.KEY_PRIORITY
      + " DESC"
      + ", "
      + DbConstants.Todo.KEY_TITLE;

  public DbLoader() {
	}

	public static synchronized DbLoader getInstance() {
    if (instance == null)
      instance = new DbLoader();

    return instance;
  }

  private void open() {
    DbHelper dbHelper = DbHelper.getInstance();
    sqLiteDatabase = dbHelper.getWritableDatabase();
  }

  public void reCreateDb() {
    open();
    dropTables();
    createTables();
  }

  private void dropTables() {
    sqLiteDatabase.execSQL(DbConstants.User.DATABASE_DROP);
    sqLiteDatabase.execSQL(DbConstants.Todo.DATABASE_DROP);
    sqLiteDatabase.execSQL(DbConstants.List.DATABASE_DROP);
    sqLiteDatabase.execSQL(DbConstants.Category.DATABASE_DROP);
  }

  private void createTables() {
    sqLiteDatabase.execSQL(DbConstants.User.DATABASE_CREATE);
    sqLiteDatabase.execSQL(DbConstants.Todo.DATABASE_CREATE);
    sqLiteDatabase.execSQL(DbConstants.List.DATABASE_CREATE);
    sqLiteDatabase.execSQL(DbConstants.Category.DATABASE_CREATE);
  }

  public Integer getLowestPositionForGivenTable(String givenTable) {
    open();
    Cursor cursor = sqLiteDatabase.query(
        givenTable,
        new String[]{ "MIN(" + DbConstants.Todo.KEY_POSITION + ")" },
        null,
        null,
        null,
        null,
        null
    );
    cursor.moveToFirst();
    return cursor.getInt(0);
  }

  // ----------------- User table methods ------------------------------------------------- //

  /**
   * @return the created User's _id, if User created successfully, otherwise -1.
   */
  public long createUser(User user) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.User.KEY_USER_ONLINE_ID, user.getUserOnlineId());
    contentValues.put(DbConstants.User.KEY_NAME, user.getName());
    contentValues.put(DbConstants.User.KEY_EMAIL, user.getEmail());
    contentValues.put(DbConstants.User.KEY_API_KEY, user.getApiKey());
    return sqLiteDatabase.insert(DbConstants.User.DATABASE_TABLE, null, contentValues);
  }

  public boolean updateUser(User user) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.User.KEY_USER_ONLINE_ID, user.getUserOnlineId());
    contentValues.put(DbConstants.User.KEY_NAME, user.getName());
    contentValues.put(DbConstants.User.KEY_EMAIL, user.getEmail());
    contentValues.put(DbConstants.User.KEY_API_KEY, user.getApiKey());
    String whereClause = DbConstants.User.KEY_ROW_ID + "=" + user.get_id();
    return sqLiteDatabase.update(
        DbConstants.User.DATABASE_TABLE,
        contentValues,
        whereClause,
        null
    ) > 0;
  }

  /**
   * @return the current User or null
   */
  public User getUser() {
    open();
    String[] columns = new String[] {
        DbConstants.User.KEY_ROW_ID,
        DbConstants.User.KEY_USER_ONLINE_ID,
        DbConstants.User.KEY_NAME,
        DbConstants.User.KEY_EMAIL,
        DbConstants.User.KEY_API_KEY
    };
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.User.DATABASE_TABLE,
        columns,
        null, null, null, null, null
    );
    if (cursor.moveToFirst()) {
      User user = new User(cursor);
      cursor.close();
      return user;
    } else {
      cursor.close();
      return null;
    }
  }

  /**
   * @return current User's userOnlineId or null
   */
  public String getUserOnlineId() {
    open();
    String[] columns = {DbConstants.User.KEY_USER_ONLINE_ID};
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.User.DATABASE_TABLE,
        columns,
        null, null, null, null, null);
    if (cursor.moveToFirst()) {
      String userOnlineId = cursor.getString(0);
      cursor.close();
      return userOnlineId;
    } else {
      cursor.close();
      return null;
    }
  }

  public String getApiKey() {
    open();

    String apiKey = null;

    String[] columns = {DbConstants.User.KEY_API_KEY};
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.User.DATABASE_TABLE,
        columns,
        null, null, null, null, null);
    cursor.moveToFirst();

    try {
      apiKey = cursor.getString(0);
    }
    catch (Exception e) {
      Log.d(TAG, "There is no API KEY in the database.");
    }
    finally {
      cursor.close();

      return apiKey;
    }
  }

  // ----------------- Todo table methods ------------------------------------------------- //

  /**
   * @return the created Todo's _id, if Todo created successfully, otherwise -1.
   */
	public long createTodo(Todo todo) {
		open();
    ContentValues contentValues = prepareTodoContentValues(todo);
    long _Id = sqLiteDatabase.insert(DbConstants.Todo.DATABASE_TABLE, null, contentValues);
    fixTodoPositions();
    return _Id;
	}

  public boolean updateTodo(Todo todo) {
    open();
    ContentValues contentValues = prepareTodoContentValues(todo);
    boolean successful = false;

    if (todo.get_id() != 0) {
      // The Todo has been modified offline, therefore todo_online_id is null in the local
      // database yet
      String whereClause = DbConstants.Todo.KEY_ROW_ID + "=" + todo.get_id();
      successful = sqLiteDatabase.update(
          DbConstants.Todo.DATABASE_TABLE,
          contentValues,
          whereClause,
          null
      ) > 0;
//      fixTodoPositions();
      return successful;
    } else {
      // The Todo has been modified online, therefore _id is unknown yet
      String whereClause = DbConstants.Todo.KEY_TODO_ONLINE_ID
          + "='"
          + todo.getTodoOnlineId()
          + "'";
      successful = sqLiteDatabase.update(
          DbConstants.Todo.DATABASE_TABLE,
          contentValues,
          whereClause,
          null
      ) > 0;
//      fixTodoPositions();
      return successful;
    }
  }

  @NonNull
  private ContentValues prepareTodoContentValues(Todo todo) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.Todo.KEY_TODO_ONLINE_ID, todo.getTodoOnlineId());
    contentValues.put(DbConstants.Todo.KEY_USER_ONLINE_ID, todo.getUserOnlineId());
    if (todo.getListOnlineId() == null || todo.getListOnlineId().equals("")) {
      contentValues.putNull(DbConstants.Todo.KEY_LIST_ONLINE_ID);
    } else {
      contentValues.put(DbConstants.Todo.KEY_LIST_ONLINE_ID, todo.getListOnlineId());
    }
    contentValues.put(DbConstants.Todo.KEY_TITLE, todo.getTitle());
    contentValues.put(DbConstants.Todo.KEY_PRIORITY, todo.isPriority() ? 1 : 0);
    if (todo.getDueDate() == null || todo.getDueDate().equals("")) {
      contentValues.putNull(DbConstants.Todo.KEY_DUE_DATE);
    } else {
      contentValues.put(DbConstants.Todo.KEY_DUE_DATE, todo.getDueDate());
    }
    if (todo.getReminderDateTime() == null || todo.getReminderDateTime().equals("")) {
      contentValues.putNull(DbConstants.Todo.KEY_REMINDER_DATE_TIME);
    } else {
      contentValues.put(DbConstants.Todo.KEY_REMINDER_DATE_TIME, todo.getReminderDateTime());
    }
    if (todo.getDescription() == null || todo.getDescription().equals("")) {
      contentValues.putNull(DbConstants.Todo.KEY_DESCRIPTION);
    } else {
      contentValues.put(DbConstants.Todo.KEY_DESCRIPTION, todo.getDescription());
    }
    contentValues.put(DbConstants.Todo.KEY_COMPLETED, todo.isCompleted() ? 1 : 0);
    contentValues.put(DbConstants.Todo.KEY_ROW_VERSION, todo.getRowVersion());
    contentValues.put(DbConstants.Todo.KEY_DELETED, todo.getDeleted() ? 1 : 0);
    contentValues.put(DbConstants.Todo.KEY_DIRTY, todo.getDirty() ? 1 : 0);
    contentValues.put(DbConstants.Todo.KEY_POSITION, todo.getPosition());
    return contentValues;
  }

  public ArrayList<Todo> getTodos(String wherePrefix) {
    open();
    String standardWherePostfix = prepareStandardWherePostfix();
    String where = wherePrefix + standardWherePostfix;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE,
        todoColumns,
        where,
        null, null, null,
        todoOrderBy
    );
    cursor.moveToFirst();
    ArrayList<Todo> todos = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      Todo todo = new Todo(cursor);
      todos.add(todo);
      cursor.moveToNext();
    }
    cursor.close();
    return todos;
  }

  public ArrayList<Todo> getPredefinedListTodos(String where) {
    open();
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE,
        todoColumns,
        where,
        null, null, null,
        todoOrderBy
    );
    cursor.moveToFirst();
    ArrayList<Todo> predefinedListTodos = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      Todo todo = new Todo(cursor);
      predefinedListTodos.add(todo);
      cursor.moveToNext();
    }
    cursor.close();
    return predefinedListTodos;
  }

  private ArrayList<Integer> getDuplicatePositionValuesForTodos() {
	  open();
	  Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE,
        new String[]{ DbConstants.Todo.KEY_POSITION + ", COUNT(*) c" },
        null,
        null,
        DbConstants.Todo.KEY_POSITION,
        "c > 1",
        DbConstants.Todo.KEY_POSITION + " ASC"
    );

    cursor.moveToFirst();
    ArrayList<Integer> duplicatePositionValuesForTodos = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      duplicatePositionValuesForTodos.add(cursor.getInt(0));
      cursor.moveToNext();
    }
    cursor.close();

    return duplicatePositionValuesForTodos;
  }

  private HashMap<Integer, ArrayList<Integer>> get_IdForDuplicatePositionValues() {
    ArrayList<Integer> duplicatePositionValuesForTodos = getDuplicatePositionValuesForTodos();
	  open();
    HashMap<Integer, ArrayList<Integer>> _IdForDuplicatePositionValues = new HashMap<>();
    ArrayList<Integer> _IdForDuplicatePositionValue = new ArrayList<>();
    for (Integer nextPositionValue : duplicatePositionValuesForTodos) {
      Cursor cursor = sqLiteDatabase.query(
          DbConstants.Todo.DATABASE_TABLE,
          new String[]{ DbConstants.Todo.KEY_ROW_ID },
          DbConstants.Todo.KEY_POSITION + " = " + nextPositionValue,
          null,
          null,
          null,
          DbConstants.Todo.KEY_ROW_ID + " ASC"
      );
      cursor.moveToFirst();
      while (!cursor.isAfterLast()) {
        _IdForDuplicatePositionValue.add(cursor.getInt(0));
        cursor.moveToNext();
      }
      cursor.close();
      _IdForDuplicatePositionValues.put(nextPositionValue, _IdForDuplicatePositionValue);
    }

    return _IdForDuplicatePositionValues;
  }

  public void fixTodoPositions() {
    HashMap<Integer, ArrayList<Integer>> _IdForDuplicatePositionValues = get_IdForDuplicatePositionValues();
	  open();
    for (Map.Entry<Integer, ArrayList<Integer>> duplicatePosition : _IdForDuplicatePositionValues.entrySet()) {
      for (int i = 1; i < duplicatePosition.getValue().size(); i++) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(
            DbConstants.Todo.KEY_POSITION,
            getLowestPositionForGivenTable(DbConstants.Todo.DATABASE_TABLE) - 100
        );
        contentValues.put(DbConstants.Todo.KEY_DIRTY, true);
        sqLiteDatabase.update(
            DbConstants.Todo.DATABASE_TABLE,
            contentValues,
            DbConstants.Todo.KEY_ROW_ID + " = " + duplicatePosition.getValue().get(i),
            null
        );
      }
    }
  }

  @NonNull
  private String prepareStandardWherePostfix() {
    return " AND "
        + DbConstants.Todo.KEY_COMPLETED
        + "="
        + 0
        + " AND "
        + DbConstants.Todo.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "'"
        + " AND "
        + DbConstants.Todo.KEY_DELETED
        + "="
        + 0;
  }

  /**
   * An interval/range for today in long representation LocalDateTime
   * @return
   */
  private DateTimeRange today() {
    // Old code
//    String pattern = "yyyy.MM.dd.";
//    Locale defaultLocale = Locale.getDefault();
//    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
//        pattern,
//        defaultLocale
//    );
//    Date today = new Date();
//    return simpleDateFormat.format(today);
    // TODO:
    // 1. Modify return value: we need an interval.
    // 2. Modify the database to use this interval.
    // Important: don't delete the old code first, only comment it out and only delete it after
    // tested the new code and it works good.

    LocalDate today;
    LocalDateTime startOfToday;
    LocalDateTime endOfToday;
    ZonedDateTime zdtStartOfToday;
    ZonedDateTime zdtEndOfToday;

    today = LocalDate.now();
    startOfToday = today.atStartOfDay();
    endOfToday = startOfToday.plusDays(1).minusNanos(1);
    zdtStartOfToday = startOfToday.atZone(ZoneId.systemDefault());
    zdtEndOfToday = endOfToday.atZone(ZoneId.systemDefault());

    return new DateTimeRange(
        zdtStartOfToday.toInstant().toEpochMilli(),
        zdtEndOfToday.toInstant().toEpochMilli()
    );
  }

  /**
   * An interval/range for the next 7 days in long representation LocalDateTime
   * @return
   */
  private DateTimeRange next7Days() {
    // Old code
//    String pattern = "yyyy.MM.dd.";
//    Locale defaultLocale = Locale.getDefault();
//    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
//        pattern,
//        defaultLocale
//    );
//    Date today = new Date();
//    return simpleDateFormat.format(today);
    // TODO:
    // 1. Modify return value: we need an interval.
    // 2. Modify the database to use this interval.
    // Important: don't delete the old code first, only comment it out and only delete it after
    // tested the new code and it works good.

    LocalDate today;
    LocalDateTime startOf7Days;
    LocalDateTime endOf7Days;
    ZonedDateTime zdtStartOf7Days;
    ZonedDateTime zdtEndOf7Days;

    today = LocalDate.now();
    startOf7Days = today.atStartOfDay();
    endOf7Days = startOf7Days.plusDays(8).minusNanos(1);
    zdtStartOf7Days = startOf7Days.atZone(ZoneId.systemDefault());
    zdtEndOf7Days = endOf7Days.atZone(ZoneId.systemDefault());

    return new DateTimeRange(
        zdtStartOf7Days.toInstant().toEpochMilli(),
        zdtEndOf7Days.toInstant().toEpochMilli()
    );
  }

  private String prepareTodayPredefinedListWherePrefix() {
    DateTimeRange todayDateTimeRange = today();
    return DbConstants.Todo.KEY_DUE_DATE
        + " BETWEEN "
        + todayDateTimeRange.getStartOfRangeLong()
        + " AND "
        + todayDateTimeRange.getEndOfRangeLong();
  }

  @NonNull
  public String prepareTodayPredefinedListWhere() {
    String todayPredefinedListWherePrefix = prepareTodayPredefinedListWherePrefix();
    String standardWherePostfix = prepareStandardWherePostfix();
    return todayPredefinedListWherePrefix
        + standardWherePostfix;
  }

  private String prepareNext7DaysPredefinedListWherePrefix() {
    // Old code
//    String pattern = "yyyy.MM.dd.";
//    Locale defaultLocale = Locale.getDefault();
//    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
//        pattern,
//        defaultLocale
//    );
//    Date today = new Date();
//    Calendar calendar = Calendar.getInstance();
//    calendar.setTime(today);
//
//    StringBuilder whereStringBuilder = new StringBuilder();
//    String todayString = simpleDateFormat.format(today);
//    appendToday(whereStringBuilder, todayString);
//    for (int i = 0; i < 6; i++) {
//      String nextDayString = prepareNextDayStringWhere(simpleDateFormat, calendar);
//      appendNextDay(whereStringBuilder, nextDayString);
//    }
//    prepareWhereStringBuilderPostfix(whereStringBuilder);
//    String where = whereStringBuilder.toString();
//    return where;
    DateTimeRange next7DaysDateTimeRange = next7Days();
    return DbConstants.Todo.KEY_DUE_DATE
        + " BETWEEN "
        + next7DaysDateTimeRange.getStartOfRangeLong()
        + " AND "
        + next7DaysDateTimeRange.getEndOfRangeLong();
  }

  // Old method. Should be deleted, because now it's useless
//  private void appendToday(StringBuilder whereStringBuilder, String todayString) {
//    whereStringBuilder.append(
//        "("
//            + DbConstants.Todo.KEY_DUE_DATE
//            + "='"
//            + todayString
//            + "' OR "
//    );
//  }

  // Old method. Should be deleted, because now it's useless
//  private String prepareNextDayStringWhere(SimpleDateFormat simpleDateFormat, Calendar calendar) {
//    calendar.roll(Calendar.DAY_OF_MONTH, true);
//    Date nextDay = new Date();
//    nextDay.setTime(calendar.getTimeInMillis());
//    return simpleDateFormat.format(nextDay);
//  }

  // Old method. Should be deleted, because now it's useless
//  private void appendNextDay(StringBuilder whereStringBuilder, String nextDayString) {
//    whereStringBuilder.append(
//        DbConstants.Todo.KEY_DUE_DATE
//            + "='"
//            + nextDayString
//            + "' OR "
//    );
//  }

  // Old method. Should be deleted, because now it's useless
//  private void prepareWhereStringBuilderPostfix(StringBuilder whereStringBuilder) {
//    whereStringBuilder.delete(whereStringBuilder.length()-4, whereStringBuilder.length());
//    whereStringBuilder.append(')');
//  }

  @NonNull
  public String prepareNext7DaysPredefinedListWhere() {
    String next7DaysWherePredefinedListPrefix = prepareNext7DaysPredefinedListWherePrefix();
    String standardWherePostfix = prepareStandardWherePostfix();
    return next7DaysWherePredefinedListPrefix
        + standardWherePostfix;
  }

  @NonNull
  public String prepareAllPredefinedListWhere() {
    return DbConstants.Todo.KEY_COMPLETED
        + "="
        + 0
        + " AND "
        + DbConstants.Todo.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "'"
        + " AND "
        + DbConstants.Todo.KEY_DELETED
        + "="
        + 0;
  }

  @NonNull
  public String prepareCompletedPredefinedListWhere() {
    return DbConstants.Todo.KEY_COMPLETED
        + "="
        + 1
        + " AND "
        + DbConstants.Todo.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "'"
        + " AND "
        + DbConstants.Todo.KEY_DELETED
        + "="
        + 0;
  }

  public String prepareSearchWhere(String queryText) {
    return prepareSearchWherePrefix(queryText) + prepareStandardWherePostfix();
  }

  private String prepareSearchWherePrefix(String queryText) {
    return "("
        + DbConstants.Todo.KEY_TITLE
        + " LIKE '%"
        + queryText
        + "%'"
        + " OR "
        + DbConstants.Todo.KEY_DESCRIPTION
        + " LIKE '%"
        + queryText
        + "%')";
  }

  public ArrayList<Todo> getTodosWithReminder() {
    String where =
        DbConstants.Todo.KEY_REMINDER_DATE_TIME
            + " IS NOT NULL AND "
            + DbConstants.Todo.KEY_REMINDER_DATE_TIME
            + " != \"\"";
    return getTodos(where);
  }

  public ArrayList<Todo> getTodosByListOnlineId(String listOnlineId) {
    String where = DbConstants.Todo.KEY_LIST_ONLINE_ID + "='" + listOnlineId + "'";
    return getTodos(where);
  }

  private ArrayList<String> getTodoOnlineIdsByListOnlineId(String listOnlineId) {
    open();
    String[] columns = {DbConstants.Todo.KEY_TODO_ONLINE_ID};
    String where = DbConstants.Todo.KEY_LIST_ONLINE_ID + "='" + listOnlineId + "'";
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE,
        columns,
        where,
        null, null, null, null
    );
    cursor.moveToFirst();
    ArrayList<String> todoOnlineIds = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      String todoOnlineId = cursor.getString(0);
      todoOnlineIds.add(todoOnlineId);
      cursor.moveToNext();
    }
    cursor.close();
    return todoOnlineIds;
  }

  public ArrayList<Todo> getTodosToUpdate() {
    open();
    String where = DbConstants.Todo.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "' AND "
        + DbConstants.Todo.KEY_DIRTY
        + "="
        + 1
        + " AND "
        + DbConstants.Todo.KEY_ROW_VERSION
        + ">"
        + 0;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE,
        todoColumns,
        where,
        null, null, null, null
    );
    cursor.moveToFirst();
    ArrayList<Todo> todosToUpdate = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      Todo todo = new Todo(cursor);
      todosToUpdate.add(todo);
      cursor.moveToNext();
    }
    cursor.close();
    return todosToUpdate;
  }

  public ArrayList<Todo> getTodosToInsert() {
    open();
    String where = DbConstants.Todo.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "' AND "
        + DbConstants.Todo.KEY_DIRTY
        + "="
        + 1
        + " AND "
        + DbConstants.Todo.KEY_ROW_VERSION
        + "="
        + 0;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE,
        todoColumns,
        where,
        null, null, null, null
    );
    cursor.moveToFirst();
    ArrayList<Todo> todosToInsert = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      Todo todo = new Todo(cursor);
      todosToInsert.add(todo);
      cursor.moveToNext();
    }
    cursor.close();
    return todosToInsert;
  }

  public boolean isTodoExists(String todoOnlineId) {
    open();
    String[] columns = {DbConstants.Todo.KEY_TODO_ONLINE_ID};
    String where = DbConstants.Todo.KEY_TODO_ONLINE_ID + "= ?";
    String[] whereArguments = {todoOnlineId};
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE,
        columns,
        where,
        whereArguments,
        null, null, null
    );
    boolean exists = cursor.getCount() > 0;
    cursor.close();
    return exists;
  }

  public int getLastTodoRowVersion() {
    open();
    String[] columns = {"MAX(" + DbConstants.Todo.KEY_ROW_VERSION + ")"};
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE,
        columns, 
        null, null, null, null, null
    );
    cursor.moveToFirst();
    int row_version = cursor.getInt(0);
    cursor.close();
    return row_version;
  }

	public Todo getTodo(String todoOnlineId) {
    open();
    String where = DbConstants.Todo.KEY_TODO_ONLINE_ID + "='" + todoOnlineId + "'";
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE, 
        todoColumns,
        where,
        null, null, null, null
    );
    if (cursor.moveToFirst()) {
      Todo todo = new Todo(cursor);
      cursor.close();
      return todo;
    } else {
      cursor.close();
      return null;
    }
  }

  public Todo getTodo(Long _id) {
    open();
    String where = DbConstants.Todo.KEY_ROW_ID + "=" + _id;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE, 
        todoColumns,
        where,
        null, null, null, null
    );
    if (cursor.moveToFirst()) {
      Todo todo = new Todo(cursor);
      cursor.close();
      return todo;
    } else {
      cursor.close();
      return null;
    }
  }

  public boolean softDeleteTodo(String todoOnlineId) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.Todo.KEY_DELETED, 1);
    contentValues.put(DbConstants.Todo.KEY_DIRTY, 1);
    String whereClause = DbConstants.Todo.KEY_TODO_ONLINE_ID + "='" + todoOnlineId + "'";
    boolean successful = sqLiteDatabase.update(
        DbConstants.Todo.DATABASE_TABLE,
        contentValues,
        whereClause,
        null
    ) > 0;
    fixTodoPositions();
    return successful;
  }

  public boolean softDeleteTodo(Todo todo) {
    String todoOnlineId = todo.getTodoOnlineId();
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.Todo.KEY_DELETED, 1);
    contentValues.put(DbConstants.Todo.KEY_DIRTY, 1);
    String whereClause = DbConstants.Todo.KEY_TODO_ONLINE_ID + "='" + todoOnlineId + "'";
    boolean successful = sqLiteDatabase.update(
        DbConstants.Todo.DATABASE_TABLE,
        contentValues,
        whereClause,
        null
    ) > 0;
    fixTodoPositions();
    return successful;
  }

  // ----------------- List table methods ------------------------------------------------- //

  /**
   * @return the created List's _id, if List created successfully, -1 otherwise.
   */
  public long createList(List list) {
    open();
    ContentValues contentValues = prepareListContentValues(list);
    return sqLiteDatabase.insert(DbConstants.List.DATABASE_TABLE, null, contentValues);
  }

  @NonNull
  private ContentValues prepareListContentValues(List list) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.List.KEY_LIST_ONLINE_ID, list.getListOnlineId());
    contentValues.put(DbConstants.List.KEY_USER_ONLINE_ID, list.getUserOnlineId());
    if (list.getCategoryOnlineId() == null || list.getCategoryOnlineId().equals("")) {
      contentValues.putNull(DbConstants.List.KEY_CATEGORY_ONLINE_ID);
    } else {
      contentValues.put(DbConstants.List.KEY_CATEGORY_ONLINE_ID, list.getCategoryOnlineId());
    }
    contentValues.put(DbConstants.List.KEY_TITLE, list.getTitle());
    contentValues.put(DbConstants.List.KEY_ROW_VERSION, list.getRowVersion());
    contentValues.put(DbConstants.List.KEY_DELETED, list.getDeleted() ? 1 : 0);
    contentValues.put(DbConstants.List.KEY_DIRTY, list.getDirty() ? 1 : 0);
    contentValues.put(DbConstants.List.KEY_POSITION, list.getPosition());
    return contentValues;
  }

  public boolean updateList(List list) {
    open();
    ContentValues contentValues = prepareListContentValues(list);

    if (list.get_id() != 0) {
      // The List has been modified offline, therefore list_online_id is null in the local
      // database yet
      String whereClause = DbConstants.List.KEY_ROW_ID + "=" + list.get_id();
      return sqLiteDatabase.update(
          DbConstants.List.DATABASE_TABLE,
          contentValues,
          whereClause,
          null
      ) > 0;
    } else {
      // The List has been modified online, therefore _id is unknown yet
      String whereClause = DbConstants.List.KEY_LIST_ONLINE_ID + "='" + list.getListOnlineId() + "'";
      return sqLiteDatabase.update(
          DbConstants.List.DATABASE_TABLE,
          contentValues,
          whereClause,
          null
      ) > 0;
    }
  }

  public boolean softDeleteList(String listOnlineId) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.List.KEY_DELETED, 1);
    contentValues.put(DbConstants.List.KEY_DIRTY, 1);
    String whereClause = DbConstants.List.KEY_LIST_ONLINE_ID + "='" + listOnlineId + "'";
    return sqLiteDatabase.update(
        DbConstants.List.DATABASE_TABLE,
        contentValues,
        whereClause,
        null
    ) > 0;
  }

  public void softDeleteListAndRelatedTodos(String listOnlineId) {
    ArrayList<String> todoOnlineIds = getTodoOnlineIdsByListOnlineId(listOnlineId);
    boolean areRelatedTodos = !todoOnlineIds.isEmpty();
    if (areRelatedTodos) {
      for (String todoOnlineId : todoOnlineIds) {
        softDeleteTodo(todoOnlineId);
      }
    }
    softDeleteList(listOnlineId);
  }

  public ArrayList<List> getListsNotInCategory() {
    open();
    String where = DbConstants.List.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "' AND "
        + DbConstants.List.KEY_CATEGORY_ONLINE_ID
        + " IS NULL"
        + " AND "
        + DbConstants.List.KEY_DELETED
        + "="
        + 0;
    String orderBy = DbConstants.List.KEY_TITLE;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.List.DATABASE_TABLE,
        listColumns,
        where,
        null, null, null,
        orderBy
    );
    cursor.moveToFirst();
    ArrayList<List> listsNotInCategory = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      List list = new List(cursor);
      listsNotInCategory.add(list);
      cursor.moveToNext();
    }
    cursor.close();
    return listsNotInCategory;
  }

  public ArrayList<List> getListsByCategoryOnlineId(String categoryOnlineId) {
    open();
    String where = DbConstants.List.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "' AND "
        + DbConstants.List.KEY_CATEGORY_ONLINE_ID
        + "='"
        + categoryOnlineId
        + "'"
        + " AND "
        + DbConstants.List.KEY_DELETED
        + "="
        + 0;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.List.DATABASE_TABLE,
        listColumns,
        where,
        null, null, null,
        DbConstants.List.KEY_TITLE
    );
    cursor.moveToFirst();
    ArrayList<List> lists = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      List list = new List(cursor);
      lists.add(list);
      cursor.moveToNext();
    }
    cursor.close();
    return lists;
  }

  public boolean isListExists(String listOnlineId) {
    open();
    String[] columns = {DbConstants.List.KEY_LIST_ONLINE_ID};
    String where = DbConstants.List.KEY_LIST_ONLINE_ID + "= ?";
    String[] whereArguments = {listOnlineId};
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.List.DATABASE_TABLE,
        columns,
        where,
        whereArguments,
        null, null, null
    );
    boolean exists = cursor.getCount() > 0;
    cursor.close();
    return exists;
  }

  public int getLastListRowVersion() {
    open();
    String[] columns = {"MAX(" + DbConstants.List.KEY_ROW_VERSION + ")"};
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.List.DATABASE_TABLE,
        columns,
        null, null, null, null, null
    );
    cursor.moveToFirst();
    int row_version = cursor.getInt(0);
    cursor.close();
    return row_version;
  }

  public ArrayList<List> getListsToUpdate() {
    open();
    String where = DbConstants.List.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "' AND "
        + DbConstants.List.KEY_DIRTY
        + "="
        + 1
        + " AND "
        + DbConstants.List.KEY_ROW_VERSION
        + ">"
        + 0;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.List.DATABASE_TABLE,
        listColumns,
        where,
        null, null, null, null
    );
    cursor.moveToFirst();
    ArrayList<List> listsToUpdate = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      List list = new List(cursor);
      listsToUpdate.add(list);
      cursor.moveToNext();
    }
    cursor.close();
    return listsToUpdate;
  }

  public ArrayList<List> getListsToInsert() {
    open();
    String where = DbConstants.List.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "' AND "
        + DbConstants.List.KEY_DIRTY
        + "="
        + 1
        + " AND "
        + DbConstants.List.KEY_ROW_VERSION
        + "="
        + 0;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.List.DATABASE_TABLE,
        listColumns,
        where,
        null, null, null, null
    );
    cursor.moveToFirst();
    ArrayList<List> listsToInsert = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      List list = new List(cursor);
      listsToInsert.add(list);
      cursor.moveToNext();
    }
    cursor.close();
    return listsToInsert;
  }

  // ----------------- Category table methods ---------------------------------------------- //

  /**
   * @return the created Category's _id, if Category created successfully, -1 otherwise.
   */
  public long createCategory(Category category) {
    open();
    ContentValues contentValues = prepareCategoryContentValues(category);
    return sqLiteDatabase.insert(DbConstants.Category.DATABASE_TABLE, null, contentValues);
  }

  public boolean softDeleteCategory(String categoryOnlineId) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.Category.KEY_DELETED, 1);
    contentValues.put(DbConstants.Category.KEY_DIRTY, 1);
    String whereClause = DbConstants.Category.KEY_CATEGORY_ONLINE_ID
        + "='"
        + categoryOnlineId
        + "'";
    return sqLiteDatabase.update(
        DbConstants.Category.DATABASE_TABLE,
        contentValues,
        whereClause,
        null
    ) > 0;
  }

  public void softDeleteCategoryAndListsAndTodos(String categoryOnlineId) {
    ArrayList<List> lists = getListsByCategoryOnlineId(categoryOnlineId);
    boolean areRelatedLists = !lists.isEmpty();
    if (areRelatedLists) {
      for (List list:lists) {
        softDeleteListAndRelatedTodos(list.getListOnlineId());
      }
    }
    softDeleteCategory(categoryOnlineId);
  }

  public boolean updateCategory(Category category) {
    open();
    ContentValues contentValues = prepareCategoryContentValues(category);

    if (category.get_id() != 0) {
      // The Category has been modified offline, therefore category_online_id is null in the local
      // database yet
      String whereClause = DbConstants.Category.KEY_ROW_ID + "=" + category.get_id();
      return sqLiteDatabase.update(
          DbConstants.Category.DATABASE_TABLE,
          contentValues,
          whereClause,
          null
      ) > 0;
    } else {
      // The Category has been modified online, therefore _id is unknown yet
      String whereClause = DbConstants.Category.KEY_CATEGORY_ONLINE_ID
          + "='"
          + category.getCategoryOnlineId()
          + "'";
      return sqLiteDatabase.update(
          DbConstants.Category.DATABASE_TABLE,
          contentValues,
          whereClause,
          null
      ) > 0;
    }
  }

  @NonNull
  private ContentValues prepareCategoryContentValues(Category category) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.Category.KEY_CATEGORY_ONLINE_ID, category.getCategoryOnlineId());
    contentValues.put(DbConstants.Category.KEY_USER_ONLINE_ID, category.getUserOnlineId());
    contentValues.put(DbConstants.Category.KEY_TITLE, category.getTitle());
    contentValues.put(DbConstants.Category.KEY_ROW_VERSION, category.getRowVersion());
    contentValues.put(DbConstants.Category.KEY_DELETED, category.getDeleted() ? 1 : 0);
    contentValues.put(DbConstants.Category.KEY_DIRTY, category.getDirty() ? 1 : 0);
    contentValues.put(DbConstants.Category.KEY_POSITION, category.getPosition());
    return contentValues;
  }

  public Category getCategoryByCategoryOnlineId(String categoryOnlineId) {
    open();
    String where = DbConstants.Category.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "'"
        + " AND "
        + DbConstants.Category.KEY_DELETED
        + "="
        + 0
        + " AND "
        + DbConstants.Category.KEY_CATEGORY_ONLINE_ID
        + "='"
        + categoryOnlineId
        + "'";
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Category.DATABASE_TABLE,
        categoryColumns,
        where,
        null, null, null, null
    );
    cursor.moveToFirst();
    Category category = new Category(cursor);
    cursor.close();
    return category;
  }

  public ArrayList<Category> getCategories() {
    open();
    String where = DbConstants.Category.KEY_USER_ONLINE_ID + "='" + getUserOnlineId() + "'" + " AND " +
        DbConstants.Category.KEY_DELETED + "=" + 0;
    String orderBy = DbConstants.Category.KEY_TITLE;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Category.DATABASE_TABLE,
        categoryColumns,
        where,
        null, null, null,
        orderBy
    );
    cursor.moveToFirst();
    ArrayList<Category> categories = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      Category category = new Category(cursor);
      categories.add(category);
      cursor.moveToNext();
    }
    cursor.close();
    return categories;
  }

  public ArrayList<Category> getCategoriesToUpdate() {
    open();
    String where = DbConstants.Category.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "' AND "
        + DbConstants.Category.KEY_DIRTY
        + "="
        + 1
        + " AND "
        + DbConstants.Category.KEY_ROW_VERSION
        + ">"
        + 0;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Category.DATABASE_TABLE,
        categoryColumns,
        where,
        null, null, null, null
    );
    cursor.moveToFirst();
    ArrayList<Category> categoriesToUpdate = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      Category category = new Category(cursor);
      categoriesToUpdate.add(category);
      cursor.moveToNext();
    }
    cursor.close();
    return categoriesToUpdate;
  }

  public ArrayList<Category> getCategoriesToInsert() {
    open();
    String where = DbConstants.Category.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "' AND "
        + DbConstants.Category.KEY_DIRTY
        + "=" + 1
        + " AND "
        + DbConstants.Category.KEY_ROW_VERSION
        + "="
        + 0;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Category.DATABASE_TABLE,
        categoryColumns,
        where,
        null, null, null, null
    );
    cursor.moveToFirst();
    ArrayList<Category> categoriesToInsert = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      Category category = new Category(cursor);
      categoriesToInsert.add(category);
      cursor.moveToNext();
    }
    cursor.close();
    return categoriesToInsert;
  }

  public boolean isCategoryExists(String categoryOnlineId) {
    open();
    String[] columns = {DbConstants.Category.KEY_CATEGORY_ONLINE_ID};
    String where = DbConstants.Category.KEY_CATEGORY_ONLINE_ID + "= ?";
    String[] whereArguments = {categoryOnlineId};
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Category.DATABASE_TABLE,
        columns,
        where,
        whereArguments,
        null, null, null
    );
    boolean exists = cursor.getCount() > 0;
    cursor.close();
    return exists;
  }

  public int getLastCategoryRowVersion() {
    open();
    String[] columns = {
        "MAX("
            + DbConstants.Category.KEY_ROW_VERSION
            + ")"
    };
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Category.DATABASE_TABLE,
        columns,
        null, null, null, null, null
    );
    cursor.moveToFirst();
    int row_version = cursor.getInt(0);
    cursor.close();
    return row_version;
  }

}
