package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

// Global state to dynamically toggle color values depending on active theme setting
var isDarkThemeActive by mutableStateOf(true)

// Premium High-Octane Gym Theme Palette - Adaptive Color Lookups
val VoltLime: Color
    get() = if (isDarkThemeActive) Color(0xFFCCFF00) else Color(0xFF426F00)       // High-energy primary neon lime vs. deep readable sporting green

val SportsTeal: Color
    get() = if (isDarkThemeActive) Color(0xFF00E5FF) else Color(0xFF006874)     // Electric form tracker cyan vs. deep athletic teal

val FlameOrange: Color
    get() = if (isDarkThemeActive) Color(0xFFFF5722) else Color(0xFFA23916)    // Secondary/tertiary alert/overload orange vs. deep orange-red

val CarbonObsidian: Color
    get() = if (isDarkThemeActive) Color(0xFF0C0D0E) else Color(0xFFF4F6F0) // Deep ultra black background vs. crisp light gray background

val DarkSpaceCharcoal: Color
    get() = if (isDarkThemeActive) Color(0xFF16181C) else Color(0xFFFFFFFF) // Elevated surface premium slate card vs. clean pure white card

val SlateStroke: Color
    get() = if (isDarkThemeActive) Color(0xFF2C2F36) else Color(0xFFE2E4DA)    // Subtle dark borders vs. soft light grey outline dividers

val IceWhite: Color
    get() = if (isDarkThemeActive) Color(0xFFF5F6F8) else Color(0xFF1A1C18)       // Dark mode readable status text vs. light mode charcoal headers

val MutedSlate: Color
    get() = if (isDarkThemeActive) Color(0xFF8E95A5) else Color(0xFF5A5F52)     // Subtext labels: soft blue-gray vs. high-contrast dark slate-gray

val AlertRed: Color
    get() = if (isDarkThemeActive) Color(0xFFFF2D55) else Color(0xFFBA1A1A)       // Fatigue alert red vs. deep warning red

val HealthyGreen: Color
    get() = if (isDarkThemeActive) Color(0xFF00E676) else Color(0xFF008035)   // Fresh recovery green vs. rich forest green
