package com.gia.poe_demo

/*
 Central object for all gamification rules.
 Defines point values, badge thresholds, and helper functions.
 Used by BadgesActivity and any screen that awards points.
 */
object GamificationManager {

    //Points values
    const val POINTS_ADD_EXPENSE    = 5
    const val POINTS_DAILY_BUDGET   = 10
    const val POINTS_STREAK_5_DAYS  = 20
    const val POINTS_SAVINGS_GOAL   = 25

    //Penalty values
    const val PENALTY_EXCEED_BUDGET   = 10
    const val PENALTY_MISSED_LOG_DAY  = 5

    //Badge definitions (using Kotlin data class – Kotlin Documentation, 2026)
    data class Badge(
        val name: String,
        val requiredPoints: Int,
        val drawableRes: Int,
        val description: String
    )

    val ALL_BADGES = listOf(
        Badge("Worker Bee",      100,  R.drawable.worker_bee,      "Reach 100 Honey Points"),
        Badge("Honey Collector", 250,  R.drawable.honey_collector, "Reach 250 Honey Points"),
        Badge("Honey Hoarder",   500,  R.drawable.honey_hoarder,   "Reach 500 Honey Points"),
        Badge("Queen Bee",       1000, R.drawable.queen_bee,       "Reach 1000 Honey Points")
    )

    // ── Helper functions
    /**
     * Returns all badges the user has earned based on their points.
     */

    fun getEarnedBadges(points: Int): List<Badge> =
        ALL_BADGES.filter { points >= it.requiredPoints }

    /**
     * Returns the next badge the user hasn't earned yet. Null if all earned.
     */
    fun getNextBadge(points: Int): Badge? =
        ALL_BADGES.firstOrNull { points < it.requiredPoints }

    /**
     * Returns progress toward the next badge as a Pair(current, target).
     * Used to set the ProgressBar max and progress values.
     * Adapted from (Kotlin Documentation, 2026 - Collection operations).
     */
    fun getProgressToNextBadge(points: Int): Pair<Int, Int> {
        val previousThreshold = ALL_BADGES
            .lastOrNull { points >= it.requiredPoints }?.requiredPoints ?: 0
        val nextThreshold = getNextBadge(points)?.requiredPoints
            ?: ALL_BADGES.last().requiredPoints
        val current = points - previousThreshold
        val target  = nextThreshold - previousThreshold
        return Pair(current, target)
    }

    /**
     * Returns a readable string like "50 pts to Honey Collector"
     * Adapted from (Kotlin Documentation, 2026 - Collection operations).
     */
    fun getNextBadgeLabel(points: Int): String {
        val next = getNextBadge(points) ?: return "All badges earned! 🎉"
        val remaining = next.requiredPoints - points
        return "$remaining pts to ${next.name}"
    }
}

/*
Kotlin Documentation, 2026. Data classes.
Available at: https://kotlinlang.org/docs/data-classes.html
[Accessed 27 April 2026].

Kotlin Documentation, 2026. Collection operations.
Available at: https://kotlinlang.org/docs/collection-operations.html
[Accessed 27 April 2026].
 */