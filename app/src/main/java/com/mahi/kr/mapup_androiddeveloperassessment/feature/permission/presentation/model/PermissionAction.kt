package com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.model

/**
 * Sealed class representing all possible user actions in the permission feature
 */
sealed class PermissionAction {
    /**
     * Called when permission state changes (granted/denied)
     *
     * @param permission The permission string
     * @param isGranted Whether the permission was granted
     * @param shouldShowRationale Whether we should show rationale (used to determine permanent denial)
     */
    data class PermissionStateChange(
        val permission: String,
        val isGranted: Boolean,
        val shouldShowRationale: Boolean = false
    ) : PermissionAction()

    /**
     * Called to update the shouldShowRationale flag for a permission
     * This helps determine if permission is permanently denied
     *
     * @param permission The permission string
     * @param shouldShowRationale The current rationale state
     */
    data class UpdateShouldShowRationale(
        val permission: String,
        val shouldShowRationale: Boolean
    ) : PermissionAction()

    /**
     * Called when permissions are about to be requested
     * Marks that we've requested permissions before (no longer first launch)
     */
    data object MarkPermissionsRequested : PermissionAction()

    /**
     * Dismiss the prominent permission dialog
     */
    data object DismissDialog : PermissionAction()
}
