package com.example.controlgastos.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlgastos.R
import com.example.controlgastos.ui.theme.Gastos

/**
 * Contenedor de cada pantalla: desplazable, con "deslizar para actualizar" y
 * margen inferior para que la navegación flotante no tape el contenido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Pantalla(
    actualizando: Boolean = false,
    onActualizar: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val columna = @Composable {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            contenido()
            Spacer(Modifier.height(144.dp))
        }
    }
    if (onActualizar == null) {
        columna()
    } else {
        PullToRefreshBox(isRefreshing = actualizando, onRefresh = onActualizar, modifier = Modifier.fillMaxSize()) { columna() }
    }
}

/** Separación vertical estándar entre secciones (gap-10). */
@Composable
fun EspacioSeccion() = Spacer(Modifier.height(40.dp))

/** Aviso discreto (p. ej. "Sin conexión. Mostrando datos guardados."). */
@Composable
fun Aviso(texto: String, modifier: Modifier = Modifier) {
    val c = Gastos.colores
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().padding(top = 16.dp).clip(forma).background(c.subtle).border(1.dp, c.line, forma)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GIcono(R.drawable.lucide_wifi_off, tamano = 16.dp, color = c.muted)
        Text(texto, style = estilo(13.sp, FontWeight.Medium, color = c.muted))
    }
}
