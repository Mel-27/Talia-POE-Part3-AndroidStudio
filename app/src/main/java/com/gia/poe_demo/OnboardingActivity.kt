package com.gia.poe_demo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// extended AppCompatActivity which is just the standard way Android activities work
// ref: https://developer.android.com/guide/components/activities/intro-activities
class OnboardingActivity : AppCompatActivity() {

    // keeping track of which screen the user is on
    private var currentScreen = 1
    private val totalScreens = 5

    // stored all five layouts in a mapOf() so I could load them dynamically with setContentView()
    // ref: https://developer.android.com/reference/androidx/appcompat/app/AppCompatActivity
    private val layouts = mapOf(
        1 to R.layout.onboarding_screen_1,
        2 to R.layout.onboarding_screen_2,
        3 to R.layout.onboarding_screen_3,
        4 to R.layout.onboarding_screen_4,
        5 to R.layout.onboarding_screen_5
    )

    // loads the first screen when the activity starts
    // ref: https://developer.android.com/guide/components/activities/activity-lifecycle#onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadScreen(currentScreen)
    }

    // used findViewById() to grab the Next and Skip buttons and set click listeners on them
    // ref: https://developer.android.com/reference/android/view/View#findViewById(int)
    // ref: https://developer.android.com/reference/android/view/View#setOnClickListener(android.view.View.OnClickListener)
    private fun loadScreen(screen: Int) {
        setContentView(layouts[screen]!!)

        val nextBtn = findViewById<android.view.View>(R.id.btnNext)
        nextBtn?.setOnClickListener {
            if (currentScreen < totalScreens) {
                currentScreen++
                loadScreen(currentScreen)
            } else {
                goToLogin()
            }
        }

        val skipBtn = findViewById<android.view.View>(R.id.btnSkip)
        skipBtn?.setOnClickListener {
            goToLogin()
        }
    }

    // navigates to LoginActivity using an Intent and calls finish() so the user cant go back
    // ref: https://developer.android.com/guide/components/intents-filters
    // ref: https://developer.android.com/reference/android/app/Activity#finish()
    private fun goToLogin() {

        val prefs = getSharedPreferences("BudgetBeePrefs", MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_done", true).apply()

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

/*
References:

Android Developers, 2024. Introduction to Activities.
Available at: https://developer.android.com/guide/components/activities/intro-activities
[Accessed 21 April 2026].

Android Developers, 2024. The Activity Lifecycle.
Available at: https://developer.android.com/guide/components/activities/activity-lifecycle#onCreate
[Accessed 19 April 2026].

Android Developers, 2024. AppCompatActivity.
Available at: https://developer.android.com/reference/androidx/appcompat/app/AppCompatActivity
[Accessed 18 April 2026].

Android Developers, 2024. View - findViewById.
Available at: https://developer.android.com/reference/android/view/View#findViewById(int)
[Accessed 19 April 2026].

Android Developers, 2024. View - setOnClickListener.
Available at: https://developer.android.com/reference/android/view/View#setOnClickListener(android.view.View.OnClickListener)
[Accessed 18 April 2026].

Android Developers, 2024. Intents and Intent Filters.
Available at: https://developer.android.com/guide/components/intents-filters
[Accessed 18 April 2026].

Android Developers, 2024. Activity - finish.
Available at: https://developer.android.com/reference/android/app/Activity#finish()
[Accessed 18 April 2026].
*/