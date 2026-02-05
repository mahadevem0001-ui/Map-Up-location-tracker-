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
import com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.components.AppScaffold
import com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.viewmodel.ThemeViewModel
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.screen.LocationTrackingScreen
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.screen.PermissionScreen
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.viewmodel.PermissionViewModel
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
 * - Shows PermissionScreen first, then LocationTrackingScreen after permissions granted
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

            // Get permission state to determine which screen to show
            val permissionViewModel: PermissionViewModel = koinViewModel()
            val permissionState by permissionViewModel.state.collectAsStateWithLifecycle()

            MapUpAndroidDeveloperAssessmentTheme(darkTheme = darkTheme) {
                AppScaffold(modifier = Modifier.fillMaxSize()) { paddingValues, snackbarHostState ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Show LocationTrackingScreen if all permissions are granted
                        // Otherwise show PermissionScreen
                        if (permissionState.deniedPermissions.isEmpty() &&
                            permissionState.hasRequestedPermissionsBefore) {
                            LocationTrackingScreen(snackbarHostState = snackbarHostState)
                        } else {
                            PermissionScreen(snackbarHostState = snackbarHostState)
                        }
                    }
                }
            }
        }
    }
}

