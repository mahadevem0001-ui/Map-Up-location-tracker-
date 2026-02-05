package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.ILocationClient
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.data.repository.AppNotificationManager
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.model.NotificationConfig
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.usecase.BuildNotificationUseCase
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.usecase.SendNotificationUseCase
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
    private val sendNotificationUseCase: SendNotificationUseCase by inject()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "LocationService"
        const val ACTION_START = "start location tracking"
        const val ACTION_STOP = "stop location tracking"

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
                    startLocationTracking()
                }
            }
            ACTION_STOP -> stopLocationTracking()
        }
        return START_STICKY // Service will be restarted if killed by system
    }


    private fun startLocationTracking() {
        isServiceRunning = true

        locationClient.getLocationUpdates(1_000L).catch { exception ->
            exception.printStackTrace()
        }.onEach { location ->
            Log.d(TAG, "startLocationTracking: location $location ")
            // Handle location update
            sendNotificationUseCase(
                NotificationConfig(
                    channelId = AppNotificationManager.LOCATION_CHANNEL_CONFIG.channelId,
                    notificationId = 1,
                    title = "Location Tracking",
                    message = "Location: ${location.latitude}, ${location.longitude}",
                    smallIconRes = android.R.drawable.stat_notify_sync,
                )
            )
        }.launchIn(serviceScope)

        startForeground(
            1, buildNotificationUseCase(
                NotificationConfig(
                    channelId = AppNotificationManager.LOCATION_CHANNEL_CONFIG.channelId,
                    notificationId = 1,
                    title = "Location Tracking",
                    message = "Location tracking is active",
                    smallIconRes = android.R.drawable.stat_notify_sync,
                )
            )
        )

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