package com.mahi.kr.mapup_androiddeveloperassessment

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.components.drawer.AppScaffoldWithDrawer
import com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.viewmodel.ThemeViewModel
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.service.LocationService
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.screen.LocationTrackingScreen
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.viewmodel.LocationViewModel
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.screen.PermissionScreen
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.viewmodel.PermissionViewModel
import com.mahi.kr.mapup_androiddeveloperassessment.ui.theme.MapUpAndroidDeveloperAssessmentTheme
import org.koin.compose.viewmodel.koinViewModel

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
            val themeState by themeViewModel.themeState.collectAsStateWithLifecycle()
            val systemInDarkTheme = isSystemInDarkTheme()
            val darkTheme = themeState.isDarkMode ?: systemInDarkTheme

            // Permission state is needed for the initial screen decision; collect early
            val permissionViewModel: PermissionViewModel = koinViewModel()
            val permissionState by permissionViewModel.state.collectAsStateWithLifecycle()

            val ready = themeState.isLoaded && permissionState.isLoaded

            MapUpAndroidDeveloperAssessmentTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.fillMaxSize()) {

                    AnimatedContent(
                        ready, label = "MainContentTransition", transitionSpec = {
                            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(
                                animationSpec = tween(500)
                            )
                        }) { isReady ->
                        if (!isReady) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            // Only create the location viewmodel once ready to avoid startup overhead
                            val locationViewModel: LocationViewModel = koinViewModel()
                            val locationState by locationViewModel.state.collectAsStateWithLifecycle()

                            AppScaffoldWithDrawer(
                                modifier = Modifier.fillMaxSize(),
                                title = "Location Tracking",
                                showDrawer = permissionState.deniedPermissions.isEmpty() && permissionState.hasRequestedPermissionsBefore,
                                currentSession = locationState.currentSession,
                                sessions = locationState.sessions,
                                onExportCsv = { locationViewModel.exportSessionsToCsv() },
                                onExportGpx = { locationViewModel.exportSessionsToGpx() },
                                themeViewModel = themeViewModel
                            ) { paddingValues, snackbarHostState ->

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(paddingValues)
                                ) {
                                    // Show LocationTrackingScreen if all permissions are granted
                                    // Otherwise show PermissionScreen
                                    if (permissionState.deniedPermissions.isEmpty() && permissionState.hasRequestedPermissionsBefore) {
                                        LocationTrackingScreen(snackbarHostState = snackbarHostState, viewModel = locationViewModel)
                                    } else {
                                        PermissionScreen(snackbarHostState = snackbarHostState, viewModel = permissionViewModel)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

