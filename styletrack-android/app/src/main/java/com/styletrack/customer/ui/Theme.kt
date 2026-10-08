package com.styletrack.customer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFFC42A6B)
private val Ink = Color(0xFF231B33)
private val Paper = Color(0xFFF5F3F8)

private val Light = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBE7EF),
    onPrimaryContainer = Color(0xFF7A1040),
    secondary = Color(0xFF12807A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9F0ED),
    onSecondaryContainer = Color(0xFF0B5D58),
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEAE6F0),
    onSurfaceVariant = Color(0xFF5A4F70),
    outline = Color(0xFFC4BDD2),
    error = Color(0xFF9C2F3F),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFF8DB8),
    onPrimary = Color(0xFF5E0B31),
    primaryContainer = Color(0xFF7A1040),
    onPrimaryContainer = Color(0xFFFBE7EF),
    secondary = Color(0xFF6FD6CD),
    onSecondary = Color(0xFF00201E),
    secondaryContainer = Color(0xFF0B5D58),
    onSecondaryContainer = Color(0xFFD9F0ED),
    background = Color(0xFF17121F),
    onBackground = Color(0xFFEDE8F5),
    surface = Color(0xFF211A2D),
    onSurface = Color(0xFFEDE8F5),
    surfaceVariant = Color(0xFF3A2E4F),
    onSurfaceVariant = Color(0xFFC9C0D9),
    outline = Color(0xFF7D7390),
    error = Color(0xFFFFB3BC),
)

@Composable
fun StyleTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
