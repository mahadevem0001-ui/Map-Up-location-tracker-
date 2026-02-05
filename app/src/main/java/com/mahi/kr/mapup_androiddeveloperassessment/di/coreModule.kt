package com.mahi.kr.mapup_androiddeveloperassessment.di

import com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.viewmodel.ThemeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val coreModule = module {
    viewModelOf(::ThemeViewModel)
}