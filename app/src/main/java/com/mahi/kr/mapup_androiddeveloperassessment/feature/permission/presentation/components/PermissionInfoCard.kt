package com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.domain.model.PermissionState
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.model.PermissionTextProviderFactory
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.viewmodel.PermissionViewModel

/**
 * Composable for displaying permission information before requesting
 * Shows what each permission is used for in a friendly, informative way
 */
@Composable
fun PermissionInfoCard(
    permission: String,
    viewModel: PermissionViewModel,
    state: PermissionState
) {
    val textProvider = PermissionTextProviderFactory.getProvider(permission)
    val isGranted = !state.deniedPermissions.containsKey(permission)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (permission) {
                        android.Manifest.permission.POST_NOTIFICATIONS -> "🔔"
                        else -> "ℹ️"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = viewModel.getPermissionName(permission),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isGranted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier.weight(1f)
                )

                if (isGranted) {
                    Text(
                        text = "✓ Granted",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = textProvider.getDescription(isPermanentlyDeclined = false),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isGranted) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                }
            )
        }
    }
}
