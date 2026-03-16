package com.minebot.app

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MineBotTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 42.sp,
        lineHeight = 46.sp,
        fontWeight = FontWeight.ExtraBold
    ),
    headlineMedium = TextStyle(
        fontSize = 32.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.ExtraBold
    ),
    titleLarge = TextStyle(
        fontSize = 28.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold
    ),
    titleMedium = TextStyle(
        fontSize = 22.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold
    ),
    bodyLarge = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium
    ),
    labelLarge = TextStyle(
        fontSize = 18.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold
    )
)
