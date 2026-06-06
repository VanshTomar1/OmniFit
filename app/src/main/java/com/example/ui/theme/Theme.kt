package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VoltLime,
    onPrimary = Color.Black,
    primaryContainer = DarkSpaceCharcoal,
    onPrimaryContainer = VoltLime,
    
    secondary = SportsTeal,
    onSecondary = Color.Black,
    secondaryContainer = DarkSpaceCharcoal,
    onSecondaryContainer = SportsTeal,
    
    tertiary = FlameOrange,
    onTertiary = Color.White,
    tertiaryContainer = DarkSpaceCharcoal.copy(alpha = 0.8f),
    onTertiaryContainer = FlameOrange,
    
    background = CarbonObsidian,
    onBackground = IceWhite,
    surface = DarkSpaceCharcoal,
    onSurface = IceWhite,
    surfaceVariant = SlateStroke,
    onSurfaceVariant = MutedSlate,
    
    outline = SlateStroke,
    error = AlertRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF527E00), // More controlled dark green-lime for readable light theme
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2FFA3),
    onPrimaryContainer = Color(0xFF162B00),
    
    secondary = Color(0xFF006874),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF97F0FF),
    onSecondaryContainer = Color(0xFF001F24),
    
    tertiary = Color(0xFFA23916),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD2),
    onTertiaryContainer = Color(0xFF3C0800),
    
    background = Color(0xFFF8FAF4),
    onBackground = Color(0xFF1A1C18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C18),
    surfaceVariant = Color(0xFFE1E4D5),
    onSurfaceVariant = Color(0xFF44483D),
    
    outline = Color(0xFF75796C),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark sports theme as default for active workout environments
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
