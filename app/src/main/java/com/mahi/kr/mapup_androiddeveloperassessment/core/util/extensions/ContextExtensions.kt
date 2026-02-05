package com.mahi.kr.mapup_androiddeveloperassessment.core.util.extensions

import android.content.Context

fun Context.hasPermission(vararg permissions: String): Boolean {
    return permissions.all {
        this.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}