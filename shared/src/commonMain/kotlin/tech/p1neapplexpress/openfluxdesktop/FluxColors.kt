package tech.p1neapplexpress.openfluxdesktop

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object FluxColors {
    // Основные цвета
    val background = Color(0xFF0A0E27)      // colorBackground
    val surfaceDark = Color(0xFF1A1F4B)     // colorSurfaceDark
    val accent = Color(0xFF00D4FF)          // colorAccent
    val success = Color(0xFF00FF88)         // colorSuccess
    val textSecondary = Color(0xFF8890B0)   // colorTextSecondary
    val textHint = Color(0xFF5A6080)        // colorTextHint
    val terminal = Color(0xFF0D1120)        // colorTerminal
    val terminalText = Color(0xFF00FF88)    // colorTerminalText
    val overlay = Color(0xCC0A0E27)         // colorOverlay
    val toolbar = Color(0xFF334F7CFF)       // toolbar_color

    // Дополнительные
    val white = Color(0xFFFFFFFF)
    val text = Color(0xFFFFFFFF)
    val red = Color(0xFFFF0000)
    val green = Color(0xFF00FF88)           // Используем colorSuccess
    val transparent = Color.Transparent
}

val fluxMaterialTheme = darkColorScheme(
    primary = FluxColors.accent,
    onPrimary = FluxColors.white,
    background = FluxColors.background,
    onBackground = FluxColors.white,
    surface = FluxColors.surfaceDark,
    onSurface = FluxColors.white,
    secondary = FluxColors.textSecondary,
    onSecondary = FluxColors.white,
    error = FluxColors.red,
    onError = FluxColors.white
)