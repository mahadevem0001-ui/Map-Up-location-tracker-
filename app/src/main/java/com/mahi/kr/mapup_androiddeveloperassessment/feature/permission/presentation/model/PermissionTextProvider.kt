package com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.model

import android.Manifest

/**
 * Interface for providing permission-specific text descriptions
 * This allows each permission to have customized messaging based on its state
 */
interface PermissionTextProvider {
    /**
     * Get description text for the permission based on its denial state
     *
     * @param isPermanentlyDeclined true if permission is permanently denied (shouldShowRationale = false)
     * @return Human-readable description of what the permission is used for
     */
    fun getDescription(isPermanentlyDeclined: Boolean): String
}

/**
 * Text provider for location permissions (FINE and COARSE)
 */
class LocationPermissionTextProvider : PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if (isPermanentlyDeclined) {
            "Location access is permanently denied. This app needs your location (both precise and approximate) to show nearby places and provide location-based services. Please enable both location permissions in Settings."
        } else {
            "This app requires access to your location to show nearby places and provide location-based services. You'll be asked to grant both Precise Location (for exact positioning) and Approximate Location (for general area) permissions."
        }
    }
}

/**
 * Text provider for notification permission (POST_NOTIFICATIONS on Android 13+)
 */
class NotificationPermissionTextProvider : PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if (isPermanentlyDeclined) {
            "Notification permission is permanently denied. This app needs notification access to alert you about important updates. Please enable it in Settings."
        } else {
            "Notification permission is required to keep you informed about important updates. Please grant this permission to continue."
        }
    }
}

/**
 * Factory to get the appropriate text provider for a given permission
 */
object PermissionTextProviderFactory {
    fun getProvider(permission: String): PermissionTextProvider {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> LocationPermissionTextProvider()

            Manifest.permission.POST_NOTIFICATIONS -> NotificationPermissionTextProvider()

            else -> object : PermissionTextProvider {
                override fun getDescription(isPermanentlyDeclined: Boolean): String {
                    return if (isPermanentlyDeclined) {
                        "This permission is permanently denied. Please enable it in Settings."
                    } else {
                        "This permission is required for the app to function properly."
                    }
                }
            }
        }
    }
}
