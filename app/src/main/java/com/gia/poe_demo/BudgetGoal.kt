package com.gia.poe_demo

import androidx.room.Entity
import androidx.room.PrimaryKey


// This data class represents a user's monthly budget goals,
// including overall limits and category-specific spending caps.
@Entity(tableName = "budget_goals")
data class BudgetGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val monthYear: String,          // e.g. "2026-03"
    val totalMonthlyBudget: Double, // overall monthly budget (R 5,000)
    val minMonthlyGoal: Double,     // minimum spend goal
    val maxMonthlyGoal: Double,     // maximum spend goal / cap
    val groceriesLimit: Double = 1000.0,
    val entertainmentLimit: Double = 500.0,
    val transportLimit: Double = 1500.0,
    val foodLimit: Double = 1000.0,
    val healthLimit: Double = 500.0
)

/*
References
-Google (n.d.) Room Persistence Library.
Available at: https://developer.android.com/training/data-storage/room
(Accessed: 27 April 2026).

-Google (n.d.) Android Developers: Activities.
Available at: https://developer.android.com/guide/components/activities/intro-activities
(Accessed: 27 April 2026).

-Google (n.d.) Kotlin Coroutines on Android.
Available at: https://developer.android.com/kotlin/coroutines
(Accessed: 27 April 2026).

-Google (n.d.) View Binding.
Available at: https://developer.android.com/topic/libraries/view-binding
(Accessed: 27 April 2026).

-JetBrains (n.d.) Kotlin Language Documentation.
Available at: https://kotlinlang.org/docs/home.html
(Accessed: 27 April 2026).

-Google (n.d.) Data Access Objects (DAO) in Room.
Available at: https://developer.android.com/training/data-storage/room/accessing-data
(Accessed: 27 April 2026).

-Google (n.d.) Android Lifecycle and LifecycleScope.
Available at: https://developer.android.com/topic/libraries/architecture/lifecycle
(Accessed: 27 April 2026).
 */