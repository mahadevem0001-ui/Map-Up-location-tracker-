package com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mahi.kr.mapup_androiddeveloperassessment.core.data.local.AppPreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing app-wide theme state
 *
 * Responsibilities:
 * - Track current theme mode (light/dark/system)
 * - Persist theme preference using DataStore
 * - Provide theme toggle functionality
 */
class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = AppPreferencesManager(application)

    /**
     * StateFlow that emits the current dark mode state
     * - true: Dark mode enabled
     * - false: Light mode enabled
     * - null: Follow system theme
     */
    val isDarkMode: StateFlow<Boolean?> = prefsManager.isDarkMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Toggle theme between light and dark mode
     */
    fun toggleTheme(currentIsDark: Boolean) {
        viewModelScope.launch {
            prefsManager.toggleTheme(currentIsDark)
        }
    }

    /**
     * Set theme mode explicitly
     *
     * @param isDark true for dark mode, false for light mode, null for system default
     */
    fun setThemeMode(isDark: Boolean?) {
        viewModelScope.launch {
            prefsManager.setDarkMode(isDark)
        }
    }
}
