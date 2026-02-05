package com.mahi.kr.mapup_androiddeveloperassessment.core.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Core data layer component for persisting application preferences
 * Uses SharedPreferences to maintain state across app restarts and process death
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "app_preferences"
        private const val KEY_HAS_REQUESTED_PERMISSIONS = "has_requested_permissions_before"
    }

    /**
     * Save that permissions have been requested at least once
     * This flag persists across app kills and relaunches
     *
     * @param hasRequested true if permissions have been requested
     */
    fun setHasRequestedPermissions(hasRequested: Boolean) {
        prefs.edit { putBoolean(KEY_HAS_REQUESTED_PERMISSIONS, hasRequested) }
    }

    /**
     * Check if permissions have been requested before
     *
     * @return true if user has clicked "Request Permissions" at least once
     */
    fun hasRequestedPermissionsBefore(): Boolean {
        return prefs.getBoolean(KEY_HAS_REQUESTED_PERMISSIONS, false)
    }

    /**
     * Clear all stored preferences (useful for testing or reset functionality)
     */
    fun clear() {
        prefs.edit { clear() }
    }
}
