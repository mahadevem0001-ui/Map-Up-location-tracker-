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
    val speed: Float? = null,
    val bearing: Float? = null, // Direction of travel in degrees (0-360), null if stationary
    val address: String? = null // Reverse geocoded address
) {
    fun toDisplayString(): String {
        return "Lat: ${"%.6f".format(latitude)}, Lng: ${"%.6f".format(longitude)}"
    }

    fun getFormattedTime(): String {
        val dateFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date(timestamp))
    }

    /**
     * Get compass direction from bearing
     * @return Direction string (N, NE, E, SE, S, SW, W, NW) or null if no bearing
     */
    fun getCompassDirection(): String? {
        return bearing?.let {
            val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            val index = ((it + 22.5) / 45).toInt() % 8
            directions[index]
        }
    }
}
