package com.gia.poe_demo.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
 * Category entity - stores expense categories locally in RoomDB
 * Each category has an emoji icon, name and optional monthly spending limit.
 * Reference: IIE PROG7313 Module Guide (2026); Android Room Docs
 */

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconEmoji: String = "\uD83D\uDED2",
    val monthlyLimit: Double = 0.0, // optional spending cap
    val createdAt: Long = System.currentTimeMillis()
)

