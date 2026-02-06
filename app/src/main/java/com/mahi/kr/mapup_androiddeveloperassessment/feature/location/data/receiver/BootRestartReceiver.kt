package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.mahi.kr.mapup_androiddeveloperassessment.core.data.local.AppPreferencesManager
import com.mahi.kr.mapup_androiddeveloperassessment.core.domain.Result
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.service.LocationService
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.repository.LocationSessionRepository
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Restarts location tracking after device reboot if it was active before shutdown.
 */
class BootRestartReceiver : BroadcastReceiver(), KoinComponent {

    private val sessionRepository: LocationSessionRepository by inject()

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }

        val prefs = AppPreferencesManager(context.applicationContext)

        val shouldRestart = runCatching {
            runBlocking { prefs.getTrackingStateSync() }
        }.getOrElse {
            Log.w(TAG, "BootRestartReceiver: failed to read tracking flag after boot", it)
            false
        }

        val intervalMs = runCatching {
            runBlocking { prefs.getTrackingIntervalSync() }
        }.getOrNull() ?: LocationService.Companion.DEFAULT_INTERVAL_MS

        if (!shouldRestart) {
            Log.d(TAG, "BootRestartReceiver: tracking not enabled; skipping restart")
            return
        }

        // Android 10+ requires background location to start a location FGS from a background receiver
        val hasBackgroundPermission =
            context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (!hasBackgroundPermission) {
            Log.w(TAG, "BootRestartReceiver: missing ACCESS_BACKGROUND_LOCATION; not restarting service")
            endActiveSessionAndClearFlag(prefs)
            return
        }

        Log.d(TAG, "BootRestartReceiver: restarting tracking with intervalMs=$intervalMs")
        val serviceIntent = Intent(context, LocationService::class.java).apply {
            action = LocationService.Companion.ACTION_START
            putExtra(LocationService.Companion.EXTRA_LOCATION_INTERVAL, intervalMs)
            putExtra(LocationService.Companion.EXTRA_FROM_BOOT, true)
        }
        runCatching {
            context.startForegroundService(serviceIntent)
        }.onFailure { error ->
            Log.e(TAG, "BootRestartReceiver: failed to start service", error)
        }
    }

    companion object {
        private const val TAG = "com.mahi.kr.BootRestartReceiver"
    }

    private fun endActiveSessionAndClearFlag(prefs: AppPreferencesManager) {
        runBlocking {
            runCatching { prefs.setTrackingState(false, null) }.onFailure {
                Log.w(TAG, "endActiveSessionAndClearFlag: failed to clear tracking flag", it)
            }

            when (val res = sessionRepository.getActiveSession()) {
                is Result.Success -> {
                    res.data?.let { session ->
                        val ended = session.copy(endTime = System.currentTimeMillis())
                        runCatching { sessionRepository.updateSession(ended) }
                            .onSuccess {
                                Log.d(
                                    TAG,
                                    "endActiveSessionAndClearFlag: closed session ${session.sessionId}"
                                )
                            }
                            .onFailure { err ->
                                Log.w(
                                    TAG,
                                    "endActiveSessionAndClearFlag: failed to update session",
                                    err
                                )
                            }
                    }
                }

                is Result.Error -> {
                    Log.w(
                        TAG,
                        "endActiveSessionAndClearFlag: failed to fetch active session ${res.error}"
                    )
                }
            }
        }
    }
}