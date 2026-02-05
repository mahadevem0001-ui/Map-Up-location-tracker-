package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mahi.kr.mapup_androiddeveloperassessment.core.domain.Result
import com.mahi.kr.mapup_androiddeveloperassessment.core.domain.onError
import com.mahi.kr.mapup_androiddeveloperassessment.core.domain.onSuccess
import com.mahi.kr.mapup_androiddeveloperassessment.core.domain.toUiText
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.LocationService
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.ILocationClient
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationData
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationSession
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.repository.LocationSessionRepository
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.usecase.ExportSessionToCsvUseCase
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.usecase.ExportSessionToGpxUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for managing location tracking
 *
 * Responsibilities:
 * - Start/Stop LocationService
 * - Collect location updates from LocationClient
 * - Persist sessions and locations to Room database
 * - Load sessions from database on initialization
 * - Manage service state
 */
class LocationViewModel(
    private val application: Application,
    private val locationClient: ILocationClient,
    private val repository: LocationSessionRepository,
    private val exportToCsvUseCase: ExportSessionToCsvUseCase,
    private val exportToGpxUseCase: ExportSessionToGpxUseCase
) : AndroidViewModel(application) {

    companion object {
        const val TAG = "LocationViewModel"
    }

    private val _state = MutableStateFlow(LocationTrackingState())
    val state: StateFlow<LocationTrackingState> = _state.asStateFlow()

    // Job to control location updates collection
    private var locationUpdatesJob: Job? = null

    // Track current session ID for adding locations
    private var currentSessionId: Long? = null

    // Current interval in milliseconds
    private var currentIntervalMs = 5000L // Default 5 seconds

    init {
        // Check if service is already running when ViewModel is created
        checkServiceState()

        // Load sessions from database
        loadSessionsFromDatabase()

        // Check for active session
        checkForActiveSession()

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
     * Update the location update interval
     * @param intervalSeconds Interval in seconds (will be clamped to valid range)
     */
    fun updateLocationInterval(intervalSeconds: Int) {
        val intervalMs = (intervalSeconds * 1000L).coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS)
        currentIntervalMs = intervalMs
        _state.update { it.copy(locationIntervalSeconds = intervalSeconds.coerceIn(1, 60)) }

        // If tracking is active, restart with new interval
        if (LocationService.isRunning()) {
            locationUpdatesJob?.cancel()
            startLocationUpdates()
        }
    }

    /**
     * Load sessions from database reactively
     */
    private fun loadSessionsFromDatabase() {
        viewModelScope.launch {
            repository.getAllSessions()
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            // Separate active and completed sessions
                            val allSessions = result.data
                            val activeSession = allSessions.find { it.isActive() }
                            val completedSessions = allSessions.filter { !it.isActive() }

                            _state.update { currentState ->
                                currentState.copy(
                                    sessions = completedSessions,
                                    currentSession = activeSession ?: currentState.currentSession
                                )
                            }

                            // If there's an active session, set the currentSessionId
                            if (activeSession != null) {
                                currentSessionId = activeSession.sessionId
                            }
                        }
                        is Result.Error -> {
                            _state.update { it.copy(error = result.error.toUiText().toString()) }
                        }
                    }
                }
        }
    }

    /**
     * Check for active session in database (now redundant as loadSessionsFromDatabase handles it)
     */
    private fun checkForActiveSession() {
        // This is now handled by loadSessionsFromDatabase
        // Keeping this method for backward compatibility
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

        viewModelScope.launch {
            try {
                val intent = Intent(application, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                    // Pass the configured interval to the service
                    putExtra(LocationService.EXTRA_LOCATION_INTERVAL, currentIntervalMs)
                }
                application.startForegroundService(intent)

                // Create new session
                val sessionId = System.currentTimeMillis()
                val newSession = LocationSession(
                    sessionId = sessionId,
                    startTime = sessionId,
                    endTime = null,
                    locations = emptyList()
                )

                // Save session to database
                repository.createSession(newSession)
                    .onSuccess {
                        currentSessionId = sessionId
                        _state.update {
                            it.copy(
                                isServiceRunning = true,
                                currentSession = newSession,
                                error = null
                            )
                        }

                        // Try to get last known location first
                        getLastKnownLocationAndSave(sessionId)

                        // Start collecting location updates in the ViewModel
                        startLocationUpdates()
                    }
                    .onError { error ->
                        _state.update { it.copy(error = error.toUiText().toString()) }
                    }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to start service: ${e.message}") }
            }
        }
    }

    /**
     * Stop the location tracking service
     */
    fun stopLocationService() {
        viewModelScope.launch {
            try {
                val intent = Intent(application, LocationService::class.java).apply {
                    action = LocationService.ACTION_STOP
                }
                application.startService(intent)

                // Cancel location updates collection
                locationUpdatesJob?.cancel()
                locationUpdatesJob = null

                // End current session and update in database
                _state.value.currentSession?.let { session ->
                    val endedSession = session.copy(
                        endTime = System.currentTimeMillis()
                    )

                    // Update session in database
                    repository.updateSession(endedSession)
                        .onSuccess {
                            currentSessionId = null
                            _state.update {
                                it.copy(
                                    isServiceRunning = false,
                                    currentSession = null,
                                    currentLocation = null,
                                    error = null
                                )
                            }
                        }
                        .onError { error ->
                            _state.update { it.copy(error = error.toUiText().toString()) }
                        }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to stop service: ${e.message}") }
            }
        }
    }

    /**
     * Start collecting location updates from the location client
     */
    private fun startLocationUpdates() {
        // Cancel existing job if any
        locationUpdatesJob?.cancel()

        locationUpdatesJob = locationClient.getLocationUpdates(currentIntervalMs) // Update with current interval
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
                    speed = location.speed,
                    bearing = location.bearing
                )

                // Save location to database
                currentSessionId?.let { sessionId ->
                    viewModelScope.launch {
                        repository.addLocationToSession(sessionId, locationData)
                            .onSuccess {
                                // Update UI state
                                _state.update { currentState ->
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
                            .onError { error ->
                                _state.update { it.copy(error = error.toUiText().toString()) }
                            }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Get last known location and save it to the session
     */
    private fun getLastKnownLocationAndSave(sessionId: Long) {
        viewModelScope.launch {
            try {
                locationClient.getLastKnownLocation()?.let { location ->
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = System.currentTimeMillis(),
                        accuracy = location.accuracy,
                        altitude = location.altitude,
                        speed = location.speed,
                        bearing = location.bearing
                    )

                    // Save to database
                    repository.addLocationToSession(sessionId, locationData)
                        .onSuccess {
                            // Update UI state
                            _state.update { currentState ->
                                val updatedSession = currentState.currentSession?.copy(
                                    locations = listOf(locationData)
                                )

                                currentState.copy(
                                    currentLocation = locationData,
                                    currentSession = updatedSession,
                                    error = null
                                )
                            }
                        }
                }
            } catch (e: Exception) {
                // Last known location is optional, don't show error
                Log.w(TAG, "Could not get last known location", e)
            }
        }
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
        viewModelScope.launch {
            repository.deleteAllSessions()
                .onSuccess {
                    _state.update { it.copy(sessions = emptyList()) }
                }
                .onError { error ->
                    _state.update { it.copy(error = error.toUiText().toString()) }
                }
        }
    }

    /**
     * Export all sessions to CSV format
     */
    fun exportSessionsToCsv() {
        viewModelScope.launch {
            try {
                val allSessions = if (_state.value.currentSession != null) {
                    _state.value.sessions + _state.value.currentSession!!
                } else {
                    _state.value.sessions
                }

                if (allSessions.isEmpty()) {
                    _state.update { it.copy(error = "No sessions to export") }
                    return@launch
                }

                val file = exportToCsvUseCase.execute(allSessions)
                exportToCsvUseCase.shareFile(file)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Export failed: ${e.message}") }
            }
        }
    }

    /**
     * Export all sessions to GPX format
     */
    fun exportSessionsToGpx() {
        viewModelScope.launch {
            try {
                val allSessions = if (_state.value.currentSession != null) {
                    _state.value.sessions + _state.value.currentSession!!
                } else {
                    _state.value.sessions
                }

                if (allSessions.isEmpty()) {
                    _state.update { it.copy(error = "No sessions to export") }
                    return@launch
                }

                val file = exportToGpxUseCase.execute(allSessions)
                exportToGpxUseCase.shareFile(file)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Export failed: ${e.message}") }
            }
        }
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
    val locationIntervalSeconds: Int = 5, // Default 5 seconds
    val error: String? = null
)

private const val MIN_INTERVAL_MS = 1000L // 1 second
private const val MAX_INTERVAL_MS = 60_000L // 60 seconds
