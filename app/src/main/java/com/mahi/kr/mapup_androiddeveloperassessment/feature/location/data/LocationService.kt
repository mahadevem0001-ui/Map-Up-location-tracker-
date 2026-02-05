package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.ILocationClient
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
import org.koin.android.ext.android.inject

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

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val geocoder: Geocoder by lazy { Geocoder(this, java.util.Locale.getDefault()) }

    // Track last notification update time to avoid spamming
    private var lastNotificationUpdateTime = 0L
    private var currentLocationAddress: String? = null

    companion object {
        private const val TAG = "LocationService"
        const val ACTION_START = "start location tracking"
        const val ACTION_STOP = "stop location tracking"
        const val EXTRA_LOCATION_INTERVAL = "location_interval_ms"
        private const val NOTIFICATION_ID = 1
        private const val DEFAULT_INTERVAL_MS = 5_000L

        @Volatile
        private var isServiceRunning = false

        fun isRunning(): Boolean = isServiceRunning
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand: isServiceRunning $isServiceRunning action $action")
        when (intent?.action) {
            ACTION_START -> {
                if (!isServiceRunning) {
                    // Get interval from intent, default to 5 seconds if not provided
                    val intervalMs = intent.getLongExtra(EXTRA_LOCATION_INTERVAL, DEFAULT_INTERVAL_MS)
                    Log.d(TAG, "onStartCommand: Starting with interval $intervalMs ms")
                    startLocationTracking(intervalMs)
                }
            }
            ACTION_STOP -> stopLocationTracking()
        }
        return START_STICKY // Service will be restarted if killed by system
    }


    private fun startLocationTracking(intervalMs: Long) {
        isServiceRunning = true

        locationClient.getLocationUpdates(intervalMs).catch { exception ->
            exception.printStackTrace()
        }.onEach { location ->
            Log.d(TAG, "startLocationTracking: location $location ")

            // Update notification with current location data
            // Use same interval as location updates for consistency
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastNotificationUpdateTime >= intervalMs) {
                updateNotificationWithLocation(location.latitude, location.longitude)
                lastNotificationUpdateTime = currentTime
            }
        }.launchIn(serviceScope)

        // Start foreground with initial notification
        startForeground(
            NOTIFICATION_ID, buildNotificationUseCase(
                NotificationConfig(
                    channelId = AppNotificationManager.LOCATION_CHANNEL_CONFIG.channelId,
                    notificationId = NOTIFICATION_ID,
                    title = "Location Tracking Active",
                    message = "Waiting for location...",
                    smallIconRes = android.R.drawable.stat_notify_sync,
                )
            )
        )
    }

    /**
     * Update the foreground notification with current location data
     */
    private fun updateNotificationWithLocation(latitude: Double, longitude: Double) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Get address in background
        getAddressFromLocation(latitude, longitude) { address ->
            currentLocationAddress = address
        }

        val locationText = buildString {
            append("Lat: ${"%.6f".format(latitude)}, Lng: ${"%.6f".format(longitude)}")
            currentLocationAddress?.let {
                append("\n📍 $it")
            }
        }

        val notification = buildNotificationUseCase(
            NotificationConfig(
                channelId = AppNotificationManager.LOCATION_CHANNEL_CONFIG.channelId,
                notificationId = NOTIFICATION_ID,
                title = "Location Tracking Active",
                message = locationText,
                smallIconRes = android.R.drawable.stat_notify_sync,
            )
        )

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Get address from coordinates using reverse geocoding
     */
    private fun getAddressFromLocation(latitude: Double, longitude: Double, callback: (String?) -> Unit) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ async API
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    val address = addresses.firstOrNull()?.let { addr ->
                        // Build concise address
                        buildString {
                            addr.featureName?.let { append("$it, ") }
                            addr.locality?.let { append("$it, ") }
                            addr.adminArea?.let { append(it) }
                        }.takeIf { it.isNotBlank() }
                    }
                    callback(address)
                }
            } else {
                // Older Android versions - synchronous (deprecated but needed for compatibility)
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()?.let { addr ->
                    buildString {
                        addr.featureName?.let { append("$it, ") }
                        addr.locality?.let { append("$it, ") }
                        addr.adminArea?.let { append(it) }
                    }.takeIf { it.isNotBlank() }
                }
                callback(address)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting address: ${e.message}")
            callback(null)
        }
    }

    private fun stopLocationTracking() {
        isServiceRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "stopLocationTracking: isServiceRunning $isServiceRunning ")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        serviceScope.cancel("Location Service is being destroyed")
        Log.d(TAG, "onDestroy: isServiceRunning $isServiceRunning ")
    }
}