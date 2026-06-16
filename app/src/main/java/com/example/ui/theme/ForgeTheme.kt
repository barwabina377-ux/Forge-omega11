package com.example.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val CyberGreen = Color(0xFF00FF41)
val CyberDark = Color(0xFF0D0D11)
val CyberGray = Color(0xFF1E1E24)
val CyberText = Color(0xFFE0E0E0)
val CyberAccent = Color(0xFF00B8FF)

val ForgeDarkColorScheme = darkColorScheme(
    primary = CyberGreen,
    onPrimary = CyberDark,
    secondary = CyberAccent,
    onSecondary = CyberDark,
    background = CyberDark,
    onBackground = CyberText,
    surface = CyberGray,
    onSurface = CyberText,
    error = Color(0xFFFF3366),
    onError = CyberText
)
