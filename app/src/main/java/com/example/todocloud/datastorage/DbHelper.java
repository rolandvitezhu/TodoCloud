package com.example.todocloud.datastorage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

	public DbHelper(Context context) {
	  super(context, DbConstants.DATABASE_NAME, null, DbConstants.DATABASE_VERSION);
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
