package com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.domain.model

/**
 * Domain model representing information about a denied permission
 *
 * @param permission The permission string (e.g., android.permission.ACCESS_FINE_LOCATION)
 * @param shouldShowRationale If false after first denial, permission is permanently denied
 */
data class DeniedPermissionInfo(
    val permission: String,
    val shouldShowRationale: Boolean = false
)
