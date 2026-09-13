package tech.p1neapplexpress.openfluxdesktop

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

object FluxColors {
    val background = Color(0xFF0A0E27)
    val surfaceDark = Color(0xFF1A1F4B)
    val accent = Color(0xFF00D4FF)
    val success = Color(0xFF00FF88)
    val textSecondary = Color(0xFF8890B0)
    val textHint = Color(0xFF5A6080)
    val terminal = Color(0xFF080B1C)
    val terminalText = Color(0xFF8FE5C0)
    val overlay = Color(0xCC0A0E27)
    val toolbar = Color(0xFF334F7CFF)

    val white = Color(0xFFFFFFFF)
    val text = Color(0xFFFFFFFF)
    val red = Color(0xFFFF5A5A)
    val green = Color(0xFF00FF88)
    val transparent = Color.Transparent

    val cardBg = Color(0xFF111635)
    val cardBgElevated = Color(0xFF161C42)
    val stroke = Color(0xFF232A55)
    val strokeStrong = Color(0xFF2E3768)

    val accentDim = Color(0xFF00A6CC)
    val accentGlow = Color(0x3300D4FF)
    val successDim = Color(0xFF00C96B)
    val warn = Color(0xFFFFB300)
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
