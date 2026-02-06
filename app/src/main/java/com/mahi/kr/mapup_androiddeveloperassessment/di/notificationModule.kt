package com.mahi.kr.mapup_androiddeveloperassessment.di

import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.data.repository.AppNotificationManager
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.repository.INotificationManager
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.usecase.BuildNotificationUseCase
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.usecase.SendNotificationUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val notificationModule = module {
    singleOf(::AppNotificationManager).bind<INotificationManager>()
    factoryOf(::SendNotificationUseCase)
    factoryOf(::BuildNotificationUseCase)
}