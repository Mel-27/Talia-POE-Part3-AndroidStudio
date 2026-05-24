package com.gia.poe_demo

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gia.poe_demo.data.database.AppDatabase
import com.gia.poe_demo.data.entities.Reminder
import com.gia.poe_demo.databinding.ActivityRemindersBinding
import com.gia.poe_demo.databinding.ItemReminderBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RemindersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRemindersBinding
    private lateinit var db: AppDatabase

    private var selectedDateMillis: Long = 0L
    private val displayFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemindersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(this)

        setupDatePicker()
        setupAddButton()
        observeReminders()

        binding.tvBack.setOnClickListener { finish() }
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()

        val pickDate = {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val cal = Calendar.getInstance().apply {
                        set(year, month, day)
                    }
                    selectedDateMillis = cal.timeInMillis
                    binding.etReminderDate.setText(displayFormat.format(cal.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.etReminderDate.setOnClickListener { pickDate() }
    }

    private fun setupAddButton() {
        binding.btnAddReminder.setOnClickListener {

            val title = binding.etReminderName.text.toString().trim()
            val description = binding.etReminderDescription.text.toString().trim()

            if (title.isBlank()) {
                toast("Enter a title")
                return@setOnClickListener
            }

            if (selectedDateMillis == 0L) {
                toast("Select a date")
                return@setOnClickListener
            }

            val reminder = Reminder(
                title = title,
                description = description,
                date = selectedDateMillis
            )

            lifecycleScope.launch {
                db.reminderDao().insert(reminder)
                toast("Reminder added ✓")
                clearForm()
            }
        }
    }

    private fun observeReminders() {
        db.reminderDao().getAll().observe(this) { reminders ->

            binding.reminderContainer.removeAllViews()

            if (reminders.isEmpty()) {
                binding.tvNoReminders.visibility = View.VISIBLE
            } else {
                binding.tvNoReminders.visibility = View.GONE
                reminders.forEach { addReminderCard(it) }
            }
        }
    }

    private fun addReminderCard(reminder: Reminder) {
        val itemBinding = ItemReminderBinding.inflate(
            LayoutInflater.from(this),
            binding.reminderContainer,
            false
        )

        val dateText = displayFormat.format(Date(reminder.date))

        itemBinding.tvReminderTitle.text = reminder.title
        itemBinding.tvReminderSubtitle.text =
            if (reminder.description.isBlank()) dateText
            else "$dateText • ${reminder.description}"

        itemBinding.btnDeleteReminder.setOnClickListener {
            lifecycleScope.launch {
                db.reminderDao().delete(reminder)
                toast("Deleted")
            }
        }

        binding.reminderContainer.addView(itemBinding.root)
    }

    private fun clearForm() {
        binding.etReminderName.text?.clear()
        binding.etReminderDescription.text?.clear()
        binding.etReminderDate.text?.clear()
        selectedDateMillis = 0L
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}