package com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.domain.model.PermissionState
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.model.PermissionTextProviderFactory
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.viewmodel.PermissionViewModel

/**
 * Composable for displaying location permission information (combines FINE and COARSE)
 * Shows both permissions as a single "Location" group with explanation
 */
@Composable
fun LocationPermissionInfoCard(
    permissions: List<String>,
    viewModel: PermissionViewModel,
    state: PermissionState,
    onInfoClick: () -> Unit
) {
    val textProvider = PermissionTextProviderFactory.getProvider(permissions.first())

    val grantedCount = permissions.count { permission ->
        !state.deniedPermissions.containsKey(permission)
    }
    val allGranted = grantedCount == permissions.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (allGranted) {
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
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📍",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Location Access",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (allGranted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier.weight(1f)
                )

                if (allGranted) {
                    Text(
                        text = "✓ Granted",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                IconButton(onClick = onInfoClick) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Location permission info"
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Includes:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (allGranted) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                }
            )
            permissions.forEach { permission ->
                val isGranted = !state.deniedPermissions.containsKey(permission)
                Row(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "• ${viewModel.getPermissionName(permission)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (allGranted) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (isGranted) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (allGranted) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = textProvider.getDescription(isPermanentlyDeclined = false),
                style = MaterialTheme.typography.bodyMedium,
                color = if (allGranted) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                }
            )
        }
    }
}
