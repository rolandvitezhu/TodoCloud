package com.rolandvitezhu.todocloud

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rolandvitezhu.todocloud.database.TodoCloudDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    private val ALL_MIGRATIONS: Array<Migration>

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            TodoCloudDatabase::class.java.canonicalName,
            FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Create earliest version of the database.
        helper.createDatabase(TEST_DB, 1).apply {
            close()
        }

        // Create a database instance, so we can run the "fixTodoPositions()" method in the
        // migrations.
        TodoCloudDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)

        // Open latest version of the database. Room will validate the schema once all migrations
        // execute.
        Room.databaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                TodoCloudDatabase::class.java,
                TEST_DB
        ).addMigrations(*ALL_MIGRATIONS).build().apply {
            openHelper.writableDatabase.close()
        }
    }

    init {
        ALL_MIGRATIONS = arrayOf(TodoCloudDatabase.MIGRATION_1_2, TodoCloudDatabase.MIGRATION_2_3,
        TodoCloudDatabase.MIGRATION_3_4)
    }
}