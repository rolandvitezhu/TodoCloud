package com.example.todocloud.datastorage;

public class DbConstants {

	public static final String DATABASE_NAME = "todo_cloud";
	public static final int DATABASE_VERSION = 1;

  public static class User {

    public static final String DATABASE_TABLE = "user";
    public static final String KEY_ROW_ID = "_id";
    public static final String KEY_USER_ONLINE_ID = "user_online_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_API_KEY = "api_key";
    public static final String DATABASE_CREATE = "create table if not exists "
        + DATABASE_TABLE
        + " ( "
        + KEY_ROW_ID
        + " integer primary key autoincrement, "
        + KEY_USER_ONLINE_ID
        + " text default null, "
        + KEY_NAME
        + " text default null, "
        + KEY_EMAIL
        + " text default null, "
        + KEY_API_KEY
        + " text default null"
        + "); ";
    public static final String DATABASE_DROP = "drop table if exists "
        + DATABASE_TABLE
        + "; ";

  }
	
	public static class Todo {
		
		public static final String DATABASE_TABLE = "layout_appbar_todolist";
		public static final String KEY_ROW_ID = "_id";
    public static final String KEY_TODO_ONLINE_ID = "todo_online_id";
    public static final String KEY_USER_ONLINE_ID = "user_online_id";
    public static final String KEY_LIST_ONLINE_ID = "list_online_id";
		public static final String KEY_TITLE = "title";
		public static final String KEY_PRIORITY = "priority";
		public static final String KEY_DUE_DATE = "due_date";
    public static final String KEY_REMINDER_DATETIME = "reminder_datetime";
		public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_COMPLETED = "completed";
    public static final String KEY_ROW_VERSION = "row_version";
    public static final String KEY_DELETED = "deleted";
    public static final String KEY_DIRTY = "dirty";
		public static final String DATABASE_CREATE = "create table if not exists "
				+ DATABASE_TABLE
				+ " ( "
				+ KEY_ROW_ID
				+ " integer primary key autoincrement, "
        + KEY_TODO_ONLINE_ID
        + " text default null, "
        + KEY_USER_ONLINE_ID
        + " text default null, "
        + KEY_LIST_ONLINE_ID
        + " text default null, "
				+ KEY_TITLE
				+ " text default null, "
				+ KEY_PRIORITY
				+ " integer default 0, "
				+ KEY_DUE_DATE
				+ " text default null, "
        + KEY_REMINDER_DATETIME
        + " text default null, "
				+ KEY_DESCRIPTION
				+ " text default null, "
        + KEY_COMPLETED
        + " integer default 0, "
        + KEY_ROW_VERSION
        + " integer default 0, "
        + KEY_DELETED
        + " integer default 0, "
        + KEY_DIRTY
        + " integer default 1"
				+ "); ";
		public static final String DATABASE_DROP = "drop table if exists "
				+ DATABASE_TABLE
				+ "; ";
		
	}

  public static class List {

    public static final String DATABASE_TABLE = "list";
    public static final String KEY_ROW_ID = "_id";
    public static final String KEY_LIST_ONLINE_ID = "list_online_id";
    public static final String KEY_USER_ONLINE_ID = "user_online_id";
    public static final String KEY_CATEGORY_ONLINE_ID = "category_online_id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_ROW_VERSION = "row_version";
    public static final String KEY_DELETED = "deleted";
    public static final String KEY_DIRTY = "dirty";
    public static final String DATABASE_CREATE = "create table if not exists "
        + DATABASE_TABLE
        + " ( "
        + KEY_ROW_ID
        + " integer primary key autoincrement, "
        + KEY_LIST_ONLINE_ID
        + " text default null, "
        + KEY_USER_ONLINE_ID
        + " text default null, "
        + KEY_CATEGORY_ONLINE_ID
        + " text default null, "
        + KEY_TITLE
        + " text default null, "
        + KEY_ROW_VERSION
        + " integer default 0, "
        + KEY_DELETED
        + " integer default 0, "
        + KEY_DIRTY
        + " integer default 1"
        + "); ";
    public static final String DATABASE_DROP = "drop table if exists "
        + DATABASE_TABLE
        + "; ";

  }

  public static class Category {

    public static final String DATABASE_TABLE = "category";
    public static final String KEY_ROW_ID = "_id";
    public static final String KEY_CATEGORY_ONLINE_ID = "category_online_id";
    public static final String KEY_USER_ONLINE_ID = "user_online_id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_ROW_VERSION = "row_version";
    public static final String KEY_DELETED = "deleted";
    public static final String KEY_DIRTY = "dirty";
    public static final String DATABASE_CREATE = "create table if not exists "
        + DATABASE_TABLE
        + " ( "
        + KEY_ROW_ID
        + " integer primary key autoincrement, "
        + KEY_CATEGORY_ONLINE_ID
        + " text default null, "
        + KEY_USER_ONLINE_ID
        + " text default null, "
        + KEY_TITLE
        + " text default null, "
        + KEY_ROW_VERSION
        + " integer default 0, "
        + KEY_DELETED
        + " integer default 0, "
        + KEY_DIRTY
        + " integer default 1"
        + "); ";
    public static final String DATABASE_DROP = "drop table if exists "
        + DATABASE_TABLE
        + "; ";

  }
	
}
