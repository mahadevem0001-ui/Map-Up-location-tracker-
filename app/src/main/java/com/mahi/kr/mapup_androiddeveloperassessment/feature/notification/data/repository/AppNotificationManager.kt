package com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.data.repository

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mahi.kr.mapup_androiddeveloperassessment.MainActivity
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.repository.INotificationManager
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.model.NotificationChannelConfig
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.model.NotificationConfig

/**
 * Generic NotificationManager for app notifications
 * Implements INotificationManager for Clean Architecture
 */
class AppNotificationManager(private val context: Context) : INotificationManager {
    companion object {
        private val TAG = "AppNotificationManager"
        val LOCATION_CHANNEL_CONFIG = NotificationChannelConfig(
            channelId = "location_channel",
            channelName = "Location Notifications",
            channelDescription = "Notifications related to location tracking and updates",
            importance = NotificationManager.IMPORTANCE_HIGH
        )
        val ALERT_CHANNEL_CONFIG = NotificationChannelConfig(
            channelId = "alert_channel",
            channelName = "Alert Notifications",
            channelDescription = "Notifications for app alerts and warnings",
            importance = NotificationManager.IMPORTANCE_DEFAULT
        )
    }

    override fun buildChannels(vararg configs: NotificationChannelConfig) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        configs.forEach { config ->
            val channel = NotificationChannel(
                config.channelId,
                config.channelName,
                config.importance
            ).apply {
                description = config.channelDescription
            }
            manager.createNotificationChannel(channel)
        }
    }

    override fun showNotification(config: NotificationConfig) {
        // Check for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= 33) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val hasPermission = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "POST_NOTIFICATIONS: hasPermission $hasPermission")
            if (!hasPermission) return // Don't show notification if permission not granted
        }
        val builder = NotificationCompat.Builder(context, config.channelId)
            .setContentTitle(config.title)
            .setContentText(config.message)
            .setPriority(config.priority)
            .setSmallIcon(config.smallIconRes)
            .setAutoCancel(config.autoCancel)
            .setOngoing(true)

        NotificationManagerCompat.from(context).notify(config.notificationId, builder.build())
    }

    override fun buildNotification(config: NotificationConfig): Notification {
        // Create intent to launch MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            // These flags ensure the app is brought to foreground properly
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        // Create PendingIntent with FLAG_IMMUTABLE for Android 12+
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, config.channelId)
            .setContentTitle(config.title)
            .setContentText(config.message)
            .setPriority(config.priority)
            .setSmallIcon(config.smallIconRes)
            .setAutoCancel(config.autoCancel)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}