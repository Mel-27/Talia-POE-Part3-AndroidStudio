package com.gia.poe_demo

import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.gia.poe_demo.databinding.ActivityExpensesListBinding
import com.gia.poe_demo.data.database.AppDatabase
import com.gia.poe_demo.data.entities.CategoryTotal
import com.gia.poe_demo.data.entities.Expense
import com.gia.poe_demo.data.remote.ExpenseModel
import com.gia.poe_demo.data.util.RealtimeDbManager
import com.gia.poe_demo.data.util.OfflineTestHelper
import com.gia.poe_demo.util.SyncManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


/**
 * ExpensesListActivity — shows expense entries for a user-selectable period.
 *
 * Features implemented:
 *  - "View list of entries in a period" — date filter chips (This Month / Last 7 Days / 3 Months / custom)
 *  - "View category totals in a period" — summary bar (total, items, avg/day)
 *  - Expense cards dynamically built from RoomDB data
 *  - Sort toggle: Newest First / Oldest First
 *  - RECEIPT link on cards — taps open the stored photo in a system image viewer
 *  - "+ ADD EXPENSE CATEGORY" button navigates to AddCategoryActivity
 *  - Light/dark theme applied from SessionManager
 *  - Sync status indicator showing cloud sync state
 *  - Manual sync button for offline recovery
 *  - Network connectivity receiver — auto-syncs when internet reconnects
 *  - isSyncing flag prevents duplicate cloud sync calls
 *
 * Reference: IIE PROG7313 Module Manual (2026)
 * Reference: Firebase Realtime Database Android Docs - https://firebase.google.com/docs/database/android/start
 * Reference: Android Developers. (2019). Understand the Activity Lifecycle. Available at: https://developer.android.com/guide/components/activities/activity-lifecycle. [Accessed 27 Apr. 2026]
 * Reference: Android Developers. (2019). Intents and Intent Filters. Available at: https://developer.android.com/guide/components/intents-filters. [Accessed 27 Apr. 2026]
 * Reference: David (2021). Using coroutines with Android Room database. Stack Overflow. Available at: https://stackoverflow.com/questions/68126665/using-coroutines-with-android-room-database. [Accessed 27 Apr. 2026]
 * Reference: Guendouz, M. (2018). Room, LiveData, and RecyclerView. Medium. Available at: https://medium.com/@guendouz/room-livedata-and-recyclerview-d8e96fb31dfe. [Accessed 26 Apr. 2026]
 * Reference: Meyta Taliti (2022). Simple List with Date Range Filter. Medium. Available at: https://medium.com/@meytataliti/simple-list-with-date-range-filter-19bd71761495. [Accessed 27 Apr. 2026]
 * Reference: user1061793 (2012). How to add days into the date in android. Stack Overflow. Available at: https://stackoverflow.com/questions/8738369/how-to-add-days-into-the-date-in-android. [Accessed 27 Apr. 2026]
 * Reference: Android Developers. (2024). Pick a date or time. Available at: https://developer.android.com/develop/ui/views/components/pickers. [Accessed 27 Apr. 2026]
 * Reference: Android Developers. (2024). BroadcastReceiver. Available at: https://developer.android.com/reference/android/content/BroadcastReceiver. [Accessed 27 Apr. 2026]
 */

class ExpensesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpensesListBinding
    private lateinit var db: AppDatabase
    private lateinit var session: SessionManager
    private lateinit var realtimeDb: RealtimeDbManager
    private lateinit var syncManager: SyncManager
    private lateinit var networkReceiver: BroadcastReceiver
    private var isSyncing = false

    private val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val displayFmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    private var filterStart = startOfMonth()
    private var filterEnd = System.currentTimeMillis()
    private var sortDesc = true

    private var expenses = listOf<Expense>()
    private var categoryMap = mapOf<Long, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)
        applyTheme()
        binding = ActivityExpensesListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getInstance(this)
        realtimeDb = RealtimeDbManager()
        syncManager = SyncManager(this)

        loadCategoryMap()
        setupFilterChips()
        setupSortButtons()
        setupAddCategoryButton()
        setupBottomNav()
        setupSyncUI()
        registerNetworkReceiver()

        listOf(
            binding.cardExpense1, binding.cardExpense2,
            binding.cardExpense3, binding.cardExpense4
        ).forEach { it.visibility = View.GONE }

        observeExpenses()

        val newExpenseId = intent.getLongExtra("SHOW_NEW_EXPENSE", -1L)
        if (newExpenseId != -1L) {
            observeExpenses()
        }

        syncLocalDataToCloud()
    }

    private fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(
            if (session.isDarkMode()) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    /**
     * Registers a BroadcastReceiver that listens for connectivity changes.
     * When the device reconnects to the internet, unsynced local expenses
     * are automatically pushed to Firebase Realtime Database.
     * Reference: Android Developers. (2024). BroadcastReceiver. Available at:
     * https://developer.android.com/reference/android/content/BroadcastReceiver. [Accessed 27 Apr. 2026]
     * Reference: IIE PROG7313 Module Manual (2026)
     */
    private fun registerNetworkReceiver() {
        networkReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (OfflineTestHelper.isNetworkAvailable(this@ExpensesListActivity)) {
                    android.util.Log.d("ExpensesList", "Network reconnected — auto-syncing")
                    syncLocalDataToCloud()
                }
            }
        }
        @Suppress("DEPRECATION")
        registerReceiver(
            networkReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(networkReceiver)
        } catch (e: Exception) {
            android.util.Log.w("ExpensesList", "Receiver already unregistered: ${e.message}")
        }
    }

    private fun loadCategoryMap() {
        lifecycleScope.launch {
            val cats = db.categoryDao().getAll()
            categoryMap = cats.associate { it.id to "${it.iconEmoji} ${it.name}" }
            android.util.Log.d("ExpensesList", "Category map: ${categoryMap.size} categories")
            renderExpenses()
        }
    }

    private fun setupFilterChips() {
        binding.chipThisMonth.setOnClickListener {
            filterStart = startOfMonth()
            filterEnd = System.currentTimeMillis()
            binding.dateRangeRow.visibility = View.GONE
            updateFilterDisplay()
            observeExpenses()
        }
        binding.chipLast7.setOnClickListener {
            filterEnd = System.currentTimeMillis()
            filterStart = filterEnd - 7L * 86_400_000
            binding.dateRangeRow.visibility = View.GONE
            updateFilterDisplay()
            observeExpenses()
        }
        binding.chip3Months.setOnClickListener {
            filterEnd = System.currentTimeMillis()
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -3)
            filterStart = cal.timeInMillis
            binding.dateRangeRow.visibility = View.GONE
            updateFilterDisplay()
            observeExpenses()
        }
        binding.chipCustomRange.setOnClickListener {
            binding.dateRangeRow.visibility =
                if (binding.dateRangeRow.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        binding.btnStartDate.setOnClickListener { pickDate(isStart = true) }
        binding.btnEndDate.setOnClickListener { pickDate(isStart = false) }
    }

    private fun updateFilterDisplay() {
        binding.tvFilterRange.text = when {
            filterStart == startOfMonth() && filterEnd > System.currentTimeMillis() - 86400000 -> "This Month"
            filterStart > System.currentTimeMillis() - 8L * 86400000 -> "Last 7 Days"
            filterStart > System.currentTimeMillis() - 91L * 86400000 -> "3 Months"
            else -> "Custom Range"
        }
    }

    private fun pickDate(isStart: Boolean) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            if (isStart) {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                filterStart = cal.timeInMillis
                binding.btnStartDate.text = dateFmt.format(cal.time)
            } else {
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                filterEnd = cal.timeInMillis
                binding.btnEndDate.text = dateFmt.format(cal.time)
            }
            updateFilterDisplay()
            observeExpenses()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun setupSortButtons() {
        binding.btnSortDesc.setOnClickListener {
            sortDesc = true
            updateSortDisplay()
            renderExpenses()
        }
        binding.btnSortAsc.setOnClickListener {
            sortDesc = false
            updateSortDisplay()
            renderExpenses()
        }
        updateSortDisplay()
    }

    private fun updateSortDisplay() {
        val honey = getColor(R.color.honey)
        val inactive = getColor(R.color.cream2)
        binding.btnSortDesc.setCardBackgroundColor(if (sortDesc) honey else inactive)
        binding.btnSortAsc.setCardBackgroundColor(if (!sortDesc) honey else inactive)
        binding.tvSortDesc.setTextColor(getColor(if (sortDesc) R.color.black_deep else R.color.muted_text))
        binding.tvSortAsc.setTextColor(getColor(if (!sortDesc) R.color.black_deep else R.color.muted_text))
    }

    private fun observeExpenses() {
        lifecycleScope.launch {
            db.expenseDao().getByPeriod(filterStart, filterEnd)
                .collectLatest { list ->
                    expenses = list
                    renderExpenses()
                    updateSummary(expenses)
                    loadCategoryTotals()
                }
        }
    }

    private fun loadCategoryTotals() {
        lifecycleScope.launch {
            db.expenseDao().getCategoryTotalsForPeriod(filterStart, filterEnd)
                .collectLatest { totals ->
                    if (totals.isNotEmpty()) {
                        updateCategorySummary(totals)
                    } else {
                        binding.tvCategoryBreakdown.text = "No expenses in this period"
                    }
                }
        }
    }

    private fun updateCategorySummary(totals: List<CategoryTotal>) {
        val sb = StringBuilder()
        totals.forEach { total ->
            val categoryName = categoryMap[total.categoryId] ?: "Unknown"
            sb.append("$categoryName: R${"%.0f".format(total.total)}\n")
        }
        binding.tvCategoryBreakdown.text =
            if (sb.isNotEmpty()) sb.toString() else "No expenses in this period"
        android.util.Log.d("ExpensesList", "Category totals: ${totals.size} categories")
    }

    /**
     * Sets up the sync status indicator and manual sync button.
     * Observes unsynced expenses from RoomDB and updates the UI label accordingly.
     * Reference: Firebase Realtime Database Android Docs - https://firebase.google.com/docs/database/android/start
     * Reference: IIE PROG7313 Module Manual (2026)
     */
    private fun setupSyncUI() {
        updateSyncStatus()
        setupManualSyncButton()
    }

    private fun updateSyncStatus() {
        lifecycleScope.launch {
            db.expenseDao().getUnsyncedExpenses().collect { unsynced ->
                val count = unsynced.size
                val tvSyncStatus = findViewById<TextView>(R.id.tvSyncStatus)
                val syncProgressBar = findViewById<ProgressBar>(R.id.syncProgressBar)

                if (count == 0) {
                    tvSyncStatus.text = "✓ All data synced to cloud"
                    tvSyncStatus.setTextColor(getColor(R.color.success_green))
                    syncProgressBar?.visibility = View.GONE
                } else {
                    tvSyncStatus.text = "⚠️ $count items waiting to sync"
                    tvSyncStatus.setTextColor(getColor(R.color.honey_dark))
                    syncProgressBar?.visibility = View.GONE
                }
            }
        }
    }

    private fun setupManualSyncButton() {
        findViewById<Button>(R.id.btnManualSync)?.setOnClickListener {
            Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show()
            syncLocalDataToCloud()
        }
    }

    /**
     * Syncs unsynced local expenses to Firebase Realtime Database.
     * Uses isSyncing flag to prevent duplicate concurrent sync calls.
     * Reference: Firebase Realtime Database Android Docs - https://firebase.google.com/docs/database/android/start
     * Reference: IIE PROG7313 Module Manual (2026)
     */
    private fun syncLocalDataToCloud() {
        if (isSyncing) return
        isSyncing = true

        lifecycleScope.launch {
            val result = syncManager.syncUnsyncedExpenses()
            when (result) {
                is SyncManager.SyncResult.Success -> {
                    android.util.Log.d("ExpensesList", "Synced ${result.count} expenses to cloud")
                    if (result.count > 0) {
                        runOnUiThread {
                            Toast.makeText(
                                this@ExpensesListActivity,
                                "${result.count} expense(s) synced to cloud",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                is SyncManager.SyncResult.Failure -> {
                    android.util.Log.e("ExpensesList", "Sync failed: ${result.error}")
                }
            }
            isSyncing = false
        }
    }

    private fun renderExpenses() {
        val container = getOrCreateDynamicContainer()
        container.removeAllViews()

        val sorted = if (sortDesc) {
            expenses.sortedByDescending { it.date }
        } else {
            expenses.sortedBy { it.date }
        }

        if (sorted.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "No expenses found for this period"
                textSize = 14f
                setTextColor(getColor(R.color.muted_text))
                gravity = android.view.Gravity.CENTER
                setPadding(0, dp(40), 0, dp(20))
            })
            return
        }

        var lastLabel = ""
        sorted.forEach { exp ->
            val label = dayLabel(exp.date)
            if (label != lastLabel) {
                lastLabel = label
                container.addView(buildDayHeader(label))
            }
            container.addView(buildExpenseCard(exp))
        }
    }

    private fun getOrCreateDynamicContainer(): LinearLayout {
        binding.root.findViewWithTag<LinearLayout>("expDynamic")?.let { return it }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            tag = "expDynamic"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(16), dp(4), dp(16), dp(80)) }
        }
        (binding.cardExpense1.parent as? LinearLayout)?.addView(container)
            ?: (binding.cardExpense4.parent as? LinearLayout)?.addView(container)
        return container
    }

    private fun buildDayHeader(label: String): TextView = TextView(this).apply {
        text = label
        textSize = 11f
        setTypeface(null, android.graphics.Typeface.BOLD)
        setTextColor(getColor(R.color.muted_text))
        setPadding(dp(4), dp(14), dp(4), dp(6))
    }

    private fun buildExpenseCard(exp: Expense): CardView = CardView(this).apply {
        radius = dp(14f)
        cardElevation = dp(3f)
        setCardBackgroundColor(getColor(R.color.white))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10) }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        val catDisplay = categoryMap[exp.categoryId] ?: "📋 Expense"
        val emoji = if (catDisplay.length >= 2) catDisplay.take(2) else "📋"
        row.addView(CardView(context).apply {
            radius = dp(12f)
            setCardBackgroundColor(getColor(R.color.cream2))
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                .also { it.marginEnd = dp(12) }
            addView(TextView(context).apply {
                text = emoji
                textSize = 20f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            })
        })

        val info = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        info.addView(TextView(context).apply {
            text = exp.description
            textSize = 14f
            setTextColor(getColor(R.color.black_deep))
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        info.addView(TextView(context).apply {
            text = "$catDisplay · ${dateFmt.format(Date(exp.date))}"
            textSize = 11f
            setTextColor(getColor(R.color.muted_text))
        })

        if (exp.startTime.isNotEmpty() && exp.endTime.isNotEmpty()) {
            info.addView(TextView(context).apply {
                text = "⏰ ${exp.startTime} - ${exp.endTime}"
                textSize = 10f
                setTextColor(getColor(R.color.muted_text))
            })
        }

        if (exp.receiptPhotoPath != null && exp.receiptPhotoPath.isNotEmpty()) {
            info.addView(TextView(context).apply {
                text = "📎 RECEIPT"
                textSize = 11f
                setTextColor(getColor(R.color.honey_dark))
                setTypeface(null, android.graphics.Typeface.BOLD)
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = dp(3) }
                setOnClickListener { openReceipt(exp.receiptPhotoPath!!) }
            })
        }
        row.addView(info)

        row.addView(TextView(context).apply {
            text = "-R${"%.0f".format(exp.amount)}"
            textSize = 15f
            setTextColor(getColor(R.color.error_red))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginStart = dp(8) }
        })

        addView(row)
    }

    private fun openReceipt(path: String) {
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(this, "Receipt photo no longer found", Toast.LENGTH_SHORT).show()
            android.util.Log.w("ExpensesList", "Receipt missing: $path")
            return
        }
        val uri: Uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        android.util.Log.d("ExpensesList", "Opening receipt: $path")
        startActivity(Intent.createChooser(intent, "View Receipt"))
    }

    private fun updateSummary(list: List<Expense>) {
        val total = list.sumOf { it.amount }
        val days = ((filterEnd - filterStart) / 86_400_000L).coerceAtLeast(1)
        binding.tvTotalExpenses.text = "R${"%.0f".format(total)}"
        binding.tvTotalItems.text = list.size.toString()
        binding.tvAvgPerDay.text = "R${"%.0f".format(total / days)}"
        android.util.Log.d(
            "ExpensesList",
            "Summary - Total: $total, Items: ${list.size}, Avg: ${total / days}"
        )
    }

    private fun setupAddCategoryButton() {
        binding.btnAddCategory.setOnClickListener {
            startActivity(Intent(this, AddCategoryActivity::class.java))
        }
    }

    private fun setupBottomNav() {
        binding.navHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        binding.fabAddExpense.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }
        binding.navGoals.setOnClickListener {
            startActivity(Intent(this, BudgetGoalsActivity::class.java))
            finish()
        }
        binding.navBadges.setOnClickListener {
            startActivity(Intent(this, BadgesActivity::class.java))
            finish()
        }
    }

    private fun dayLabel(millis: Long): String {
        val today = Calendar.getInstance()
        val d = Calendar.getInstance().apply { timeInMillis = millis }

        fun sameDay(a: Calendar, b: Calendar): Boolean {
            return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                    a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
        }

        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            sameDay(today, d) -> "TODAY"
            sameDay(yesterday, d) -> "YESTERDAY"
            else -> SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date(millis))
                .uppercase()
        }
    }

    private fun startOfMonth(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}