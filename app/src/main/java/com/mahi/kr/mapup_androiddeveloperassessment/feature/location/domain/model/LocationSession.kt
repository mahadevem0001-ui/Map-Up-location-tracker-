package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model

import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.util.LocationUtils

/**
 * Represents a tracking session with start/stop times and location history
 */
data class LocationSession(
    val sessionId: Long = System.currentTimeMillis(),
    val startTime: Long,
    val endTime: Long? = null,
    val locations: List<LocationData> = emptyList()
) {
    fun isActive(): Boolean = endTime == null

    fun getFormattedDuration(): String {
        val duration = (endTime ?: System.currentTimeMillis()) - startTime
        val minutes = (duration / 1000) / 60
        val seconds = (duration / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun getFormattedStartTime(): String {
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm:ss", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date(startTime))
    }

    fun getFormattedEndTime(): String {
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm:ss", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date(endTime ?: System.currentTimeMillis()))
    }

    fun getLocationCount(): Int = locations.size

    /**
     * Calculate total distance traveled in this session
     * @return Distance in meters
     */
    fun getTotalDistance(): Double {
        return LocationUtils.calculateTotalDistance(locations)
    }

    /**
     * Get formatted distance string
     * @return Formatted distance (e.g., "1.5 km" or "250 m")
     */
    fun getFormattedDistance(): String {
        return LocationUtils.formatDistance(getTotalDistance())
    }

    /**
     * Calculate average speed for this session
     * @return Average speed in m/s, or null if not enough data
     */
    fun getAverageSpeed(): Double? {
        return LocationUtils.calculateAverageSpeed(locations)
    }

    /**
     * Get formatted average speed
     * @return Formatted speed in km/h, or "N/A"
     */
    fun getFormattedAverageSpeed(): String {
        val speedMs = getAverageSpeed()
        return speedMs?.let {
            String.format("%.1f km/h", it * 3.6)
        } ?: "N/A"
    }
}
