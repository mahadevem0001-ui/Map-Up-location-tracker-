package com.mahi.kr.mapup_androiddeveloperassessment.di

import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.repository.FusedLocationClientImpl
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.local.LocationDatabase
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.repository.LocationSessionRepositoryImpl
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.repository.ILocationClient
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.repository.LocationSessionRepository
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.usecase.ExportSessionToCsvUseCase
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.usecase.ExportSessionToGpxUseCase
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.viewmodel.LocationViewModel
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.usecase.BuildNotificationUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val locationModule = module {
    // Room Database
    single {
        Room.databaseBuilder(
            androidContext(),
            LocationDatabase::class.java,
            LocationDatabase.DATABASE_NAME
        )
            .addMigrations(LocationDatabase.MIGRATION_1_2)
            .build()
    }

    // DAOs
    single { get<LocationDatabase>().locationSessionDao() }
    single { get<LocationDatabase>().locationDao() }

    // Repository
    singleOf(::LocationSessionRepositoryImpl).bind<LocationSessionRepository>()

    // Provide FusedLocationProviderClient from Google Play Services
    single<FusedLocationProviderClient> {
        LocationServices.getFusedLocationProviderClient(androidContext())
    }

    // Provide ILocationClient implementation
    factoryOf(::FusedLocationClientImpl).bind<ILocationClient>()

    // Provide use cases
    factoryOf(::BuildNotificationUseCase)
    factory { ExportSessionToCsvUseCase(androidContext()) }
    factory { ExportSessionToGpxUseCase(androidContext()) }

    // Provide ViewModel
    viewModelOf(::LocationViewModel)
}