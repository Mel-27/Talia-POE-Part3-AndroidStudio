package com.gia.poe_demo

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
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
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import kotlinx.coroutines.flow.collectLatest
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
 *
 * Reference: IIE PROG7313 Module Manual (2026)
 */

class ExpensesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpensesListBinding
    private lateinit var db: AppDatabase
    private lateinit var session: SessionManager

    private val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val displayFmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    private var filterStart = startOfMonth()
    private var filterEnd = System.currentTimeMillis()
    private var sortDesc = true    // newest first

    private var expenses = listOf<Expense>()
    private var categoryMap = mapOf<Long, String>()   // id → "emoji name"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)
        applyTheme()
        binding = ActivityExpensesListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getInstance(this)

        loadCategoryMap()
        setupFilterChips()
        setupSortButtons()
        setupAddCategoryButton()
        setupBottomNav()

        // Hide static placeholder cards from the XML layout
        listOf(
            binding.cardExpense1, binding.cardExpense2,
            binding.cardExpense3, binding.cardExpense4
        ).forEach { it.visibility = View.GONE }

        observeExpenses()

        // Handle new expense from AddExpenseActivity
        val newExpenseId = intent.getLongExtra("SHOW_NEW_EXPENSE", -1L)
        if (newExpenseId != -1L) {
            // Trigger refresh to show new expense
            observeExpenses()
        }

    }

    private fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(
            if (session.isDarkMode()) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    // Category map
    private fun loadCategoryMap() {
        lifecycleScope.launch {
            val cats = db.categoryDao().getAll()
            categoryMap = cats.associate { it.id to "${it.iconEmoji} ${it.name}" }
            android.util.Log.d("ExpensesList", "Category map: ${categoryMap.size} categories")
            renderExpenses()
        }
    }

    // Filter chips
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

    // Sort buttons
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
                    loadCategoryTotals() // you forgot this call before
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

    // Build expense cards
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

        // Category emoji badge
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

        // Description + category + date + optional receipt link
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

        // Time display if available
        if (exp.startTime.isNotEmpty() && exp.endTime.isNotEmpty()) {
            info.addView(TextView(context).apply {
                text = "⏰ ${exp.startTime} - ${exp.endTime}"
                textSize = 10f
                setTextColor(getColor(R.color.muted_text))
            })
        }

        // RECEIPT tap target — only shown when a photo was stored
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

        // Amount (right aligned)
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

    // Summary bar
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

    // Add category button
    private fun setupAddCategoryButton() {
        binding.btnAddCategory.setOnClickListener {
            startActivity(Intent(this, AddCategoryActivity::class.java))
        }
    }

    // Bottom nav
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

    // Helpers
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

/*
/**
* References:
* Used for onCreate() and onResume() lifecycle methods to load and refresh expenses correctly
* when the screen starts and returns to focus:
* Android Developers. (2019). Understand the Activity Lifecycle  |  Android Developers. Available at:
* https://developer.android.com/guide/components/activities/activity-lifecycle.
* [Accessed 27 Apr. 2026]
*
* Used for Intent navigation when opening ReceiptViewActivity and passing data (photoPath)
* between screens:
* Android Developers. (2019). Intents and Intent Filters  |  Android Developers. Available at:
* https://developer.android.com/guide/components/intents-filters.
* [Accessed 27 Apr. 2026]
*
* Used for loading Room database data asynchronously using coroutines (lifecycleScope):
* David (2021). Using coroutines with Android Room database. Stack Overflow. Available at:
* https://stackoverflow.com/questions/68126665/using-coroutines-with-android-room-database.
* [Accessed 27 Apr. 2026]
*
* Used for loading data from Room database when the expense list changes:
* Guendouz, M. (2018). Room, LiveData, and RecyclerView. Medium. Available at:
* https://medium.com/@guendouz/room-livedata-and-recyclerview-d8e96fb31dfe
* [Accessed 26 Apr. 2026]
*
* Used for implementing data filtering logic:
* Meyta Taliti (2022). Simple List with Date Range Filter - Meyta Taliti - Medium. Medium. Available at:
* https://medium.com/@meytataliti/simple-list-with-date-range-filter-19bd71761495.
* [Accessed 27 Apr. 2026]
*
* user1061793 (2012). How to add days into the date in android. Stack Overflow. Available at:
* https://stackoverflow.com/questions/8738369/how-to-add-days-into-the-date-in-android.
* [Accessed 27 Apr. 2026]
*
* Used for implementing the Material date picker for custom date range selection:
* Android Developers. (2024). Pick a date or time | Android Developers. Available at:
* https://developer.android.com/develop/ui/views/components/pickers.
* [Accessed 27 Apr. 2026]
*/
 */