package com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.viewmodel

import android.Manifest
import android.app.Application
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.mahi.kr.mapup_androiddeveloperassessment.core.data.local.AppPreferencesManager
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.domain.model.DeniedPermissionInfo
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.domain.model.PermissionState
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.model.PermissionAction
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.model.PermissionEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for permission management following MVVM and Clean Architecture principles
 *
 * Responsibilities:
 * - Manage permission state (denied list, rationale flags)
 * - Persist permission request flag across app restarts
 * - Send one-time events to UI (prominent dialogs)
 * - Handle permission state changes from UI
 */
class PermissionViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val TAG = "PermissionViewModel"

        /**
         * Set of required permissions for the app
         * Includes location permissions and notification permission (Android 13+)
         */
        val requiredPermissionsSet = buildSet<String> {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        /**
         * Factory for creating PermissionViewModel instances
         * Required because AndroidViewModel needs Application parameter
         */
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from CreationExtras
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                    "Application not available in CreationExtras"
                }
                return PermissionViewModel(application) as T
            }
        }
    }

    // Persistence manager to store state across app restarts
    private val prefsManager = AppPreferencesManager(application)

    // Store Application context for permission checking
    private val appContext = application.applicationContext

    // StateFlow for UI state - using MutableStateFlow for internal updates
    // IMPORTANT: Must be initialized BEFORE init block because init calls checkAndRestorePermissionStates()
    // which needs to access _state
    private val _state = MutableStateFlow(PermissionState())
    val state = _state.asStateFlow()

    // Channel for one-time events - prevents duplicate event delivery on config changes
    private val _events = Channel<PermissionEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        Log.d(TAG, "PermissionViewModel initialized with permissions: $requiredPermissionsSet")

        // Observe hasRequestedPermissionsBefore from DataStore
        viewModelScope.launch {
            prefsManager.hasRequestedPermissionsBefore.collect { hasRequested ->
                Log.d(TAG, "Loaded persisted state - hasRequestedPermissionsBefore: $hasRequested")

                // Update state with persisted value
                _state.update { it.copy(hasRequestedPermissionsBefore = hasRequested) }

                // If permissions were requested before, check current states and restore denied list
                if (hasRequested) {
                    Log.d(TAG, "Permissions were requested before - checking current states")
                    checkAndRestorePermissionStates()
                }
            }
        }
    }

    /**
     * Check current permission states and restore denied permissions list
     * Called during ViewModel initialization if permissions were requested before
     * This ensures correct UI state after app restart/process death
     */
    private fun checkAndRestorePermissionStates() {
        requiredPermissionsSet.forEach { permission ->
            val isGranted = appContext.checkSelfPermission(permission) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                // Permission not granted - add to denied list
                // Note: We can't check shouldShowRationale here because we need Activity context
                // The Activity will update this via DisposableEffect when it's available
                val deniedInfo = DeniedPermissionInfo(
                    permission = permission,
                    shouldShowRationale = false // Will be updated by Activity's DisposableEffect
                )

                _state.update { currentState ->
                    val deniedPermissions = currentState.deniedPermissions.toMutableMap()
                    deniedPermissions[permission] = deniedInfo
                    currentState.copy(deniedPermissions = deniedPermissions)
                }

                Log.d(TAG, "Restored denied permission: $permission")
            }
        }
    }

    /**
     * Handle actions from the UI
     */
    fun onAction(action: PermissionAction) {
        when (action) {
            is PermissionAction.PermissionStateChange -> {
                handlePermissionStateChange(action)
            }

            is PermissionAction.UpdateShouldShowRationale -> {
                updateShouldShowRationale(action.permission, action.shouldShowRationale)
            }

            is PermissionAction.MarkPermissionsRequested -> {
                markPermissionsRequested()
            }

            is PermissionAction.DismissDialog -> {
                dismissProminentDialog()
            }
        }
    }

    /**
     * Handle permission state changes when user grants or denies permissions
     *
     * Logic:
     * - If granted: Remove from denied list
     * - If denied: Add to denied list with shouldShowRationale flag
     * - If permanently denied AND hasRequestedPermissionsBefore: Trigger prominent dialog
     *
     * Important: Prominent dialog only shows AFTER first request, not on initial launch
     * This prevents confusing users on first app open
     */
    private fun handlePermissionStateChange(action: PermissionAction.PermissionStateChange) {
        _state.update { currentState ->
            val deniedPermissions = currentState.deniedPermissions.toMutableMap()

            if (action.isGranted) {
                // Permission granted - remove from denied list
                deniedPermissions.remove(action.permission)
                Log.d(TAG, "Permission granted: ${action.permission}")
            } else {
                // Permission denied - add/update in denied list
                val deniedInfo = DeniedPermissionInfo(
                    permission = action.permission,
                    shouldShowRationale = action.shouldShowRationale
                )
                deniedPermissions[action.permission] = deniedInfo

                Log.d(TAG, "Permission denied: ${action.permission}, shouldShowRationale: ${action.shouldShowRationale}, hasRequestedBefore: ${currentState.hasRequestedPermissionsBefore}")

                // Check if this is a permanently denied prominent permission
                // shouldShowRationale = false means user selected "Don't ask again" or permanently denied
                // ONLY show prominent dialog if we've requested permissions at least once before
                // This prevents showing the dialog on very first app launch
                if (isProminentPermission(action.permission) &&
                    !action.shouldShowRationale &&
                    currentState.hasRequestedPermissionsBefore) {
                    // Send event to show prominent dialog for permanently denied permissions
                    viewModelScope.launch {
                        val permanentlyDenied = deniedPermissions.values.filter {
                            isProminentPermission(it.permission) && !it.shouldShowRationale
                        }
                        _events.send(
                            PermissionEvent.ShowProminentDeniedPermissionsDialog(permanentlyDenied)
                        )
                        Log.d(TAG, "Triggering prominent dialog for permanently denied permissions: $permanentlyDenied")
                    }
                }
            }

            currentState.copy(deniedPermissions = deniedPermissions)
        }
    }

    /**
     * Update shouldShowRationale flag for a permission
     * Called before requesting permission to track the rationale state
     */
    private fun updateShouldShowRationale(permission: String, shouldShowRationale: Boolean) {
        _state.update { currentState ->
            val deniedPermissions = currentState.deniedPermissions.toMutableMap()

            // Only update if permission is already in denied list
            deniedPermissions[permission]?.let { info ->
                deniedPermissions[permission] = info.copy(shouldShowRationale = shouldShowRationale)
            }

            currentState.copy(deniedPermissions = deniedPermissions)
        }
    }

    /**
     * Mark that permissions have been requested at least once
     * This flag is used to determine if we should show prominent dialog on permanent denial
     * IMPORTANT: This is persisted to DataStore to survive app restarts
     */
    private fun markPermissionsRequested() {
        viewModelScope.launch {
            _state.update { it.copy(hasRequestedPermissionsBefore = true) }
            prefsManager.setHasRequestedPermissions(true)
            Log.d(TAG, "Marked permissions as requested (persisted)")
        }
    }

    /**
     * Dismiss the prominent permission dialog
     */
    private fun dismissProminentDialog() {
        _state.update { it.copy(showProminentDialog = false) }
        Log.d(TAG, "Prominent dialog dismissed")
    }

    /**
     * Check if permission is prominent (critical for app functionality)
     * Prominent permissions require special UI treatment when permanently denied
     */
    private fun isProminentPermission(permission: String): Boolean {
        return permission == Manifest.permission.ACCESS_FINE_LOCATION ||
                permission == Manifest.permission.ACCESS_COARSE_LOCATION ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        permission == Manifest.permission.POST_NOTIFICATIONS)
    }

    /**
     * Get all permanently denied permissions (shouldShowRationale = false)
     */
    fun getPermanentlyDeniedPermissions(): List<DeniedPermissionInfo> {
        return _state.value.deniedPermissions.values.filter { !it.shouldShowRationale }
    }

    /**
     * Get all denied permissions that can still show rationale (first time denial)
     */
    fun getDeniedWithRationalePermissions(): List<DeniedPermissionInfo> {
        return _state.value.deniedPermissions.values.filter { it.shouldShowRationale }
    }

    /**
     * Get human-readable name for permission
     */
    fun getPermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> "Location (Precise)"
            Manifest.permission.ACCESS_COARSE_LOCATION -> "Location (Approximate)"
            Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
            else -> permission.substringAfterLast('.')
        }
    }
}
