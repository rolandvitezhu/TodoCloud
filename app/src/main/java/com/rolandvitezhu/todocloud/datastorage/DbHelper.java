package com.rolandvitezhu.todocloud.datastorage;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.rolandvitezhu.todocloud.app.AppController;

import java.util.Objects;

import javax.inject.Inject;

public class DbHelper extends SQLiteOpenHelper {

  @Inject
  DbLoader dbLoader;

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
    Objects.requireNonNull(AppController.Companion.getInstance()).getAppComponent().inject(this);
  }

	@Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(DbConstants.User.CREATE_TABLE);
    db.execSQL(DbConstants.Todo.CREATE_TABLE_2);
    db.execSQL(DbConstants.List.CREATE_TABLE_3);
    db.execSQL(DbConstants.Category.CREATE_TABLE_3);
  }

	@Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 2) {
      db.execSQL(DbConstants.Todo.MIGRATE_TABLE_2_PART_1);
      db.execSQL(DbConstants.Todo.MIGRATE_TABLE_2_PART_2);
      dbLoader.fixTodoPositions(db);
      db.execSQL(DbConstants.Todo.MIGRATE_TABLE_2_PART_3);
      db.execSQL(DbConstants.Todo.MIGRATE_TABLE_2_PART_4);
      db.execSQL(DbConstants.Todo.MIGRATE_TABLE_2_PART_5);
    }
    if (oldVersion < 3) {
      db.execSQL(DbConstants.List.MIGRATE_TABLE_3_PART_1);
      db.execSQL(DbConstants.List.MIGRATE_TABLE_3_PART_2);
      db.execSQL(DbConstants.List.MIGRATE_TABLE_3_PART_3);
      db.execSQL(DbConstants.List.MIGRATE_TABLE_3_PART_4);
      db.execSQL(DbConstants.List.MIGRATE_TABLE_3_PART_5);

      db.execSQL(DbConstants.Category.MIGRATE_TABLE_3_PART_1);
      db.execSQL(DbConstants.Category.MIGRATE_TABLE_3_PART_2);
      db.execSQL(DbConstants.Category.MIGRATE_TABLE_3_PART_3);
      db.execSQL(DbConstants.Category.MIGRATE_TABLE_3_PART_4);
      db.execSQL(DbConstants.Category.MIGRATE_TABLE_3_PART_5);
    }
  }

}
