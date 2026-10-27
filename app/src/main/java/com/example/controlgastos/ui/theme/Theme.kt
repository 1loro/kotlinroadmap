package com.example.controlgastos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.controlgastos.R
import com.example.controlgastos.domain.model.Tema

/**
 * Tokens de color de gastos. (src/app/globals.css de la web): se usan por
 * NOMBRE de rol, igual que en Tailwind (`bg-canvas`, `text-muted`...).
 */
@Immutable
data class Colores(
    val canvas: Color,
    val surface: Color,
    val subtle: Color,
    val fg: Color,
    val muted: Color,
    val line: Color,
    val primary: Color,
    val primaryFg: Color,
    val accent: Color,
    val accentSoft: Color,
    val positive: Color,
    /** Color de la sombra de las tarjetas elevadas (rojo muy suave). */
    val sombra: Color,
    val esOscuro: Boolean,
)

val ColoresClaros = Colores(
    canvas = Color(0xFFFAF9F9),
    surface = Color(0xFFFFFFFF),
    subtle = Color(0xFFF6F1F2),
    fg = Color(0xFF0F0F12),
    muted = Color(0xFF7A788C),
    line = Color(0xFFEEE0E2),
    primary = Color(0xFF0F0F12),
    primaryFg = Color(0xFFFFFFFF),
    accent = Color(0xFFE8002D),
    accentSoft = Color(0xFFFDE8EC),
    positive = Color(0xFF0F8A5F),
    sombra = Color(0x47E8002D),
    esOscuro = false,
)

val ColoresOscuros = Colores(
    canvas = Color(0xFF0C0C0E),
    surface = Color(0xFF161618),
    subtle = Color(0xFF131315),
    fg = Color(0xFFF0F0F2),
    muted = Color(0xFF8A889C),
    line = Color(0xFF2A1820),
    primary = Color(0xFFFFFFFF),
    primaryFg = Color(0xFF0F0F12),
    accent = Color(0xFFFF1A3C),
    accentSoft = Color(0xFF1E0A0F),
    positive = Color(0xFF34D399),
    sombra = Color(0x2EFF1A3C),
    esOscuro = true,
)

val LocalColores = staticCompositionLocalOf { ColoresClaros }

/** Acceso corto: `Gastos.colores.accent`. */
object Gastos {
    val colores: Colores
        @Composable get() = LocalColores.current
}

// Geist, la misma tipografía de la web (licencia OFL).
val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
)

private val base = TextStyle(fontFamily = Geist)

private val tipografia = Typography(
    displayLarge = base, displayMedium = base, displaySmall = base,
    headlineLarge = base, headlineMedium = base, headlineSmall = base,
    titleLarge = base, titleMedium = base, titleSmall = base,
    bodyLarge = base.copy(fontSize = 16.sp), bodyMedium = base.copy(fontSize = 15.sp), bodySmall = base.copy(fontSize = 13.sp),
    labelLarge = base.copy(fontWeight = FontWeight.Medium), labelMedium = base, labelSmall = base,
)

@Composable
fun GastosTheme(tema: Tema = Tema.SISTEMA, content: @Composable () -> Unit) {
    val oscuro = when (tema) {
        Tema.CLARO -> false
        Tema.OSCURO -> true
        Tema.SISTEMA -> isSystemInDarkTheme()
    }
    val c = if (oscuro) ColoresOscuros else ColoresClaros
    // Los componentes de Material 3 que se usan (diálogos, selector de fecha,
    // menús) toman sus colores de estos roles.
    val esquema = (if (oscuro) darkColorScheme() else lightColorScheme()).copy(
        primary = c.accent,
        onPrimary = Color.White,
        primaryContainer = c.accentSoft,
        onPrimaryContainer = c.accent,
        secondary = c.fg,
        background = c.canvas,
        onBackground = c.fg,
        surface = c.surface,
        onSurface = c.fg,
        surfaceVariant = c.subtle,
        onSurfaceVariant = c.muted,
        surfaceContainerLowest = c.surface,
        surfaceContainerLow = c.surface,
        surfaceContainer = c.surface,
        surfaceContainerHigh = c.surface,
        surfaceContainerHighest = c.subtle,
        outline = c.line,
        outlineVariant = c.line,
        error = c.accent,
    )
    CompositionLocalProvider(LocalColores provides c) {
        MaterialTheme(colorScheme = esquema, typography = tipografia, content = content)
    }
}
