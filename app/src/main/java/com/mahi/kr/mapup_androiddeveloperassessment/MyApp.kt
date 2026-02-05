package com.mahi.kr.mapup_androiddeveloperassessment

import android.app.Application
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.data.repository.AppNotificationManager
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.repository.INotificationManager
import com.mahi.kr.mapup_androiddeveloperassessment.di.coreModule
import com.mahi.kr.mapup_androiddeveloperassessment.di.locationModule
import com.mahi.kr.mapup_androiddeveloperassessment.di.notificationModule
import com.mahi.kr.mapup_androiddeveloperassessment.di.permissionsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Start Koin
        startKoin {
            androidLogger()
            androidContext(this@MyApp)
            modules(listOf(permissionsModule, coreModule, notificationModule, locationModule))
        }

        // Build notification channels at startup
        val notificationManager: INotificationManager =
            org.koin.java.KoinJavaComponent.get(INotificationManager::class.java)
        notificationManager.buildChannels(
            AppNotificationManager.LOCATION_CHANNEL_CONFIG,
            AppNotificationManager.ALERT_CHANNEL_CONFIG
        )
    }
}