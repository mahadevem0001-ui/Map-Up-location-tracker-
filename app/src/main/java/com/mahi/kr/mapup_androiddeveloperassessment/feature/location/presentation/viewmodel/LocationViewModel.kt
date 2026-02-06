package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.viewmodel

import android.Manifest
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mahi.kr.mapup_androiddeveloperassessment.core.data.local.AppPreferencesManager
import com.mahi.kr.mapup_androiddeveloperassessment.core.domain.Result
import com.mahi.kr.mapup_androiddeveloperassessment.core.domain.onError
import com.mahi.kr.mapup_androiddeveloperassessment.core.domain.onSuccess
import com.mahi.kr.mapup_androiddeveloperassessment.core.domain.toUiText
import com.mahi.kr.mapup_androiddeveloperassessment.core.util.extensions.hasPermission
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.service.LocationService
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.repository.ILocationClient
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationData
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationSession
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.repository.LocationSessionRepository
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.usecase.ExportSessionToCsvUseCase
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.usecase.ExportSessionToGpxUseCase
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.model.LocationEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume

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
    private val exportToGpxUseCase: ExportSessionToGpxUseCase,
    private val prefsManager: AppPreferencesManager
) : AndroidViewModel(application) {

    companion object {
        const val TAG = "com.mahi.kr.LocationViewModel"
    }

    private val _state = MutableStateFlow(LocationTrackingState())
    val state: StateFlow<LocationTrackingState> = _state.asStateFlow()

    private val eventChannel = Channel<LocationEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    // Geocoder for reverse geocoding
    private val geocoder: Geocoder by lazy { Geocoder(application, java.util.Locale.getDefault()) }

    // Job to control location updates collection
    private var locationUpdatesJob: Job? = null

    // Job to monitor provider changes
    private var providerMonitorJob: Job? = null

    // Track current session ID for adding locations
    private var currentSessionId: Long? = null

    // Current interval in milliseconds
    private var currentIntervalMs = 5000L // Default 5 seconds

    private fun emitEvent(event: LocationEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    init {
        // Check if service is already running when ViewModel is created
        checkServiceState()

        // Restore last configured interval if we have one persisted by the service
        restorePersistedInterval()

        // Keep UI/service state in sync with persisted tracking flag (covers notification stop action)
        observeTrackingFlag()

        // Establish initial permission/provider status
        refreshPrerequisites()

//        // Start monitoring provider changes
//        startProviderMonitor()

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
        val persistedRunning = readPersistedServiceRunning()
        val effectiveRunning = isRunning || persistedRunning
        Log.d(TAG, "checkServiceState: isRunning=$isRunning persisted=$persistedRunning effective=$effectiveRunning")
        _state.update { it.copy(isServiceRunning = effectiveRunning) }

        // If the service is not running but we still have an active session, close it gracefully
        if (!effectiveRunning) {
            closeStaleActiveSession()
        }

        // Refresh permission/provider state
        refreshPrerequisites()
    }

    /**
     * Update the location update interval
     * @param intervalSeconds Interval in seconds (will be clamped to valid range)
     */
    fun updateLocationInterval(intervalSeconds: Int) {
        val intervalMs = (intervalSeconds * 1000L).coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS)
        Log.d(TAG, "updateLocationInterval: seconds=$intervalSeconds -> ms=$intervalMs")
        currentIntervalMs = intervalMs
        persistInterval(intervalMs)
        _state.update { it.copy(locationIntervalSeconds = intervalSeconds.coerceIn(1, 240)) }

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

                            _state.update {
                                it.copy(
                                    sessions = completedSessions,
                                    currentSession = activeSession
                                )
                            }

                            // If there's an active session, set the currentSessionId
                            if (activeSession != null) {
                                currentSessionId = activeSession.sessionId
                            } else {
                                currentSessionId = null
                            }
                        }
                        is Result.Error -> {
                            Log.e(TAG, "loadSessionsFromDatabase: failed ${result.error.toUiText()}", )
                            emitEvent(LocationEvent.ShowMessage(result.error.toUiText().toString()))
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
        Log.d(TAG, "startLocationService: requested intervalMs=$currentIntervalMs")
        // Gate on permission/provider availability
        if (!refreshPrerequisites()) {
            emitEvent(LocationEvent.ShowMessage(_state.value.startBlockReason ?: "Cannot start tracking"))
            return
        }

        // Check if service is already running
        if (LocationService.isRunning()) {
            Log.d(TAG, "startLocationService: already running, skipping")
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
                        Log.e(TAG, "createSession failed ${error.toUiText()}")
                        emitEvent(LocationEvent.ShowMessage(error.toUiText().toString()))
                    }
            } catch (e: Exception) {
                Log.e(TAG, "startLocationService: exception", e)
                emitEvent(LocationEvent.ShowMessage("Failed to start service: ${e.message}"))
            }
        }
    }

    /**
     * Stop the location tracking service
     */
    fun stopLocationService() {
        Log.d(TAG, "stopLocationService: requested")
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
                            Log.e(TAG, "updateSession on stop failed ${error.toUiText()}")
                            emitEvent(LocationEvent.ShowMessage(error.toUiText().toString()))
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "stopLocationService: exception", e)
                emitEvent(LocationEvent.ShowMessage("Failed to stop service: ${e.message}"))
            }
        }
    }

    /**
     * When the service stops (or was killed), but an active session remains in state/database,
     * close it to avoid showing a stuck "active" session while the service is off.
     */
    private fun closeStaleActiveSession() {
        val activeSession = _state.value.currentSession?.takeIf { it.isActive() }
        if (activeSession == null) return

        val endedSession = activeSession.copy(endTime = System.currentTimeMillis())
        viewModelScope.launch {
            repository.updateSession(endedSession)
                .onSuccess {
                    currentSessionId = null
                    _state.update {
                        it.copy(
                            currentSession = null,
                            currentLocation = null
                        )
                    }
                }
                .onError { error ->
                    emitEvent(LocationEvent.ShowMessage(error.toUiText().toString()))
                }
        }
    }

    /**
     * Start collecting location updates from the location client
     */
    private fun startLocationUpdates() {
        // Cancel existing job if any
        locationUpdatesJob?.cancel()

        Log.d(TAG, "startLocationUpdates: intervalMs=$currentIntervalMs")

        // Start monitoring provider changes
        startProviderMonitor()

        locationUpdatesJob = locationClient.getLocationUpdates(currentIntervalMs) // Update with current interval
            .catch { exception ->
                when {
                    exception is SecurityException || exception.message?.contains("permission", ignoreCase = true) == true -> {
                        Log.e(TAG, "locationUpdates: permission revoked", exception)
                        handlePermissionsRevoked("Location permission is required. Tracking stopped.2")
                    }
                    exception.message?.contains("provider", ignoreCase = true) == true ||
                        exception.message?.contains("gps", ignoreCase = true) == true ||
                        exception.message?.contains("disabled", ignoreCase = true) == true -> {
                        Log.w(TAG, "locationUpdates: provider disabled ${exception.message}")
                        handleProviderDisabled("Location provider disabled. Tracking stopped.")
                    }
                    else -> {
                        Log.e(TAG, "locationUpdates: unexpected error", exception)
                        _state.update { it.copy(isServiceRunning = false, error = null) }
                        emitEvent(LocationEvent.ShowMessage("Location error: ${exception.message}"))
                    }
                }
            }
            .onEach { location ->
                // Get address via reverse geocoding
                val address = getAddressFromLocation(location.latitude, location.longitude)

                val locationData = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = System.currentTimeMillis(),
                    accuracy = location.accuracy,
                    altitude = location.altitude,
                    speed = location.speed,
                    bearing = location.bearing,
                    address = address // Add reverse geocoded address
                )

                // Update UI only; persistence now handled inside LocationService to avoid duplicates
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
            .launchIn(viewModelScope)
    }

    /**
     * Get last known location and save it to the session
     */
    private fun getLastKnownLocationAndSave(sessionId: Long) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "getLastKnownLocationAndSave: sessionId=$sessionId")
                locationClient.getLastKnownLocation()?.let { location ->
                    val address = getAddressFromLocation(location.latitude, location.longitude)
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = System.currentTimeMillis(),
                        accuracy = location.accuracy,
                        altitude = location.altitude,
                        speed = location.speed,
                        bearing = location.bearing,
                        address = address
                    )

                    // Update UI only; persistence handled by LocationService
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
            } catch (e: Exception) {
                when {
                    e is SecurityException -> {
                        Log.e(TAG, "getLastKnownLocation: permission revoked", e)
                        handlePermissionsRevoked("Location permission is required. Tracking stopped. 3")
                    }
                    e.message?.contains("provider", ignoreCase = true) == true ||
                        e.message?.contains("gps", ignoreCase = true) == true ||
                        e.message?.contains("disabled", ignoreCase = true) == true -> {
                        Log.w(TAG, "getLastKnownLocation: provider disabled ${e.message}")
                        handleProviderDisabled("Location provider disabled. Tracking stopped.")
                    }
                    else -> Log.w(TAG, "Could not get last known location", e)
                }
            }
        }
    }

    /**
     * Keep view state aligned with persisted tracking flag so notification Stop action
     * (which stops the service) also clears UI/session state.
     */
    private fun observeTrackingFlag() {
        viewModelScope.launch {
            prefsManager.isTracking.collect { isTracking ->
                if (!isTracking && (_state.value.isServiceRunning || _state.value.currentSession != null)) {
                    Log.d(TAG, "observeTrackingFlag: tracking=false -> closing active session and clearing state")

                    locationUpdatesJob?.cancel()
                    locationUpdatesJob = null

                    closeStaleActiveSession()
                    closeActiveSessionFromRepository()

                    currentSessionId = null
                    _state.update {
                        it.copy(
                            isServiceRunning = false,
                            currentSession = null,
                            currentLocation = null,
                            error = null
                        )
                    }
                } else if (isTracking && !_state.value.isServiceRunning) {
                    _state.update { it.copy(isServiceRunning = true) }
                }
            }
        }
    }

    /**
     * Listen for provider status changes and react when both providers are off.
     */
    private fun startProviderMonitor() {
        providerMonitorJob?.cancel()

        providerMonitorJob = callbackFlow {
            val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            val lm = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                    trySend(enabled).isSuccess
                }
            }

            // Emit initial value
            val initialEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            trySend(initialEnabled).isSuccess

            application.registerReceiver(receiver, filter)
            awaitClose {
                runCatching { application.unregisterReceiver(receiver) }
            }
        }
            .onEach { providersEnabled ->
                Log.d(TAG, "startProviderMonitor: providersEnabled=$providersEnabled")
                val hasPermission = application.hasPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )

                val reason = when {
                    !hasPermission -> "Location permission is required to start tracking."
                    !providersEnabled -> "Enable GPS or Location Services to start tracking."
                    else -> null
                }

                _state.update {
                    it.copy(
                        hasLocationPermission = hasPermission,
                        providersEnabled = providersEnabled,
                        startBlockReason = reason
                    )
                }

                if (!providersEnabled && _state.value.isServiceRunning) {
                    Log.w(TAG, "Provider monitor: providers disabled while running; stopping service")
                    handleProviderDisabled("Location provider disabled. Tracking stopped.")
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Handle revoked permissions: stop service, close active session, clear current location, and surface an error.
     */
    private fun handlePermissionsRevoked(message: String) {
        Log.d(TAG, "handlePermissionsRevoked: message $message")
        stopServiceAndClearState(
            reason = message,
            permissionRevoked = true,
            providersDisabled = false
        )
    }

    /**
     * Handle provider disabled events similarly to permission revoke: stop service and close active session.
     */
    private fun handleProviderDisabled(message: String) {
        Log.d(TAG, "handleProviderDisabled: message $message")
        stopServiceAndClearState(
            reason = message,
            permissionRevoked = false,
            providersDisabled = true
        )
    }

    /**
     * Public hook for UI or permission screen to force-stop tracking when permission is revoked.
     */
    fun handlePermissionRevokedFromUi(reason: String = "Location permission is required. Tracking stopped-1") {
        Log.w(TAG, "handlePermissionRevokedFromUi: $reason")
        stopServiceAndClearState(reason = reason, permissionRevoked = true, providersDisabled = false)
    }

    private fun stopServiceAndClearState(
        reason: String,
        permissionRevoked: Boolean,
        providersDisabled: Boolean
    ) {
        Log.w(TAG, "stopServiceAndClearState: reason=$reason, permissionRevoked=$permissionRevoked, providersDisabled=$providersDisabled")
        viewModelScope.launch {
            try {
                val intent = Intent(application, LocationService::class.java).apply {
                    action = LocationService.ACTION_STOP
                }
                application.startService(intent)
            } catch (_: Exception) {
                // Ignore service stop errors; proceed to clear state
            }

            locationUpdatesJob?.cancel()
            locationUpdatesJob = null

            // Close any active session and persist end time
            closeStaleActiveSession()
            closeActiveSessionFromRepository()

            _state.update {
                it.copy(
                    isServiceRunning = false,
                    currentLocation = null,
                    hasLocationPermission = if (permissionRevoked) false else it.hasLocationPermission,
                    providersEnabled = if (providersDisabled) false else it.providersEnabled,
                    startBlockReason = reason,
                    error = null
                )
            }

            when {
                permissionRevoked -> emitEvent(LocationEvent.PermissionRevoked(reason))
                providersDisabled -> emitEvent(LocationEvent.ProviderDisabled(reason))
                else -> emitEvent(LocationEvent.ShowMessage(reason))
            }
        }
    }

    private suspend fun closeActiveSessionFromRepository() {
        repository.getAllSessions().firstOrNull()?.let { result ->
            if (result is Result.Success) {
                result.data.find { it.isActive() }?.let { active ->
                    val ended = active.copy(endTime = System.currentTimeMillis())
                    repository.updateSession(ended)
                }
            }
        }
    }

    private fun readPersistedServiceRunning(): Boolean {
        return runCatching {
            runBlocking { prefsManager.getTrackingStateSync() }
        }.getOrElse { false }
    }

    private fun persistInterval(intervalMs: Long) {
        viewModelScope.launch {
            runCatching { prefsManager.setTrackingInterval(intervalMs) }
                .onFailure { error -> Log.w(TAG, "persistInterval: failed to save interval", error) }
        }
    }

    private fun restorePersistedInterval() {
        viewModelScope.launch {
            val persisted = runCatching { prefsManager.getTrackingIntervalSync() }.getOrNull()

            persisted?.let { intervalMs ->
                currentIntervalMs = intervalMs
                val seconds = (intervalMs / 1000L).toInt().coerceIn(1, 240)
                _state.update { it.copy(locationIntervalSeconds = seconds) }
                Log.d(TAG, "restorePersistedInterval: restored intervalMs=$intervalMs")
            }
        }
    }

    /**
     * External hook for UI/permission callback: when the app detects location permission revoked,
     * stop the service and close the active session immediately.
     */
    fun onPermissionRevoked() {
        handlePermissionsRevoked("Location permission is required. Tracking stopped.")
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
                    emitEvent(LocationEvent.ShowMessage(error.toUiText().toString()))
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
                    emitEvent(LocationEvent.ShowMessage("No sessions to export"))
                    return@launch
                }

                val file = exportToCsvUseCase.execute(allSessions)
                exportToCsvUseCase.shareFile(file)
            } catch (e: Exception) {
                emitEvent(LocationEvent.ShowMessage("Export failed: ${e.message}"))
            }
        }
    }

    /**
     * Refresh permission and provider status, and capture a blocking reason if any.
     * @return true if tracking can start, false otherwise
     */
    private fun refreshPrerequisites(): Boolean {
        val hasPermission = application.hasPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providersEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        val reason = when {
            !hasPermission -> "Location permission is required to start tracking."
            !providersEnabled -> "Enable GPS or Location Services to start tracking."
            else -> null
        }

        Log.d(TAG, "refreshPrerequisites: hasPermission=$hasPermission, providersEnabled=$providersEnabled, reason=$reason")

        _state.update {
            it.copy(
                hasLocationPermission = hasPermission,
                providersEnabled = providersEnabled,
                startBlockReason = reason
            )
        }

        return reason == null
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
                    emitEvent(LocationEvent.ShowMessage("No sessions to export"))
                    return@launch
                }

                val file = exportToGpxUseCase.execute(allSessions)
                exportToGpxUseCase.shareFile(file)
            } catch (e: Exception) {
                emitEvent(LocationEvent.ShowMessage("Export failed: ${e.message}"))
            }
        }
    }

    /**
     * Get address from coordinates using reverse geocoding
     * Returns null if geocoding fails or address not available
     */
    private suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ async API
                kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()?.let { addr ->
                            buildString {
                                addr.thoroughfare?.let { append("$it, ") }
                                addr.locality?.let { append("$it, ") }
                                addr.adminArea?.let { append(it) }
                            }.takeIf { it.isNotBlank() }
                        }
                        continuation.resume(address)
                    }
                }
            } else {
                // Older Android versions - synchronous (deprecated but needed for compatibility)
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.let { addr ->
                    buildString {
                        addr.thoroughfare?.let { append("$it, ") }
                        addr.locality?.let { append("$it, ") }
                        addr.adminArea?.let { append(it) }
                    }.takeIf { it.isNotBlank() }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting address: ${e.message}")
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationUpdatesJob?.cancel()
        providerMonitorJob?.cancel()
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
    val error: String? = null,
    val hasLocationPermission: Boolean = true,
    val providersEnabled: Boolean = true,
    val startBlockReason: String? = null
)

private const val MIN_INTERVAL_MS = 1000L // 1 second
private const val MAX_INTERVAL_MS = 60_000L // 60 seconds
