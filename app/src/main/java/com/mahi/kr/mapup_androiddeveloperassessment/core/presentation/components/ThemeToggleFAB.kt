package com.mahi.kr.mapup_androiddeveloperassessment.core.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mahi.kr.mapup_androiddeveloperassessment.R

/**
 * Floating Action Button for toggling between light and dark themes
 *
 * Shows moon icon in light mode, sun icon in dark mode
 */
@Composable
fun ThemeToggleFAB(
    isDarkMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onToggle,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Icon(
            painter = painterResource(id = if (isDarkMode) R.drawable.night_mode else R.drawable.light_mode),
            contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
            modifier = Modifier
                .size(MaterialTheme.typography.titleLarge.fontSize.value.dp)
                .clip(CircleShape)
        )
    }
}
