package com.rolandvitezhu.todocloud.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.data.Category
import com.rolandvitezhu.todocloud.data.List
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.data.User
import kotlinx.coroutines.runBlocking

@Database(entities = [User::class, Todo::class, List::class, Category::class],
        version = 4, exportSchema = true)
abstract class TodoCloudDatabase : RoomDatabase() {

    abstract val todoCloudDatabaseDao: TodoCloudDatabaseDao

    companion object {
        @Volatile
        private var INSTANCE: TodoCloudDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE " +
                                "   temp_table " +
                                "( " +
                                "   _id integer primary key autoincrement, " +
                                "   todo_online_id text default null, " +
                                "   user_online_id text default null, " +
                                "   list_online_id text default null, " +
                                "   title text default null, " +
                                "   priority integer default 0, " +
                                "   due_date integer default null, " +
                                "   reminder_date_time integer default null, " +
                                "   description text default null, " +
                                "   completed integer default 0, " +
                                "   row_version integer default 0, " +
                                "   deleted integer default 0, " +
                                "   dirty integer default 1, " +
                                "   position real" +
                                "); ")
                database.execSQL("UPDATE " +
                                "   todo " +
                                "SET " +
                                "   position = 5 " +
                                "WHERE " +
                                "   position = 0.0 OR position IS NULL; ")
                runBlocking {
                    if (this@Companion.INSTANCE != null)
                        this@Companion.INSTANCE!!.todoCloudDatabaseDao.fixTodoPositions()
                    else if (AppController != null && AppController.appContext != null &&
                            AppController.appContext.applicationContext != null)
                        getInstance(AppController.appContext.applicationContext)
                                .todoCloudDatabaseDao.fixTodoPositions()
                }
                database.execSQL("INSERT INTO " +
                                "   temp_table " +
                                "SELECT " +
                                "   _id, " +
                                "   todo_online_id, " +
                                "   user_online_id, " +
                                "   list_online_id, " +
                                "   title, " +
                                "   priority, " +
                                "   due_date, " +
                                "   reminder_date_time, " +
                                "   description, " +
                                "   completed, " +
                                "   row_version, " +
                                "   deleted, " +
                                "   dirty, " +
                                "   CAST(ABS(position) AS REAL) " +
                                "FROM " +
                                "   todo; ")
                database.execSQL("DROP TABLE " +
                                "   todo; ")
                database.execSQL("ALTER TABLE " +
                        "   temp_table " +
                        "RENAME TO " +
                        "   todo; ")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE " +
                                "   temp_table " +
                                "( " +
                                "   _id integer primary key autoincrement, " +
                                "   list_online_id text default null, " +
                                "   user_online_id text default null, " +
                                "   category_online_id text default null, " +
                                "   title text default null, " +
                                "   row_version integer default 0, " +
                                "   deleted integer default 0, " +
                                "   dirty integer default 1, " +
                                "   position real " +
                                "); ")
                database.execSQL("UPDATE " +
                        "   list " +
                        "SET " +
                        "   position = 5 " +
                        "WHERE " +
                        "   position = 0.0 OR position IS NULL; ")
                database.execSQL("INSERT INTO " +
                                "   temp_table " +
                                "SELECT " +
                                "   _id, " +
                                "   list_online_id, " +
                                "   user_online_id, " +
                                "   category_online_id, " +
                                "   title, " +
                                "   row_version, " +
                                "   deleted, " +
                                "   dirty, " +
                                "   CAST(ABS(position) AS REAL) " +
                                "FROM " +
                                "   list; ")
                database.execSQL("DROP TABLE " +
                                "   list; ")
                database.execSQL("ALTER TABLE " +
                                "   temp_table " +
                                "RENAME TO " +
                                "   list; ")

                database.execSQL("CREATE TABLE " +
                                "   temp_table " +
                                "( " +
                                "   _id integer primary key autoincrement, " +
                                "   category_online_id text default null, " +
                                "   user_online_id text default null, " +
                                "   title text default null, " +
                                "   row_version integer default 0, " +
                                "   deleted integer default 0, " +
                                "   dirty integer default 1, " +
                                "   position real " +
                                "); ")
                database.execSQL("UPDATE " +
                                "   category " +
                                "SET " +
                                "   position = 5 " +
                                "WHERE " +
                                "   position = 0.0 OR position IS NULL; ")
                database.execSQL("INSERT INTO " +
                                "   temp_table " +
                                "SELECT " +
                                "   _id, " +
                                "   category_online_id, " +
                                "   user_online_id, " +
                                "   title, " +
                                "   row_version, " +
                                "   deleted, " +
                                "   dirty, " +
                                "   CAST(ABS(position) AS REAL) " +
                                "FROM " +
                                "   category; ")
                database.execSQL("DROP TABLE " +
                                "   category; ")
                database.execSQL("ALTER TABLE " +
                        "   temp_table " +
                        "RENAME TO " +
                        "   category; ")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE " +
                        "   temp_table " +
                        "(  " +
                        "   _id integer primary key autoincrement, " +
                        "   user_online_id text default null, " +
                        "   name text not null, " +
                        "   email text not null, " +
                        "   api_key text default null " +
                        "); ")
                database.execSQL("INSERT INTO " +
                        "   temp_table " +
                        "SELECT " +
                        "   _id, " +
                        "   user_online_id, " +
                        "   name, " +
                        "   email, " +
                        "   api_key " +
                        "FROM " +
                        "   user; ")
                database.execSQL("DROP TABLE" +
                        "   user; ")
                database.execSQL("ALTER TABLE " +
                        "   temp_table " +
                        "RENAME TO " +
                        "   user; ")

                database.execSQL("CREATE TABLE " +
                        "   temp_table " +
                        "(  " +
                        "   _id integer primary key autoincrement, " +
                        "   todo_online_id text default null, " +
                        "   user_online_id text default null, " +
                        "   list_online_id text default null, " +
                        "   title text default null, " +
                        "   priority integer default null, " +
                        "   due_date integer not null, " +
                        "   reminder_date_time integer not null, " +
                        "   description text default null, " +
                        "   completed integer default 0, " +
                        "   row_version integer not null, " +
                        "   deleted integer default null, " +
                        "   dirty integer not null, " +
                        "   position real not null " +
                        "); ")
                database.execSQL("INSERT INTO " +
                        "   temp_table " +
                        "SELECT " +
                        "   _id, " +
                        "   todo_online_id, " +
                        "   user_online_id, " +
                        "   list_online_id, " +
                        "   title, " +
                        "   priority, " +
                        "   due_date, " +
                        "   reminder_date_time, " +
                        "   description, " +
                        "   completed, " +
                        "   row_version, " +
                        "   deleted, " +
                        "   dirty, " +
                        "   position " +
                        "FROM " +
                        "   todo; ")
                database.execSQL("DROP TABLE " +
                        "   todo; ")
                database.execSQL("ALTER TABLE " +
                        "   temp_table " +
                        "RENAME TO " +
                        "   todo; ")

                database.execSQL("CREATE TABLE " +
                        "   temp_table " +
                        "(  " +
                        "   _id integer primary key autoincrement, " +
                        "   list_online_id text default null, " +
                        "   user_online_id text default null, " +
                        "   category_online_id text default null, " +
                        "   title text not null, " +
                        "   row_version integer not null, " +
                        "   deleted integer default null, " +
                        "   dirty integer not null, " +
                        "   position real not null " +
                        "); ")
                database.execSQL("INSERT INTO " +
                        "   temp_table " +
                        "SELECT " +
                        "   _id, " +
                        "   list_online_id, " +
                        "   user_online_id, " +
                        "   category_online_id, " +
                        "   title, " +
                        "   row_version, " +
                        "   deleted, " +
                        "   dirty, " +
                        "   position " +
                        "FROM " +
                        "   list; ")
                database.execSQL("DROP TABLE " +
                        "   list; ")
                database.execSQL("ALTER TABLE " +
                        "   temp_table " +
                        "RENAME TO " +
                        "   list; ")

                database.execSQL("CREATE TABLE " +
                        "   temp_table " +
                        "(  " +
                        "   _id integer primary key autoincrement, " +
                        "   category_online_id text default null, " +
                        "   user_online_id text default null, " +
                        "   title text not null, " +
                        "   row_version integer not null, " +
                        "   deleted integer default null, " +
                        "   dirty integer not null, " +
                        "   position real not null " +
                        "); ")
                database.execSQL("INSERT INTO " +
                        "   temp_table " +
                        "SELECT " +
                        "   _id, " +
                        "   category_online_id, " +
                        "   user_online_id, " +
                        "   title, " +
                        "   row_version, " +
                        "   deleted, " +
                        "   dirty, " +
                        "   position " +
                        "FROM " +
                        "   category; ")
                database.execSQL("DROP TABLE " +
                        "   category; ")
                database.execSQL("ALTER TABLE " +
                        "   temp_table " +
                        "RENAME TO " +
                        "   category; ")
            }
        }

        fun getInstance(context: Context): TodoCloudDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context,
                            TodoCloudDatabase::class.java,
                            "todo_cloud"
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()

                    INSTANCE = instance
                }

                return instance
            }
        }
    }
}