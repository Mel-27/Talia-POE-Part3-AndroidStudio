package com.gia.poe_demo

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.gia.poe_demo.databinding.ActivityCategoriesBinding
import kotlinx.coroutines.launch
import com.gia.poe_demo.data.entities.Category
import com.gia.poe_demo.AddExpenseActivity
import com.gia.poe_demo.data.database.AppDatabase


/**
 * AddCategoryActivity - creating a new expense category.
 * Features: name input, budget limit, emoji icon picker with tabs.
 * Saves to local RoomDB via CategoryDao.
 * Reference: IIE PROG7313 Module Manual (2026)
 */

class AddCategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesBinding
    private lateinit var db: AppDatabase
    private lateinit var session: SessionManager

    private var selectedEmoji = "🛒"
    private var selectedLabel = "Cart"
    private var currentIcons = listOf<Pair<String, String>>()

    // Icon lists per tab
    private val foodIcons = listOf(
        "🛒" to "Cart", "🍕" to "Pizza", "☕" to "Coffee", "🍔" to "Burger",
        "🥗" to "Salad", "🍷" to "Wine", "🍣" to "Sushi", "🥐" to "Bakery",
        "🍦" to "Treats", "🥤" to "Drinks", "🥩" to "Meat", "🥦" to "Veg",
        "🍰" to "Cake", "🍜" to "Noodles", "🥚" to "Eggs"
    )
    private val homeIcons = listOf(
        "🏠" to "Home", "🛋️" to "Lounge", "🧹" to "Clean", "💡" to "Electric",
        "🚿" to "Water", "📦" to "Storage", "🔧" to "Repairs", "🪑" to "Furniture",
        "🧺" to "Laundry", "🍳" to "Kitchen"
    )
    private val travelIcons = listOf(
        "🚗" to "Car", "✈️" to "Flight", "🚌" to "Bus", "🚂" to "Train",
        "⛽" to "Fuel", "🚕" to "Taxi", "🏍️" to "Moto", "🧳" to "Luggage",
        "🏖️" to "Beach", "🚲" to "Cycle"
    )
    private val funIcons = listOf(
        "🎮" to "Gaming", "🎬" to "Cinema", "🎵" to "Music", "🎨" to "Art",
        "📚" to "Books", "🎯" to "Sport", "🎉" to "Party", "🏋️" to "Gym",
        "🎭" to "Theatre", "🎸" to "Concert"
    )
    private val healthIcons = listOf(
        "💊" to "Meds", "🏥" to "Hospital", "🦷" to "Dental", "👓" to "Optical",
        "🧴" to "Skincare", "🩺" to "Doctor", "🧘" to "Yoga", "🏃" to "Running",
        "🩹" to "First Aid", "💪" to "Fitness"
    )
    private val otherIcons = listOf(
        "💼" to "Work", "🎓" to "School", "💰" to "Finance", "🧾" to "Bills",
        "📱" to "Phone", "💻" to "Tech", "🐾" to "Pets", "👗" to "Clothes",
        "🎁" to "Gift", "✨" to "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)
        applyTheme()
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getInstance(this)

        currentIcons = foodIcons
        loadIconGrid(foodIcons)
        highlightTab(binding.tabFood)
        setupTabs()
        setupSaveButton()

        binding.tvBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
        setupBottomNav()
    }

    private fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(
            if (session.isDarkMode()) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun setupTabs() {
        val tabMap = mapOf(
            binding.tabFood to foodIcons,
            binding.tabHome to homeIcons,
            binding.tabTransport to travelIcons,
            binding.tabFun to funIcons,
            binding.tabHealth to healthIcons,
            binding.tabOther to otherIcons
        )
        tabMap.forEach { (tab, icons) ->
            tab.setOnClickListener {
                currentIcons = icons
                loadIconGrid(icons)
                highlightTab(tab)
            }
        }
    }

    private fun highlightTab(active: TextView) {
        listOf(
            binding.tabFood, binding.tabHome, binding.tabTransport,
            binding.tabFun, binding.tabHealth, binding.tabOther
        ).forEach { tab ->
            tab.setBackgroundResource(
                if (tab == active) R.drawable.tab_selected_bg
                else R.drawable.tab_unselected_bg
            )
            tab.setTextColor(
                getColor(if (tab == active) R.color.black_deep else R.color.muted_text)
            )
        }
    }

    /**
     * Dynamically builds the icon grid inside iconGridContainer.
     * 5 icons per row using LinearLayout weights.
     */
    private fun loadIconGrid(icons: List<Pair<String, String>>) {
        val container = binding.iconGridContainer
        container.removeAllViews()
        val columns = 5
        var row: LinearLayout? = null

        icons.forEachIndexed { i, (emoji, label) ->
            if (i % columns == 0) {
                row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(8) }
                }
                container.addView(row)
            }
            row?.addView(buildCell(emoji, label))
        }
        // Fill empty cells in last row
        val rem = icons.size % columns
        if (rem != 0) {
            repeat(columns - rem) {
                row?.addView(android.view.View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(60), 1f)
                })
            }
        }
    }

    private fun buildCell(emoji: String, label: String): LinearLayout {
        val isSelected = emoji == selectedEmoji
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            isClickable = true
            isFocusable = true
            setPadding(dp(6), dp(6), dp(6), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).also { it.marginEnd = dp(4) }
            setBackgroundResource(
                if (isSelected) R.drawable.icon_selected_bg
                else R.drawable.icon_unselected_bg
            )
            addView(TextView(context).apply {
                text = emoji
                textSize = 22f
                gravity = android.view.Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = label
                textSize = 9f
                gravity = android.view.Gravity.CENTER
                setTextColor(
                    getColor(if (isSelected) R.color.black_deep else R.color.muted_text)
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = dp(2) }
            })
            setOnClickListener {
                selectedEmoji = emoji
                selectedLabel = label
                binding.tvSelectedIcon.text = emoji
                binding.tvSelectedIconLabel.text = label
                loadIconGrid(currentIcons)
                android.util.Log.d("AddCategory", "Icon selected: $emoji $label")
            }
        }
    }

    /**
     * Validates inputs then inserts a new Category into RoomDB.
     */
    private fun setupSaveButton() {
        binding.btnSaveCategory.setOnClickListener {
            val name = binding.etCategoryName.text.toString().trim()
            val limitStr = binding.etBudgetLimit.text.toString().trim()

            if (name.isEmpty()) {
                binding.tilCategoryName.error = "Please enter a category name"
                return@setOnClickListener
            }
            binding.tilCategoryName.error = null

            val limit = limitStr.toDoubleOrNull() ?: 0.0
            if (limit < 0) {
                binding.tilBudgetLimit.error = "Limit cannot be negative"
                return@setOnClickListener
            }
            binding.tilBudgetLimit.error = null

            lifecycleScope.launch {
                val cat = Category(
                    name = name,
                    iconEmoji = selectedEmoji,
                    monthlyLimit = limit
                )
                val newId = db.categoryDao().insert(cat)
                android.util.Log.d(
                    "AddCategory",
                    "Category saved id=$newId name=$name emoji=$selectedEmoji limit=$limit"
                )
                runOnUiThread {
                    Toast.makeText(
                        this@AddCategoryActivity,
                        "✅ \"$name\" saved!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun setupBottomNav() {
        binding.navHome.setOnClickListener {
            startActivity(Intent(this, ExpensesListActivity::class.java))
            finish()
        }
        binding.navExpenses.setOnClickListener {
            startActivity(Intent(this, ExpensesListActivity::class.java))
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

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}