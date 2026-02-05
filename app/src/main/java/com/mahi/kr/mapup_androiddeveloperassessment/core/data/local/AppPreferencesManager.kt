package com.mahi.kr.mapup_androiddeveloperassessment.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore extension for the application context
 * Single DataStore instance for all app preferences
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

/**
 * Unified preferences manager using DataStore for all app preferences
 *
 * This class manages all application preferences in a single DataStore instance:
 * - Theme preferences (dark mode)
 * - Permission preferences (has requested permissions)
 * - Future preferences can be added here
 *
 * Benefits of unified approach:
 * - Single source of truth for all preferences
 * - Consistent API across the app
 * - Type-safe preference keys
 * - Reactive Flow-based updates
 * - Better performance (single DataStore instance)
 * - Easier to maintain and test
 */
class AppPreferencesManager(private val context: Context) {

    companion object {
        // Theme preference keys
        private val KEY_IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")

        // Permission preference keys
        private val KEY_HAS_REQUESTED_PERMISSIONS = booleanPreferencesKey("has_requested_permissions_before")

        // Future preference keys can be added here
        // private val KEY_EXAMPLE = stringPreferencesKey("example_key")
    }

    // ========== Theme Preferences ==========

    /**
     * Flow that emits the current dark mode state
     *
     * @return Flow<Boolean?>
     *   - true: Dark mode enabled
     *   - false: Light mode enabled
     *   - null: Follow system theme (default)
     */
    val isDarkMode: Flow<Boolean?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_IS_DARK_MODE]
        }

    /**
     * Set dark mode preference
     *
     * @param isDark true for dark mode, false for light mode, null for system default
     */
    suspend fun setDarkMode(isDark: Boolean?) {
        context.dataStore.edit { preferences ->
            if (isDark == null) {
                preferences.remove(KEY_IS_DARK_MODE)
            } else {
                preferences[KEY_IS_DARK_MODE] = isDark
            }
        }
    }

    /**
     * Toggle between light and dark mode
     *
     * @param currentIsDark the current dark mode state
     */
    suspend fun toggleTheme(currentIsDark: Boolean) {
        setDarkMode(!currentIsDark)
    }

    // ========== Permission Preferences ==========

    /**
     * Flow that emits whether permissions have been requested before
     *
     * @return Flow<Boolean> true if user has requested permissions at least once
     */
    val hasRequestedPermissionsBefore: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_HAS_REQUESTED_PERMISSIONS] ?: false
        }

    /**
     * Save that permissions have been requested at least once
     * This flag persists across app kills and relaunches
     *
     * @param hasRequested true if permissions have been requested
     */
    suspend fun setHasRequestedPermissions(hasRequested: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HAS_REQUESTED_PERMISSIONS] = hasRequested
        }
    }

    /**
     * Get current value of hasRequestedPermissions synchronously
     * Note: This is a suspend function and requires a coroutine scope
     *
     * @return true if permissions have been requested before
     */
    suspend fun getHasRequestedPermissionsSync(): Boolean {
        var result = false
        context.dataStore.data.collect { preferences ->
            result = preferences[KEY_HAS_REQUESTED_PERMISSIONS] ?: false
        }
        return result
    }

    // ========== Utility Functions ==========

    /**
     * Clear all stored preferences (useful for testing or reset functionality)
     */
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Clear only theme preferences
     */
    suspend fun clearThemePreferences() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_IS_DARK_MODE)
        }
    }

    /**
     * Clear only permission preferences
     */
    suspend fun clearPermissionPreferences() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_HAS_REQUESTED_PERMISSIONS)
        }
    }
}
