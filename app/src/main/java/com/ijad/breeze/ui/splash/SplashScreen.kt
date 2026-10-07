package com.ijad.breeze.ui.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ijad.breeze.ui.components.RoundButton
import com.ijad.breeze.ui.components.breezeBackground
import com.ijad.breeze.ui.theme.CoolTint
import com.ijad.breeze.ui.theme.CssEaseOut
import com.ijad.breeze.ui.theme.LocalReduceMotion
import com.ijad.breeze.ui.theme.fanIcon
import com.ijad.breeze.ui.theme.ink

/** Welcome — docs/design/01-welcome.png */
@Composable
fun SplashScreen(onGetStarted: () -> Unit) {
    val tint = CoolTint
    Column(
        modifier = Modifier
            .fillMaxSize()
            .breezeBackground(tint)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            BreezeRings()
            Spacer(Modifier.height(48.dp))
            Column(
                modifier = Modifier.widthIn(max = 280.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "BREEZE",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.6.sp,
                    color = tint.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Your AC, in your pocket.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = ink(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Works with most brands, no setup headaches.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ink(0.55f),
                    textAlign = TextAlign.Center
                )
            }
        }
        RoundButton(
            text = "Get started",
            onClick = onGetStarted,
            tint = tint,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
        )
    }
}

/**
 * Four expanding rings — `ring-out` 3s ease-out infinite, delays 0 / .75 / 1.5 / 2.25s
 * (scale .85→1.75, opacity .65→0) — around a fan icon spinning with `fan-spin` 4s linear.
 * Under reduced motion everything stays at rest, like CSS with `animation: none`.
 */
@Composable
private fun BreezeRings() {
    val tint = CoolTint
    val reduceMotion = LocalReduceMotion.current
    val elapsed by produceState(0L, reduceMotion) {
        if (reduceMotion) return@produceState
        val start = withFrameMillis { it }
        while (true) withFrameMillis { value = it - start }
    }
    val spin = if (reduceMotion) 0f else (elapsed % 4000L) / 4000f * 360f
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(200.dp)) {
            val base = 80.dp.toPx() // 160dp ring diameter
            val stroke = 1.5.dp.toPx()
            for (i in 1..4) {
                val delay = (i - 1) * 750L
                // Before its delay a ring shows its un-animated style (scale 1, opacity 1).
                val (scale, opacity) = if (reduceMotion || elapsed < delay) {
                    1f to 1f
                } else {
                    val e = CssEaseOut.transform(((elapsed - delay) % 3000L) / 3000f)
                    (0.85f + 0.90f * e) to (0.65f * (1f - e))
                }
                drawCircle(
                    color = tint.copy(alpha = (0.6f - i * 0.1f) * opacity),
                    radius = base * scale,
                    style = Stroke(stroke)
                )
            }
            drawCircle(tint.copy(alpha = 0.22f), radius = 55.dp.toPx(), style = Stroke(stroke))
            drawCircle(tint.copy(alpha = 0.32f), radius = 38.dp.toPx(), style = Stroke(stroke))
        }
        Box(
            Modifier
                .size(60.dp)
                .background(tint.copy(alpha = 0.16f), CircleShape)
                .border(1.5.dp, tint.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                fanIcon(),
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(30.dp)
                    .graphicsLayer { rotationZ = spin }
            )
        }
    }
}
