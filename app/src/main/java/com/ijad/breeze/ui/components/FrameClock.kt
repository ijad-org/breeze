package com.ijad.breeze.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis

/**
 * Milliseconds since the first frame, updated every frame, for hand-driven CSS-style loops.
 * Starts at [initial]; while [paused] (reduced motion) it stops where it is.
 */
@Composable
fun rememberFrameMillis(initial: Long, paused: Boolean): State<Long> {
    val elapsed = remember { mutableLongStateOf(initial) }
    LaunchedEffect(paused) {
        if (paused) return@LaunchedEffect
        val start = withFrameMillis { it }
        while (true) withFrameMillis { elapsed.longValue = it - start }
    }
    return elapsed
}
