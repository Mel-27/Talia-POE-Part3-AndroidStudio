package com.gia.poe_demo.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Expense entity - stores individual expense records locally in RoomDB
 * receiptPhotoPath stores the absolute file path to an optional receipt image.
 * Reference: IIE PROG7313 Module Manual (2026); Android Room Docs
 */

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val description: String,
    val amount: Double,
    val date: Long, // epoch millis for the expense date
    val startTime: String = "", // e.g. "10:30"
    val endTime: String = "", // e.g. "11:00"
    val receiptPhotoPath: String? = null, // null = no photo attached
    val createdAt: Long = System.currentTimeMillis()
)