package com.example.controlgastos.ui.movimientos

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controlgastos.R
import com.example.controlgastos.domain.model.Ingreso
import com.example.controlgastos.domain.nombreMes
import com.example.controlgastos.ui.componentes.Aviso
import com.example.controlgastos.ui.componentes.BotonContorno
import com.example.controlgastos.ui.componentes.BotonPrimario
import com.example.controlgastos.ui.componentes.DesgloseCategorias
import com.example.controlgastos.ui.componentes.EncabezadoPagina
import com.example.controlgastos.ui.componentes.EspacioSeccion
import com.example.controlgastos.ui.componentes.EstadoVacio
import com.example.controlgastos.ui.componentes.ListaGastos
import com.example.controlgastos.ui.componentes.ListaIngresos
import com.example.controlgastos.ui.componentes.Pantalla
import com.example.controlgastos.ui.componentes.SelectorMes
import com.example.controlgastos.ui.componentes.Tarjeta
import com.example.controlgastos.ui.componentes.TituloSeccion
import com.example.controlgastos.ui.componentes.estilo
import com.example.controlgastos.ui.formatearDinero
import com.example.controlgastos.ui.theme.Gastos

// Movimientos de la web (src/app/(app)/movimientos/page.tsx) + ingresos locales.

@Composable
fun MovimientosRuta(
    onNuevoGasto: () -> Unit,
    onNuevoIngreso: () -> Unit,
    onGasto: (Long) -> Unit,
    viewModel: MovimientosViewModel = hiltViewModel(),
) {
    val e by viewModel.uiState.collectAsStateWithLifecycle()
    var ingresoAEliminar by remember { mutableStateOf<Ingreso?>(null) }

    Pantalla(e.actualizando, { viewModel.actualizar() }) {
        e.aviso?.let { Aviso(it) }
        EncabezadoPagina(
            titulo = "${nombreMes(e.mes)},",
            acento = "${e.mes.year}.",
            pastilla = "${e.gastos.size} ${if (e.gastos.size == 1) "movimiento" else "movimientos"} · ${formatearDinero(e.total, e.moneda)}",
        )
        SelectorMes(e.mes, e.mesActual, viewModel::cambiarMes)
        EspacioSeccion()

        TituloSeccion("Historial", "Día a día")
        Tarjeta {
            if (e.gastos.isNotEmpty()) {
                ListaGastos(e.gastos, e.moneda, e.hoy) { onGasto(it.id) }
            } else {
                EstadoVacio(R.drawable.lucide_receipt_text, "Nada por aquí", "No registraste gastos en este mes.") {
                    BotonPrimario("Registrar gasto", onNuevoGasto, alto = 44.dp, icono = R.drawable.lucide_plus)
                }
            }
        }

        if (e.porCategoria.isNotEmpty()) {
            EspacioSeccion()
            TituloSeccion("Por categoría", "Resumen")
            Tarjeta { DesgloseCategorias(e.porCategoria, e.total, e.moneda) }
        }

        EspacioSeccion()
        TituloSeccion("Ingresos", "Solo en este teléfono") {
            BotonContorno("Ingreso", onNuevoIngreso, icono = R.drawable.lucide_plus)
        }
        Tarjeta {
            if (e.ingresos.isNotEmpty()) {
                ListaIngresos(e.ingresos, e.moneda, e.hoy) { ingresoAEliminar = it }
            } else {
                Text("No registraste ingresos en este mes.", style = estilo(14.sp, color = Gastos.colores.muted))
            }
        }
    }

    ingresoAEliminar?.let { ingreso ->
        AlertDialog(
            onDismissRequest = { ingresoAEliminar = null },
            title = { Text("¿Eliminar este ingreso?") },
            text = { Text("${ingreso.nota ?: ingreso.tipo.etiqueta} · ${formatearDinero(ingreso.monto, e.moneda)}") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarIngreso(ingreso.id)
                    ingresoAEliminar = null
                }) { Text("Eliminar", color = Gastos.colores.accent) }
            },
            dismissButton = { TextButton(onClick = { ingresoAEliminar = null }) { Text("Cancelar") } },
        )
    }
}
