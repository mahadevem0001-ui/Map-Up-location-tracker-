package com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.components.drawer

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mahi.kr.mapup_androiddeveloperassessment.R
import com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.viewmodel.ThemeViewModel
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationSession
import kotlinx.coroutines.launch

/**
 * Root composable with TopBar and NavigationDrawer
 *
 * Features:
 * - TopBar with menu button and theme toggle
 * - Navigation Drawer showing location summary
 * - Centralized theme management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffoldWithDrawer(
    modifier: Modifier = Modifier,
    title: String = "Location Tracking",
    showDrawer: Boolean = true,
    currentSession: LocationSession? = null,
    sessions: List<LocationSession> = emptyList(),
    onExportCsv: () -> Unit = {},
    onExportGpx: () -> Unit = {},
    themeViewModel: ThemeViewModel = viewModel(),
    content: @Composable (paddingValue: PaddingValues, snackbarHostState: SnackbarHostState) -> Unit
) {
    // Theme state
    val systemInDarkTheme = isSystemInDarkTheme()
    val themeMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()
    val isDarkMode = themeMode ?: systemInDarkTheme

    // Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }

    val scaffoldContent: @Composable () -> Unit = {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        if (showDrawer) {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Dashboard"
                                )
                            }
                        }
                    },
                    actions = {
                        // Theme toggle in TopBar
                        IconButton(
                            onClick = {
                                themeViewModel.toggleTheme(isDarkMode)
                            }
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (isDarkMode) R.drawable.light_mode else R.drawable.night_mode
                                ),
                                contentDescription = if (isDarkMode) "Light Mode" else "Dark Mode"
                            )
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            content(paddingValues, snackbarHostState)
        }
    }

    if (showDrawer) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    DrawerContent(
                        currentSession = currentSession,
                        sessions = sessions,
                        onExportCsv = onExportCsv,
                        onExportGpx = onExportGpx,
                        onCloseDrawer = {
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        ) {
            scaffoldContent()
        }
    } else {
        scaffoldContent()
    }
}

