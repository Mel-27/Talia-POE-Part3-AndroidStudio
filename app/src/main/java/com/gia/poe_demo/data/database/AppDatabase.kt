package com.gia.poe_demo.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gia.poe_demo.BudgetGoal
import com.gia.poe_demo.BudgetGoalDao
import com.gia.poe_demo.HoneyPointsDao
import com.gia.poe_demo.data.dao.CategoryDao
import com.gia.poe_demo.data.dao.UserDao
import com.gia.poe_demo.data.entities.User
import com.gia.poe_demo.data.entities.Expense
import com.gia.poe_demo.HoneyPoints
import com.gia.poe_demo.data.dao.ExpenseDao
import com.gia.poe_demo.data.dao.ReminderDao
import com.gia.poe_demo.data.entities.Category
import com.gia.poe_demo.data.entities.Reminder

// @Database tells Room this is the main database class, listing all entities and the version number
// exportSchema = false just means we're not saving the schema to a file
// ref: https://developer.android.com/training/data-storage/room#database
// ref: https://developer.android.com/reference/androidx/room/Database
// NOTE: Version increased from 7 to 8 to add syncedToCloud column migration
@Database(entities = [User::class, Expense::class , Category::class, Reminder::class, BudgetGoal::class, HoneyPoints::class], version = 8, exportSchema = false)
// abstract class extending RoomDatabase so Room can generate the implementation at compile time
// ref: https://developer.android.com/reference/androidx/room/RoomDatabase
abstract class AppDatabase : RoomDatabase() {

    // abstract function that gives access to the UserDao
    // ref: https://developer.android.com/training/data-storage/room/accessing-data
    abstract fun userDao(): UserDao

    // abstract function that gives access to the ExpenseDao
    // ref: https://developer.android.com/training/data-storage/room/accessing-data
    abstract fun expenseDao(): ExpenseDao

    abstract fun categoryDao(): CategoryDao

    abstract fun reminderDao(): ReminderDao

    abstract fun budgetGoalDao(): BudgetGoalDao

    abstract fun honeyPointsDao(): HoneyPointsDao

    companion object {
        // @Volatile makes sure INSTANCE is always up to date across all threads
        // ref: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-volatile/
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ============================================================
        // MIGRATION FROM VERSION 7 TO 8
        // Adds syncedToCloud column to expenses and categories tables
        // Required for cloud sync functionality
        // ============================================================

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add syncedToCloud column to expenses table (default 0 = false)
                    database.execSQL("ALTER TABLE expenses ADD COLUMN syncedToCloud INTEGER NOT NULL DEFAULT 0")

                    // Add syncedToCloud column to categories table (default 0 = false)
                    database.execSQL("ALTER TABLE categories ADD COLUMN syncedToCloud INTEGER NOT NULL DEFAULT 0")

                    android.util.Log.d("AppDatabase", "Migration 7->8 completed: added syncedToCloud columns")
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Migration 7->8 failed", e)
                    throw e
                }
            }
        }

        // singleton pattern so only one instance of the database gets created
        // synchronized block prevents multiple threads from creating it at the same time
        // ref: https://developer.android.com/training/data-storage/room#database
        // ref: https://kotlinlang.org/docs/reference/coroutines/shared-mutable-state-and-concurrency.html
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budgetbee_db"
                )
                    // Add migration for syncedToCloud column
                    .addMigrations(MIGRATION_7_8)
                    // Keep destructive migration as fallback for development
                    // Comment these lines in production to preserve user data
                    // .fallbackToDestructiveMigration()  // Deletes old data on version change
                    // .fallbackToDestructiveMigrationOnDowngrade()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): AppDatabase = getDatabase(context)
    }
}

/*
References:

Android Developers, 2024. Save data in a local database using Room.
Available at: https://developer.android.com/training/data-storage/room#database
[Accessed 20 April 2026].

Android Developers, 2024. Database.
Available at: https://developer.android.com/reference/androidx/room/Database
[Accessed 20 April 2026].

Android Developers, 2024. RoomDatabase.
Available at: https://developer.android.com/reference/androidx/room/RoomDatabase
[Accessed 20 April 2026].

Android Developers, 2024. Access data using Room DAOs.
Available at: https://developer.android.com/training/data-storage/room/accessing-data
[Accessed 20 April 2026].

Android Developers, 2024. Room - databaseBuilder.
Available at: https://developer.android.com/reference/androidx/room/Room#databaseBuilder(android.content.Context,java.lang.Class,java.lang.String)
[Accessed 20 April 2026].

Android Developers, 2024. RoomDatabase.Builder - fallbackToDestructiveMigration.
Available at: https://developer.android.com/reference/androidx/room/RoomDatabase.Builder#fallbackToDestructiveMigration()
[Accessed 20 April 2026].

Android Developers, 2024. Migrating Room databases.
Available at: https://developer.android.com/training/data-storage/room/migrating-db-versions
[Accessed 26 May 2026].

Kotlin, 2024. Volatile.
Available at: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-volatile/
[Accessed 20 April 2026].

Kotlin, 2024. Shared Mutable State and Concurrency.
Available at: https://kotlinlang.org/docs/reference/coroutines/shared-mutable-state-and-concurrency.html
[Accessed 20 April 2026].
*/