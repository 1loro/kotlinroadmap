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

// Paleta verde "bancaria". Material 3 deriva el resto de los roles de color.
private val Verde = Color(0xFF0B6E4F)
private val VerdeClaro = Color(0xFF7FD8B4)
private val Rojo = Color(0xFFB3261E)
private val RojoClaro = Color(0xFFF2B8B5)

private val LightColors = lightColorScheme(
    primary = Verde,
    secondary = Color(0xFF4C6358),
    tertiary = Color(0xFF3D6373),
    error = Rojo,
)

private val DarkColors = darkColorScheme(
    primary = VerdeClaro,
    secondary = Color(0xFFB3CCBF),
    tertiary = Color(0xFFA5CCDF),
    error = RojoClaro,
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
