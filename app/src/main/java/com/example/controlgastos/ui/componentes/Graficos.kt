package com.example.controlgastos.ui.componentes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controlgastos.domain.model.TotalCategoria
import com.example.controlgastos.domain.model.TotalMes
import com.example.controlgastos.domain.etiquetaMes
import com.example.controlgastos.domain.nombreMesCorto
import com.example.controlgastos.ui.formatearDinero
import com.example.controlgastos.ui.theme.Gastos
import java.time.YearMonth
import kotlin.math.roundToInt

// Gráficos de la web (src/components/charts.tsx).

/** Barras por mes: el mes destacado en rojo con su total encima. */
@Composable
fun BarrasMeses(datos: List<TotalMes>, destacado: YearMonth, moneda: String, alto: Dp = 208.dp) {
    val c = Gastos.colores
    val maximo = (datos.maxOfOrNull { it.total } ?: 0.0).coerceAtLeast(1.0)
    // Las barras crecen desde 0 al aparecer (transition de altura en la web).
    var mostrado by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mostrado = true }

    // Con muchas barras (12 meses en Comparar) se juntan más, como sm:gap-3 / gap-2.
    val muchas = datos.size > 8
    Row(
        Modifier.fillMaxWidth().height(alto).padding(top = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(if (muchas) 4.dp else 8.dp),
    ) {
        datos.forEachIndexed { i, (mes, total) ->
            val activo = mes == destacado
            val fraccion by animateFloatAsState(
                if (mostrado) (total / maximo).toFloat().coerceAtLeast(0.04f) else 0f,
                tween(700),
                label = "barra",
            )
            Column(
                Modifier.weight(1f).fillMaxHeight().semantics {
                    contentDescription = "${etiquetaMes(mes)}: ${formatearDinero(total, moneda)}"
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    val altoBarra = maxHeight * fraccion
                    Box(
                        Modifier.fillMaxWidth().height(altoBarra).clip(RoundedCornerShape(12.dp))
                            .background(if (activo) c.accent else c.line),
                    )
                    if (activo && total > 0) {
                        // Globo con el total sobre la barra. Es más ancho que la barra: en
                        // los extremos se alinea al borde para no salirse de la tarjeta.
                        val lado = when (i) {
                            0 -> Alignment.Start
                            datos.lastIndex -> Alignment.End
                            else -> Alignment.CenterHorizontally
                        }
                        Text(
                            formatearDinero(total, moneda, compacto = true),
                            modifier = Modifier.align(Alignment.BottomCenter).offset(y = -(altoBarra + 6.dp))
                                .wrapContentWidth(align = lado, unbounded = true)
                                .clip(CircleShape).background(c.primary).padding(horizontal = 10.dp, vertical = 4.dp),
                            style = estilo(11.sp, FontWeight.SemiBold, color = c.primaryFg, tabular = true),
                            maxLines = 1,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                // Una sola línea: si no cabe, sobresale centrada (no se parte "Nov" en "No/v").
                Text(
                    nombreMesCorto(mes),
                    modifier = Modifier.wrapContentWidth(unbounded = true),
                    style = estilo(if (muchas) 11.sp else 12.sp, if (activo) FontWeight.SemiBold else FontWeight.Normal, color = if (activo) c.fg else c.muted),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/** Gasto por categoría con su barra de porcentaje. */
@Composable
fun DesgloseCategorias(items: List<TotalCategoria>, total: Double, moneda: String) {
    val c = Gastos.colores
    Column {
        items.forEachIndexed { i, cat ->
            if (i > 0) HorizontalDivider(color = c.line)
            val porcentaje = if (total > 0) cat.total / total * 100 else 0.0
            Row(
                Modifier.fillMaxWidth().padding(top = if (i == 0) 0.dp else 14.dp, bottom = if (i == items.lastIndex) 0.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CajaCategoria(cat.icono)
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            cat.nombre,
                            modifier = Modifier.weight(1f),
                            style = estilo(15.sp, FontWeight.Medium, color = c.fg),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(formatearDinero(cat.total, moneda), style = estilo(15.sp, FontWeight.SemiBold, color = c.fg, tabular = true))
                    }
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        BarraProgreso((porcentaje / 100).toFloat().coerceAtLeast(0.02f), Modifier.weight(1f))
                        Text(
                            "${porcentaje.roundToInt()}%",
                            modifier = Modifier.width(44.dp),
                            style = estilo(12.sp, color = c.muted, tabular = true).copy(textAlign = TextAlign.End),
                        )
                    }
                }
            }
        }
    }
}

/** @param transparente sin riel de fondo (barras de Comparar). */
@Composable
fun BarraProgreso(fraccion: Float, modifier: Modifier = Modifier, alto: Dp = 6.dp, apagada: Boolean = false, transparente: Boolean = false) {
    val c = Gastos.colores
    Box(modifier.height(alto).clip(CircleShape).background(if (transparente) Color.Transparent else c.subtle)) {
        Box(Modifier.fillMaxWidth(fraccion).fillMaxHeight().clip(CircleShape).background(if (apagada) c.line else c.accent))
    }
}
