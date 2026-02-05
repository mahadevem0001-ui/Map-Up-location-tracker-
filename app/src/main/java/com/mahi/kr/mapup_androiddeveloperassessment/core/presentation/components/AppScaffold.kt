package com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch

/**
 * Root composable that provides common scaffold structure for all features
 *
 * This composable acts as a container that:
 * - Manages theme state
 * - Provides Scaffold with common elements (FAB, Snackbar)
 * - Exposes content lambda with PaddingValues
 * - Can be extended with TopAppBar, BottomBar, etc.
 *
 * Benefits:
 * - Consistent UI across all features
 * - Centralized theme management
 * - Reusable Scaffold configuration
 * - Easy to add common elements (TopBar, NavBar)
 *
 * @param modifier Modifier for the Scaffold
 * @param showFAB Whether to show the theme toggle FAB
 * @param topBar Optional top app bar
 * @param bottomBar Optional bottom navigation bar
 * @param snackbarHost Optional custom snackbar host
 * @param themeViewModel ViewModel for theme management
 * @param content The feature-specific content with access to PaddingValues
 */
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    showFAB: Boolean = true,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    themeViewModel: ThemeViewModel = viewModel(),
    content: @Composable (paddingValue: PaddingValues, snackbarHostState: SnackbarHostState) -> Unit
) {
    // Theme state
    val systemInDarkTheme = isSystemInDarkTheme()
    val themeMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()
    val isDarkMode = themeMode ?: systemInDarkTheme

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = { snackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (showFAB) {
                ThemeToggleFAB(
                    isDarkMode = isDarkMode,
                    onToggle = {
                        themeViewModel.toggleTheme(isDarkMode)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = if (isDarkMode) "Switched to Light Mode" else "Switched to Dark Mode",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        content(paddingValues, snackbarHostState)
    }
}

/**
 * Extension function for AppScaffold with SnackbarHostState exposed
 * Use this when you need to show custom snackbars from your feature
 *
 * @param modifier Modifier for the Scaffold
 * @param showFAB Whether to show the theme toggle FAB
 * @param topBar Optional top app bar
 * @param bottomBar Optional bottom navigation bar
 * @param themeViewModel ViewModel for theme management
 * @param content Feature content with access to PaddingValues and SnackbarHostState
 */
@Composable
fun AppScaffoldWithSnackbar(
    modifier: Modifier = Modifier,
    showFAB: Boolean = true,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    themeViewModel: ThemeViewModel = viewModel(),
    content: @Composable (PaddingValues, SnackbarHostState) -> Unit
) {
    // Theme state
    val systemInDarkTheme = isSystemInDarkTheme()
    val themeMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()
    val isDarkMode = themeMode ?: systemInDarkTheme

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (showFAB) {
                ThemeToggleFAB(
                    isDarkMode = isDarkMode,
                    onToggle = {
                        themeViewModel.toggleTheme(isDarkMode)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = if (isDarkMode) "Switched to Light Mode" else "Switched to Dark Mode",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        content(paddingValues, snackbarHostState)
    }
}
