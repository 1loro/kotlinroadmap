package com.example.controlgastos.ui.gasto

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controlgastos.R
import com.example.controlgastos.domain.model.TipoIngreso
import com.example.controlgastos.ui.componentes.BotonIcono
import com.example.controlgastos.ui.componentes.BotonPrimario
import com.example.controlgastos.ui.componentes.CajaCategoria
import com.example.controlgastos.ui.componentes.CampoTexto
import com.example.controlgastos.ui.componentes.EncabezadoPagina
import com.example.controlgastos.ui.componentes.Esqueleto
import com.example.controlgastos.ui.componentes.EstadoVacio
import com.example.controlgastos.ui.componentes.GIcono
import com.example.controlgastos.ui.componentes.GrupoFormulario
import com.example.controlgastos.ui.componentes.MensajeError
import com.example.controlgastos.ui.componentes.Pantalla
import com.example.controlgastos.ui.componentes.Segmentado
import com.example.controlgastos.ui.componentes.TarjetaElevada
import com.example.controlgastos.ui.componentes.estilo
import com.example.controlgastos.ui.formatearFechaNumerica
import com.example.controlgastos.ui.simboloMoneda
import com.example.controlgastos.ui.theme.Gastos
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// Formulario de la web (src/components/expense-form.tsx): nuevo y editar.

private val ORIGEN = mapOf(
    "shortcut" to "Registrado desde el Atajo",
    "android" to "Registrado desde Android",
)

@Composable
fun GastoFormRuta(onCerrar: () -> Unit, viewModel: GastoFormViewModel = hiltViewModel()) {
    val e by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(e.terminado) { if (e.terminado) onCerrar() }
    var confirmarBorrado by remember { mutableStateOf(false) }
    val esIngreso = e.tipo == TipoMovimiento.INGRESO

    Pantalla {
        EncabezadoPagina(
            titulo = if (e.edicion) "Editar" else "Nuevo",
            acento = if (esIngreso) "ingreso." else "gasto.",
            pastilla = if (e.edicion) ORIGEN[e.origen] ?: "Registrado en la app" else null,
            accion = { BotonIcono(R.drawable.lucide_x, "Cerrar", onCerrar) },
        )
        when {
            e.cargando -> Esqueleto(Modifier.fillMaxWidth().height(144.dp))
            e.noExiste -> EstadoVacio(R.drawable.lucide_receipt_text, "Gasto no encontrado", "Puede que se haya eliminado desde otro dispositivo.")
            else -> Formulario(e, viewModel) { confirmarBorrado = true }
        }
    }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Eliminar este gasto?") },
            text = { Text("También se borra de tu cuenta en la web y en el iPhone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarBorrado = false
                    viewModel.eliminar()
                }) { Text("Eliminar", color = Gastos.colores.accent) }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Formulario(e: GastoFormUiState, vm: GastoFormViewModel, onEliminar: () -> Unit) {
    val c = Gastos.colores
    val esIngreso = e.tipo == TipoMovimiento.INGRESO
    // Ingresos en verde para distinguirlos de los gastos.
    val acento = if (esIngreso) c.positive else c.accent

    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        if (!e.edicion) {
            Segmentado(
                listOf(TipoMovimiento.GASTO to "Gasto", TipoMovimiento.INGRESO to "Ingreso"),
                e.tipo,
                vm::cambiarTipo,
            )
        }

        CampoMonto(e.montoTexto, simboloMoneda(e.moneda), if (esIngreso) "¿Cuánto recibiste?" else "¿Cuánto gastaste?", autoFoco = !e.edicion, onCambio = vm::cambiarMonto)

        if (esIngreso) {
            GrupoFormulario("Tipo de ingreso") {
                Cuadricula(
                    items = TipoIngreso.entries.map { Triple(it.ordinal.toLong(), it.etiqueta, it.icono) },
                    elegido = e.tipoIngreso?.ordinal?.toLong(),
                    acento = acento,
                ) { vm.elegirTipoIngreso(TipoIngreso.entries[it.toInt()]) }
            }
        } else {
            GrupoFormulario("Categoría") {
                Cuadricula(e.categorias.map { Triple(it.id, it.nombre, it.icono) }, e.categoriaId, acento, vm::elegirCategoria)
            }
        }

        GrupoFormulario("Fecha") { SelectorFecha(e.fecha, e.hoy, vm::elegirFecha) }

        GrupoFormulario("Nota", "(opcional)") {
            CampoTexto(e.nota, vm::cambiarNota, "Nota", placeholder = "Ej: almuerzo con el equipo", largoMaximo = 200)
        }

        e.error?.let { MensajeError(it) }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BotonPrimario(
                when {
                    e.edicion -> "Guardar cambios"
                    esIngreso -> "Registrar ingreso"
                    else -> "Registrar gasto"
                },
                vm::guardar,
                Modifier.fillMaxWidth(),
                alto = 52.dp,
                icono = R.drawable.lucide_check,
                cargando = e.guardando,
            )
            if (e.edicion) {
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onEliminar),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GIcono(R.drawable.lucide_trash_2, tamano = 16.dp, color = c.accent)
                    Text("Eliminar gasto", style = estilo(15.sp, FontWeight.Medium, color = c.accent))
                }
            }
        }
    }
}

/** Tarjeta con el monto en grande: "$ 12.500". */
@Composable
private fun CampoMonto(valor: String, simbolo: String, etiqueta: String, autoFoco: Boolean, onCambio: (String) -> Unit) {
    val c = Gastos.colores
    val foco = remember { FocusRequester() }
    LaunchedEffect(autoFoco) { if (autoFoco) runCatching { foco.requestFocus() } }
    TarjetaElevada(relleno = 0.dp) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp)) {
            Text(etiqueta, style = estilo(14.sp, color = c.muted))
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(simbolo, style = estilo(34.sp, FontWeight.SemiBold, (-0.02).em, c.muted))
                Box(Modifier.weight(1f)) {
                    val grande = estilo(52.sp, FontWeight.SemiBold, (-0.05).em, c.fg, tabular = true)
                    if (valor.isEmpty()) Text("0", style = grande.copy(color = c.muted.copy(alpha = 0.4f)))
                    BasicTextField(
                        value = valor,
                        onValueChange = onCambio,
                        modifier = Modifier.fillMaxWidth().focusRequester(foco).semantics { contentDescription = "Monto" },
                        singleLine = true,
                        textStyle = grande,
                        cursorBrush = SolidColor(c.accent),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    )
                }
            }
        }
    }
}

/** Cuadrícula de 3 columnas con ícono y nombre (categorías o tipos de ingreso). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Cuadricula(items: List<Triple<Long, String, String>>, elegido: Long?, acento: Color, onElegir: (Long) -> Unit) {
    val c = Gastos.colores
    FlowRow(
        maxItemsInEachRow = 3,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEach { (id, nombre, icono) ->
            val activo = id == elegido
            val forma = RoundedCornerShape(16.dp)
            Column(
                Modifier.weight(1f).defaultMinSize(minHeight = 96.dp).clip(forma)
                    .background(if (activo) (if (acento == c.accent) c.accentSoft else acento.copy(alpha = 0.12f)) else c.surface)
                    .border(1.dp, if (activo) acento else c.line, forma)
                    .clickable(role = Role.Button) { onElegir(id) }
                    .semantics { selected = activo }
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                CajaCategoria(icono, chica = true, activa = activo, colorAcento = acento)
                Spacer(Modifier.height(12.dp))
                Text(
                    nombre,
                    style = estilo(12.5.sp, FontWeight.Medium, (-0.01).em, if (activo) acento else c.fg, interlineado = 15.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // Completa la última fila para que las celdas mantengan su ancho.
        repeat((3 - items.size % 3) % 3) { Spacer(Modifier.weight(1f)) }
    }
}

/** Hoy / Ayer / Otra fecha (con calendario, sin fechas futuras). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SelectorFecha(fecha: LocalDate, hoy: LocalDate, onElegir: (LocalDate) -> Unit) {
    val ayer = hoy.minusDays(1)
    val otra = fecha != hoy && fecha != ayer
    var calendario by remember { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("Hoy", fecha == hoy) { onElegir(hoy) }
        Chip("Ayer", fecha == ayer) { onElegir(ayer) }
        Chip(if (otra) formatearFechaNumerica(fecha) else "Otra fecha", otra, R.drawable.lucide_calendar_days) { calendario = true }
    }
    if (calendario) {
        val millisHoy = hoy.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= millisHoy
            },
        )
        DatePickerDialog(
            onDismissRequest = { calendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { onElegir(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    calendario = false
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { calendario = false }) { Text("Cancelar") } },
        ) { DatePicker(estado) }
    }
}

@Composable
private fun Chip(texto: String, activo: Boolean, icono: Int? = null, onClick: () -> Unit) {
    val c = Gastos.colores
    Row(
        Modifier.height(40.dp).clip(CircleShape).background(if (activo) c.primary else c.surface)
            .border(1.dp, if (activo) c.primary else c.line, CircleShape)
            .clickable(role = Role.Button, onClick = onClick).semantics { selected = activo }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icono != null) GIcono(icono, tamano = 16.dp, color = if (activo) c.primaryFg else c.fg)
        Text(texto, style = estilo(14.sp, FontWeight.Medium, color = if (activo) c.primaryFg else c.fg))
    }
}
