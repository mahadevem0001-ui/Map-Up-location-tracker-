package com.mahi.kr.mapup_androiddeveloperassessment.core.util.extensions

import android.content.Context
import android.os.Build
import java.util.jar.Manifest

fun Context.hasPermission(vararg permissions: String): Boolean {
    return permissions.all {
        this.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}


val requiredPermissionsSet = buildSet<String> {
    add(android.Manifest.permission.ACCESS_FINE_LOCATION)
    add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(android.Manifest.permission.POST_NOTIFICATIONS)
    }
}

fun Context.hasAllPermissions(permissions: Array<String> = requiredPermissionsSet.toTypedArray()): Boolean {
    return permissions.all {
        this.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}


fun Context.hasActiveProvider() = (this.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager)
        .getProviders(true)
        .isNotEmpty()

