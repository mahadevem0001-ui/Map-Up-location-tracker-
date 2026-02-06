package com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.model

import android.R
import androidx.core.app.NotificationCompat

/**
 * Builder for notification configuration
 */
data class NotificationConfig(
    val channelId: String,
    val title: String,
    val message: String,
    val notificationId: Int,
    val smallIconRes: Int = R.drawable.ic_menu_mylocation,
    val priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    val autoCancel: Boolean = true,
    val actionTitle: String? = null,
    val actionIconRes: Int? = null,
    val actionIntent: android.app.PendingIntent? = null
)
