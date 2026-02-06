package com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.mahi.kr.mapup_androiddeveloperassessment.core.data.local.AppPreferencesManager
import kotlinx.coroutines.runBlocking

/**
 * Restarts location tracking after device reboot if it was active before shutdown.
 */
class BootRestartReceiver : BroadcastReceiver() {

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
        }.getOrNull() ?: LocationService.DEFAULT_INTERVAL_MS

        if (!shouldRestart) {
            Log.d(TAG, "BootRestartReceiver: tracking not enabled; skipping restart")
            return
        }

        // Android 10+ requires background location to start a location FGS from a background receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackgroundPermission =
                context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasBackgroundPermission) {
                Log.w(TAG, "BootRestartReceiver: missing ACCESS_BACKGROUND_LOCATION; not restarting service")
                return
            }
        }

        Log.d(TAG, "BootRestartReceiver: restarting tracking with intervalMs=$intervalMs")
        val serviceIntent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_START
            putExtra(LocationService.EXTRA_LOCATION_INTERVAL, intervalMs)
            putExtra(LocationService.EXTRA_FROM_BOOT, true)
        }
        runCatching {
            context.startForegroundService(serviceIntent)
        }.onFailure { error ->
            Log.e(TAG, "BootRestartReceiver: failed to start service", error)
        }
    }

    companion object {
        private const val TAG = "BootRestartReceiver"
    }
}
