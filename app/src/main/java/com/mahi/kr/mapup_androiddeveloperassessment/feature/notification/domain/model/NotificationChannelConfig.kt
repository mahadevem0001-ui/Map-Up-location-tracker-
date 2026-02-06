package com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.model

import android.app.NotificationManager

/**
 * Builder for notification channel configuration
 */
data class NotificationChannelConfig(
    val channelId: String,
    val channelName: String,
    val channelDescription: String,
    val importance: Int = NotificationManager.IMPORTANCE_DEFAULT
)
