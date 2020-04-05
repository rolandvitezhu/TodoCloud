package com.rolandvitezhu.todocloud.datastorage;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.rolandvitezhu.todocloud.app.AppController;

public class DbHelper extends SQLiteOpenHelper {

  private static DbHelper instance;

  public static synchronized DbHelper getInstance() {
    if (instance == null) instance = new DbHelper();
    return instance;
  }

	private DbHelper() {
	  super(
        AppController.Companion.getAppContext(),
        DbConstants.DATABASE_NAME,
        null,
        DbConstants.DATABASE_VERSION
    );
  }

	@Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(DbConstants.User.DATABASE_CREATE);
    db.execSQL(DbConstants.Todo.DATABASE_CREATE);
    db.execSQL(DbConstants.List.DATABASE_CREATE);
    db.execSQL(DbConstants.Category.DATABASE_CREATE);
  }

	@Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL(DbConstants.User.DATABASE_DROP);
    db.execSQL(DbConstants.Todo.DATABASE_DROP);
    db.execSQL(DbConstants.List.DATABASE_DROP);
    db.execSQL(DbConstants.Category.DATABASE_DROP);
    onCreate(db);
  }

}
