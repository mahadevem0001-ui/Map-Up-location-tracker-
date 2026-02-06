package com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.domain.model

import androidx.compose.runtime.Immutable

/**
 * Domain model representing the complete permission state of the application
 *
 * @param deniedPermissions Map of permission string to DeniedPermissionInfo
 * @param showProminentDialog Whether to show the prominent permission dialog
 * @param hasRequestedPermissionsBefore Flag to track if permissions have been requested at least once
 */
@Immutable
data class PermissionState(
    val deniedPermissions: Map<String, DeniedPermissionInfo> = emptyMap(),
    val showProminentDialog: Boolean = false,
    val hasRequestedPermissionsBefore: Boolean = false,
    val isLoaded: Boolean = false
)
