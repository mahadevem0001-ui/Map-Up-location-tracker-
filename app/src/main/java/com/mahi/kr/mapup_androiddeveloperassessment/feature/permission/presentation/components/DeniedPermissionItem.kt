package com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.domain.model.DeniedPermissionInfo
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.model.PermissionTextProviderFactory
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.viewmodel.PermissionViewModel
import com.mahi.kr.mapup_androiddeveloperassessment.ui.theme.*

/**
 * Composable for displaying individual denied permission item
 *
 * Visual distinction:
 * - Red background: Permanently denied (shouldShowRationale = false)
 * - Orange background: First time denied (shouldShowRationale = true)
 *
 * Theme-aware: Uses appropriate colors for light and dark themes
 */
@Composable
fun DeniedPermissionItem(
    deniedInfo: DeniedPermissionInfo,
    viewModel: PermissionViewModel
) {
    val isPermanentlyDenied = !deniedInfo.shouldShowRationale
    val textProvider = PermissionTextProviderFactory.getProvider(deniedInfo.permission)
    val isDark = isSystemInDarkTheme()

    // Theme-aware background colors
    val backgroundColor = when {
        isPermanentlyDenied && isDark -> PermissionPermanentDeniedDark
        isPermanentlyDenied && !isDark -> PermissionPermanentDeniedLight
        !isPermanentlyDenied && isDark -> PermissionDeniedDark
        else -> PermissionDeniedLight
    }

    // Theme-aware text color with proper contrast
    // Dark theme: Use white/light text on dark backgrounds
    // Light theme: Use dark text on light backgrounds
    val textColor = if (isDark) {
        Color.White  // White text on dark background
    } else {
        Color(0xFF1C1B1F)  // Dark text on light background
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = viewModel.getPermissionName(deniedInfo.permission),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isPermanentlyDenied) {
                    "⚠️ Permanently Denied - Go to Settings to enable"
                } else {
                    "❌ Denied - You can request again"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = textProvider.getDescription(isPermanentlyDeclined = isPermanentlyDenied),
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = deniedInfo.permission,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.6f),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}
