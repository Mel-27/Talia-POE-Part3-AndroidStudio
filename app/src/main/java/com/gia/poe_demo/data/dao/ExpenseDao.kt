package com.gia.poe_demo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gia.poe_demo.data.entities.Expense
import com.gia.poe_demo.data.entities.CategoryTotal

// @Dao marks this interface as a Room Database Access Object
// ref: https://developer.android.com/training/data-storage/room/accessing-data
// ref: https://developer.android.com/reference/androidx/room/Dao
@Dao
interface ExpenseDao {

    @Insert
    suspend fun insert(expense: Expense)

    @Insert
    suspend fun insertExpense(expense: Expense): Long

    @Query("""
        SELECT * FROM expenses
        ORDER BY date DESC
    """)
    fun getAllExpenses(): kotlinx.coroutines.flow.Flow<List<Expense>>

    @Query("""
        SELECT * FROM expenses
        WHERE date BETWEEN :start AND :end
        ORDER BY date DESC
    """)
    fun getByPeriod(
        start: Long,
        end: Long
    ): kotlinx.coroutines.flow.Flow<List<Expense>>

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
    ): kotlinx.coroutines.flow.Flow<List<CategoryTotal>>
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

Android Developers, 2024. Kotlin coroutines on Android.
Available at: https://developer.android.com/kotlin/coroutines
[Accessed 22 April 2026].
*/