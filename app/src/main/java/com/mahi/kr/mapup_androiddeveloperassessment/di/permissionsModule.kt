package com.mahi.kr.mapup_androiddeveloperassessment.di

import com.mahi.kr.mapup_androiddeveloperassessment.core.data.local.AppPreferencesManager
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.viewmodel.PermissionViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val permissionsModule = module {

    singleOf(::AppPreferencesManager)

    viewModelOf(::PermissionViewModel)
}