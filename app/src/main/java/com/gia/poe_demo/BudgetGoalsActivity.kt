package com.gia.poe_demo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.gia.poe_demo.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class BudgetGoalsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    private val currencyFormat =
        NumberFormat.getNumberInstance(
            Locale.Builder()
                .setLanguage("en")
                .setRegion("ZA")
                .build()
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_goals)

        db = AppDatabase.getInstance(this)

        setupNavigation()
        setupButton()
        loadData()
    }

    private fun setupNavigation() {

        findViewById<View>(R.id.navHome)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<View>(R.id.navExpenses)?.setOnClickListener {
            startActivity(Intent(this, ExpensesListActivity::class.java))
        }

        findViewById<View>(R.id.fabAddExpense)?.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        findViewById<View>(R.id.navBadges)?.setOnClickListener {
            startActivity(Intent(this, BadgesActivity::class.java))
        }
    }




    private fun showSavedGoals(min: Double, max: Double) {

        val etMin = findViewById<EditText>(R.id.etMinBudget)
        val etMax = findViewById<EditText>(R.id.etMaxBudget)

        // Hide inputs
        etMin.visibility = View.GONE
        etMax.visibility = View.GONE

        // Create display text views dynamically (no XML changes needed)
        val parent = etMin.parent as View

        val tvMin = TextView(this).apply {
            text = "Minimum: R ${currencyFormat.format(min)}"
            textSize = 16f
        }

        val tvMax = TextView(this).apply {
            text = "Maximum: R ${currencyFormat.format(max)}"
            textSize = 16f
        }

        (parent as? android.view.ViewGroup)?.apply {
            addView(tvMin, 0)
            addView(tvMax, 1)
        }
    }

    private fun loadData() {
        lifecycleScope.launch {

            val now = System.currentTimeMillis()

            val start = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val expenses = withContext(Dispatchers.IO) {
                db.expenseDao().getByPeriod(start, now).firstOrNull() ?: emptyList()
            }

            val total = expenses.sumOf { it.amount }

            val groceries = total * 0.30
            val entertainment = total * 0.20
            val transport = total * 0.15
            val food = total * 0.20
            val health = total * 0.15

            updateBar(R.id.barGroceries, groceries)
            updateBar(R.id.barEntertainment, entertainment)
            updateBar(R.id.barTransport, transport)
            updateBar(R.id.barFood, food)
            updateBar(R.id.barHealth, health)

            updateLabel(R.id.barLabelGroceries, groceries)
            updateLabel(R.id.barLabelEntertainment, entertainment)
            updateLabel(R.id.barLabelTransport, transport)
            updateLabel(R.id.barLabelFood, food)
            updateLabel(R.id.barLabelHealth, health)
        }
    }

    private fun updateBar(id: Int, value: Double) {
        val bar = findViewById<View>(id) ?: return

        val height = (value / 1000 * 150).toInt().coerceAtLeast(20)

        bar.layoutParams.height = height
        bar.requestLayout()
    }

    private fun updateLabel(id: Int, value: Double) {
        findViewById<TextView>(id)?.text = "R${value.toInt()}"
    }

    private fun setupButton() {

        val prefs = getSharedPreferences("budget_prefs", MODE_PRIVATE)

        findViewById<MaterialButton>(R.id.btnUpdateMonthlyBudget)?.setOnClickListener {

            val min = findViewById<EditText>(R.id.etMinBudget)
                ?.text.toString().toDoubleOrNull()

            val max = findViewById<EditText>(R.id.etMaxBudget)
                ?.text.toString().toDoubleOrNull()

            if (min == null || max == null) {
                Toast.makeText(this, "Enter valid amounts", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (min > max) {
                Toast.makeText(this, "Minimum cannot be more than maximum", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            prefs.edit()
                .putFloat("min_budget", min.toFloat())
                .putFloat("max_budget", max.toFloat())
                .apply()

            Toast.makeText(this, "Goals Saved", Toast.LENGTH_SHORT).show()


            showSavedGoals(min, max)
        }
    }
}