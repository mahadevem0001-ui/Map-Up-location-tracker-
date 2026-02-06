package com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.usecase

import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.model.NotificationConfig
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.repository.INotificationManager

/**
 * Use case for build a notification (Clean Architecture)
 */
class BuildNotificationUseCase(private val notificationManager: INotificationManager) {
    operator fun invoke(config: NotificationConfig) =
        notificationManager.buildNotification(config)

}