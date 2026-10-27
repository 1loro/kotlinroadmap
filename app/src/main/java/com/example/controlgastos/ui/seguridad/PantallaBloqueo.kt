package com.example.controlgastos.ui.seguridad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.controlgastos.R
import com.example.controlgastos.ui.componentes.BotonPrimario
import com.example.controlgastos.ui.componentes.GIcono
import com.example.controlgastos.ui.componentes.Logo
import com.example.controlgastos.ui.componentes.estilo
import com.example.controlgastos.ui.theme.Gastos

/**
 * Cubre por completo la app mientras está bloqueada. Es opaca y consume los
 * toques, para que no se vea ni se pueda tocar nada de lo que hay debajo.
 */
@Composable
fun PantallaBloqueo(error: String?, onDesbloquear: () -> Unit, modifier: Modifier = Modifier) {
    val c = Gastos.colores
    Box(
        modifier.fillMaxSize().background(c.canvas)
            // Absorbe los toques (sin efecto visual) para que no lleguen a la app.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Logo(grande = true)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.size(64.dp).clip(CircleShape).background(c.accentSoft), Alignment.Center) {
                GIcono(R.drawable.lucide_lock, tamano = 28.dp, color = c.accent)
            }
            Text(
                "gastos. está bloqueado",
                style = estilo(26.sp, FontWeight.SemiBold, (-0.04).em, c.fg).copy(textAlign = TextAlign.Center),
            )
            Text(
                error ?: "Usa tu huella o el bloqueo de pantalla para continuar.",
                style = estilo(15.sp, if (error != null) FontWeight.Medium else FontWeight.Normal, color = if (error != null) c.accent else c.muted)
                    .copy(textAlign = TextAlign.Center),
            )
            BotonPrimario("Desbloquear", onDesbloquear, icono = R.drawable.lucide_fingerprint_pattern)
        }
    }
}
