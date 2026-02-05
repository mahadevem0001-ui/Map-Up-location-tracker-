package com.mahi.kr.mapup_androiddeveloperassessment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
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
        // Disable motion-event splitting early to prevent InputDispatcher crash on some devices
        (window?.decorView as? ViewGroup)?.isMotionEventSplittingEnabled = false
        setContent {

            // Reinforce motion-event splitting disable after compose view attaches
            val composeView = LocalView.current
            SideEffect {
                (composeView.parent as? ViewGroup)?.isMotionEventSplittingEnabled = false
            }

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
                        val permissionsDenied = permissionState.deniedPermissions.isNotEmpty()

                        Log.d(
                            this::class.simpleName,
                            "onCreate: locationPermissionsDenied $permissionsDenied LocationService.isRunning() ${LocationService.isRunning()}"
                        )
                        // If location permissions are denied and service is running, stop it
                        if (permissionsDenied && LocationService.isRunning()) {
//                            Intent(this@MainActivity, LocationService::class.java).apply {
//                                action = LocationService.ACTION_STOP
//                            }.also {
//                                startService(it)
//                            }
                            locationViewModel.stopLocationService()

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
                            permissionState.hasRequestedPermissionsBefore
                        ) {
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

