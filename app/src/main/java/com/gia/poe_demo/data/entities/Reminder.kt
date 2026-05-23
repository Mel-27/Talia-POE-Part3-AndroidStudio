package com.gia.poe_demo.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val description: String,

    val date: Long,          // trigger date
    val isCompleted: Boolean = false
)