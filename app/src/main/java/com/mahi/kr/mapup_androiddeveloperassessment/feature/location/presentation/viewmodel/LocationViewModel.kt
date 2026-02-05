package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.LocationService
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.ILocationClient
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * ViewModel for managing location tracking
 *
 * Responsibilities:
 * - Start/Stop LocationService
 * - Collect location updates from LocationClient
 * - Maintain list of location history for display
 * - Manage service state
 */
class LocationViewModel(
    private val application: Application,
    private val locationClient: ILocationClient
) : AndroidViewModel(application) {

    companion object {
        const val TAG = "LocationViewModel"
        const val MAX_LOCATION_HISTORY = 100 // Keep last 100 locations
    }

    private val _state = MutableStateFlow(LocationTrackingState())
    val state: StateFlow<LocationTrackingState> = _state.asStateFlow()

    /**
     * Start the location tracking service
     */
    fun startLocationService() {
        try {
            val intent = Intent(application, LocationService::class.java).apply {
                action = LocationService.ACTION_START
            }
            application.startForegroundService(intent)

            _state.update { it.copy(isServiceRunning = true, error = null) }

            // Also start collecting location updates in the ViewModel
            startLocationUpdates()
        } catch (e: Exception) {
            _state.update { it.copy(error = "Failed to start service: ${e.message}") }
        }
    }

    /**
     * Stop the location tracking service
     */
    fun stopLocationService() {
        try {
            val intent = Intent(application, LocationService::class.java).apply {
                action = LocationService.ACTION_STOP
            }
            application.startService(intent)

            _state.update { it.copy(isServiceRunning = false, error = null) }
        } catch (e: Exception) {
            _state.update { it.copy(error = "Failed to stop service: ${e.message}") }
        }
    }

    /**
     * Start collecting location updates from the location client
     */
    private fun startLocationUpdates() {
        locationClient.getLocationUpdates(5_000L) // Update every 5 seconds
            .catch { exception ->
                _state.update {
                    it.copy(
                        error = "Location error: ${exception.message}",
                        isServiceRunning = false
                    )
                }
            }
            .onEach { location ->
                val locationData = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = System.currentTimeMillis(),
                    accuracy = location.accuracy,
                    altitude = location.altitude,
                    speed = location.speed
                )

                _state.update { currentState ->
                    val updatedHistory = (listOf(locationData) + currentState.locationHistory)
                        .take(MAX_LOCATION_HISTORY)

                    currentState.copy(
                        currentLocation = locationData,
                        locationHistory = updatedHistory,
                        error = null
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Clear the error message
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Clear location history
     */
    fun clearHistory() {
        _state.update { it.copy(locationHistory = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        // Service will continue running in foreground, user must explicitly stop it
    }
}

/**
 * State for location tracking
 */
data class LocationTrackingState(
    val isServiceRunning: Boolean = false,
    val currentLocation: LocationData? = null,
    val locationHistory: List<LocationData> = emptyList(),
    val error: String? = null
)
