package com.ijad.breeze.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ijad.breeze.R

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

private fun style(size: Int, weight: FontWeight, lineHeight: Float = size * 1.3f) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp
)

/** Type scale from the prototype's component sheet: 64 temp, 28 title, 18–20 bars, 15 body, 11–12 labels. */
val Typography = Typography(
    displayLarge = style(72, FontWeight.ExtraBold, 72f),
    displayMedium = style(64, FontWeight.ExtraBold, 64f),
    headlineLarge = style(28, FontWeight.ExtraBold, 35f),
    headlineMedium = style(22, FontWeight.ExtraBold),
    titleLarge = style(20, FontWeight.Bold),
    titleMedium = style(18, FontWeight.Bold),
    titleSmall = style(15, FontWeight.SemiBold),
    bodyLarge = style(16, FontWeight.Normal, 25.6f),
    bodyMedium = style(15, FontWeight.Medium),
    bodySmall = style(13, FontWeight.Medium),
    labelLarge = style(15, FontWeight.SemiBold),
    labelMedium = style(13, FontWeight.SemiBold),
    labelSmall = style(11, FontWeight.Bold)
)
