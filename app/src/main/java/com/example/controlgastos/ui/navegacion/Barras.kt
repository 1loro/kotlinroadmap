package com.example.controlgastos.ui.navegacion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.controlgastos.R
import com.example.controlgastos.domain.model.Usuario
import com.example.controlgastos.ui.componentes.Avatar
import com.example.controlgastos.ui.componentes.BotonIcono
import com.example.controlgastos.ui.componentes.Divisor
import com.example.controlgastos.ui.componentes.GIcono
import com.example.controlgastos.ui.componentes.Logo
import com.example.controlgastos.ui.theme.Gastos
import kotlin.reflect.KClass

// Barras de la web (src/components/nav.tsx).

/** Logo a la izquierda; botón de tema y foto de perfil a la derecha. */
@Composable
fun BarraSuperior(
    usuario: Usuario,
    avatar: ByteArray?,
    onLogo: () -> Unit,
    onTema: () -> Unit,
    onPerfil: () -> Unit,
) {
    val c = Gastos.colores
    Column(Modifier.fillMaxWidth().background(c.canvas.copy(alpha = 0.95f)).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                Box(Modifier.clip(CircleShape).clickable(onClick = onLogo).semantics { contentDescription = "Inicio" }) { Logo() }
            }
            BotonIcono(
                if (c.esOscuro) R.drawable.lucide_sun else R.drawable.lucide_moon,
                if (c.esOscuro) "Activar modo claro" else "Activar modo oscuro",
                onTema,
            )
            Box(
                Modifier.padding(start = 8.dp).clip(CircleShape).clickable(onClick = onPerfil)
                    .semantics { contentDescription = "Tu perfil" },
            ) { Avatar(usuario, avatar) }
        }
        Divisor()
    }
}

data class ItemNavegacion(val etiqueta: String, val icono: Int, val ruta: Any)

val ITEMS_NAVEGACION = listOf(
    ItemNavegacion("Inicio", R.drawable.lucide_house, Inicio),
    ItemNavegacion("Movimientos", R.drawable.lucide_arrow_left_right, Movimientos()),
    ItemNavegacion("Nuevo gasto", R.drawable.lucide_plus, NuevoGasto()),
    ItemNavegacion("Comparar", R.drawable.lucide_chart_no_axes_column, Comparar),
    ItemNavegacion("Ajustes", R.drawable.lucide_settings_2, Ajustes),
)

/** Navegación flotante en píldora; el ítem activo, en un círculo rojo. */
@Composable
fun BarraInferior(activo: KClass<*>?, onNavegar: (Any) -> Unit, modifier: Modifier = Modifier) {
    val c = Gastos.colores
    Row(
        modifier.shadow(24.dp, CircleShape, ambientColor = c.sombra, spotColor = c.sombra)
            .clip(CircleShape).background(c.surface.copy(alpha = 0.95f)).border(1.dp, c.line, CircleShape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ITEMS_NAVEGACION.forEach { item ->
            val seleccionado = item.ruta::class == activo
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(if (seleccionado) c.accent else Color.Transparent)
                    .clickable(role = Role.Tab) { onNavegar(item.ruta) }
                    .semantics {
                        contentDescription = item.etiqueta
                        selected = seleccionado
                    },
                Alignment.Center,
            ) {
                GIcono(item.icono, tamano = 21.dp, color = if (seleccionado) Color.White else c.muted)
            }
        }
    }
}
