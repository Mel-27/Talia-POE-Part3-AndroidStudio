package com.gia.poe_demo.data.database


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gia.poe_demo.BudgetGoal
import com.gia.poe_demo.BudgetGoalDao
import com.gia.poe_demo.HoneyPoints
import com.gia.poe_demo.HoneyPointsDao
import com.gia.poe_demo.data.dao.CategoryDao
import com.gia.poe_demo.data.dao.ExpenseDao
import com.gia.poe_demo.data.dao.ReminderDao   // ← from data.dao
import com.gia.poe_demo.data.dao.UserDao
import com.gia.poe_demo.data.entities.Category
import com.gia.poe_demo.data.entities.Expense
import com.gia.poe_demo.data.entities.Reminder  // ← from data.entities
import com.gia.poe_demo.data.entities.User

@Database(
    entities = [
        User::class,
        Expense::class,
        Category::class,
        Reminder::class,
        BudgetGoal::class,
        HoneyPoints::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun budgetGoalDao(): BudgetGoalDao
    abstract fun honeyPointsDao(): HoneyPointsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budgetbee_db"
                ).build()
            }.also { INSTANCE = it }
        }

        fun getInstance(context: Context): AppDatabase = getDatabase(context)
    }
}