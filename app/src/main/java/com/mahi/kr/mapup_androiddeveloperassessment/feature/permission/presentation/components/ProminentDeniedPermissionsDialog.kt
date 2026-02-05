package com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.domain.model.DeniedPermissionInfo
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.viewmodel.PermissionViewModel

/**
 * Dialog displayed when prominent permissions are permanently denied
 *
 * Provides:
 * - List of permanently denied permissions
 * - Explanation why permissions are needed
 * - Direct navigation to app settings
 */
@Composable
fun ProminentDeniedPermissionsDialog(
    deniedPermissions: List<DeniedPermissionInfo>,
    viewModel: PermissionViewModel,
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Permissions Required",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "The following permissions are essential for this app to function properly:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                deniedPermissions.forEach { deniedInfo ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "• ${viewModel.getPermissionName(deniedInfo.permission)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Please enable these permissions in Settings to continue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onGoToSettings) {
                Text("Go to Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
