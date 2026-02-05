package com.mahi.kr.mapup_androiddeveloperassessment

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.components.AppScaffoldWithDrawer
import com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.viewmodel.ThemeViewModel
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.LocationService
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

            // Get location state for drawer summary
            val locationViewModel: com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.viewmodel.LocationViewModel =
                koinViewModel()
            val locationState by locationViewModel.state.collectAsStateWithLifecycle()

            MapUpAndroidDeveloperAssessmentTheme(darkTheme = darkTheme) {
                AppScaffoldWithDrawer(
                    modifier = Modifier.fillMaxSize(),
                    title = "Location Tracking",
                    currentSession = locationState.currentSession,
                    sessions = locationState.sessions,
                    onExportCsv = { locationViewModel.exportSessionsToCsv() },
                    onExportGpx = { locationViewModel.exportSessionsToGpx() },
                    themeViewModel = themeViewModel
                ) { paddingValues, snackbarHostState ->

                    // Monitor permission state and stop service if location permissions are removed
                    LaunchedEffect(permissionState.deniedPermissions) {
                        val locationPermissionsDenied = permissionState.deniedPermissions.keys.any { permission ->
                            permission == Manifest.permission.ACCESS_FINE_LOCATION ||
                            permission == Manifest.permission.ACCESS_COARSE_LOCATION
                        }

                        // If location permissions are denied and service is running, stop it
                        if (locationPermissionsDenied && LocationService.isRunning()) {
                            val intent = Intent(this@MainActivity, LocationService::class.java).apply {
                                action = LocationService.ACTION_STOP
                            }
                            startService(intent)
                        }
                    }

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

