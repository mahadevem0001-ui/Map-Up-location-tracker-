package com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.screen

import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mahi.kr.mapup_androiddeveloperassessment.core.util.compose.ObserveAsEvents
import com.mahi.kr.mapup_androiddeveloperassessment.core.util.extensions.openAppSettings
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.LocationService
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.viewmodel.LocationViewModel
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.domain.model.DeniedPermissionInfo
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.components.DeniedPermissionItem
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.components.LocationPermissionInfoCard
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.components.PermissionInfoCard
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.components.ProminentDeniedPermissionsDialog
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.model.PermissionAction
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.model.PermissionEvent
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.viewmodel.PermissionViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Main permission screen that handles permission requests and displays permission states
 *
 * Features:
 * - Lifecycle-aware permission checking
 * - Informative permission cards before requesting
 * - Real-time status updates when returning from Settings
 * - Grouped location permissions display
 * - Denied permissions list with visual distinction
 * - Prominent dialog for permanently denied permissions
 * - Uses AppScaffold for consistent UI
 */
@Composable
fun PermissionScreen(
    viewModel: PermissionViewModel = /*viewModel(factory = PermissionViewModel.Factory)*/koinViewModel<PermissionViewModel>(),
    snackbarHostState: SnackbarHostState
) {
    val logTag = "PermissionScreen"
    val context = LocalContext.current
    val activity = context as? ComponentActivity ?: return

    // Location ViewModel to stop tracking when permissions are revoked
    val locationViewModel: LocationViewModel = koinViewModel()

    val state by viewModel.state.collectAsStateWithLifecycle()

    var showLocationInfoDialog by remember { mutableStateOf(false) }


    var showProminentDialog by remember { mutableStateOf(false) }
    var deniedPermissionsList by remember { mutableStateOf<List<DeniedPermissionInfo>>(emptyList()) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(logTag, "Permission launcher result: ${permissions.map { it.key to it.value }} : ${deniedPermissionsList.isNotEmpty()}")
        permissions.entries.forEach { entry ->
            val permission = entry.key
            val isGranted = entry.value
            val shouldShowRationale = activity.shouldShowRequestPermissionRationale(permission)

            viewModel.onAction(
                PermissionAction.PermissionStateChange(
                    permission = permission,
                    isGranted = isGranted,
                    shouldShowRationale = shouldShowRationale
                )
            )

        }

//        if (!isGranted/* && (permission == android.Manifest.permission.ACCESS_FINE_LOCATION || permission == android.Manifest.permission.ACCESS_COARSE_LOCATION)*/) {
//            Log.w(logTag, "Location permission denied from launcher: $permission")
//            locationViewModel.handlePermissionRevokedFromUi()
//        }


        if(permissions.entries.any {  (_, granted) ->
            !granted
        } && deniedPermissionsList.isNotEmpty()){
            Log.d(logTag, "Some permissions denied, showing prominent dialog if needed")
            locationViewModel.handlePermissionRevokedFromUi()
        }
    }

    // Lifecycle observer to check permissions on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d(logTag, "Lifecycle ON_RESUME: re-checking permissions")
                PermissionViewModel.requiredPermissionsSet.forEach { permission ->
                    val isGranted = ContextCompat.checkSelfPermission(
                        activity,
                        permission
                    ) == PackageManager.PERMISSION_GRANTED

                    val shouldShowRationale =
                        activity.shouldShowRequestPermissionRationale(permission)

                    viewModel.onAction(
                        PermissionAction.PermissionStateChange(
                            permission = permission,
                            isGranted = isGranted,
                            shouldShowRationale = shouldShowRationale
                        )
                    )

                    if (!isGranted /*&& (permission == android.Manifest.permission.ACCESS_FINE_LOCATION || permission == android.Manifest.permission.ACCESS_COARSE_LOCATION)*/) {
                        Log.w(logTag, "Location permission missing on resume: $permission")
                        locationViewModel.handlePermissionRevokedFromUi()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
//    LaunchedEffect(Unit) {
//        Log.d(logTag, "LaunchedEffect: sending ACTION_STOP to LocationService on entry")
//        Intent(context, LocationService::class.java).apply {
//            action = LocationService.ACTION_STOP
//        }.also {
//            context.startService(it)
//        }
//    }

    // Observe events
    ObserveAsEvents(flow = viewModel.events) { event ->
        when (event) {
            is PermissionEvent.ShowProminentDeniedPermissionsDialog -> {
                Log.d(logTag, "ShowProminentDeniedPermissionsDialog for ${event.deniedPermissions.size} perms")
                deniedPermissionsList = event.deniedPermissions
                showProminentDialog = true
            }
        }
    }

//    AppScaffold(
//        modifier = Modifier.fillMaxSize()
//    ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
//                .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Permission Management",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Content based on state
        if (!state.hasRequestedPermissionsBefore) {
            // Informative view
            Text(
                text = "This app requires the following permissions to function properly:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Group permissions
            val groupedPermissions = PermissionViewModel.requiredPermissionsSet
                .groupBy { permission ->
                    when (permission) {
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION -> "location"

                        else -> permission
                    }
                }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = groupedPermissions.entries.toList(),
                    key = { it.key }
                ) { entry ->
                    if (entry.key == "location") {
                        LocationPermissionInfoCard(
                            permissions = entry.value,
                            viewModel = viewModel,
                            state = state,
                            onInfoClick = { showLocationInfoDialog = true }
                        )
                    } else {
                        PermissionInfoCard(
                            permission = entry.value.first(),
                            viewModel = viewModel,
                            state = state
                        )
                    }
                }
            }
        } else {
            // Status view
            Text(
                text = "Permission Status:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Request button
        Button(
            onClick = {
                Log.d(logTag, "Request permissions button clicked")
                viewModel.onAction(PermissionAction.MarkPermissionsRequested)

                PermissionViewModel.requiredPermissionsSet.forEach { permission ->
                    val shouldShowRationale =
                        activity.shouldShowRequestPermissionRationale(permission)
                    viewModel.onAction(
                        PermissionAction.UpdateShouldShowRationale(
                            permission = permission,
                            shouldShowRationale = shouldShowRationale
                        )
                    )
                }

                permissionLauncher.launch(
                    PermissionViewModel.requiredPermissionsSet.toTypedArray()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (!state.hasRequestedPermissionsBefore) "Request Permissions"
                else "Request Permissions Again"
            )
        }

        // Denied permissions section
        if (state.hasRequestedPermissionsBefore) {
            HorizontalDivider()

            if (state.deniedPermissions.isNotEmpty()) {
                Text(
                    text = "Denied Permissions (${state.deniedPermissions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = state.deniedPermissions.values.toList(),
                        key = { it.permission }
                    ) { deniedInfo ->
                        DeniedPermissionItem(
                            deniedInfo = deniedInfo,
                            viewModel = viewModel
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "✓ All permissions granted",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
//    }

    // Prominent dialog
    if (showProminentDialog && deniedPermissionsList.isNotEmpty()) {
        ProminentDeniedPermissionsDialog(
            deniedPermissions = deniedPermissionsList,
            viewModel = viewModel,
            onDismiss = {
                showProminentDialog = false
                viewModel.onAction(PermissionAction.DismissDialog)
            },
            onGoToSettings = {
                activity.openAppSettings()
                showProminentDialog = false
            }
        )
    }

    AnimatedVisibility(showLocationInfoDialog) {
        AlertDialog(
            onDismissRequest = { showLocationInfoDialog = false },
            title = { Text("Background vs in-use location") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Allow all the time: Open app settings and choose \"Allow all the time\" so tracking can restart automatically after device reboot without opening the app.")
                    Text("• While using the app: Use the regular permission prompt. After a reboot, you'll need to reopen the app to restart tracking.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    activity.openAppSettings()
                    showLocationInfoDialog = false
                }) { Text("Open app settings") }
            },
            dismissButton = {
                TextButton(onClick = { showLocationInfoDialog = false }) { Text("Close") }
            }
        )
    }
}
