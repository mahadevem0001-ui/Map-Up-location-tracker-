package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.service

import android.Manifest
import android.R
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Geocoder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.mahi.kr.mapup_androiddeveloperassessment.core.data.local.AppPreferencesManager
import com.mahi.kr.mapup_androiddeveloperassessment.core.domain.Result
import com.mahi.kr.mapup_androiddeveloperassessment.core.util.extensions.hasActiveProvider
import com.mahi.kr.mapup_androiddeveloperassessment.core.util.extensions.hasAllPermissions
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationData
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.model.LocationSession
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.repository.ILocationClient
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.repository.LocationSessionRepository
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.data.repository.AppNotificationManager
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.model.NotificationConfig
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.usecase.BuildNotificationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Foreground service for continuous location tracking
 *
 * Uses Koin for dependency injection via inject() delegates
 * This is required because Android system instantiates Service classes
 */
class LocationService : Service() {

    // Inject dependencies using Koin
    private val locationClient: ILocationClient by inject()
    private val buildNotificationUseCase: BuildNotificationUseCase by inject()
    private val prefsManager: AppPreferencesManager by inject()
    private val locationSessionRepository: LocationSessionRepository by inject()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val geocoder: Geocoder by lazy { Geocoder(this, Locale.getDefault()) }

    // Track last notification update time to avoid spamming
    private var lastNotificationUpdateTime = 0L
    private var currentLocationAddress: String? = null
    private var activeSessionId: Long? = null

    companion object {
        private const val TAG = "com.mahi.kr.LocationService"
        const val ACTION_START = "start location tracking"
        const val ACTION_STOP = "stop location tracking"
        const val EXTRA_LOCATION_INTERVAL = "location_interval_ms"
        const val EXTRA_FROM_BOOT = "location_from_boot"
        private const val NOTIFICATION_ID = 1
        const val DEFAULT_INTERVAL_MS = 5_000L
        private const val GEOCODER_TIMEOUT_MS = 2_000L

        @Volatile
        private var isServiceRunning = false

        fun isRunning(): Boolean = isServiceRunning
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand: isServiceRunning=$isServiceRunning action=$action")
        when (intent?.action) {
            ACTION_START -> {
                val startedFromBoot = intent.getBooleanExtra(EXTRA_FROM_BOOT, false)
                if (startedFromBoot && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val hasBackgroundPermission =
                        checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                                PackageManager.PERMISSION_GRANTED

                    if (!hasBackgroundPermission) {
                        Log.w(
                            TAG,
                            "onStartCommand: boot restart without background location; ignoring start"
                        )
                        stopSelf()
                        return START_NOT_STICKY
                    }
                }
                if (!isServiceRunning) {
                    val intervalMs =
                        intent.getLongExtra(EXTRA_LOCATION_INTERVAL, DEFAULT_INTERVAL_MS)
                    Log.d(TAG, "onStartCommand: starting with intervalMs=$intervalMs")
                    startLocationTracking(intervalMs)
                } else {
                    Log.d(TAG, "onStartCommand: start requested but already running; ignoring")
                }
            }

            ACTION_STOP -> {
                Log.d(TAG, "onStartCommand: stop requested")
                stopLocationTracking()
            }

            null -> {
                val hasPerms = hasAllPermissions()
                val hasProvider = hasActiveProvider()

                if (hasPerms && hasProvider) {
                    val persistedTracking = readPersistedTracking()
                    val persistedInterval = readPersistedIntervalMs() ?: DEFAULT_INTERVAL_MS
                    if (persistedTracking && !isServiceRunning) {
                        Log.w(
                            TAG,
                            "onStartCommand: null intent after restart; resuming tracking intervalMs=$persistedInterval"
                        )
                        startLocationTracking(persistedInterval)
                    } else {
                        Log.d(
                            TAG,
                            "onStartCommand: null intent and no persisted tracking; ignoring"
                        )
                    }
                }

                //Stopping the service when it is not running is a safeguard to ensure the service is fully stopped and resources are released.
                if (!hasPerms || !hasProvider) {
                    Log.w(
                        TAG,
                        "onStartCommand: null intent but missing permissions or provider; ensuring service is stopped"
                    )
//                    closeActiveSessionImmediate()
                    stopLocationTracking()
                }
            }

            else -> Log.w(TAG, "onStartCommand: unknown action $action, ignoring")
        }
        return START_STICKY // Service will be restarted if killed by system
    }


    private fun startLocationTracking(intervalMs: Long) {
        Log.d(TAG, "startLocationTracking: begin intervalMs=$intervalMs")

        // Ensure required runtime permissions before promoting to FGS
        if (!hasAllPermissions()) {
            Log.w(TAG, "startLocationTracking: missing location permissions; aborting start")
            persistServiceState(false, null)
            stopSelf()
            return
        }

        val sessionId = ensureActiveSession()
        activeSessionId = sessionId

        isServiceRunning = true
        persistServiceState(true, intervalMs)

        val stopServiceIntent = Intent(this, LocationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopServiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        locationClient.getLocationUpdates(intervalMs).catch { exception ->
            Log.e(TAG, "startLocationTracking: location stream error", exception)
        }.onEach { location ->
            Log.d(
                TAG,
                "startLocationTracking: location lat=${location.latitude} lng=${location.longitude} acc=${location.accuracy}"
            )

            val eventTime = System.currentTimeMillis()
            val address = getAddressFromLocation(location.latitude, location.longitude)
            currentLocationAddress = address

            val locationData = LocationData(
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = eventTime,
                accuracy = location.accuracy,
                altitude = location.altitude,
                speed = location.speed,
                bearing = location.bearing,
                address = address
            )

            Log.d(TAG, "startLocationTracking: activeSessionId $activeSessionId eventTime $eventTime lastNotificationUpdateTime $lastNotificationUpdateTime e-l ${(eventTime - lastNotificationUpdateTime)} 70% of interval ${(intervalMs * 0.7).toLong()}")
            activeSessionId?.let { persistLocation(it, locationData) }

            // Update notification with current location data (throttled)
            if (eventTime - lastNotificationUpdateTime >= (intervalMs * 0.7).toLong()) {
                Log.d(TAG, "startLocationTracking: updating notification (throttled)")
                updateNotificationWithLocation(location.latitude, location.longitude, address)
                lastNotificationUpdateTime = eventTime
            }
        }.launchIn(serviceScope)

        // Start foreground with initial notification
        val notification = buildNotificationUseCase(
            NotificationConfig(
                channelId = AppNotificationManager.Companion.LOCATION_CHANNEL_CONFIG.channelId,
                notificationId = NOTIFICATION_ID,
                title = "Location Tracking Active",
                message = "Waiting for location...",
                smallIconRes = R.drawable.stat_notify_sync,
                actionTitle = "Stop",
                actionIconRes = R.drawable.ic_media_pause,
                actionIntent = stopPendingIntent,
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Update the foreground notification with current location data
     */
    private fun updateNotificationWithLocation(
        latitude: Double,
        longitude: Double,
        address: String?
    ) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val stopServiceIntent = Intent(this, LocationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopServiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val locationText = buildString {
            append("Lat: ${"%.6f".format(latitude)}, Lng: ${"%.6f".format(longitude)}")
            address?.takeIf { it.isNotBlank() }?.let {
                append("\n📍 $it")
            }
        }

        val notification = buildNotificationUseCase(
            NotificationConfig(
                channelId = AppNotificationManager.Companion.LOCATION_CHANNEL_CONFIG.channelId,
                notificationId = NOTIFICATION_ID,
                title = "Location Tracking Active",
                message = locationText,
                smallIconRes = R.drawable.stat_notify_sync,
                actionTitle = "Stop",
                actionIconRes = R.drawable.ic_media_pause,
                actionIntent = stopPendingIntent,
            )
        )

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Get address from coordinates using reverse geocoding
     */
    private suspend fun getAddressFromLocation(
        latitude: Double,
        longitude: Double
    ): String? {
        return withTimeoutOrNull(GEOCODER_TIMEOUT_MS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    try {
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            if (cont.isActive) {
                                val address = addresses.firstOrNull()?.let { addr ->
                                    buildString {
                                        addr.featureName?.let { append("$it, ") }
                                        addr.locality?.let { append("$it, ") }
                                        addr.adminArea?.let { append(it) }
                                    }.takeIf { it.isNotBlank() }
                                }
                                cont.resume(address)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "getAddressFromLocation: geocoder failed", e)
                        if (cont.isActive) cont.resume(null)
                    }
                }
            } else {
                try {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    addresses?.firstOrNull()?.let { addr ->
                        buildString {
                            addr.featureName?.let { append("$it, ") }
                            addr.locality?.let { append("$it, ") }
                            addr.adminArea?.let { append(it) }
                        }.takeIf { it.isNotBlank() }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "getAddressFromLocation: geocoder failed", e)
                    null
                }
            }
        }
    }

    private fun stopLocationTracking() {
        Log.d(TAG, "stopLocationTracking: begin")
        isServiceRunning = false
        persistServiceState(false, null)
//        closeActiveSession()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "stopLocationTracking: isServiceRunning=$isServiceRunning")
    }

    private fun ensureActiveSession(): Long = runBlocking(Dispatchers.IO) {
        when (val activeResult = locationSessionRepository.getActiveSession()) {
            is Result.Success -> {
                activeResult.data?.sessionId ?: createNewSession()
            }

            is Result.Error -> createNewSession()
        }
    }

    private suspend fun createNewSession(): Long {
        val sessionId = System.currentTimeMillis()
        val newSession = LocationSession(
            sessionId = sessionId,
            startTime = sessionId,
            endTime = null,
            locations = emptyList()
        )

        when (val createResult = locationSessionRepository.createSession(newSession)) {
            is Result.Success -> Unit
            is Result.Error -> Log.e(
                TAG,
                "createNewSession: failed to create session ${createResult.error}"
            )
        }

        return sessionId
    }

    private fun persistLocation(sessionId: Long, locationData: LocationData) {
        serviceScope.launch {
            Log.d(TAG, "persistLocation: sessionId: $sessionId locationData $locationData")
            when (val result =
                locationSessionRepository.addLocationToSession(sessionId, locationData)) {
                is Result.Success -> Log.d(TAG, "persistLocation: saved for session=$sessionId")
                is Result.Error -> Log.e(
                    TAG,
                    "persistLocation: failed to save location ${result.error}"
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        persistServiceState(false, null)
        closeActiveSessionImmediate()
        serviceScope.cancel("Location Service is being destroyed")
        Log.d(TAG, "onDestroy: isServiceRunning=$isServiceRunning")
    }

    private fun persistServiceState(isTracking: Boolean, intervalMs: Long?) {
        runCatching {
            runBlocking { prefsManager.setTrackingState(isTracking, intervalMs) }
        }.onFailure { error ->
            Log.w(TAG, "persistServiceState: failed to save state", error)
        }
    }

    private fun readPersistedTracking(): Boolean {
        return runCatching {
            runBlocking { prefsManager.getTrackingStateSync() }
        }.getOrElse { false }
    }

    private fun readPersistedIntervalMs(): Long? {
        return runCatching {
            runBlocking { prefsManager.getTrackingIntervalSync() }
        }.getOrNull()
    }

    private fun closeActiveSession() {
        serviceScope.launch {
            val active = when (val res = locationSessionRepository.getActiveSession()) {
                is Result.Success -> res.data
                is Result.Error -> {
                    Log.w(TAG, "closeActiveSession: failed to fetch active session ${res.error}")
                    null
                }
            }

            active?.let { session ->
                val ended = session.copy(endTime = System.currentTimeMillis())
                when (val update = locationSessionRepository.updateSession(ended)) {
                    is Result.Success -> Log.d(
                        TAG,
                        "closeActiveSession: session ${session.sessionId} closed"
                    )

                    is Result.Error -> Log.e(
                        TAG,
                        "closeActiveSession: failed to update session ${update.error}"
                    )
                }
            }
        }
    }

    private fun closeActiveSessionImmediate() {
        runBlocking(Dispatchers.IO) {
            when (val res = locationSessionRepository.getActiveSession()) {
                is Result.Success -> res.data?.let { session ->
                    val ended = session.copy(endTime = System.currentTimeMillis())
                    when (val update = locationSessionRepository.updateSession(ended)) {
                        is Result.Success -> Log.d(
                            TAG,
                            "closeActiveSessionImmediate: session ${session.sessionId} closed"
                        )

                        is Result.Error -> Log.e(
                            TAG,
                            "closeActiveSessionImmediate: failed ${update.error}"
                        )
                    }
                }

                is Result.Error -> Log.w(
                    TAG,
                    "closeActiveSessionImmediate: failed to fetch active session ${res.error}"
                )
            }
        }
    }
}