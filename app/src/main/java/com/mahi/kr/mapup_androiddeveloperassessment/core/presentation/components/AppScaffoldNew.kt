package com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    currentSession: LocationSession? = null,
    sessions: List<LocationSession> = emptyList(),
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    currentSession = currentSession,
                    sessions = sessions,
                    onCloseDrawer = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
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
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Dashboard"
                            )
                        }
                    },
                    actions = {
                        // Theme toggle in TopBar
                        IconButton(
                            onClick = {
                                themeViewModel.toggleTheme(isDarkMode)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = if (isDarkMode) "Light Mode" else "Dark Mode",
                                        duration = SnackbarDuration.Short
                                    )
                                }
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
}

/**
 * Navigation Drawer content showing location summary
 */
@Composable
private fun DrawerContent(
    currentSession: LocationSession?,
    sessions: List<LocationSession>,
    onCloseDrawer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Simple Header
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        HorizontalDivider()

        // Active Session Section (minimal design)
        if (currentSession != null) {
            Text(
                text = "Active Session",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MinimalStatRow("Locations", "${currentSession.locations.size}")

                    if (currentSession.locations.isNotEmpty()) {
                        MinimalStatRow("Distance", currentSession.getFormattedDistance())
                        MinimalStatRow("Avg Speed", currentSession.getFormattedAverageSpeed())
                    }

                    MinimalStatRow("Duration", currentSession.getFormattedDuration())
                }
            }
        }

        // Dashboard Stats Section
        Text(
            text = "Dashboard Stats",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        if (sessions.isNotEmpty() || currentSession != null) {
            val allSessions = if (currentSession != null) sessions + currentSession else sessions
            val totalSessions = allSessions.size
            val totalLocations = allSessions.sumOf { it.locations.size }
            val totalDistance = allSessions.sumOf { it.getTotalDistance() }
            val totalDuration = allSessions.sumOf { it.getDurationMillis().toDouble() }.toLong()
            val avgSpeed = if (totalDistance > 0.0 && totalDuration > 0) {
                (totalDistance / 1000.0) / (totalDuration / 3600000.0)
            } else 0.0

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MinimalStatRow("Total Sessions", totalSessions.toString())
                    MinimalStatRow("Total Locations", totalLocations.toString())
                    MinimalStatRow("Total Distance", formatDistance(totalDistance))
                    MinimalStatRow("Average Speed", "${"%.1f".format(avgSpeed)} km/h")
                    MinimalStatRow("Total Time", formatDuration(totalDuration))
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Start tracking to see stats",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Close button
        TextButton(
            onClick = onCloseDrawer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Close")
        }
    }
}

/**
 * Minimal stat row - clean label-value pair
 */
@Composable
private fun MinimalStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Helper function to format distance
 */
private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        "${"%.2f".format(meters / 1000)} km"
    } else {
        "${"%.0f".format(meters)} m"
    }
}

/**
 * Helper function to format duration
 */
private fun formatDuration(millis: Long): String {
    val hours = millis / 3600000
    val minutes = (millis % 3600000) / 60000
    val seconds = (millis % 60000) / 1000

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}
