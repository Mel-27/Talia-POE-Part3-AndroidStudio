package com.gia.poe_demo.data.util

import com.gia.poe_demo.data.database.AppDatabase
import com.gia.poe_demo.data.entities.Category

object CategorySeeder {

    suspend fun seed(db: AppDatabase) {
        val dao = db.categoryDao()

        val count = dao.getCount()
        if (count > 0) return

        val defaults = listOf(
            Category(name = "Food", iconEmoji = "🍕", monthlyLimit = 0.0),
            Category(name = "Home", iconEmoji = "🏠", monthlyLimit = 0.0),
            Category(name = "Transport", iconEmoji = "🚗", monthlyLimit = 0.0),
            Category(name = "Fun", iconEmoji = "🎮", monthlyLimit = 0.0),
            Category(name = "Health", iconEmoji = "💊", monthlyLimit = 0.0),
            Category(name = "Other", iconEmoji = "✨", monthlyLimit = 0.0)
        )

        dao.insertAll(defaults)
    }
}