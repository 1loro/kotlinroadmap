package com.example.controlgastos.ui.comparar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controlgastos.domain.cambioPorcentual
import com.example.controlgastos.domain.nombreMesCorto
import com.example.controlgastos.ui.componentes.Aviso
import com.example.controlgastos.ui.componentes.BarraProgreso
import com.example.controlgastos.ui.componentes.BarrasMeses
import com.example.controlgastos.ui.componentes.CajaCategoria
import com.example.controlgastos.ui.componentes.Delta
import com.example.controlgastos.ui.componentes.EncabezadoPagina
import com.example.controlgastos.ui.componentes.EspacioSeccion
import com.example.controlgastos.ui.componentes.Pantalla
import com.example.controlgastos.ui.componentes.SelectorMes
import com.example.controlgastos.ui.componentes.Tarjeta
import com.example.controlgastos.ui.componentes.TarjetaElevada
import com.example.controlgastos.ui.componentes.TituloSeccion
import com.example.controlgastos.ui.componentes.estilo
import com.example.controlgastos.ui.formatearDinero
import com.example.controlgastos.ui.theme.Gastos
import java.time.YearMonth
import kotlin.math.abs

// Comparar de la web (src/app/(app)/comparar/page.tsx).

@Composable
fun CompararRuta(viewModel: CompararViewModel = hiltViewModel()) {
    val e by viewModel.uiState.collectAsStateWithLifecycle()
    val c = Gastos.colores
    val diferencia = e.totalB - e.totalA

    Pantalla(e.actualizando, viewModel::actualizar) {
        e.aviso?.let { Aviso(it) }
        EncabezadoPagina(
            titulo = "Compara tus",
            acento = "meses.",
            descripcion = "Elige dos meses y mira en qué categorías gastaste más o menos.",
        )

        TarjetaElevada {
            FilaMes("Mes A", destacado = false, total = formatearDinero(e.totalA, e.moneda)) {
                SelectorMes(e.a, e.mesActual, viewModel::cambiarA, compacto = true)
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = c.line)
            FilaMes("Mes B", destacado = true, total = formatearDinero(e.totalB, e.moneda)) {
                SelectorMes(e.b, e.mesActual, viewModel::cambiarB, compacto = true)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(20.dp)).background(c.subtle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (diferencia == 0.0) {
                        "Gastaste lo mismo"
                    } else {
                        "Gastaste ${formatearDinero(abs(diferencia), e.moneda)} ${if (diferencia > 0) "más" else "menos"} en ${nombreMesCorto(e.b).lowercase()}"
                    },
                    modifier = Modifier.weight(1f),
                    style = estilo(14.sp, color = c.muted),
                )
                Spacer(Modifier.width(12.dp))
                Delta(cambioPorcentual(e.totalB, e.totalA))
            }
        }

        EspacioSeccion()
        TituloSeccion("Tendencia", "Últimos 12 meses")
        Tarjeta { BarrasMeses(e.tendencia, e.b, e.moneda) }

        EspacioSeccion()
        TituloSeccion("Detalle", "Categoría por categoría") { Leyenda(e.a, e.b) }
        Tarjeta {
            if (e.filas.isEmpty()) {
                Text(
                    "No hay gastos en ninguno de los dos meses.",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    style = estilo(14.sp, color = c.muted).copy(textAlign = TextAlign.Center),
                )
            } else {
                val maximo = e.filas.maxOf { maxOf(it.a, it.b) }.coerceAtLeast(1.0)
                e.filas.forEachIndexed { i, fila ->
                    if (i > 0) HorizontalDivider(color = c.line)
                    Row(
                        Modifier.padding(top = if (i == 0) 0.dp else 16.dp, bottom = if (i == e.filas.lastIndex) 0.dp else 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        CajaCategoria(fila.icono)
                        Column(Modifier.weight(1f)) {
                            Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    fila.nombre,
                                    modifier = Modifier.weight(1f),
                                    style = estilo(15.sp, FontWeight.Medium, color = c.fg),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Delta(fila.cambio)
                            }
                            FilaBarra(fila.a, maximo, e.moneda, apagada = true)
                            FilaBarra(fila.b, maximo, e.moneda, apagada = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaMes(etiqueta: String, destacado: Boolean, total: String, selector: @Composable () -> Unit) {
    val c = Gastos.colores
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (destacado) c.accent else c.line))
                Text(etiqueta, style = estilo(13.sp, color = if (destacado) c.accent else c.muted))
            }
            Text(
                total,
                modifier = Modifier.padding(top = 2.dp),
                style = estilo(26.sp, FontWeight.SemiBold, (-0.04).em, c.fg, tabular = true),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        selector()
    }
}

@Composable
private fun FilaBarra(valor: Double, maximo: Double, moneda: String, apagada: Boolean) {
    val c = Gastos.colores
    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            if (valor > 0) BarraProgreso((valor / maximo).toFloat().coerceAtLeast(0.03f), alto = 8.dp, apagada = apagada, transparente = true)
        }
        Text(
            formatearDinero(valor, moneda),
            modifier = Modifier.width(104.dp),
            style = estilo(13.sp, if (apagada) FontWeight.Normal else FontWeight.SemiBold, color = if (apagada) c.muted else c.fg, tabular = true)
                .copy(textAlign = TextAlign.End),
        )
    }
}

@Composable
private fun Leyenda(a: YearMonth, b: YearMonth) {
    val c = Gastos.colores
    Row(Modifier.padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(a to c.line, b to c.accent).forEach { (mes, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Text(nombreMesCorto(mes), style = estilo(12.sp, color = c.muted))
            }
        }
    }
}
