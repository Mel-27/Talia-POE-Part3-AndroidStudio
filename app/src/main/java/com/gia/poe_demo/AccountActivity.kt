package com.gia.poe_demo

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.gia.poe_demo.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Activity responsible for handling account settings
class AccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        // grabbing the Room DB instance using the singleton pattern
        // ref: https://developer.android.com/training/data-storage/room
        val db = AppDatabase.getDatabase(this)
        val userDao = db.userDao()

        // grabbing the logged-in user info from SharedPreferences
        // ref: https://developer.android.com/training/data-storage/shared-preferences
        val prefs = getSharedPreferences("BudgetBeePrefs", MODE_PRIVATE)
        val loggedInUsername = prefs.getString("loggedInUsername", "") ?: ""
        val loggedInEmail = prefs.getString("loggedInEmail", "") ?: ""

        // used findViewById() to grab the UI elements
        // ref: https://developer.android.com/reference/android/view/View#findViewById(int)
        val tvAvatarLarge = findViewById<TextView>(R.id.tvAvatarLarge)
        val tvAccountName = findViewById<TextView>(R.id.tvAccountName)
        val tvAccountEmail = findViewById<TextView>(R.id.tvAccountEmail)
        val etName = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etName)
        val etEmail = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEmail)

        // loading the logged-in user from Room DB and displaying their info
        // looking up by username since that is what is saved to SharedPreferences
        // used Dispatchers.IO to run DB operations off the main thread
        // ref: https://developer.android.com/training/data-storage/room/accessing-data
        // ref: https://developer.android.com/topic/libraries/architecture/coroutines#lifecyclescope
        lifecycleScope.launch(Dispatchers.IO) {
            val user = userDao.getUserByUsername(loggedInUsername)
            // switching back to main thread to update UI
            // ref: https://developer.android.com/kotlin/coroutines/coroutines-adv#main-safety
            withContext(Dispatchers.Main) {
                if (user != null) {
                    // display full name, username and email from DB
                    tvAvatarLarge.text = user.fullName.first().uppercase()
                    tvAccountName.text = user.fullName
                    tvAccountEmail.text = user.email
                    etName.setText(user.fullName)
                    etEmail.setText(user.email)
                } else {
                    // fallback to SharedPreferences if DB lookup fails
                    tvAvatarLarge.text = loggedInUsername.firstOrNull()?.uppercase() ?: "?"
                    tvAccountName.text = loggedInUsername
                    tvAccountEmail.text = loggedInEmail
                    etName.setText(loggedInUsername)
                    etEmail.setText(loggedInEmail)
                }
            }
        }

        // back button closes the activity and returns to previous screen
        // ref: https://developer.android.com/reference/android/app/Activity#finish()
        findViewById<TextView>(R.id.tvBack).setOnClickListener { finish() }

        // light mode button - switches app theme to light mode
        // Adapted from (GeekforGeeks, 2022)
        findViewById<CardView>(R.id.btnLight).setOnClickListener {
            Toast.makeText(this, "Light tapped", Toast.LENGTH_SHORT).show()
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // dark mode button - switches app theme to dark mode
        // Adapted from (GeekforGeeks, 2022)
        findViewById<TextView>(R.id.btnDark).setOnClickListener {
            Toast.makeText(this, "Dark tapped", Toast.LENGTH_SHORT).show()
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        // save changes button updates the users name and email in Room DB
        // ref: https://developer.android.com/training/data-storage/room/accessing-data
        findViewById<android.view.View>(R.id.btnSaveProfile).setOnClickListener {
            val newName = etName.text.toString().trim()
            val newEmail = etEmail.text.toString().trim()

            if (newName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // used Dispatchers.IO to run DB update off the main thread
            // ref: https://developer.android.com/topic/libraries/architecture/coroutines#lifecyclescope
            lifecycleScope.launch(Dispatchers.IO) {
                val user = userDao.getUserByUsername(loggedInUsername)
                if (user != null) {
                    // @Update tells Room to handle the SQL update using the primary key
                    // ref: https://developer.android.com/training/data-storage/room/accessing-data#update
                    userDao.updateUser(user.copy(fullName = newName, email = newEmail))

                    // update SharedPreferences with new name and email
                    // ref: https://developer.android.com/training/data-storage/shared-preferences
                    prefs.edit()
                        .putString("loggedInFullName", newName)
                        .putString("loggedInEmail", newEmail)
                        .apply()

                    // logging all users to Logcat to verify update was successful
                    // ref: https://developer.android.com/reference/android/util/Log
                    val allUsers = userDao.getAllUsers()
                    android.util.Log.d("RoomDB", "Users after update: $allUsers")

                    // switching back to main thread to update UI
                    // ref: https://developer.android.com/kotlin/coroutines/coroutines-adv#main-safety
                    withContext(Dispatchers.Main) {
                        tvAvatarLarge.text = newName.first().uppercase()
                        tvAccountName.text = newName
                        tvAccountEmail.text = newEmail
                        Toast.makeText(this@AccountActivity, "Profile updated!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // sign out clears SharedPreferences and navigates back to LoginActivity
        // ref: https://developer.android.com/training/data-storage/shared-preferences
        findViewById<android.view.View>(R.id.rowSignOut).setOnClickListener {
            // used AlertDialog to confirm sign out before proceeding
            // ref: https://developer.android.com/guide/topics/ui/dialogs
            AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ ->

                    // clearing flags so splash routes to login on next launch
                    // ref: https://developer.android.com/training/data-storage/shared-preferences
                    prefs.edit()
                        .putBoolean("isRegistered", false)
                        .remove("loggedInUsername")
                        .remove("loggedInFullName")
                        .remove("loggedInEmail")
                        .apply()

                    // clearing the back stack so user cant navigate back to dashboard
                    // ref: https://developer.android.com/guide/components/activities/tasks-and-back-stack
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // delete account removes the user from Room DB and signs them out
        // ref: https://developer.android.com/training/data-storage/room/accessing-data
        findViewById<android.view.View>(R.id.rowDeleteAccount).setOnClickListener {
            // used AlertDialog to confirm delete before proceeding
            // ref: https://developer.android.com/guide/topics/ui/dialogs
            AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure? This cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    // used Dispatchers.IO to run DB delete off the main thread
                    // ref: https://developer.android.com/topic/libraries/architecture/coroutines#lifecyclescope
                    lifecycleScope.launch(Dispatchers.IO) {
                        val user = userDao.getUserByUsername(loggedInUsername)
                        if (user != null) {
                            // @Delete tells Room to handle the SQL delete using the primary key
                            // ref: https://developer.android.com/training/data-storage/room/accessing-data#delete
                            userDao.deleteUser(user)

                            // logging all users to Logcat to verify delete was successful
                            // ref: https://developer.android.com/reference/android/util/Log
                            val allUsers = userDao.getAllUsers()
                            android.util.Log.d("RoomDB", "Users after delete: $allUsers")

                            // verify the user no longer exists in the DB
                            // ref: https://developer.android.com/training/data-storage/room/accessing-data#query
                            val deletedUser = userDao.getUserByUsername(loggedInUsername)

                            // switching back to main thread to show toast and navigate
                            // ref: https://developer.android.com/kotlin/coroutines/coroutines-adv#main-safety
                            withContext(Dispatchers.Main) {
                                if (deletedUser == null) {
                                    Toast.makeText(
                                        this@AccountActivity,
                                        "Account deleted — user no longer exists",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                prefs.edit()
                                    .putBoolean("isRegistered", false)
                                    .remove("loggedInUsername")
                                    .remove("loggedInFullName")
                                    .remove("loggedInEmail")
                                    .apply()

                                val intent = Intent(this@AccountActivity, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@AccountActivity,
                                    "User not found",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}

/*
References:
GeeksforGeeks, 2022. How to check if an app is in dark mode and change it to light mode in Android.
Available at: https://www.geeksforgeeks.org/android/how-to-check-if-an-app-is-in-dark-mode-and-change-it-to-light-mode-in-android/
[Accessed 27 April 2026].

Android Developers, 2024. Save data in a local database using Room.
Available at: https://developer.android.com/training/data-storage/room
[Accessed 27 April 2026].

Android Developers, 2024. Access data using Room DAOs.
Available at: https://developer.android.com/training/data-storage/room/accessing-data
[Accessed 27 April 2026].

Android Developers, 2024. Save key-value data with SharedPreferences.
Available at: https://developer.android.com/training/data-storage/shared-preferences
[Accessed 27 April 2026].

Android Developers, 2024. View - findViewById.
Available at: https://developer.android.com/reference/android/view/View#findViewById(int)
[Accessed 27 April 2026].

Android Developers, 2024. Use Kotlin coroutines with lifecycle-aware components.
Available at: https://developer.android.com/topic/libraries/architecture/coroutines#lifecyclescope
[Accessed 27 April 2026].

Android Developers, 2024. Improve app performance with Kotlin coroutines.
Available at: https://developer.android.com/kotlin/coroutines/coroutines-adv#main-safety
[Accessed 27 April 2026].

Android Developers, 2024. AlertDialog.
Available at: https://developer.android.com/guide/topics/ui/dialogs
[Accessed 27 April 2026].

Android Developers, 2024. Tasks and Back Stack.
Available at: https://developer.android.com/guide/components/activities/tasks-and-back-stack
[Accessed 27 April 2026].

Android Developers, 2024. Log.
Available at: https://developer.android.com/reference/android/util/Log
[Accessed 27 April 2026].

Android Developers, 2024. Activity - finish.
Available at: https://developer.android.com/reference/android/app/Activity#finish()
[Accessed 27 April 2026].
*/