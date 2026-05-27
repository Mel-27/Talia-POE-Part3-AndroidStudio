package com.gia.poe_demo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gia.poe_demo.data.entities.CategoryTotal
import com.gia.poe_demo.data.entities.Expense
import kotlinx.coroutines.flow.Flow

// @Dao marks this interface as a Room Database Access Object
// ref: https://developer.android.com/training/data-storage/room/accessing-data
// ref: https://developer.android.com/reference/androidx/room/Dao
@Dao
interface ExpenseDao {

    @Insert
    suspend fun insert(expense: Expense): Long

    @Insert
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Query("""
        SELECT * FROM expenses
        ORDER BY date DESC
    """)
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("""
        SELECT * FROM expenses
        WHERE date BETWEEN :start AND :end
        ORDER BY date DESC
    """)
    fun getByPeriod(
        start: Long,
        end: Long
    ): Flow<List<Expense>>

    @Query("""
        DELETE FROM expenses
        WHERE id = :expenseId
    """)
    suspend fun deleteExpense(expenseId: Long)

    @Query("""
        SELECT * FROM expenses
        WHERE id = :expenseId
        LIMIT 1
    """)
    suspend fun getExpenseById(expenseId: Long): Expense?

    @Query("""
        SELECT categoryId AS categoryId,
               SUM(amount) AS total
        FROM expenses
        WHERE date BETWEEN :start AND :end
        GROUP BY categoryId
    """)
    fun getCategoryTotalsForPeriod(
        start: Long,
        end: Long
    ): Flow<List<CategoryTotal>>

    @Query("UPDATE expenses SET receiptPhotoPath = :url WHERE id = :id")
    suspend fun updateReceiptUrl(id: Long, url: String)

    @Query("SELECT * FROM expenses WHERE syncedToCloud = 0")
    fun getUnsyncedExpenses(): Flow<List<Expense>>

    @Query("UPDATE expenses SET syncedToCloud = 1 WHERE id = :expenseId")
    suspend fun markAsSynced(expenseId: Long)

}

/*
References:

Android Developers, 2024. Access data using Room DAOs.
Available at: https://developer.android.com/training/data-storage/room/accessing-data
[Accessed 22 April 2026].

Android Developers, 2024. Dao.
Available at: https://developer.android.com/reference/androidx/room/Dao
[Accessed 22 April 2026].

Android Developers, 2024. Insert.
Available at: https://developer.android.com/reference/androidx/room/Insert
[Accessed 22 April 2026].

Android Developers, 2024. Query.
Available at: https://developer.android.com/reference/androidx/room/Query
[Accessed 22 April 2026].

Android Developers, 2024. Update.
Available at: https://developer.android.com/reference/androidx/room/Update
[Accessed 22 April 2026].

Android Developers, 2024. Kotlin coroutines on Android.
Available at: https://developer.android.com/kotlin/coroutines
[Accessed 22 April 2026].
*/