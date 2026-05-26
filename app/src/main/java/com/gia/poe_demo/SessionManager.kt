package com.gia.poe_demo

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * SessionManager — persists app preferences using SharedPreferences.
 *
 * - Dark mode toggle persistence
 * - Onboarding completion tracking
 *
 * - User session management (login state)
 * - User profile storage (ID, username, email, full name)
 * - Cloud sync tracking (last sync time)
 * - First launch detection
 *
 * Reference: IIE PROG7313 Module Manual (2026)
 */

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "budget_bee_prefs"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_ONBOARDING_DONE = "onboarding_done"

        // New keys for user session and sync
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LOGIN_TIMESTAMP = "login_timestamp"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_FIRST_LAUNCH = "first_launch"
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

    /**
     * Save user session after successful login
     * Stores user credentials for persistent login across app restarts
     */
    fun saveUserSession(userId: String, username: String, email: String, fullName: String = "") {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
            putString(KEY_FULL_NAME, fullName)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
        Log.d("SessionManager", "User session saved: $username ($userId)")
    }

    /**
     * Check if user is currently logged in
     */
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    /**
     * Get current user's Firebase UID
     */
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    /**
     * Get current user's username
     */
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    /**
     * Get current user's email
     */
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    /**
     * Get current user's full name
     */
    fun getFullName(): String? = prefs.getString(KEY_FULL_NAME, null)

    /**
     * Get login timestamp
     */
    fun getLoginTimestamp(): Long = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0)

    /**
     * Clear user session (logout)
     */
    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_USERNAME)
            remove(KEY_EMAIL)
            remove(KEY_FULL_NAME)
            remove(KEY_LOGIN_TIMESTAMP)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
        Log.d("SessionManager", "User session cleared")
    }

    /**
     * Get the last time a sync was performed (milliseconds since epoch)
     */
    fun getLastSyncTime(): Long = prefs.getLong(KEY_LAST_SYNC_TIME, 0)

    /**
     * Update the last sync time to current time
     */
    fun updateLastSyncTime() {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis()).apply()
        Log.d("SessionManager", "Last sync time updated")
    }

    /**
     * Check if this is the first app launch
     */
    fun isFirstLaunch(): Boolean = prefs.getBoolean(KEY_FIRST_LAUNCH, true)

    /**
     * Mark first launch as completed
     * Call this after showing onboarding or initial setup
     */
    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
        Log.d("SessionManager", "First launch completed")
    }

    /**
     * Check if user is logged in AND has a valid session
     * Useful for determining whether to show login screen or main app
     */
    fun hasValidSession(): Boolean {
        return isLoggedIn() && getUserId() != null
    }
}

/*
References:

Android Developers, 2024. Save key-value data with SharedPreferences.
Available at: https://developer.android.com/training/data-storage/shared-preferences
[Accessed 25 May 2026].

Independent Institute of Education (IIE), 2026. PROG7313 Programming 3C Module Manual. Durban.
*/