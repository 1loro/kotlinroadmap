package com.example.controlgastos.ui.indicadores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controlgastos.R
import com.example.controlgastos.domain.model.Indicador
import com.example.controlgastos.ui.componentes.BotonIcono
import com.example.controlgastos.ui.componentes.BotonPrimario
import com.example.controlgastos.ui.componentes.EncabezadoPagina
import com.example.controlgastos.ui.componentes.Esqueleto
import com.example.controlgastos.ui.componentes.EstadoVacio
import com.example.controlgastos.ui.componentes.MensajeError
import com.example.controlgastos.ui.componentes.Pantalla
import com.example.controlgastos.ui.componentes.Tarjeta
import com.example.controlgastos.ui.componentes.estilo
import com.example.controlgastos.ui.formatearDiaChile
import com.example.controlgastos.ui.formatearValorIndicador
import com.example.controlgastos.ui.theme.Gastos

// Indicadores del Banco Central de Chile (mindicador.cl), con el diseño de gastos.

@Composable
fun IndicadoresRuta(onVolver: () -> Unit, viewModel: IndicadoresViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    IndicadoresScreen(estado, onActualizar = viewModel::actualizar, onVolver = onVolver)
}

@Composable
fun IndicadoresScreen(estado: IndicadoresUiState, onActualizar: () -> Unit, onVolver: () -> Unit) {
    val c = Gastos.colores
    val actualizando = (estado as? IndicadoresUiState.Exito)?.actualizando == true
    Pantalla(actualizando, onActualizar) {
        EncabezadoPagina(
            titulo = "Indicadores,",
            acento = "hoy en Chile.",
            descripcion = "Dólar observado, euro, UF y UTM del Banco Central. Funcionan sin conexión con el último valor guardado.",
            accion = { BotonIcono(R.drawable.lucide_x, "Cerrar", onVolver) },
        )
        when (estado) {
            IndicadoresUiState.Cargando -> Esqueleto(Modifier.fillMaxWidth().height(288.dp))

            is IndicadoresUiState.Error -> Tarjeta {
                EstadoVacio(R.drawable.lucide_triangle_alert, estado.mensaje, "No hay indicadores guardados todavía. Revisa tu conexión e intenta de nuevo.") {
                    BotonPrimario("Reintentar", onActualizar, alto = 44.dp, icono = R.drawable.lucide_refresh_cw)
                }
            }

            is IndicadoresUiState.Exito -> {
                estado.aviso?.let { aviso ->
                    MensajeError(
                        "$aviso. Mostrando datos guardados" +
                            (estado.indicadores.maxOfOrNull { it.fecha }?.let { " del ${formatearDiaChile(it)}" } ?: ""),
                        Modifier.padding(bottom = 16.dp),
                    )
                }
                Tarjeta {
                    if (estado.indicadores.isEmpty()) {
                        Text("No hay indicadores disponibles", style = estilo(14.sp, color = c.muted))
                    }
                    estado.indicadores.forEachIndexed { i, indicador ->
                        if (i > 0) HorizontalDivider(color = c.line)
                        FilaIndicador(indicador, primera = i == 0, ultima = i == estado.indicadores.lastIndex)
                    }
                }
                Text(
                    "Fuente: mindicador.cl (Banco Central de Chile) · Desliza hacia abajo para actualizar",
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    style = estilo(12.sp, color = c.muted).copy(textAlign = TextAlign.Center),
                )
            }
        }
    }
}

@Composable
private fun FilaIndicador(indicador: Indicador, primera: Boolean, ultima: Boolean) {
    val c = Gastos.colores
    Row(
        Modifier.fillMaxWidth().padding(top = if (primera) 0.dp else 16.dp, bottom = if (ultima) 0.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(indicador.nombre, style = estilo(15.sp, FontWeight.Medium, color = c.fg))
            Text("Valor del ${formatearDiaChile(indicador.fecha)}", style = estilo(13.sp, color = c.muted))
        }
        Text(formatearValorIndicador(indicador.valor), style = estilo(20.sp, FontWeight.SemiBold, (-0.03).em, c.fg, tabular = true))
    }
}
