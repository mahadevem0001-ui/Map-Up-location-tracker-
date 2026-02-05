package com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.repository

import android.app.Notification
import androidx.core.app.NotificationCompat
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.model.NotificationChannelConfig
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.model.NotificationConfig

/**
 * Abstraction for notification management (Clean Architecture)
 */
interface INotificationManager {
    fun buildChannels(vararg configs: NotificationChannelConfig)
    fun showNotification(config: NotificationConfig)

    fun buildNotification(config: NotificationConfig): Notification
}