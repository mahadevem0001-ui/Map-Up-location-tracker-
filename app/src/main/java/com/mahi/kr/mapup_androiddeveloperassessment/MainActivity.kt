package com.mahi.kr.mapup_androiddeveloperassessment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.components.AppScaffold
import com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.viewmodel.ThemeViewModel
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.screen.PermissionScreen
import com.mahi.kr.mapup_androiddeveloperassessment.ui.theme.MapUpAndroidDeveloperAssessmentTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * Main Activity - Entry point of the application
 *
 * Following Clean Architecture principles:
 * - Minimal Activity code
 * - Delegates to feature modules
 * - Only responsible for setting up the Compose UI
 * - Manages app-wide theme state
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val themeViewModel: ThemeViewModel = koinViewModel<ThemeViewModel>()
            val themeMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()
            val systemInDarkTheme = isSystemInDarkTheme()
            val darkTheme = themeMode ?: systemInDarkTheme

            MapUpAndroidDeveloperAssessmentTheme(darkTheme = darkTheme) {
                AppScaffold(modifier = Modifier.fillMaxSize()) { paddingValues, snackbarHostState ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        PermissionScreen(snackbarHostState = snackbarHostState)
                    }
                }
            }
        }
    }
}

