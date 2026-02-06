package com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.components.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.components.MinimalStatRow
import com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.components.formatDistance
import com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.components.formatDuration
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationSession


/**
 * Navigation Drawer content showing location summary
 */
@Composable
fun DrawerContent(
    currentSession: LocationSession?,
    sessions: List<LocationSession>,
    onExportCsv: () -> Unit,
    onExportGpx: () -> Unit,
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

        // Export Section
        if (sessions.isNotEmpty() || currentSession != null) {
            HorizontalDivider()

            Text(
                text = "Export Data",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("CSV")
                }

                OutlinedButton(
                    onClick = onExportGpx,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("GPX")
                }
            }
        }

        // Close button
        TextButton(
            onClick = onCloseDrawer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Close")
        }
    }
}

