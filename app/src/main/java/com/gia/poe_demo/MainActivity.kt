package com.gia.poe_demo

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.gia.poe_demo.data.database.AppDatabase
import com.gia.poe_demo.util.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * MainActivity — dashboard screen showing budget summary, top categories,
 * recent transactions, and upcoming reminders.
 * Now triggers a startup sync to Firebase Realtime Database on launch.
 * Reference: IIE PROG7313 Module Manual (2026)
 * Reference: Firebase Realtime Database Android Docs - https://firebase.google.com/docs/database/android/start
 * Reference: Android Developers. SharedPreferences. Available at:
 * https://developer.android.com/training/data-storage/shared-preferences. [Accessed 27 Apr. 2026]
 * Reference: Android Developers. (2019). Understand the Activity Lifecycle. Available at:
 * https://developer.android.com/guide/components/activities/activity-lifecycle. [Accessed 27 Apr. 2026]
 */
class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var syncManager: SyncManager
    private val currencyFormat = NumberFormat.getNumberInstance(Locale("en", "ZA"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getInstance(this)
        syncManager = SyncManager(this)

        setupNavigation()
        loadUserData()
        loadDashboardSummary()
        loadUpcomingReminders()
        triggerStartupSync()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
        loadDashboardSummary()
        loadUpcomingReminders()
    }

    /**
     * Syncs any unsynced local expenses to Firebase Realtime Database on app startup.
     * Runs in the background via coroutine — does not block the UI.
     * Reference: Firebase Realtime Database Android Docs - https://firebase.google.com/docs/database/android/start
     * Reference: IIE PROG7313 Module Manual (2026)
     */
    private fun triggerStartupSync() {
        lifecycleScope.launch {
            val result = syncManager.syncUnsyncedExpenses()
            when (result) {
                is SyncManager.SyncResult.Success -> {
                    android.util.Log.d(
                        "MainActivity",
                        "Startup sync complete: ${result.count} expense(s) synced"
                    )
                }
                is SyncManager.SyncResult.Failure -> {
                    android.util.Log.e(
                        "MainActivity",
                        "Startup sync failed: ${result.error}"
                    )
                }
            }
        }
    }

    private fun loadUserData() {
        val prefs = getSharedPreferences("BudgetBeePrefs", MODE_PRIVATE)
        val loggedInUsername = prefs.getString("loggedInUsername", "") ?: ""
        val loggedInFullName = prefs.getString("loggedInFullName", "") ?: ""

        val displayName = if (loggedInFullName.isNotEmpty()) loggedInFullName else loggedInUsername
        val firstName = displayName.split(" ").firstOrNull() ?: displayName

        findViewById<TextView>(R.id.tvUserName)?.text = "$firstName 🐝"
        val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        findViewById<TextView>(R.id.tvAvatarInitial)?.text = initial

        val userId = prefs.getInt("USER_ID", -1)
        if (userId == -1) return

        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                db.userDao().getAllUsers().find { it.id == userId.toLong() }
            }
            user?.let {
                findViewById<TextView>(R.id.tvStreak)?.text = "🔥${it.streak}"
            }
        }
    }

    private fun loadDashboardSummary() {
        val cal = Calendar.getInstance()

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val now = System.currentTimeMillis()

        val monthYear = String.format(
            Locale.getDefault(), "%d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1
        )

        lifecycleScope.launch {
            db.expenseDao().getByPeriod(startOfMonth, now).collect { expenses ->
                val totalSpent = expenses.sumOf { it.amount }

                val goal = withContext(Dispatchers.IO) {
                    db.budgetGoalDao().getGoalForMonth(monthYear)
                }

                val budget = goal?.totalMonthlyBudget ?: 0.0
                val remaining = budget - totalSpent

                val progress = if (budget > 0)
                    ((totalSpent / budget) * 100).toInt().coerceIn(0, 100)
                else 0

                findViewById<TextView>(R.id.tvBalanceAmount)?.text =
                    if (budget == 0.0) "No budget set"
                    else "R ${currencyFormat.format(remaining)}"

                findViewById<TextView>(R.id.tvBalanceSub)?.text =
                    "of R ${currencyFormat.format(budget)} budget"

                findViewById<TextView>(R.id.tvSpentAmount)?.text =
                    "R ${currencyFormat.format(totalSpent)}"

                val saved = (budget - totalSpent).coerceAtLeast(0.0)
                findViewById<TextView>(R.id.tvSavedAmount)?.text =
                    "R ${currencyFormat.format(saved)}"

                findViewById<ProgressBar>(R.id.progressBudget)?.progress = progress

                findViewById<TextView>(R.id.tvBudgetSpent)?.text =
                    "R${currencyFormat.format(totalSpent)} spent"

                findViewById<TextView>(R.id.tvBudgetGoal)?.text =
                    "R${currencyFormat.format(budget)} goal"

                val badge = findViewById<TextView>(R.id.tvBudgetBadge)

                if (progress >= 90) {
                    badge?.text = "Over Budget"
                    badge?.setBackgroundColor(getColor(R.color.error_red))
                } else {
                    badge?.text = "On Track"
                    badge?.setBackgroundColor(getColor(R.color.success_green))
                }

                loadTopCategories(startOfMonth, now, goal)

                loadRecentTransactions(
                    expenses.sortedByDescending { it.date }.take(3)
                )
            }
        }
    }

    private fun loadTopCategories(start: Long, end: Long, goal: BudgetGoal?) {
        lifecycleScope.launch {
            db.expenseDao().getCategoryTotalsForPeriod(start, end).collect { totals ->
                val categories = withContext(Dispatchers.IO) { db.categoryDao().getAll() }
                val categoryMap = categories.associateBy { it.id }
                val top2 = totals.sortedByDescending { it.total }.take(2)

                val container = findViewById<LinearLayout>(R.id.topSpendingContainer)
                container?.removeAllViews()

                top2.forEach { total ->
                    val cat = categoryMap[total.categoryId] ?: return@forEach
                    val limit = when (cat.name.lowercase(Locale.getDefault())) {
                        "groceries" -> goal?.groceriesLimit ?: 1000.0
                        "entertainment" -> goal?.entertainmentLimit ?: 500.0
                        "transport" -> goal?.transportLimit ?: 1500.0
                        "food" -> goal?.foodLimit ?: 1000.0
                        "health" -> goal?.healthLimit ?: 500.0
                        else -> cat.monthlyLimit.takeIf { it > 0 } ?: 1000.0
                    }
                    val pct = ((total.total / limit) * 100).toInt().coerceIn(0, 100)
                    val isOver = total.total > limit
                    val density = resources.displayMetrics.density

                    val row = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also { it.bottomMargin = (12 * density).toInt() }
                    }

                    val badge = CardView(this@MainActivity).apply {
                        radius = (13 * density)
                        setCardBackgroundColor(0xFFFFF3D0.toInt())
                        layoutParams = LinearLayout.LayoutParams(
                            (40 * density).toInt(), (40 * density).toInt()
                        )
                        addView(TextView(this@MainActivity).apply {
                            text = cat.iconEmoji
                            textSize = 20f
                            gravity = Gravity.CENTER
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT
                            )
                        })
                    }
                    row.addView(badge)

                    val info = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        ).also { it.marginStart = (10 * density).toInt() }
                    }
                    info.addView(TextView(this@MainActivity).apply {
                        text = cat.name
                        textSize = 13f
                        setTextColor(getColor(R.color.black_deep))
                        setTypeface(null, Typeface.BOLD)
                    })
                    info.addView(TextView(this@MainActivity).apply {
                        text = "R${currencyFormat.format(total.total)} / R${currencyFormat.format(limit)}" +
                                if (isOver) " · Over limit!" else ""
                        textSize = 11f
                        setTextColor(getColor(if (isOver) R.color.error_red else R.color.muted_text))
                    })
                    val pb = ProgressBar(this@MainActivity, null,
                        android.R.attr.progressBarStyleHorizontal).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (6 * density).toInt()
                        ).also { it.topMargin = (5 * density).toInt() }
                        max = 100
                        progress = pct
                        progressDrawable = getDrawable(
                            if (isOver) R.drawable.progress_red else R.drawable.progress_honey
                        )
                        isIndeterminate = false
                    }
                    info.addView(pb)
                    row.addView(info)

                    row.addView(TextView(this@MainActivity).apply {
                        text = "R${currencyFormat.format(total.total)}"
                        textSize = 16f
                        setTextColor(getColor(if (isOver) R.color.error_red else R.color.black_deep))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also { it.marginStart = (10 * density).toInt() }
                    })

                    container?.addView(row)
                }
            }
        }
    }

    private fun loadRecentTransactions(
        recent: List<com.gia.poe_demo.data.entities.Expense>
    ) {
        lifecycleScope.launch {
            val categories = withContext(Dispatchers.IO) { db.categoryDao().getAll() }
            val categoryMap = categories.associateBy { it.id }
            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            val density = resources.displayMetrics.density

            val container = findViewById<LinearLayout>(R.id.recentTransactionsContainer)
            container?.removeAllViews()

            recent.forEach { expense ->
                val cat = categoryMap[expense.categoryId]

                val row = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(0, (12 * density).toInt(), 0, (12 * density).toInt())
                }

                val emojiCard = CardView(this@MainActivity).apply {
                    radius = (14 * density)
                    setCardBackgroundColor(0xFFFFF3D0.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        (42 * density).toInt(), (42 * density).toInt()
                    )
                    addView(TextView(this@MainActivity).apply {
                        text = cat?.iconEmoji ?: "📋"
                        textSize = 22f
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                    })
                }
                row.addView(emojiCard)

                val info = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    ).also { it.marginStart = (12 * density).toInt() }
                }
                info.addView(TextView(this@MainActivity).apply {
                    text = expense.description
                    textSize = 14f
                    setTextColor(getColor(R.color.black_deep))
                    setTypeface(null, Typeface.BOLD)
                })
                info.addView(TextView(this@MainActivity).apply {
                    text = "${cat?.name ?: "Expense"} · ${sdf.format(Date(expense.date))}"
                    textSize = 11f
                    setTextColor(getColor(R.color.muted_text))
                })
                row.addView(info)

                row.addView(TextView(this@MainActivity).apply {
                    text = "-R ${currencyFormat.format(expense.amount)}"
                    textSize = 17f
                    setTextColor(getColor(R.color.error_red))
                    gravity = Gravity.END
                })

                container?.addView(row)

                val divider = View(this@MainActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    )
                    setBackgroundColor(getColor(R.color.input_stroke))
                }
                container?.addView(divider)
            }
        }
    }

    private fun loadUpcomingReminders() {
        db.reminderDao().getAll().observe(this) { reminders ->
            val container = findViewById<LinearLayout>(R.id.upcomingPaymentsContainer)
            val emptyView = findViewById<TextView>(R.id.tvNoReminders)
            container?.removeAllViews()

            val upcoming = reminders
                .filter { !it.isCompleted && it.date >= System.currentTimeMillis() }
                .sortedBy { it.date }
                .take(3)

            if (upcoming.isEmpty()) {
                emptyView?.visibility = View.VISIBLE
            } else {
                emptyView?.visibility = View.GONE
                val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                val density = resources.displayMetrics.density

                upcoming.forEachIndexed { index, reminder ->
                    val isLast = index == upcoming.lastIndex
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                    val dotCol = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER_HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            (24 * density).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                    dotCol.addView(View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            (12 * density).toInt(), (12 * density).toInt()
                        ).also { it.topMargin = (6 * density).toInt() }
                        setBackgroundResource(R.drawable.cal_dot_honey)
                    })
                    if (!isLast) {
                        dotCol.addView(View(this).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                (2 * density).toInt(), (50 * density).toInt()
                            )
                            setBackgroundColor(getColor(R.color.input_stroke))
                        })
                    }
                    row.addView(dotCol)

                    val content = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        ).also {
                            it.marginStart = (10 * density).toInt()
                            it.bottomMargin = (12 * density).toInt()
                        }
                    }
                    content.addView(TextView(this).apply {
                        text = sdf.format(Date(reminder.date))
                        textSize = 11f
                        setTextColor(getColor(R.color.muted_text))
                    })
                    content.addView(TextView(this).apply {
                        text = "🔔  ${reminder.title}"
                        textSize = 14f
                        setTextColor(getColor(R.color.black_deep))
                        setTypeface(null, Typeface.BOLD)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also { it.topMargin = (4 * density).toInt() }
                    })
                    row.addView(content)
                    container?.addView(row)
                }
            }
        }
    }

    private fun setupNavigation() {
        findViewById<CardView>(R.id.btnAccount)?.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
        }
        findViewById<View>(R.id.btnReminders)?.setOnClickListener {
            startActivity(Intent(this, RemindersActivity::class.java))
        }
        findViewById<View>(R.id.navExpenses)?.setOnClickListener {
            startActivity(Intent(this, ExpensesListActivity::class.java))
        }
        findViewById<View>(R.id.fabAddExpense)?.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }
        findViewById<View>(R.id.navGoals)?.setOnClickListener {
            startActivity(Intent(this, BudgetGoalsActivity::class.java))
        }
        findViewById<View>(R.id.navBadges)?.setOnClickListener {
            startActivity(Intent(this, BadgesActivity::class.java))
        }
        findViewById<TextView>(R.id.tvSeeAllReminders)?.setOnClickListener {
            startActivity(Intent(this, RemindersActivity::class.java))
        }
        findViewById<TextView>(R.id.tvSeeAllTx)?.setOnClickListener {
            startActivity(Intent(this, ExpensesListActivity::class.java))
        }
        findViewById<TextView>(R.id.tvSeeAllCats)?.setOnClickListener {
            startActivity(Intent(this, ExpensesListActivity::class.java))
        }
    }
}