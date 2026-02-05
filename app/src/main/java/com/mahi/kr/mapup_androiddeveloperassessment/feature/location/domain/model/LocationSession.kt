package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model

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
}
