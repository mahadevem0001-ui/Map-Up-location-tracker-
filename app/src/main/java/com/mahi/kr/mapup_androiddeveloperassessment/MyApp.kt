package com.mahi.kr.mapup_androiddeveloperassessment

import android.app.Application
import com.mahi.kr.mapup_androiddeveloperassessment.di.coreModule
import com.mahi.kr.mapup_androiddeveloperassessment.di.permissionsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MyApp)
            modules(coreModule,permissionsModule)
        }
    }
}