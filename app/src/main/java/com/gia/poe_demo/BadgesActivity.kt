package com.gia.poe_demo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.gia.poe_demo.HoneyPoints
import com.gia.poe_demo.data.database.AppDatabase
import kotlinx.coroutines.launch

/*
BadgesActivity — Displays the Hive Rewards gamification screen.
 Features:

 *   - Shows current Honey Points for the logged-in user
 *   - Progress bar toward next badge
 *   - Badge gallery (earned = full colour, locked = greyed out)
 *   - How to Earn Points section (static display)
 *   - Bottom navigation

 * Points are stored in the honey_points table via HoneyPointsDao.
 * Points are awarded by other screens (e.g. AddExpenseActivity awards +5).
 */
class BadgesActivity : AppCompatActivity() {

    private val TAG = "BadgesActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_badges)

        Log.d(TAG, "BadgesActivity started")

        setupBottomNav()
        loadAndDisplayPoints()
    }

    // Refresh points every time screen comes into focus
    // (handles returning after an expense was added)
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: refreshing points")
        loadAndDisplayPoints()
    }

    /**
     * Reads the logged-in userId from SharedPreferences,
     * fetches their HoneyPoints from the DB, then updates all UI elements.
     * Adapted from (Android Developers,2026 - Save data using SharedPreferences.)
     * Adapted from (Android Developers,2026 -Use Kotlin coroutines with lifecycle-aware components.)
     */

    private fun loadAndDisplayPoints() {

        lifecycleScope.launch {
            try {
                val prefs  = getSharedPreferences("BudgetBeePrefs", MODE_PRIVATE)
                val userId = prefs.getInt("USER_ID", -1)
                Log.d(TAG, "Loading points for userId=$userId")

                val db = AppDatabase.getInstance(this@BadgesActivity)

                // If no record exists yet, create one with 0 points
                var honeyData = db.honeyPointsDao().getPointsForUser(userId)
                if (honeyData == null) {
                    Log.d(TAG, "No points record found — creating new one")
                    honeyData = HoneyPoints(userId = userId, points = 0)
                    db.honeyPointsDao().upsert(honeyData)
                }

                val points = honeyData.points
                Log.d(TAG, "User has $points Honey Points")

                // Update all UI on main thread
                updatePointsDisplay(points)
                updateProgressBar(points)
                updateBadgeGallery(points)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading points: ${e.message}", e)
            }
        }
    }
    /**
     * Updates the large points number and the subtitle label
     * Adapted from (Medium , 2019 - Logging in Kotlin— the right way.)
     */
    private fun updatePointsDisplay(points: Int) {
        findViewById<TextView>(R.id.tvHoneyPoints).text = "$points 🍯"
        findViewById<TextView>(R.id.tvNextBadgeSubtitle).text = GamificationManager.getNextBadgeLabel(points)
        // Find the subtitle TextView
        // that says "50 pts to Honey Collector" in the XML
        // It doesn't have an ID in the XML so we'll use the progress label instead
        Log.d(TAG, "Next badge label: ${GamificationManager.getNextBadgeLabel(points)}")
    }



    /**
     * Updates the progress bar and the "200 / 250 pts" label.
     * Adapted from (Android Developers, 2026 -  ProgressBar.)
     */
    private fun updateProgressBar(points: Int) {
        val (current, target) = GamificationManager.getProgressToNextBadge(points)
        findViewById<ProgressBar>(R.id.progressNextBadge).max = target
        findViewById<ProgressBar>(R.id.progressNextBadge).progress = current
        findViewById<TextView>(R.id.tvProgressLabel).text = "$current / $target pts"
        Log.d(TAG, "Progress: $current / $target")
    }


    /**
     * Updates each badge card to show earned (full opacity, green tick)
     * or locked (greyed out, lock icon) based on current points.
     *
     * The four badge cards in the XML correspond to:
     *   badge1 = Worker Bee    (100 pts)
     *   badge2 = Honey Collector (250 pts)
     *   badge3 = Honey Hoarder  (500 pts)
     *   badge4 = Queen Bee     (1000 pts)
     */
    private fun updateBadgeGallery(points: Int) {
        val earnedBadges = GamificationManager.getEarnedBadges(points)
        val earnedNames  = earnedBadges.map { it.name }.toSet()

        // Map each badge name to its card's parent CardView and status TextView
        // We find them by their position in the XML — each badge card contains
        // a TextView at the top that says either "Earned" or "Locked"
        updateSingleBadge(
            cardId     = R.id.cardBadge1,
            badgeName  = "Worker Bee",
            earnedNames = earnedNames
        )
        updateSingleBadge(
            cardId     = R.id.cardBadge2,
            badgeName  = "Honey Collector",
            earnedNames = earnedNames
        )
        updateSingleBadge(
            cardId     = R.id.cardBadge3,
            badgeName  = "Honey Hoarder",
            earnedNames = earnedNames
        )
        updateSingleBadge(
            cardId     = R.id.cardBadge4,
            badgeName  = "Queen Bee",
            earnedNames = earnedNames
        )

        Log.d(TAG, "Earned badges: $earnedNames")
    }

    /**
     * Updates a single badge card's appearance based on whether it's earned.
     * Earned = full opacity, green "Earned" label
     * Locked = 0.45 alpha, grey "Locked" label
     */

    private fun updateSingleBadge(
        cardId: Int,
        badgeName: String,
        earnedNames: Set<String>
    ) {
        val card = findViewById<CardView>(cardId) ?: return
        val isEarned = badgeName in earnedNames

        if (isEarned) {
            card.alpha = 1.0f
            // Find the status TextView inside this card
            val statusTv = card.findViewWithTag<TextView>("tvBadgeStatus_$badgeName")
                ?: card.getChildAt(0)?.let {
                    (it as? android.widget.LinearLayout)?.getChildAt(0) as? TextView
                }
            statusTv?.text      = "✅ Earned"
            statusTv?.setTextColor(getColor(R.color.success_green))
        } else {
            card.alpha = 0.45f
            val statusTv = card.findViewWithTag<TextView>("tvBadgeStatus_$badgeName")
                ?: card.getChildAt(0)?.let {
                    (it as? android.widget.LinearLayout)?.getChildAt(0) as? TextView
                }
            statusTv?.text      = "🔒 Locked"
            statusTv?.setTextColor(getColor(R.color.muted_text))
        }
    }


    //Bottom nav
    //Adapted from (Medium, 2018 - findViewById in Kotlin,Stack Overflow ,2017 - Log.e with Kotlin.)
    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            Log.d(TAG, "navHome clicked")
            finish()
        }

        findViewById<LinearLayout>(R.id.navExpenses).setOnClickListener {
            Log.d(TAG, "navExpenses clicked")
            //startActivity(Intent(this, ExpenseListActivity::class.java))
        }

        findViewById<CardView>(R.id.fabAddExpense).setOnClickListener {
            Log.d(TAG, "FAB clicked")
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navGoals).setOnClickListener {
            Log.d(TAG, "navGoals clicked")
            //startActivity(Intent(this, GoalsActivity::class.java))

        }


        findViewById<LinearLayout>(R.id.navBadges).setOnClickListener {
            Log.d(TAG, "Already on Badges")
        }
    }
}
/*
References:

Android Developers, 2026. Use Kotlin coroutines with lifecycle-aware components.
Available at: https://developer.android.com/topic/libraries/architecture/coroutines
[Accessed 27 April 2026].

Android Ideas (Medium), 2018. findViewById in Kotlin.
Available at: https://medium.com/android-ideas/findviewbyid-in-kotlin-ce4d22193c79
[Accessed 27 April 2026].

Android Developers, 2026. Save data using SharedPreferences.
Available at: https://developer.android.com/training/data-storage/shared-preferences
[Accessed 27 April 2026].

Android Developers, 2026. ProgressBar.
Available at: https://developer.android.com/reference/android/widget/ProgressBar
[Accessed 27 April 2026].

Android Ideas (Medium), 2018. findViewById in Kotlin.
Available at: https://medium.com/android-ideas/findviewbyid-in-kotlin-ce4d22193c79
[Accessed 27 April 2026].

 Medium. (2019). Logging in Kotlin— the right way.
 Available at: https://muthuraj57.medium.com/logging-in-kotlin-the-right-way-d7a357bb0343
 [Accessed 27 Apr. 2026].

 Stack Overflow .(2017). Log.e with Kotlin.  .
 Available at: https://stackoverflow.com/questions/44158802/log-e-with-kotlin.
[Accessed 27 April 2026].


 */