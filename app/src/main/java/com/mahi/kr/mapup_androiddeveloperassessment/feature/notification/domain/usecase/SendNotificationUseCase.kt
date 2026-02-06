package com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.usecase

import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.model.NotificationConfig
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.repository.INotificationManager

/**
 * Use case for sending a notification (Clean Architecture)
 */
class SendNotificationUseCase(private val notificationManager: INotificationManager) {
    operator fun invoke(config: NotificationConfig) {
        notificationManager.showNotification(config)
    }
}


