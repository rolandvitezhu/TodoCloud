package com.example.todocloud.datastorage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.todocloud.data.Category;
import com.example.todocloud.data.List;
import com.example.todocloud.data.Todo;
import com.example.todocloud.data.User;

import java.util.ArrayList;

public class DbLoader {

	private DbHelper dbHelper;
	private SQLiteDatabase sqLiteDatabase;
  private Context context;

	public DbLoader(Context context) {
		this.context = context;
	}

  private void open() {
    dbHelper = DbHelper.getInstance(context);
    sqLiteDatabase = dbHelper.getWritableDatabase();
  }

  public void reCreateDb() {
    open();
    sqLiteDatabase.execSQL(DbConstants.User.DATABASE_DROP);
    sqLiteDatabase.execSQL(DbConstants.Todo.DATABASE_DROP);
    sqLiteDatabase.execSQL(DbConstants.List.DATABASE_DROP);
    sqLiteDatabase.execSQL(DbConstants.Category.DATABASE_DROP);
    sqLiteDatabase.execSQL(DbConstants.User.DATABASE_CREATE);
    sqLiteDatabase.execSQL(DbConstants.Todo.DATABASE_CREATE);
    sqLiteDatabase.execSQL(DbConstants.List.DATABASE_CREATE);
    sqLiteDatabase.execSQL(DbConstants.Category.DATABASE_CREATE);
  }

  // ----------------- User tábla metódusai. ------------------------------------------------- //

  /**
   * Felveszi az adatbázisba a megadott User-t, siker esetén visszaadja a hozzá tartozó id-t.
   * @param user Az adatbázisba felveendő User.
   * @return Siker esetén a felvett User _id-ját adja vissza, egyébként -1-et.
   */
  public long createUser(User user) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.User.KEY_USER_ONLINE_ID, user.getUserOnlineId());
    contentValues.put(DbConstants.User.KEY_NAME, user.getName());
    contentValues.put(DbConstants.User.KEY_EMAIL, user.getEmail());
    contentValues.put(DbConstants.User.KEY_API_KEY, user.getApiKey());
    long response = sqLiteDatabase.insert(DbConstants.User.DATABASE_TABLE, null, contentValues);
    return response;
  }

  /**
   * Frissíti az adatbázisban a megadott User-t.
   * @return Siker esetén true-t, egyébként false-t ad vissza.
   */
  public boolean updateUser(User user) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.User.KEY_USER_ONLINE_ID, user.getUserOnlineId());
    contentValues.put(DbConstants.User.KEY_NAME, user.getName());
    contentValues.put(DbConstants.User.KEY_EMAIL, user.getEmail());
    contentValues.put(DbConstants.User.KEY_API_KEY, user.getApiKey());
    boolean response = sqLiteDatabase.update(DbConstants.User.DATABASE_TABLE, contentValues,
        DbConstants.User.KEY_ROW_ID + "=" + user.get_id(), null) > 0;
    return response;
  }

  /**
   * Lekéri az adatbázisból a User-t (jelen helyzetben a helyi adatbázis csak egyetlen felhasználót
   * tartalmazhat).
   * @return A bejelentkezett User.
   */
  public User getUser() {
    open();
    Cursor cursor = sqLiteDatabase.query(DbConstants.User.DATABASE_TABLE, new String[] {
        DbConstants.User.KEY_ROW_ID, DbConstants.User.KEY_USER_ONLINE_ID,
        DbConstants.User.KEY_NAME, DbConstants.User.KEY_EMAIL,
        DbConstants.User.KEY_API_KEY }, null, null, null, null, null);
    if (cursor.moveToFirst()) {
      User user = new User();
      user.set_id(cursor.getLong(0));
      user.setUserOnlineId(cursor.getString(1));
      user.setName(cursor.getString(2));
      user.setEmail(cursor.getString(3));
      user.setApiKey(cursor.getString(4));
      cursor.close();
      return user;
    } else {
      cursor.close();
      return null;
    }
  }

  /**
   * Lekéri az adatbázisból a felhasználóhoz tartozó userOnlineId-t (jelen helyzetben a helyi
   * adatbázis csak egyetlen felhasználót tartalmazhat).
   * @return A felhasználóhoz tartozó userOnlineId.
   */
  public String getUserOnlineId() {
    open();
    Cursor cursor = sqLiteDatabase.query(DbConstants.User.DATABASE_TABLE, new String[] {
        DbConstants.User.KEY_USER_ONLINE_ID }, null, null, null, null, null);
    if (cursor.moveToFirst()) {
      String userOnlineId = cursor.getString(0);
      cursor.close();
      return userOnlineId;
    } else {
      cursor.close();
      return null;
    }
  }

  /**
   * Lekéri az adatbázisból a felhasználóhoz tartozó apiKey-t (jelen helyzetben a helyi adatbázis
   * csak egyetlen felhasználót tartalmazhat).
   * @return A felhasználóhoz tartozó apiKey.
   */
  public String getApiKey() {
    open();
    Cursor cursor = sqLiteDatabase.query(DbConstants.User.DATABASE_TABLE, new String[] {
        DbConstants.User.KEY_API_KEY }, null, null, null, null, null);
    cursor.moveToFirst();
    String apiKey = cursor.getString(0);
    cursor.close();
    return apiKey;
  }

  // ----------------- Todo tábla metódusai. ------------------------------------------------- //

  /**
   * Felveszi az adatbázisba a megadott Todo-t, siker esetén visszaadja a hozzá tartozó id-t.
   * @param todo Az adatbázisba felveendő Todo.
   * @return Siker esetén a felvett Todo _id-ját adja vissza, egyébként -1-et.
   */
	public long createTodo(Todo todo) {
		open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.Todo.KEY_TODO_ONLINE_ID, todo.getTodoOnlineId());
    contentValues.put(DbConstants.Todo.KEY_USER_ONLINE_ID, todo.getUserOnlineId());
    if (todo.getListOnlineId() == null) {
      // Null esetén null-t szúrunk be.
      contentValues.putNull(DbConstants.Todo.KEY_LIST_ONLINE_ID);
    } else if (!todo.getListOnlineId().equals("")) {
      // Ha nem null és nem üres String, akkor magát a String-et szúrjuk be.
      contentValues.put(DbConstants.Todo.KEY_LIST_ONLINE_ID, todo.getListOnlineId());
    } else {
      // Üres String esetén null-t szúrunk be.
      contentValues.putNull(DbConstants.Todo.KEY_LIST_ONLINE_ID);
    }
		contentValues.put(DbConstants.Todo.KEY_TITLE, todo.getTitle());
		contentValues.put(DbConstants.Todo.KEY_PRIORITY, todo.isPriority() ? 1 : 0);
		contentValues.put(DbConstants.Todo.KEY_DUE_DATE, todo.getDueDate());
    if (todo.getReminderDateTime() == null) {
      // Null esetén null-t szúrunk be.
      contentValues.putNull(DbConstants.Todo.KEY_REMINDER_DATETIME);
    } else if (!todo.getReminderDateTime().equals("")) {
      // Ha nem null és nem üres String, akkor magát a String-et szúrjuk be.
      contentValues.put(DbConstants.Todo.KEY_REMINDER_DATETIME, todo.getReminderDateTime());
    } else {
      // Üres String esetén null-t szúrunk be.
      contentValues.putNull(DbConstants.Todo.KEY_REMINDER_DATETIME);
    }
    if (todo.getDescription() == null) {
      // Null esetén null-t szúrunk be.
      contentValues.putNull(DbConstants.Todo.KEY_DESCRIPTION);
    } else if (!todo.getDescription().equals("")) {
      // Ha nem null és nem üres String, akkor magát a String-et szúrjuk be.
      contentValues.put(DbConstants.Todo.KEY_DESCRIPTION, todo.getDescription());
    } else {
      // Üres String esetén null-t szúrunk be.
      contentValues.putNull(DbConstants.Todo.KEY_DESCRIPTION);
    }
    contentValues.put(DbConstants.Todo.KEY_COMPLETED, todo.isCompleted() ? 1 : 0);
    contentValues.put(DbConstants.Todo.KEY_ROW_VERSION, todo.getRowVersion());
    contentValues.put(DbConstants.Todo.KEY_DELETED, todo.getDeleted() ? 1 : 0);
    contentValues.put(DbConstants.Todo.KEY_DIRTY, todo.getDirty() ? 1 : 0);
    long response = sqLiteDatabase.insert(DbConstants.Todo.DATABASE_TABLE, null, contentValues);
    return response;
	}

  /**
   * Frissíti az adatbázisban a megadott Todo-t.
   * @param todo A frissítendő Todo.
   * @return Siker esetén true-t, egyébként false-t ad vissza.
   */
  public boolean updateTodo(Todo todo) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.Todo.KEY_TODO_ONLINE_ID, todo.getTodoOnlineId());
    contentValues.put(DbConstants.Todo.KEY_USER_ONLINE_ID, todo.getUserOnlineId());
    if (todo.getListOnlineId() == null) {
      // Null esetén null-t szúrunk be.
      contentValues.putNull(DbConstants.Todo.KEY_LIST_ONLINE_ID);
    } else if (!todo.getListOnlineId().equals("")) {
      // Ha nem null és nem üres String, akkor magát a String-et szúrjuk be.
      contentValues.put(DbConstants.Todo.KEY_LIST_ONLINE_ID, todo.getListOnlineId());
    } else {
      // Üres String esetén null-t szúrunk be.
      contentValues.putNull(DbConstants.Todo.KEY_LIST_ONLINE_ID);
    }
    contentValues.put(DbConstants.Todo.KEY_TITLE, todo.getTitle());
    contentValues.put(DbConstants.Todo.KEY_PRIORITY, todo.isPriority() ? 1 : 0);
    contentValues.put(DbConstants.Todo.KEY_DUE_DATE, todo.getDueDate());
    if (todo.getReminderDateTime() == null) {
      // Null esetén null-t szúrunk be.
      contentValues.putNull(DbConstants.Todo.KEY_REMINDER_DATETIME);
    } else if (!todo.getReminderDateTime().equals("")) {
      // Ha nem null és nem üres String, akkor magát a String-et szúrjuk be.
      contentValues.put(DbConstants.Todo.KEY_REMINDER_DATETIME, todo.getReminderDateTime());
    } else {
      // Üres String esetén null-t szúrunk be.
      contentValues.putNull(DbConstants.Todo.KEY_REMINDER_DATETIME);
    }
    if (todo.getDescription() == null) {
      // Null esetén null-t szúrunk be.
      contentValues.putNull(DbConstants.Todo.KEY_DESCRIPTION);
    } else if (!todo.getDescription().equals("")) {
      // Ha nem null és nem üres String, akkor magát a String-et szúrjuk be.
      contentValues.put(DbConstants.Todo.KEY_DESCRIPTION, todo.getDescription());
    } else {
      // Üres String esetén null-t szúrunk be.
      contentValues.putNull(DbConstants.Todo.KEY_DESCRIPTION);
    }
    contentValues.put(DbConstants.Todo.KEY_COMPLETED, todo.isCompleted() ? 1 : 0);
    contentValues.put(DbConstants.Todo.KEY_ROW_VERSION, todo.getRowVersion());
    contentValues.put(DbConstants.Todo.KEY_DELETED, todo.getDeleted() ? 1 : 0);
    contentValues.put(DbConstants.Todo.KEY_DIRTY, todo.getDirty() ? 1 : 0);

    if (todo.get_id() != 0) {
      // Offline módosítottuk a tennivalót (a todo_online_id a helyi adatbázisban még null).
      return sqLiteDatabase.update(DbConstants.Todo.DATABASE_TABLE, contentValues,
          DbConstants.Todo.KEY_ROW_ID + "=" + todo.get_id(), null) > 0;
    } else {
      // Online módosítottuk a tennivalót (az _id ismeretlen).
      return sqLiteDatabase.update(DbConstants.Todo.DATABASE_TABLE, contentValues,
          DbConstants.Todo.KEY_TODO_ONLINE_ID + "='" + todo.getTodoOnlineId() + "'", null) > 0;
    }
  }

  /**
   * Lekéri az adatbázisból a jelenlegi User-hez tartozó összes Todo-t a következő szempontok
   * szerint rendezve: esedékesség dátuma szerint növekvő, prioritás szerint csökkenő, cím szerint
   * növekvő.
   * @return A jelenlegi User-hez tartozó Todo-k.
   */
	public ArrayList<Todo> getTodos() {
    open();
    Cursor cursor = sqLiteDatabase.query(DbConstants.Todo.DATABASE_TABLE, new String[] {
        DbConstants.Todo.KEY_ROW_ID, DbConstants.Todo.KEY_TODO_ONLINE_ID,
        DbConstants.Todo.KEY_USER_ONLINE_ID, DbConstants.Todo.KEY_LIST_ONLINE_ID,
        DbConstants.Todo.KEY_TITLE, DbConstants.Todo.KEY_PRIORITY,
        DbConstants.Todo.KEY_DUE_DATE, DbConstants.Todo.KEY_REMINDER_DATETIME,
        DbConstants.Todo.KEY_DESCRIPTION, DbConstants.Todo.KEY_COMPLETED,
        DbConstants.Todo.KEY_ROW_VERSION, DbConstants.Todo.KEY_DELETED,
        DbConstants.Todo.KEY_DIRTY },
        DbConstants.Todo.KEY_USER_ONLINE_ID + "='" + getUserOnlineId() + "'",
        null, null, null,
        DbConstants.Todo.KEY_DUE_DATE + ", " + DbConstants.Todo.KEY_PRIORITY + " DESC" + ", "
            + DbConstants.Todo.KEY_TITLE);
    cursor.moveToFirst();
    ArrayList<Todo> todos = new ArrayList<>();
		while (!cursor.isAfterLast()) {
			todos.add(getTodoByCursor(cursor));
			cursor.moveToNext();
    }
    cursor.close();
		return todos;
	}

  /**
   * Lekéri az adatbázisból a jelenlegi User-hez tartozó összes Todo-t, ahol completed == 0, a
   * következő szempontok szerint rendezve: esedékesség dátuma szerint növekvő, prioritás szerint
   * csökkenő, cím szerint növekvő. A where paraméterrel egyedi lekérdezésre van lehetőség (null
   * megadása esetén az eredeti lekérdezés fut le).
   * @param where Egyedi where feltétel adható meg, vagy null.
   * @return A jelenlegi User-hez tartozó Todo-k.
   */
  public ArrayList<Todo> getTodos(String where) {
    open();
    // Ha a nem elvégzett teendőket szeretnénk lekérni, akkor a where feltételhez hozzáfűzzük azt a
    // feltételt, ami csak a nem elvégzetteket adja vissza. Ha az elvégzett teendőket szeretnénk
    // lekérni, akkor a where feltétellel nem kell tennünk semmit.
    if (where == null) {
      where = DbConstants.Todo.KEY_COMPLETED + "=" + 0 + " AND " +
          DbConstants.Todo.KEY_USER_ONLINE_ID + "='" + getUserOnlineId() + "'" + " AND " +
          DbConstants.Todo.KEY_DELETED + "=" + 0;
    } else if (!where.equals(DbConstants.Todo.KEY_COMPLETED + "=" + 1 + " AND " +
        DbConstants.Todo.KEY_USER_ONLINE_ID + "='" + getUserOnlineId() + "'" + " AND " +
        DbConstants.Todo.KEY_DELETED + "=" + 0)) {
      where = where + " AND " + DbConstants.Todo.KEY_COMPLETED + "=" + 0 + " AND " +
          DbConstants.Todo.KEY_USER_ONLINE_ID + "='" + getUserOnlineId() + "'" + " AND " +
          DbConstants.Todo.KEY_DELETED + "=" + 0;
    }
    Cursor cursor = sqLiteDatabase.query(DbConstants.Todo.DATABASE_TABLE, new String[] {
        DbConstants.Todo.KEY_ROW_ID, DbConstants.Todo.KEY_TODO_ONLINE_ID,
        DbConstants.Todo.KEY_USER_ONLINE_ID, DbConstants.Todo.KEY_LIST_ONLINE_ID,
        DbConstants.Todo.KEY_TITLE, DbConstants.Todo.KEY_PRIORITY,
        DbConstants.Todo.KEY_DUE_DATE, DbConstants.Todo.KEY_REMINDER_DATETIME,
        DbConstants.Todo.KEY_DESCRIPTION, DbConstants.Todo.KEY_COMPLETED,
        DbConstants.Todo.KEY_ROW_VERSION, DbConstants.Todo.KEY_DELETED,
        DbConstants.Todo.KEY_DIRTY }, where, null, null, null,
        DbConstants.Todo.KEY_DUE_DATE + ", " + DbConstants.Todo.KEY_PRIORITY + " DESC" + ", "
            + DbConstants.Todo.KEY_TITLE);
    cursor.moveToFirst();
    ArrayList<Todo> todos = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      todos.add(getTodoByCursor(cursor));
      cursor.moveToNext();
    }
    cursor.close();
    return todos;
  }

  /**
   * Lekéri az adatbázisból a jelenlegi User-hez tartozó, emlékeztetővel rendelkező, el nem végzett
   * Todo-kat.
   * @return Emlékeztetővel rendelkező, el nem végzett Todo-k.
   */
  public ArrayList<Todo> getTodosWithReminder() {
    String where = DbConstants.Todo.KEY_REMINDER_DATETIME + "!= -1";
    return getTodos(where);
  }

  /**
   * Lekéri az adatbázisból az adott List-hez tartozó Todo-kat.
   * @param listOnlineId Az adott List-hez tartozó listOnlineId.
   * @return Az adott List-hez tartozó Todo-k.
   */
  public ArrayList<Todo> getTodosByListOnlineId(String listOnlineId) {
    String where = DbConstants.Todo.KEY_LIST_ONLINE_ID + "='" + listOnlineId + "'";
    ArrayList<Todo> todos = getTodos(where);
    return todos;
  }

  /**
   * Lekéri az adatbázisból a megadott listOnlineId-hoz tartozó todoOnlineId-kat.
   * @param listOnlineId A megadott listOnlineId.
   * @return A megadott listOnlineId-hoz tartozó todoOnlineId-k.
   */
  public ArrayList<String> getTodoOnlineIdsByListOnlineId(String listOnlineId) {
    open();
    Cursor cursor = sqLiteDatabase.query(DbConstants.Todo.DATABASE_TABLE, new String[]{
            DbConstants.Todo.KEY_TODO_ONLINE_ID },
        DbConstants.Todo.KEY_LIST_ONLINE_ID + "='" + listOnlineId + "'", null, null, null, null);
    cursor.moveToFirst();
    ArrayList<String> todoOnlineIds = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      todoOnlineIds.add(cursor.getString(0));
      cursor.moveToNext();
    }
    cursor.close();
    return todoOnlineIds;
  }

  /**
   * Lekéri az adatbázisból, a todo táblából az összes olyan sort, amit updatelni kell a távoli
   * adatbázisban. Feltételek: dirty == 1 és row_version > 0.
   * @return A frissítendő Todo-kat tartalmazza.
   */
  public ArrayList<Todo> getUpdatableTodos() {
    open();
    String where = DbConstants.Todo.KEY_USER_ONLINE_ID + "='" + getUserOnlineId() + "' AND " +
        DbConstants.Todo.KEY_DIRTY + "=" + 1 + " AND "
        + DbConstants.Todo.KEY_ROW_VERSION + ">" + 0;
    Cursor cursor = sqLiteDatabase.query(DbConstants.Todo.DATABASE_TABLE, new String[]{
        DbConstants.Todo.KEY_ROW_ID, DbConstants.Todo.KEY_TODO_ONLINE_ID,
        DbConstants.Todo.KEY_USER_ONLINE_ID, DbConstants.Todo.KEY_LIST_ONLINE_ID,
        DbConstants.Todo.KEY_TITLE, DbConstants.Todo.KEY_PRIORITY,
        DbConstants.Todo.KEY_DUE_DATE, DbConstants.Todo.KEY_REMINDER_DATETIME,
        DbConstants.Todo.KEY_DESCRIPTION, DbConstants.Todo.KEY_COMPLETED,
        DbConstants.Todo.KEY_ROW_VERSION, DbConstants.Todo.KEY_DELETED,
        DbConstants.Todo.KEY_DIRTY }, where, null, null, null, null);
    cursor.moveToFirst();
    ArrayList<Todo> todos = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      todos.add(getTodoByCursor(cursor));
      cursor.moveToNext();
    }
    cursor.close();
    return todos;
  }

  /**
   * Lekéri az adatbázisból, a todo táblából az összes olyan sort, amit be kell szúrni a távoli
   * adatbázisba. Feltételek: dirty == 1 és row_version == 0.
   * @return A beszúrandó Todo-kat tartalmazza.
   */
  public ArrayList<Todo> getInsertableTodos() {
    open();
    String where = DbConstants.Todo.KEY_USER_ONLINE_ID + "='" + getUserOnlineId() + "' AND " +
        DbConstants.Todo.KEY_DIRTY + "=" + 1 + " AND " +
        DbConstants.Todo.KEY_ROW_VERSION + "=" + 0;
    Cursor cursor = sqLiteDatabase.query(DbConstants.Todo.DATABASE_TABLE, new String[] {
        DbConstants.Todo.KEY_ROW_ID, DbConstants.Todo.KEY_TODO_ONLINE_ID,
        DbConstants.Todo.KEY_USER_ONLINE_ID, DbConstants.Todo.KEY_LIST_ONLINE_ID,
        DbConstants.Todo.KEY_TITLE, DbConstants.Todo.KEY_PRIORITY,
        DbConstants.Todo.KEY_DUE_DATE, DbConstants.Todo.KEY_REMINDER_DATETIME,
        DbConstants.Todo.KEY_DESCRIPTION, DbConstants.Todo.KEY_COMPLETED,
        DbConstants.Todo.KEY_ROW_VERSION, DbConstants.Todo.KEY_DELETED,
        DbConstants.Todo.KEY_DIRTY }, where, null, null, null, null);
    cursor.moveToFirst();
    ArrayList<Todo> todos = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      todos.add(getTodoByCursor(cursor));
      cursor.moveToNext();
    }
    cursor.close();
    return todos;
  }

  /**
   * Megvizsgálja, hogy az adatbázis tartalmazza-e a megadott todoOnlineId-t.
   * @param todoOnlineId A vizsgálandó todoOnlineId.
   * @return Ha az adatbázis tartalmazza a megadott todoOnlineId-t, akkor true, egyébként false.
   */
  public boolean isTodoExists(String todoOnlineId) {
    open();
    Cursor cursor = sqLiteDatabase.query(DbConstants.Todo.DATABASE_TABLE, new String[] {
            DbConstants.Todo.KEY_TODO_ONLINE_ID }, DbConstants.Todo.KEY_TODO_ONLINE_ID + "= ?",
        new String[] {todoOnlineId}, null, null, null);
    boolean exists = cursor.getCount() > 0;
    cursor.close();
    return exists;
  }

  /**
   * Lekéri az adatbázisból a todo tábla aktuális sorverzióját. Üres adattábla, illetve row_version
   * nélküli rekordokat tartalmazó adattábla esetén is megfelelően működik, mivel az int alapértel-
   * mezett értéke 0.
   * @return A todo tábla aktuális sorverziója.
   */
  public int getTodoRowVersion() {
    open();
    Cursor cursor = sqLiteDatabase.query(DbConstants.Todo.DATABASE_TABLE, new String[] {
        "MAX(" + DbConstants.Todo.KEY_ROW_VERSION + ")" }, null, null, null, null, null);
    cursor.moveToFirst();
    int row_version = cursor.getInt(0);
    return row_version;
  }

	/**
   * Lekéri az adatbázisból a megadott todoOnlineId-hoz tartozó Todo-t.
   * @param todoOnlineId A lekérendő Todo-hoz tartozó todoOnlineId.
   * @return A megadott todoOnlineId-hoz tartozó Todo vagy null.
   */
  public Todo getTodo(String todoOnlineId) {
    open();
    Cursor cursor = sqLiteDatabase.query(DbConstants.Todo.DATABASE_TABLE, new String[] {
        DbConstants.Todo.KEY_ROW_ID, DbConstants.Todo.KEY_TODO_ONLINE_ID,
        DbConstants.Todo.KEY_USER_ONLINE_ID, DbConstants.Todo.KEY_LIST_ONLINE_ID,
        DbConstants.Todo.KEY_TITLE, DbConstants.Todo.KEY_PRIORITY,
        DbConstants.Todo.KEY_DUE_DATE, DbConstants.Todo.KEY_REMINDER_DATETIME,
        DbConstants.Todo.KEY_DESCRIPTION, DbConstants.Todo.KEY_COMPLETED,
        DbConstants.Todo.KEY_ROW_VERSION, DbConstants.Todo.KEY_DELETED,
        DbConstants.Todo.KEY_DIRTY },
        DbConstants.Todo.KEY_TODO_ONLINE_ID + "='" + todoOnlineId + "'",
        null, null, null, null);
    if (cursor.moveToFirst())
      return getTodoByCursor(cursor);
    return null;
  }

  /**
   * Lekéri az adatbázisból a megadott _id-hoz tartozó Todo-t.
   * @param _id A lekérendő Todo-hoz tartozó _id.
   * @return A megadott _id-hoz tartozó Todo vagy null.
   */
  public Todo getTodo(Long _id) {
    open();
    Cursor cursor = sqLiteDatabase.query(DbConstants.Todo.DATABASE_TABLE, new String[] {
            DbConstants.Todo.KEY_ROW_ID, DbConstants.Todo.KEY_TODO_ONLINE_ID,
            DbConstants.Todo.KEY_USER_ONLINE_ID, DbConstants.Todo.KEY_LIST_ONLINE_ID,
            DbConstants.Todo.KEY_TITLE, DbConstants.Todo.KEY_PRIORITY,
            DbConstants.Todo.KEY_DUE_DATE, DbConstants.Todo.KEY_REMINDER_DATETIME,
            DbConstants.Todo.KEY_DESCRIPTION, DbConstants.Todo.KEY_COMPLETED,
            DbConstants.Todo.KEY_ROW_VERSION, DbConstants.Todo.KEY_DELETED,
            DbConstants.Todo.KEY_DIRTY },
        DbConstants.Todo.KEY_ROW_ID + "=" + _id,
        null, null, null, null);
    if (cursor.moveToFirst())
      return getTodoByCursor(cursor);
    return null;
  }

	/**
   * Lekéri az adatbázisból a megadott Cursor aktuális pozíciójában lévő Todo-t.
   * @param cursor A megadott Cursor.
   * @return A megadott Cursor aktuális pozíciójában lévő Todo.
   */
	public Todo getTodoByCursor(Cursor cursor) {
		return new Todo(
				cursor.getLong(0),
        cursor.getString(1),
        cursor.getString(2),
        cursor.getString(3),
        cursor.getString(4),
        cursor.getInt(5) != 0,
        cursor.getString(6),
        cursor.getString(7),
        cursor.getString(8),
        cursor.getInt(9) != 0,
        cursor.getInt(10),
        cursor.getInt(11) != 0,
        cursor.getInt(12) != 0
				);
	}

  /**
   * Törli az adatbázisból az adott Todo-t (soft delete).
   * @param todoOnlineId A törlendő Todo-hoz tartozó todoOnlineId.
   * @return Siker esetén true-t, egyébként false-t ad vissza.
   */
  public boolean softDeleteTodo(String todoOnlineId) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.Todo.KEY_DELETED, 1);
    contentValues.put(DbConstants.Todo.KEY_DIRTY, 1);
    boolean response = sqLiteDatabase.update(DbConstants.Todo.DATABASE_TABLE, contentValues,
        DbConstants.Todo.KEY_TODO_ONLINE_ID + "='" + todoOnlineId + "'", null) > 0;
    return response;
  }

  public boolean softDeleteTodo(Todo todoToSoftDelete) {
    String todoOnlineId = todoToSoftDelete.getTodoOnlineId();
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.Todo.KEY_DELETED, 1);
    contentValues.put(DbConstants.Todo.KEY_DIRTY, 1);
    boolean response = sqLiteDatabase.update(DbConstants.Todo.DATABASE_TABLE, contentValues,
        DbConstants.Todo.KEY_TODO_ONLINE_ID + "='" + todoOnlineId + "'", null) > 0;
    return response;
  }

  // ----------------- List tábla metódusai. ------------------------------------------------- //

  /**
   * Felveszi az adatbázisba a megadott List-et, siker esetén visszaadja a hozzá tartozó id-t.
   * @param list Az adatbázisba felveendő List.
   * @return Siker esetén a felvett List _id-ját adja vissza, egyébként -1-et.
   */
  public long createList(List list) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.List.KEY_LIST_ONLINE_ID, list.getListOnlineId());
    contentValues.put(DbConstants.List.KEY_USER_ONLINE_ID, list.getUserOnlineId());
    if (list.getCategoryOnlineId() == null) {
      // Null esetén null-t szúrunk be.
      contentValues.putNull(DbConstants.List.KEY_CATEGORY_ONLINE_ID);
    } else if (!list.getCategoryOnlineId().equals("")) {
      // Ha nem null és nem üres String, akkor magát a String-et szúrjuk be.
      contentValues.put(DbConstants.List.KEY_CATEGORY_ONLINE_ID, list.getCategoryOnlineId());
    } else {
      // Üres String esetén null-t szúrunk be.
      contentValues.putNull(DbConstants.List.KEY_CATEGORY_ONLINE_ID);
    }
    contentValues.put(DbConstants.List.KEY_TITLE, list.getTitle());
    contentValues.put(DbConstants.List.KEY_ROW_VERSION, list.getRowVersion());
    contentValues.put(DbConstants.List.KEY_DELETED, list.getDeleted() ? 1 : 0);
    contentValues.put(DbConstants.List.KEY_DIRTY, list.getDirty() ? 1 : 0);
    long response = sqLiteDatabase.insert(DbConstants.List.DATABASE_TABLE, null, contentValues);
    return response;
  }

  /**
   * Törli az adatbázisból a megadott List-et (soft delete).
   * @param listOnlineId A törlendő List-hez tartozó listOnlineId.
   * @return Siker esetén true-t, egyébként false-t ad vissza.
   */
  public boolean deleteList(String listOnlineId) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.List.KEY_DELETED, 1);
    contentValues.put(DbConstants.List.KEY_DIRTY, 1);
    boolean response = sqLiteDatabase.update(DbConstants.List.DATABASE_TABLE, contentValues,
        DbConstants.List.KEY_LIST_ONLINE_ID + "='" + listOnlineId + "'", null) > 0;
    return response;
  }

  /**
   * Törli az adatbázisból a megadott listOnlineId-hoz tartozó List-et és a hozzá tartozó Todo-kat.
   * @param listOnlineId Az adott List-hez tartozó listOnlineId.
   */
  public void deleteListAndTodos(String listOnlineId) {
    ArrayList<String> todoOnlineIds = getTodoOnlineIdsByListOnlineId(listOnlineId);
    // Ha az adott List-hez tartozik Todo, akkor töröljük azt.
    if (!todoOnlineIds.isEmpty()) {
      for (String todoOnlineId : todoOnlineIds) {
        softDeleteTodo(todoOnlineId);
      }
    }
    deleteList(listOnlineId);
  }

  /**
   * Frissíti az adatbázisban a megadott List-et.
   * @param list A frissítendő List.
   * @return Siker esetén true-t, egyébként false-t ad vissza.
   */
  public boolean updateList(List list) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.List.KEY_LIST_ONLINE_ID, list.getListOnlineId());
    contentValues.put(DbConstants.List.KEY_USER_ONLINE_ID, list.getUserOnlineId());
    if (list.getCategoryOnlineId() == null) {
      // Null esetén null-t szúrunk be.
      contentValues.putNull(DbConstants.List.KEY_CATEGORY_ONLINE_ID);
    } else if (!list.getCategoryOnlineId().equals("")) {
      // Ha nem null és nem üres String, akkor magát a String-et szúrjuk be.
      contentValues.put(DbConstants.List.KEY_CATEGORY_ONLINE_ID, list.getCategoryOnlineId());
    } else {
      // Üres String esetén null-t szúrunk be.
      contentValues.putNull(DbConstants.List.KEY_CATEGORY_ONLINE_ID);
    }
    contentValues.put(DbConstants.List.KEY_TITLE, list.getTitle());
    contentValues.put(DbConstants.List.KEY_ROW_VERSION, list.getRowVersion());
    contentValues.put(DbConstants.List.KEY_DELETED, list.getDeleted());
    contentValues.put(DbConstants.List.KEY_DIRTY, list.getDirty());

    if (list.get_id() != 0) {
      // Offline módosítottuk a List-et (a list_online_id a helyi adatbázisban még null).
      return sqLiteDatabase.update(DbConstants.List.DATABASE_TABLE, contentValues,
          DbConstants.List.KEY_ROW_ID + "=" + list.get_id(), null) > 0;
    } else {
      // Online módosítottuk a List-et (az _id ismeretlen).
      return sqLiteDatabase.update(DbConstants.List.DATABASE_TABLE, contentValues,
          DbConstants.List.KEY_LIST_ONLINE_ID + "='" + list.getListOnlineId() + "'", null) > 0;
    }
  }

  /**
   * Lekéri az adatbázisból az összes Category-hez nem tartozó List-et, cím szerint növekvő
   * rendezettséggel.
   * @return Az összes Category-hez nem tartozó List, cím szerint növekvő rendezettséggel.
   */
  public ArrayList<List> getListsNotInCategory() {
    open();
    Cursor cursor = sqLiteDatabase.query(DbConstants.List.DATABASE_TABLE, new String[] {
        DbConstants.List.KEY_ROW_ID, DbConstants.List.KEY_LIST_ONLINE_ID,
        DbConstants.List.KEY_USER_ONLINE_ID, DbConstants.List.KEY_CATEGORY_ONLINE_ID,
        DbConstants.List.KEY_TITLE, DbConstants.List.KEY_ROW_VERSION,
        DbConstants.List.KEY_DELETED, DbConstants.List.KEY_DIRTY },
        DbConstants.List.KEY_USER_ONLINE_ID + "='" + getUserOnlineId() + "' AND " +
        DbConstants.List.KEY_CATEGORY_ONLINE_ID + " IS NULL" + " AND " +
        DbConstants.List.KEY_DELETED + "=" + 0, null, null, null,
        DbConstants.List.KEY_TITLE);
    cursor.moveToFirst();
    ArrayList<List> lists = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      lists.add(getListByCursor(cursor));
      cursor.moveToNext();
    }
    cursor.close();
    return lists;
  }

  /**
   * Lekéri az adatbázisból az adott Category-hez tartozó összes List-et.
   * @param categoryOnlineId Az adott Category-hez tartozó categoryOnlineId.
   * @return Az adott Category-hoz tartozó List-ek.
   */
  public ArrayList<List> getListsByCategoryOnlineId(String categoryOnlineId) {
    open();
    Cursor cursor = sqLiteDatabase.query(DbConstants.List.DATABASE_TABLE, new String[]{
        DbConstants.List.KEY_ROW_ID, DbConstants.List.KEY_LIST_ONLINE_ID,
        DbConstants.List.KEY_USER_ONLINE_ID, DbConstants.List.KEY_CATEGORY_ONLINE_ID,
        DbConstants.List.KEY_TITLE, DbConstants.List.KEY_ROW_VERSION,
        DbConstants.List.KEY_DELETED, DbConstants.List.KEY_DIRTY },
        DbConstants.List.KEY_USER_ONLINE_ID + "='" + getUserOnlineId() + "' AND " +
        DbConstants.List.KEY_CATEGORY_ONLINE_ID + "='" + categoryOnlineId + "'" + " AND " +
        DbConstants.List.KEY_DELETED + "=" + 0,
        null, null, null,
        DbConstants.List.KEY_TITLE);
    cursor.moveToFirst();
    ArrayList<List> lists = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      lists.add(getListByCursor(cursor));
      cursor.moveToNext();
    }
    cursor.close();
    return lists;
  }

  /**
   * Megvizsgálja, hogy az adatbázis tartalmazza-e a megadott listOnlineId-t.
   * @param listOnlineId A vizsgálandó listOnlineId.
   * @return Ha az adatbázis tartalmazza a megadott listOnlineId-t, akkor true, egyébként false.
   */
  public boolean isListExists(String listOnlineId) {
    open();
    Cursor cursor = sqLiteDatabase.query(DbConstants.List.DATABASE_TABLE, new String[] {
            DbConstants.List.KEY_LIST_ONLINE_ID }, DbConstants.List.KEY_LIST_ONLINE_ID + "= ?",
        new String[] {listOnlineId}, null, null, null);
    boolean exists = cursor.getCount() > 0;
    cursor.close();
    return exists;
  }

  /**
   * Lekéri az adatbázisból a list tábla aktuális sorverzióját. Üres adattábla, illetve row_version
   * nélküli rekordokat tartalmazó adattábla esetén is megfelelően működik, mivel az int alapértel-
   * mezett értéke 0.
   * @return A list tábla aktuális sorverziója.
   */
  public int getListRowVersion() {
    open();
    Cursor cursor = sqLiteDatabase.query(DbConstants.List.DATABASE_TABLE, new String[] {
        "MAX(" + DbConstants.List.KEY_ROW_VERSION + ")" }, null, null, null, null, null);
    cursor.moveToFirst();
    int row_version = cursor.getInt(0);
    return row_version;
  }

  /**
   * Lekéri az adatbázisból, a list táblából az összes olyan sort, amit updatelni kell a távoli
   * adatbázisban. Feltételek: dirty == 1 és row_version > 0.
   * @return A frissítendő List-eket tartalmazza.
   */
  public ArrayList<List> getUpdatableLists() {
    open();
    String where = DbConstants.List.KEY_USER_ONLINE_ID + "='" + getUserOnlineId() + "' AND " +
        DbConstants.List.KEY_DIRTY + "=" + 1 + " AND "
        + DbConstants.List.KEY_ROW_VERSION + ">" + 0;
    Cursor cursor = sqLiteDatabase.query(DbConstants.List.DATABASE_TABLE, new String[]{
        DbConstants.List.KEY_ROW_ID, DbConstants.List.KEY_LIST_ONLINE_ID,
        DbConstants.List.KEY_USER_ONLINE_ID, DbConstants.List.KEY_CATEGORY_ONLINE_ID,
        DbConstants.List.KEY_TITLE, DbConstants.List.KEY_ROW_VERSION,
        DbConstants.List.KEY_DELETED, DbConstants.List.KEY_DIRTY }, where, null, null, null, null);
    cursor.moveToFirst();
    ArrayList<List> lists = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      lists.add(getListByCursor(cursor));
      cursor.moveToNext();
    }
    cursor.close();
    return lists;
  }

  /**
   * Lekéri az adatbázisból, a list táblából az összes olyan sort, amit be kell szúrni a távoli
   * adatbázisba. Feltételek: dirty == 1 és row_version == 0.
   * @return A beszúrandó List-eket tartalmazza.
   */
  public ArrayList<List> getInsertableLists() {
    open();
    String where = DbConstants.List.KEY_USER_ONLINE_ID + "='" + getUserOnlineId() + "' AND " +
        DbConstants.List.KEY_DIRTY + "=" + 1 + " AND " +
        DbConstants.List.KEY_ROW_VERSION + "=" + 0;
    Cursor cursor = sqLiteDatabase.query(DbConstants.List.DATABASE_TABLE, new String[] {
        DbConstants.List.KEY_ROW_ID, DbConstants.List.KEY_LIST_ONLINE_ID,
        DbConstants.List.KEY_USER_ONLINE_ID, DbConstants.List.KEY_CATEGORY_ONLINE_ID,
        DbConstants.List.KEY_TITLE, DbConstants.List.KEY_ROW_VERSION,
        DbConstants.List.KEY_DELETED, DbConstants.List.KEY_DIRTY }, where, null, null, null, null);
    cursor.moveToFirst();
    ArrayList<List> lists = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      lists.add(getListByCursor(cursor));
      cursor.moveToNext();
    }
    cursor.close();
    return lists;
  }

  /**
   * Lekéri az adatbázisból a megadott Cursor aktuális pozíciójában lévő List-et.
   * @param cursor A megadott Cursor.
   * @return A megadott Cursor aktuális pozíciójában lévő List.
   */
  public List getListByCursor(Cursor cursor) {
    return new List(
        cursor.getLong(0),
        cursor.getString(1),
        cursor.getString(2),
        cursor.getString(3),
        cursor.getString(4),
        cursor.getInt(5),
        cursor.getInt(6) != 0,
        cursor.getInt(7) != 0
    );
  }

  // ----------------- Category tábla metódusai. ---------------------------------------------- //

  /**
   * Felveszi az adatbázisba a megadott Category-t, siker esetén visszaadja a hozzá tartozó id-t.
   * @param category Az adatbázisba felveendő Category.
   * @return Siker esetén a felvett Category _id-ját adja vissza, egyébként -1-et.
   */
  public long createCategory(Category category) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.Category.KEY_CATEGORY_ONLINE_ID, category.getCategoryOnlineId());
    contentValues.put(DbConstants.Category.KEY_USER_ONLINE_ID, category.getUserOnlineId());
    contentValues.put(DbConstants.Category.KEY_TITLE, category.getTitle());
    contentValues.put(DbConstants.Category.KEY_ROW_VERSION, category.getRowVersion());
    contentValues.put(DbConstants.Category.KEY_DELETED, category.getDeleted() ? 1 : 0);
    contentValues.put(DbConstants.Category.KEY_DIRTY, category.getDirty() ? 1 : 0);
    long response = sqLiteDatabase.insert(DbConstants.Category.DATABASE_TABLE, null,
        contentValues);
    return response;
  }

  /**
   * Törli az adatbázisból az adott Category-t (soft delete).
   * @param categoryOnlineId A törlendő Category-hoz tartozó categoryOnlineId.
   * @return Siker esetén true-t, egyébként false-t ad vissza.
   */
  public boolean deleteCategory(String categoryOnlineId) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.Category.KEY_DELETED, 1);
    contentValues.put(DbConstants.Category.KEY_DIRTY, 1);
    boolean response = sqLiteDatabase.update(DbConstants.Category.DATABASE_TABLE, contentValues,
        DbConstants.Category.KEY_CATEGORY_ONLINE_ID + "='" + categoryOnlineId + "'", null) > 0;
    return response;
  }

  /**
   * Törli az adatbázisból az adott Category-t, a hozzá tartozó List-ekkel és Todo-kkal együtt.
   * @param categoryOnlineId Az adott Category-hez tartozó categoryOnlineId.
   */
  public void deleteCategoryAndListsAndTodos(String categoryOnlineId) {
    ArrayList<List> lists = getListsByCategoryOnlineId(categoryOnlineId);
    // Ha az adott Category-hez tartozik List, akkor töröljük azt.
    if (!lists.isEmpty()) {
      for (List list:lists) {
        deleteListAndTodos(list.getListOnlineId());
      }
    }
    deleteCategory(categoryOnlineId);
  }

  /**
   * Frissíti az adatbázisban a megadott Category-t.
   * @param category A frissítendő Category.
   * @return Siker esetén true-t, egyébként false-t ad vissza.
   */
  public boolean updateCategory(Category category) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.Category.KEY_CATEGORY_ONLINE_ID, category.getCategoryOnlineId());
    contentValues.put(DbConstants.Category.KEY_USER_ONLINE_ID, category.getUserOnlineId());
    contentValues.put(DbConstants.Category.KEY_TITLE, category.getTitle());
    contentValues.put(DbConstants.Category.KEY_ROW_VERSION, category.getRowVersion());
    contentValues.put(DbConstants.Category.KEY_DELETED, category.getDeleted() ? 1 : 0);
    contentValues.put(DbConstants.Category.KEY_DIRTY, category.getDirty() ? 1 : 0);

    if (category.get_id() != 0) {
      // Offline módosítottuk a Category-t (a category_online_id a helyi adatbázisban még null).
      return sqLiteDatabase.update(DbConstants.Category.DATABASE_TABLE, contentValues,
          DbConstants.Category.KEY_ROW_ID + "=" + category.get_id(), null) > 0;
    } else {
      // Online módosítottuk a Category-t (az _id ismeretlen).
      return sqLiteDatabase.update(DbConstants.Category.DATABASE_TABLE, contentValues,
          DbConstants.Category.KEY_CATEGORY_ONLINE_ID + "='" + category.getCategoryOnlineId()
              + "'", null) > 0;
    }
  }

  /**
   * Lekéri az adatbázisból a megadott categoryOnlineId-hoz tartozó Category-t.
   * @param categoryOnlineId A lekérendő Category-hoz tartozó categoryOnlineId.
   * @return A megadott categoryOnlineId-hoz tartozó Category.
   */
  public Category getCategoryByCategoryOnlineId(String categoryOnlineId) {
    open();
    Cursor cursor = sqLiteDatabase.query(DbConstants.Category.DATABASE_TABLE, new String[]{
        DbConstants.Category.KEY_ROW_ID, DbConstants.Category.KEY_CATEGORY_ONLINE_ID,
        DbConstants.Category.KEY_USER_ONLINE_ID, DbConstants.Category.KEY_TITLE,
        DbConstants.Category.KEY_ROW_VERSION, DbConstants.Category.KEY_DELETED,
        DbConstants.Category.KEY_DIRTY },
        DbConstants.Category.KEY_USER_ONLINE_ID + "='" + getUserOnlineId() + "'" + " AND " +
        DbConstants.Category.KEY_DELETED + "=" + 0 + " AND " +
        DbConstants.Category.KEY_CATEGORY_ONLINE_ID + "='" + categoryOnlineId + "'",
        null, null, null, null);
    cursor.moveToFirst();
    Category category = getCategoryByCursor(cursor);
    cursor.close();
    return category;
  }

  /**
   * Lekéri az adatbázisból a jelenlegi User-hez tartozó összes Category-t, cím szerinti növekvő
   * rendezettséggel.
   * @return A jelenlegi User-hez tartozó Category-k, cím szerinti növekvő rendezettséggel.
   */
  public ArrayList<Category> getCategories() {
    open();
    Cursor cursor = sqLiteDatabase.query(DbConstants.Category.DATABASE_TABLE, new String[]{
        DbConstants.Category.KEY_ROW_ID, DbConstants.Category.KEY_CATEGORY_ONLINE_ID,
        DbConstants.Category.KEY_USER_ONLINE_ID, DbConstants.Category.KEY_TITLE,
        DbConstants.Category.KEY_ROW_VERSION, DbConstants.Category.KEY_DELETED,
        DbConstants.Category.KEY_DIRTY },
        DbConstants.Category.KEY_USER_ONLINE_ID + "='" + getUserOnlineId() + "'" + " AND " +
        DbConstants.Category.KEY_DELETED + "=" + 0,
        null, null, null,
        DbConstants.Category.KEY_TITLE);
    cursor.moveToFirst();
    ArrayList<Category> categories = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      categories.add(getCategoryByCursor(cursor));
      cursor.moveToNext();
    }
    cursor.close();
    return categories;
  }

  /**
   * Lekéri az adatbázisból, a category táblából az összes olyan sort, amit updatelni kell a
   * távoli adatbázisban. Feltételek: dirty == 1 és row_version > 0.
   * @return A frissítendő Category-ket tartalmazza.
   */
  public ArrayList<Category> getUpdatableCategories() {
    open();
    String where = DbConstants.Category.KEY_USER_ONLINE_ID + "='" + getUserOnlineId() + "' AND " +
        DbConstants.Category.KEY_DIRTY + "=" + 1 + " AND "
        + DbConstants.Category.KEY_ROW_VERSION + ">" + 0;
    Cursor cursor = sqLiteDatabase.query(DbConstants.Category.DATABASE_TABLE, new String[]{
        DbConstants.Category.KEY_ROW_ID, DbConstants.Category.KEY_CATEGORY_ONLINE_ID,
        DbConstants.Category.KEY_USER_ONLINE_ID, DbConstants.Category.KEY_TITLE,
        DbConstants.Category.KEY_ROW_VERSION, DbConstants.Category.KEY_DELETED,
        DbConstants.Category.KEY_DIRTY }, where, null, null, null, null);
    cursor.moveToFirst();
    ArrayList<Category> categories = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      categories.add(getCategoryByCursor(cursor));
      cursor.moveToNext();
    }
    cursor.close();
    return categories;
  }

  /**
   * Lekéri az adatbázisból, a category táblából az összes olyan sort, amit be kell szúrni a
   * távoli adatbázisba. Feltételek: dirty == 1 és row_version == 0.
   * @return A beszúrandó Category-kat tartalmazza.
   */
  public ArrayList<Category> getInsertableCategories() {
    open();
    String where = DbConstants.Category.KEY_USER_ONLINE_ID + "='" + getUserOnlineId() + "' AND " +
        DbConstants.Category.KEY_DIRTY + "=" + 1 + " AND " +
        DbConstants.Category.KEY_ROW_VERSION + "=" + 0;
    Cursor cursor = sqLiteDatabase.query(DbConstants.Category.DATABASE_TABLE, new String[] {
        DbConstants.Category.KEY_ROW_ID, DbConstants.Category.KEY_CATEGORY_ONLINE_ID,
        DbConstants.Category.KEY_USER_ONLINE_ID, DbConstants.Category.KEY_TITLE,
        DbConstants.Category.KEY_ROW_VERSION, DbConstants.Category.KEY_DELETED,
        DbConstants.Category.KEY_DIRTY }, where, null, null, null, null);
    cursor.moveToFirst();
    ArrayList<Category> categories = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      categories.add(getCategoryByCursor(cursor));
      cursor.moveToNext();
    }
    cursor.close();
    return categories;
  }

  /**
   * Megvizsgálja, hogy az adatbázis tartalmazza-e a megadott categoryOnlineId-t.
   * @param categoryOnlineId A vizsgálandó categoryOnlineId.
   * @return Ha az adatbázis tartalmazza a megadott categoryOnlineId-t, akkor true, egyébként
   * false.
   */
  public boolean isCategoryExists(String categoryOnlineId) {
    open();
    Cursor cursor = sqLiteDatabase.query(DbConstants.Category.DATABASE_TABLE, new String[] {
            DbConstants.Category.KEY_CATEGORY_ONLINE_ID },
        DbConstants.Category.KEY_CATEGORY_ONLINE_ID + "= ?",
        new String[] {categoryOnlineId}, null, null, null);
    boolean exists = cursor.getCount() > 0;
    cursor.close();
    return exists;
  }

  /**
   * Lekéri az adatbázisból a category tábla aktuális sorverzióját. Üres adattábla, illetve
   * row_version nélküli rekordokat tartalmazó adattábla esetén is megfelelően működik, mivel az
   * int alapértelmezett értéke 0.
   * @return A category tábla aktuális sorverziója.
   */
  public int getCategoryRowVersion() {
    open();
    Cursor cursor = sqLiteDatabase.query(DbConstants.Category.DATABASE_TABLE, new String[] {
        "MAX(" + DbConstants.Category.KEY_ROW_VERSION + ")" }, null, null, null, null, null);
    cursor.moveToFirst();
    int row_version = cursor.getInt(0);
    return row_version;
  }

  /**
   * Lekéri az adatbázisból a Cursor aktuális pozíciójában lévő Category-t.
   * @param cursor A megadott Cursor.
   * @return A megadott Cursor aktuális pozícióján lévő Category.
   */
  public Category getCategoryByCursor(Cursor cursor) {
    return new Category(
        cursor.getLong(0),
        cursor.getString(1),
        cursor.getString(2),
        cursor.getString(3),
        cursor.getInt(4),
        cursor.getInt(5) != 0,
        cursor.getInt(6) != 0
    );
  }

}
