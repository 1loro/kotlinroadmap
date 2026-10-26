package com.example.controlgastos.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Paleta verde "bancaria" generada a partir del color semilla #0B6E4F
// (Material Theme Builder). Material 3 trabaja con ROLES de color: si solo
// definimos algunos, el resto toma la paleta base violeta, así que se
// declaran todos los que usan los componentes (containers, surfaces, etc.).
private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6E4F), onPrimary = Color.White,
    primaryContainer = Color(0xFFA2F2CE), onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF4C6358), onSecondary = Color.White,
    secondaryContainer = Color(0xFFCEE9DA), onSecondaryContainer = Color(0xFF092016),
    tertiary = Color(0xFF3D6373), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC1E8FB), onTertiaryContainer = Color(0xFF001F29),
    error = Color(0xFFB3261E), onError = Color.White,
    errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF5FBF6), onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF5FBF6), onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DE), onSurfaceVariant = Color(0xFF404944),
    outline = Color(0xFF707973), outlineVariant = Color(0xFFBFC9C2),
    surfaceDim = Color(0xFFD6DBD7), surfaceBright = Color(0xFFF5FBF6),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFEFF5F0),
    surfaceContainer = Color(0xFFE9EFEA), surfaceContainerHigh = Color(0xFFE4EAE5),
    surfaceContainerHighest = Color(0xFFDEE4DF),
    inverseSurface = Color(0xFF2B322E), inverseOnSurface = Color(0xFFECF2ED),
    inversePrimary = Color(0xFF86D6B3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF86D6B3), onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF00513B), onPrimaryContainer = Color(0xFFA2F2CE),
    secondary = Color(0xFFB3CCBF), onSecondary = Color(0xFF1F352B),
    secondaryContainer = Color(0xFF354B41), onSecondaryContainer = Color(0xFFCEE9DA),
    tertiary = Color(0xFFA5CCDF), onTertiary = Color(0xFF073543),
    tertiaryContainer = Color(0xFF244C5B), onTertiaryContainer = Color(0xFFC1E8FB),
    error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF0F1512), onBackground = Color(0xFFDEE4DF),
    surface = Color(0xFF0F1512), onSurface = Color(0xFFDEE4DF),
    surfaceVariant = Color(0xFF404944), onSurfaceVariant = Color(0xFFBFC9C2),
    outline = Color(0xFF89938D), outlineVariant = Color(0xFF404944),
    surfaceDim = Color(0xFF0F1512), surfaceBright = Color(0xFF353B38),
    surfaceContainerLowest = Color(0xFF0A0F0D), surfaceContainerLow = Color(0xFF171D1A),
    surfaceContainer = Color(0xFF1B211E), surfaceContainerHigh = Color(0xFF252B28),
    surfaceContainerHighest = Color(0xFF303633),
    inverseSurface = Color(0xFFDEE4DF), inverseOnSurface = Color(0xFF2B322E),
    inversePrimary = Color(0xFF0B6E4F),
)

@Composable
fun ControlGastosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Android 12+ puede tomar colores del fondo de pantalla del usuario.
    // Desactivado por defecto para mantener la identidad visual de la app.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
