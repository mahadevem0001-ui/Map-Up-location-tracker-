package com.mahi.kr.mapup_androiddeveloperassessment.di

import android.content.Context
import com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.viewmodel.ThemeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.reflect.KProperty

val coreModule = module {
    viewModelOf(::ThemeViewModel)
}