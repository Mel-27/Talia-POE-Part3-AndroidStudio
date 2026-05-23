package com.gia.poe_demo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gia.poe_demo.data.database.AppDatabase
import com.gia.poe_demo.data.util.CategorySeeder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// @SuppressLint used here to suppress the CustomSplashScreen lint warning since we built our own
// ref: https://developer.android.com/reference/android/annotation/SuppressLint
@SuppressLint("CustomSplashScreen")
// extended AppCompatActivity which is just the standard way Android activities work
// ref: https://developer.android.com/guide/components/activities/intro-activities
class SplashActivity : AppCompatActivity() {

    // Handler and Runnable used to animate the loading dots on the splash screen
    // ref: https://developer.android.com/reference/android/os/Handler
    private lateinit var loadingDotsHandler: Handler
    private lateinit var loadingDotsRunnable: Runnable
    private val dotStates = listOf(".", "..", "...")
    private var dotIndex = 0

    // how long the splash screen shows and how fast the dots cycle
    private val SPLASH_DURATION = 3000L
    private val DOT_INTERVAL = 400L

    // loads the splash screen layout when the activity starts
    // ref: https://developer.android.com/guide/components/activities/activity-lifecycle#onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // used findViewById() to grab the loading dots TextView
        // ref: https://developer.android.com/reference/android/view/View#findViewById(int)
        val tvLoadingDots = findViewById<android.widget.TextView>(R.id.tvLoadingDots)

        // Handler tied to the main looper so UI updates run on the main thread
        // ref: https://developer.android.com/reference/android/os/Looper#getMainLooper()
        loadingDotsHandler = Handler(Looper.getMainLooper())

        // Runnable cycles through dot states every 400ms to animate the loading dots
        // ref: https://developer.android.com/reference/android/os/Handler#postDelayed(java.lang.Runnable,%20long)
        loadingDotsRunnable = object : Runnable {
            override fun run() {
                tvLoadingDots.text = dotStates[dotIndex % dotStates.size]
                dotIndex++
                loadingDotsHandler.postDelayed(this, DOT_INTERVAL)
            }
        }
        loadingDotsHandler.post(loadingDotsRunnable)

        // seeding default categories into the database on first launch
        // ref: https://developer.android.com/topic/libraries/architecture/coroutines#lifecyclescope
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                CategorySeeder.seed(AppDatabase.getInstance(this@SplashActivity))
            }
        }

        // after 3 seconds checks SharedPreferences to decide where to navigate
        // ref: https://developer.android.com/training/data-storage/shared-preferences
        Handler(Looper.getMainLooper()).postDelayed({

            // using BudgetBeePrefs to match what LoginActivity and RegisterActivity save to
            // ref: https://developer.android.com/training/data-storage/shared-preferences
            val prefs = getSharedPreferences("BudgetBeePrefs", MODE_PRIVATE)

            val onboardingDone = prefs.getBoolean("onboarding_done", false)
            val isRegistered = prefs.getBoolean("isRegistered", false)

            // routes based on onboarding and login state
            // ref: https://developer.android.com/guide/components/intents-filters
            val nextActivity = when {
                !onboardingDone -> OnboardingActivity::class.java
                !isRegistered -> LoginActivity::class.java
                else -> MainActivity::class.java
            }

            // calling finish() so the user cant navigate back to the splash screen
            // ref: https://developer.android.com/reference/android/app/Activity#finish()
            startActivity(Intent(this, nextActivity))
            finish()

        }, SPLASH_DURATION)
    }

    // removing the Handler callbacks in onDestroy to avoid memory leaks
    // ref: https://developer.android.com/guide/components/activities/activity-lifecycle#onDestroy
    override fun onDestroy() {
        super.onDestroy()
        loadingDotsHandler.removeCallbacks(loadingDotsRunnable)
    }
}

/*
References:

Android Developers, 2024. SuppressLint.
Available at: https://developer.android.com/reference/android/annotation/SuppressLint
[Accessed 27 April 2026].

Android Developers, 2024. Introduction to Activities.
Available at: https://developer.android.com/guide/components/activities/intro-activities
[Accessed 27 April 2026].

Android Developers, 2024. The Activity Lifecycle.
Available at: https://developer.android.com/guide/components/activities/activity-lifecycle#onCreate
[Accessed 27 April 2026].

Android Developers, 2024. Handler.
Available at: https://developer.android.com/reference/android/os/Handler
[Accessed 27 April 2026].

Android Developers, 2024. Handler - postDelayed.
Available at: https://developer.android.com/reference/android/os/Handler#postDelayed(java.lang.Runnable,%20long)
[Accessed 27 April 2026].

Android Developers, 2024. Looper - getMainLooper.
Available at: https://developer.android.com/reference/android/os/Looper#getMainLooper()
[Accessed 27 April 2026].

Android Developers, 2024. View - findViewById.
Available at: https://developer.android.com/reference/android/view/View#findViewById(int)
[Accessed 27 April 2026].

Android Developers, 2024. Save key-value data with SharedPreferences.
Available at: https://developer.android.com/training/data-storage/shared-preferences
[Accessed 27 April 2026].

Android Developers, 2024. Intents and Intent Filters.
Available at: https://developer.android.com/guide/components/intents-filters
[Accessed 27 April 2026].

Android Developers, 2024. Activity - finish.
Available at: https://developer.android.com/reference/android/app/Activity#finish()
[Accessed 27 April 2026].

Android Developers, 2024. Activity Lifecycle - onDestroy.
Available at: https://developer.android.com/guide/components/activities/activity-lifecycle#onDestroy
[Accessed 27 April 2026].

Android Developers, 2024. Use Kotlin coroutines with lifecycle-aware components.
Available at: https://developer.android.com/topic/libraries/architecture/coroutines#lifecyclescope
[Accessed 27 April 2026].
*/