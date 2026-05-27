package com.gia.poe_demo.data.remote

data class ExpenseModel(
    val id: String = "",
    val categoryId: Long = 0,
    val description: String = "",
    val amount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val startTime: String = "",
    val endTime: String = "",
    val receiptPhotoUrl: String = "",
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class CategoryModel(
    val id: String = "",
    val name: String = "",
    val iconEmoji: String = "",
    val monthlyLimit: Double = 0.0,
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)