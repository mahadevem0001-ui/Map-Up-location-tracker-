package com.mahi.kr.mapup_androiddeveloperassessment.di

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.data.FusedLocationClientImpl
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.presentation.viewmodel.LocationViewModel
import com.mahi.kr.mapup_androiddeveloperassessment.feature.location.domain.ILocationClient
import com.mahi.kr.mapup_androiddeveloperassessment.feature.notification.domain.usecase.BuildNotificationUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val locationModule = module {
    // Provide FusedLocationProviderClient from Google Play Services
    single<FusedLocationProviderClient> {
        LocationServices.getFusedLocationProviderClient(androidContext())
    }

    // Provide ILocationClient implementation
    factoryOf(::FusedLocationClientImpl).bind<ILocationClient>()

    // Provide use cases
    factoryOf(::BuildNotificationUseCase)

    // Provide ViewModel
    viewModelOf(::LocationViewModel)
}