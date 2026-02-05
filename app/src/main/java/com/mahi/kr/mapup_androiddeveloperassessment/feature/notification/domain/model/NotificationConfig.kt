package com.mahi.kr.mapup_androiddeveloperassessment.domain.notification.model

import androidx.core.app.NotificationCompat

/**
 * Builder for notification configuration
 */
data class NotificationConfig(
    val channelId: String,
    val title: String,
    val message: String,
    val notificationId: Int,
    val smallIconRes: Int = android.R.drawable.ic_menu_mylocation,
    val priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    val autoCancel: Boolean = true
)
