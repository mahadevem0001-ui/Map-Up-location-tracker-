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
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * Manager for app theme preferences using DataStore
 * Provides reactive Flow-based theme state management
 */
class ThemePreferencesManager(private val context: Context) {

    companion object {
        private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    }

    /**
     * Flow that emits the current dark mode state
     * Defaults to system theme if not set
     */
    val isDarkMode: Flow<Boolean?> = context.dataStore.data
        .map { preferences ->
            preferences[IS_DARK_MODE]
        }

    /**
     * Toggle dark mode on/off
     *
     * @param isDark true for dark mode, false for light mode, null for system default
     */
    suspend fun setDarkMode(isDark: Boolean?) {
        context.dataStore.edit { preferences ->
            if (isDark == null) {
                preferences.remove(IS_DARK_MODE)
            } else {
                preferences[IS_DARK_MODE] = isDark
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
}
