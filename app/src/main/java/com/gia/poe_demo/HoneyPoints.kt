package com.gia.poe_demo

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
HoneyPoints entity — stores gamification points per user.
 */
@Entity(tableName = "honey_points")
data class HoneyPoints(
    @PrimaryKey val userId: Int,
    val points: Int = 0,
    val streakCount: Int = 0,  // how many consecutive days logged
    val lastLogDate: String = "" // "dd MM yyyy" — last day an expense was logged
)