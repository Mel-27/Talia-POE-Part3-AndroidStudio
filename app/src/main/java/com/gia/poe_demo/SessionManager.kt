package com.gia.poe_demo

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * SessionManager — persists app preferences using SharedPreferences.
 * Stores dark mode toggle so the theme survives app restarts.
 * Reference: IIE PROG7313 Module Manual (2026)
 */

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "budget_bee_prefs"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
    }

    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        android.util.Log.d("SessionManager", "Dark mode set to: $enabled")
    }

    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    fun setOnboardingDone() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    fun isOnboardingDone(): Boolean = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
}