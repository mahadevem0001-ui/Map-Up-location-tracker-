package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.LocationService
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.ILocationClient
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationData
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationSession
import kotlinx.coroutines.Job
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
        const val MAX_SESSIONS = 50 // Keep last 50 sessions
    }

    private val _state = MutableStateFlow(LocationTrackingState())
    val state: StateFlow<LocationTrackingState> = _state.asStateFlow()

    // Job to control location updates collection
    private var locationUpdatesJob: Job? = null

    init {
        // Check if service is already running when ViewModel is created
        checkServiceState()

        // If service is running, start collecting location updates
        if (LocationService.isRunning()) {
            startLocationUpdates()
        }
    }

    /**
     * Check if the service is currently running and update state accordingly
     */
    fun checkServiceState() {
        val isRunning = LocationService.isRunning()
        _state.update { it.copy(isServiceRunning = isRunning) }
    }

    /**
     * Start the location tracking service
     */
    fun startLocationService() {
        // Check if service is already running
        if (LocationService.isRunning()) {
            _state.update { it.copy(isServiceRunning = true, error = null) }
            return
        }

        try {
            val intent = Intent(application, LocationService::class.java).apply {
                action = LocationService.ACTION_START
            }
            application.startForegroundService(intent)

            // Create new session
            val newSession = LocationSession(
                startTime = System.currentTimeMillis(),
                endTime = null,
                locations = emptyList()
            )

            _state.update {
                it.copy(
                    isServiceRunning = true,
                    currentSession = newSession,
                    error = null
                )
            }

            // Start collecting location updates in the ViewModel
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

            // Cancel location updates collection
            locationUpdatesJob?.cancel()
            locationUpdatesJob = null

            // End current session and add to history
            _state.update { currentState ->
                val endedSession = currentState.currentSession?.copy(
                    endTime = System.currentTimeMillis()
                )

                val updatedSessions = if (endedSession != null && endedSession.locations.isNotEmpty()) {
                    (listOf(endedSession) + currentState.sessions).take(MAX_SESSIONS)
                } else {
                    currentState.sessions
                }

                currentState.copy(
                    isServiceRunning = false,
                    currentSession = null,
                    currentLocation = null,
                    sessions = updatedSessions,
                    error = null
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(error = "Failed to stop service: ${e.message}") }
        }
    }

    /**
     * Start collecting location updates from the location client
     */
    private fun startLocationUpdates() {
        // Cancel existing job if any
        locationUpdatesJob?.cancel()

        locationUpdatesJob = locationClient.getLocationUpdates(5_000L) // Update every 5 seconds
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
                    // Add location to current session
                    val updatedSession = currentState.currentSession?.copy(
                        locations = currentState.currentSession.locations + locationData
                    )

                    currentState.copy(
                        currentLocation = locationData,
                        currentSession = updatedSession,
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
     * Clear all session history
     */
    fun clearAllSessions() {
        _state.update { it.copy(sessions = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        locationUpdatesJob?.cancel()
        // Service will continue running in foreground, user must explicitly stop it
    }
}

/**
 * State for location tracking with session management
 */
data class LocationTrackingState(
    val isServiceRunning: Boolean = false,
    val currentLocation: LocationData? = null,
    val currentSession: LocationSession? = null,
    val sessions: List<LocationSession> = emptyList(),
    val error: String? = null
)
