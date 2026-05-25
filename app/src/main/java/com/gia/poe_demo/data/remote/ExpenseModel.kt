package com.gia.poe_demo.data.remote

/**
 * Expense data model for Firebase Realtime Database
 *
 * This model matches the Expense entity but is optimized for
 * cloud storage and real-time synchronization.
 *
 * Reference: Firebase Realtime Database
 * https://firebase.google.com/docs/database/android/start
 */

data class ExpenseModel(
    val id: String = "", // unique Firebase key
    val categoryId: Long = 0,
    val categoryName: String = "",
    val categoryIcon: String = "",
    val description: String = "",
    val amount: Double = 0.0, // expense amount
    val date: Long = System.currentTimeMillis(),
    val startTime: String = "",
    val endTime: String = "",
    val receiptPhotoUrl: String = "",
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

