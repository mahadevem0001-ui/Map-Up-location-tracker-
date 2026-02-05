package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationSession

/**
 * Dialog displaying the tracked route on a map
 */
@Composable
fun SessionMapDialog(
    session: LocationSession,
    onDismiss: () -> Unit
) {
    var selectedIndex by remember(session.sessionId) { mutableIntStateOf(0) }
    val totalPoints = session.locations.size
    if (selectedIndex >= totalPoints && totalPoints > 0) selectedIndex = totalPoints - 1

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .padding(12.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Route Map Visualization",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Text(
                            text = "${session.locations.size} location points tracked",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (session.locations.size > 1) {
                            Text(
                                text = "🔵 Accuracy circles • 📍 Tap markers for details",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }

                HorizontalDivider()

                // Detail navigator
                if (session.locations.isNotEmpty()) {
                    val loc = session.locations[selectedIndex.coerceIn(0, session.locations.lastIndex)]
                    val addressLine = loc.address ?: "Lat: ${"%.6f".format(loc.latitude)}, Lon: ${"%.6f".format(loc.longitude)}"
                    val coordsLine = "Lat: ${"%.6f".format(loc.latitude)}, Lon: ${"%.6f".format(loc.longitude)}"
                    val speedText = loc.speed?.let { "Speed: ${"%.1f".format(it * 3.6f)} km/h" } ?: "Speed: --"
                    val accuracyText = loc.accuracy?.let { "Accuracy: ${"%.1f".format(it)} m" } ?: "Accuracy: --"
                    val bearingText = loc.bearing?.let {
                        val dir = loc.getCompassDirection() ?: ""
                        "Bearing: ${"%.0f".format(it)}° $dir"
                    } ?: "Bearing: --"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (selectedIndex > 0) selectedIndex -= 1 },
                            enabled = selectedIndex > 0
                        ) {
                            Icon(Icons.Filled.ArrowBackIos, contentDescription = "Previous point")
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Point ${selectedIndex + 1} of ${session.locations.size}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = addressLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (loc.address != null) {
                                Text(
                                    text = coordsLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = listOf(speedText, accuracyText, bearingText).joinToString("  •  "),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { if (selectedIndex < session.locations.lastIndex) selectedIndex += 1 },
                            enabled = selectedIndex < session.locations.lastIndex
                        ) {
                            Icon(Icons.Filled.ArrowForwardIos, contentDescription = "Next point")
                        }
                    }

                    HorizontalDivider()
                }

                // Map
                if (session.locations.isNotEmpty()) {
                    SessionMapView(
                        session = session,
                        selectedIndex = selectedIndex,
                        onSelectedIndexChange = { newIndex ->
                            selectedIndex = newIndex.coerceIn(0, session.locations.lastIndex)
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No location data available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Route Stats
                HorizontalDivider()

                // Legend for marker colors
                if (session.locations.size > 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendItem(color = "🟢", label = "Start")
                        LegendItem(color = "🔵", label = "Route Points")
                        LegendItem(color = "🔴", label = "End")
                    }
                    HorizontalDivider()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RouteStatItem(
                        label = "Distance",
                        value = session.getFormattedDistance()
                    )
                    RouteStatItem(
                        label = "Duration",
                        value = session.getFormattedDuration()
                    )
                    RouteStatItem(
                        label = "Avg Speed",
                        value = session.getFormattedAverageSpeed()
                    )
                }
            }
        }
    }
}

/**
 * Map view showing the tracked route
 */
@Composable
fun SessionMapView(
    session: LocationSession,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (session.locations.isEmpty()) return

    // Convert locations to LatLng
    val routePoints = remember(session.locations) {
        session.locations.map { location ->
            LatLng(location.latitude, location.longitude)
        }
    }

    // Marker states so we can control info windows for selection
    val markerStates = remember(session.sessionId, session.locations) {
        session.locations.map { location ->
            MarkerState(position = LatLng(location.latitude, location.longitude))
        }
    }

    // Calculate camera position (center of route)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            routePoints.first(),
            15f
        )
    }

    // Move camera to show all points
    LaunchedEffect(routePoints) {
        if (routePoints.size > 1) {
            val bounds = com.google.android.gms.maps.model.LatLngBounds.builder().apply {
                routePoints.forEach { include(it) }
            }.build()
            
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngBounds(
                    bounds,
                    100
                )
            )
        }
    }

    // Focus on selected point when arrows are used
    LaunchedEffect(selectedIndex) {
        if (routePoints.isNotEmpty()) {
            val target = routePoints[selectedIndex.coerceIn(0, routePoints.lastIndex)]
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 17f))
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = false
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            compassEnabled = true,
            myLocationButtonEnabled = false
        )
    ) {
        // Draw route line first (so it appears below markers)
        if (routePoints.size > 1) {
            Polyline(
                points = routePoints,
                color = androidx.compose.ui.graphics.Color.Blue,
                width = 8f
            )
        }

        // Show all location points as markers with accuracy circles
        session.locations.forEachIndexed { index, location ->
            val isFirst = index == 0
            val isLast = index == session.locations.size - 1
            val totalPoints = session.locations.size
            val isSelected = index == selectedIndex
            val markerState = markerStates[index]

            // Determine marker color and details
            val markerColor = when {
                isSelected -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE
                isFirst -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                isLast -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
                else -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE
            }

            val pointLabel = when {
                isFirst -> "🟢 START"
                isLast -> "🔴 END"
                else -> "📍 #${index + 1}"
            }

            // Build detailed title with index count
            val title = "$pointLabel (${index + 1}/$totalPoints)"

            // Build detailed snippet with essential info prominently displayed
            val headline = location.address?.let { "📍 $it" } ?: pointLabel
            val snippet = headline

            val position = markerState.position

            // Draw accuracy circle if accuracy is available
            location.accuracy?.let { accuracy ->
                Circle(
                    center = position,
                    radius = accuracy.toDouble(),
                    fillColor = when {
                        isFirst -> androidx.compose.ui.graphics.Color.Green.copy(alpha = 0.1f)
                        isLast -> androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.1f)
                        else -> androidx.compose.ui.graphics.Color.Blue.copy(alpha = 0.08f)
                    },
                    strokeColor = when {
                        isFirst -> androidx.compose.ui.graphics.Color.Green.copy(alpha = 0.3f)
                        isLast -> androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.3f)
                        else -> androidx.compose.ui.graphics.Color.Blue.copy(alpha = 0.2f)
                    },
                    strokeWidth = 2f
                )
            }

            // Draw marker
            Marker(
                state = markerState,
                title = title,
                snippet = snippet,
                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(markerColor),
                alpha = when {
                    isSelected -> 1.0f
                    isFirst || isLast -> 0.95f
                    else -> 0.82f
                },
                zIndex = when {
                    isSelected -> 2f
                    isFirst || isLast -> 1.5f
                    else -> 1f
                },
                onClick = {
                    onSelectedIndexChange(index)
                    // Consume to keep camera/selection sync; show info window for selected
                    markerState.showInfoWindow()
                    true
                }
            )
        }
    }

    // Keep selected marker info window visible
    LaunchedEffect(selectedIndex, markerStates) {
        markerStates.forEachIndexed { idx, state ->
            if (idx == selectedIndex) state.showInfoWindow() else state.hideInfoWindow()
        }
    }
}

/**
 * Stat item for route details
 */
@Composable
private fun RouteStatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Legend item for marker colors
 */
@Composable
private fun LegendItem(
    color: String,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = color,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

