package com.gia.poe_demo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/*
Handles reading and updating points for the logged-in user.
Adapted from (Android Developers,2019)
 */
@Dao
interface HoneyPointsDao {

    // Insert or replace — used when creating a new user's points row
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(honeyPoints: HoneyPoints)

    // Get points record for a specific user
    @Query("SELECT * FROM honey_points WHERE userId = :userId")
    suspend fun getPointsForUser(userId: Int): HoneyPoints?

    // Add points (e.g. +5 for adding an expense)
    @Query("UPDATE honey_points SET points = points + :amount WHERE userId = :userId")
    suspend fun addPoints(userId: Int, amount: Int)

    // Deduct points (penalty system)
    @Query("UPDATE honey_points SET points = MAX(0, points - :amount) WHERE userId = :userId")
    suspend fun deductPoints(userId: Int, amount: Int)

    // Update streak and last log date
    @Query("UPDATE honey_points SET streakCount = :streak, lastLogDate = :date WHERE userId = :userId")
    suspend fun updateStreak(userId: Int, streak: Int, date: String)
}

/*
References:
Android Developers. (2019). Accessing data using Room DAOs  |
Android Developers. [online]
Available at: https://developer.android.com/training/data-storage/room/accessing-data.

 */