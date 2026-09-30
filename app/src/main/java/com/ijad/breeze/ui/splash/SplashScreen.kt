package com.ijad.breeze.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ijad.breeze.ui.components.BreezeFanMark
import com.ijad.breeze.ui.components.PrimaryPillButton
import com.ijad.breeze.ui.theme.BreezeBlue
import com.ijad.breeze.ui.theme.BreezeGlow

@Composable
fun SplashScreen(onGetStarted: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFEFF6FF), MaterialTheme.colorScheme.background, Color(0xFFF8FAFC))
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Soft glow rings behind fan mark
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(148.dp)
                        .clip(CircleShape)
                        .background(BreezeGlow.copy(alpha = 0.18f))
                )
                Box(
                    Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(BreezeBlue.copy(alpha = 0.12f))
                )
                Box(
                    Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                )
                BreezeFanMark(size = 56.dp)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "BREEZE",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BreezeBlue
                )
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = "Your AC, in your pocket.",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Works with most brands, no setup headaches.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        PrimaryPillButton(
            text = "Get started",
            onClick = onGetStarted,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}
