package com.gia.poe_demo

import androidx.room.*


// This DAO handles all database operations related to budget goals,
// such as retrieving, saving, and deleting monthly goals.
@Dao
interface BudgetGoalDao {

    @Query("SELECT * FROM budget_goals WHERE monthYear = :monthYear LIMIT 1")
    suspend fun getGoalForMonth(monthYear: String): BudgetGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(goal: BudgetGoal)

    @Query("DELETE FROM budget_goals WHERE monthYear = :monthYear")
    suspend fun deleteForMonth(monthYear: String)


}
