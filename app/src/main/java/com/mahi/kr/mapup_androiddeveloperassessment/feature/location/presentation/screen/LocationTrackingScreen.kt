package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahi.kr.mapup_androiddeveloperassessment.core.util.extensions.hasAllPermissions
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationData
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationSession
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.components.SessionCard
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.model.LocationEvent
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.components.SessionMapDialog
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.viewmodel.LocationViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Screen for displaying location tracking controls and location history
 *
 * Features:
 * - Start/Stop buttons for location service
 * - Current location display
 * - Location history in a lazy list
 * - Error handling with Snackbar
 * - Auto-scroll to show active session
 */
@Composable
fun LocationTrackingScreen(
    viewModel: LocationViewModel = koinViewModel(),
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is LocationEvent.ShowMessage -> snackbarHostState.showSnackbar(
                    message = event.message, duration = SnackbarDuration.Short
                )

                is LocationEvent.PermissionRevoked -> {
                    if (!context.hasAllPermissions()) {
                        snackbarHostState.showSnackbar(
                            message = event.message, duration = SnackbarDuration.Short
                        )
                    }
                }

                is LocationEvent.ProviderDisabled -> {
                    snackbarHostState.showSnackbar(
                        message = event.message, duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Auto-scroll to top when active session becomes available
    LaunchedEffect(state.currentSession?.sessionId) {
        if (state.currentSession != null) {
            // Scroll to top to show the active session
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Service Status Card
        Card(
            modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = if (state.isServiceRunning) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = if (state.isServiceRunning) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = "Service Status: ${if (state.isServiceRunning) "Running" else "Stopped"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (state.isServiceRunning && state.currentLocation != null) {
                    HorizontalDivider()
                    Text(
                        text = "Current Location:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state.currentLocation!!.toDisplayString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Updated at: ${state.currentLocation!!.getFormattedTime()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Interval Configuration Card (only show when not tracking)
        AnimatedVisibility(visible = !state.isServiceRunning) {
            Card(
                modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Update Interval",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Current interval display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${state.locationIntervalSeconds} seconds",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Interval selection chips
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Text(
                            text = "Quick:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        listOf(1, 5, 10, 30, 60, 120, 180, 240).forEach { seconds ->
                            FilterChip(
                                selected = state.locationIntervalSeconds == seconds,
                                onClick = { viewModel.updateLocationInterval(seconds) },
                                label = {
                                    Text(
                                        text = when {
                                            seconds < 60 -> "${seconds}s"
                                            seconds % 60 == 0 -> "${seconds / 60}m"
                                            else -> "${seconds}s"
                                        }, style = MaterialTheme.typography.labelMedium
                                    )
                                })
                        }
                    }
                }
            }
        }

        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.startLocationService() },
                enabled = !state.isServiceRunning,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Tracking")
            }

            Button(
                onClick = { viewModel.stopLocationService() },
                enabled = state.isServiceRunning,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Stop",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop Tracking")
            }
        }

        // Location History Header with Clear Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tracking Sessions (${state.sessions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (state.sessions.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearAllSessions() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear All Sessions",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        AnimatedContent(
            state.currentSession == null && state.sessions.isEmpty(),
            label = "Tracking Sessions Transition",
            transitionSpec = {
                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(
                    animationSpec = tween(500)
                )
            }) { trackingSessionsInActive ->
            if (trackingSessionsInActive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tracking sessions yet.\nStart tracking to create a new session.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Current active session
                    state.currentSession?.let { session ->
                        item(key = "current_session") {
                            SessionCard(
                                session = session, isActive = true
                            )
                        }
                    }

                    // Previous sessions
                    items(
                        items = state.sessions, key = { it.sessionId }) { session ->
                        SessionCard(
                            session = session, isActive = false
                        )
                    }
                }
            }
        }
    }
}
