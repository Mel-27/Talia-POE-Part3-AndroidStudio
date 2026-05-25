package com.gia.poe_demo.data.remote

/**
 * Category data model for Firebase Realtime Database
 *
 * Stores user-defined categories with budget limits.
 */

data class CategoryModel(
    val id: String = "",                    // Unique Firebase key
    val name: String = "",                  // Category name
    val iconEmoji: String = "",             // Icon emoji
    val monthlyLimit: Double = 0.0,         // Monthly budget limit
    val userId: String = "",                // Owner user ID
    val createdAt: Long = System.currentTimeMillis()  // Creation timestamp
)