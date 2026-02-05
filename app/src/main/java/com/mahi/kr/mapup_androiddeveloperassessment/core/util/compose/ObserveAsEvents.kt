package com.mahi.kr.mapup_androiddeveloperassessment.core.util.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

/**
 * Composable utility to observe a [Flow] as one-time events within a Composable
 *
 * Delivers each emission to [onEvent] only when the lifecycle is at least STARTED.
 * Useful for handling one-off events (e.g., navigation, toasts, dialogs) in Compose.
 *
 * This prevents events from being replayed on configuration changes.
 *
 * @param flow The flow of events to observe
 * @param key1 Optional key to control recomposition
 * @param key2 Optional key to control recomposition
 * @param onEvent Lambda to handle each event emission
 */
@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    key1: Any? = null,
    key2: Any? = null,
    onEvent: suspend (T) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, key1, key2) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                flow.onEach { onEvent(it) }.launchIn(this)
            }
        }
    }
}
