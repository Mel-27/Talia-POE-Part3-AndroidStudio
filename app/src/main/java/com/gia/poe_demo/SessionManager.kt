package com.gia.poe_demo

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * SessionManager — persists app preferences and user session using SharedPreferences.
 * Stores dark mode toggle, onboarding state, user login details, and last sync time.
 * Reference: IIE PROG7313 Module Manual (2026)
 * Reference: Android Developers. SharedPreferences. Available at:
 * https://developer.android.com/reference/android/content/SharedPreferences. [Accessed 27 Apr. 2026]
 */

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "budget_bee_prefs"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_EMAIL = "email"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_LAST_SYNC = "last_sync_time"
    }

    // ============ THEME ============

    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        android.util.Log.d("SessionManager", "Dark mode set to: $enabled")
    }

    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    // ============ ONBOARDING ============

    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    fun setOnboardingDone() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    fun isOnboardingDone(): Boolean = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    // ============ USER SESSION ============

    fun saveUserSession(userId: String, username: String, email: String) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
        android.util.Log.d("SessionManager", "User session saved: $username ($userId)")
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_USERNAME)
            remove(KEY_EMAIL)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
        android.util.Log.d("SessionManager", "User session cleared")
    }

    // ============ SYNC ============

    fun updateLastSyncTime() {
        prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
        android.util.Log.d("SessionManager", "Last sync time updated")
    }

    fun getLastSyncTime(): Long = prefs.getLong(KEY_LAST_SYNC, 0)
}