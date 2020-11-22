package com.rolandvitezhu.todocloud.di.module

import androidx.room.Room
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.database.TodoCloudDatabase
import com.rolandvitezhu.todocloud.database.TodoCloudDatabaseDao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

//https://stackoverflow.com/questions/59087662/cannot-inject-interface-in-kotlin-dagger2-mvvm
//Todo: Create a new module. Use "NetworkModule" as an example.
@Module
class PersistenceModule {

    @Provides
    @Singleton
    fun provideTodoCloudDatabase(): TodoCloudDatabase {
        return Room.databaseBuilder(
                AppController.appContext,
                TodoCloudDatabase::class.java,
                "todo_cloud"
        ).addMigrations(
                TodoCloudDatabase.MIGRATION_1_2,
                TodoCloudDatabase.MIGRATION_2_3,
                TodoCloudDatabase.MIGRATION_3_4).
        build()
    }

    @Provides
    @Singleton
    fun provideTodoCloudDatabaseDao(todoCloudDatabase: TodoCloudDatabase): TodoCloudDatabaseDao {
        return todoCloudDatabase.todoCloudDatabaseDao
    }
}