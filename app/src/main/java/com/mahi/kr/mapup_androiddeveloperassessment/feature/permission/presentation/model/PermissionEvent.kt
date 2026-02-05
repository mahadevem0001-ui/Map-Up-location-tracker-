package com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.model

import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.domain.model.DeniedPermissionInfo

/**
 * Sealed class representing one-time events sent from ViewModel to UI
 */
sealed class PermissionEvent {
    /**
     * Event to show prominent dialog for permanently denied permissions
     * Triggered when critical permissions are permanently denied (shouldShowRationale = false)
     *
     * @param deniedPermissions List of permissions that are permanently denied
     */
    data class ShowProminentDeniedPermissionsDialog(
        val deniedPermissions: List<DeniedPermissionInfo>
    ) : PermissionEvent()
}
