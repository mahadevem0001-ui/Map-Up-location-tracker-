package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model

/**
 * Data class representing a location update with timestamp
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracy: Float? = null,
    val altitude: Double? = null,
    val speed: Float? = null
) {
    fun toDisplayString(): String {
        return "Lat: ${"%.6f".format(latitude)}, Lng: ${"%.6f".format(longitude)}"
    }

    fun getFormattedTime(): String {
        val dateFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date(timestamp))
    }
}
