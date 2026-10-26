package com.example.controlgastos.ui.seguridad

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.controlgastos.ui.theme.ControlGastosTheme

/**
 * Cubre por completo la app mientras está bloqueada. Es opaca y consume los
 * toques, para que no se vea ni se pueda tocar nada de lo que hay debajo.
 */
@Composable
fun PantallaBloqueo(error: String?, onDesbloquear: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            // Absorbe los toques (sin efecto visual) para que no lleguen a la app.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Text("Control de Gastos está bloqueado", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                error ?: "Usa tu huella o el bloqueo de pantalla para continuar.",
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onDesbloquear) { Text("Desbloquear") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaBloqueoPreview() {
    ControlGastosTheme { PantallaBloqueo(error = null, onDesbloquear = {}) }
}
