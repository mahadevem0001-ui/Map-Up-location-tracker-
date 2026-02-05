package com.mahi.kr.mapup_androiddeveloperassessment.core.util.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Extension function to open the app's settings page
 * Useful for directing users to manually enable permissions
 */
fun Activity.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}
