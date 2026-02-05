package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.util

import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationData
import kotlin.math.*

/**
 * Location utilities for distance and bearing calculations
 */
object LocationUtils {

    private const val EARTH_RADIUS_METERS = 6371000.0 // Earth's radius in meters

    /**
     * Calculate distance between two locations using Haversine formula
     * @param from Starting location
     * @param to Ending location
     * @return Distance in meters
     */
    fun calculateDistance(from: LocationData, to: LocationData): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val deltaLat = Math.toRadians(to.latitude - from.latitude)
        val deltaLon = Math.toRadians(to.longitude - from.longitude)

        val a = sin(deltaLat / 2).pow(2.0) +
                cos(lat1) * cos(lat2) *
                sin(deltaLon / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Calculate bearing between two locations
     * @param from Starting location
     * @param to Ending location
     * @return Bearing in degrees (0-360)
     */
    fun calculateBearing(from: LocationData, to: LocationData): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val deltaLon = Math.toRadians(to.longitude - from.longitude)

        val y = sin(deltaLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) -
                sin(lat1) * cos(lat2) * cos(deltaLon)

        val bearing = Math.toDegrees(atan2(y, x))

        return (bearing + 360) % 360 // Normalize to 0-360
    }

    /**
     * Calculate total distance for a list of locations
     * @param locations List of locations in order
     * @return Total distance in meters
     */
    fun calculateTotalDistance(locations: List<LocationData>): Double {
        if (locations.size < 2) return 0.0

        var totalDistance = 0.0
        for (i in 0 until locations.size - 1) {
            totalDistance += calculateDistance(locations[i], locations[i + 1])
        }

        return totalDistance
    }

    /**
     * Format distance for display
     * @param meters Distance in meters
     * @return Formatted string (e.g., "1.5 km" or "250 m")
     */
    fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format("%.2f km", meters / 1000)
        } else {
            String.format("%.0f m", meters)
        }
    }

    /**
     * Calculate average speed
     * @param locations List of locations with timestamps
     * @return Average speed in m/s, or null if not enough data
     */
    fun calculateAverageSpeed(locations: List<LocationData>): Double? {
        if (locations.size < 2) return null

        val distance = calculateTotalDistance(locations)
        val timeSeconds = (locations.last().timestamp - locations.first().timestamp) / 1000.0

        return if (timeSeconds > 0) distance / timeSeconds else null
    }
}
