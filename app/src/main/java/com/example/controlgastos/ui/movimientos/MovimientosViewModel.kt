package com.example.controlgastos.ui.movimientos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controlgastos.domain.hoyEn
import com.example.controlgastos.domain.model.Gasto
import com.example.controlgastos.domain.model.Ingreso
import com.example.controlgastos.domain.model.TotalCategoria
import com.example.controlgastos.domain.repository.ErrorApp
import com.example.controlgastos.domain.repository.EstadoSesion
import com.example.controlgastos.domain.repository.GastosRepository
import com.example.controlgastos.domain.repository.IngresosRepository
import com.example.controlgastos.domain.repository.SesionRepository
import com.example.controlgastos.domain.totalesPorCategoria
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class MovimientosUiState(
    val mes: YearMonth,
    val mesActual: YearMonth,
    val hoy: LocalDate,
    val moneda: String,
    val gastos: List<Gasto> = emptyList(),
    val porCategoria: List<TotalCategoria> = emptyList(),
    val ingresos: List<Ingreso> = emptyList(),
    val actualizando: Boolean = false,
    val aviso: String? = null,
) {
    val total: Double get() = gastos.sumOf { it.monto }
}

@HiltViewModel
class MovimientosViewModel @Inject constructor(
    sesion: SesionRepository,
    private val gastos: GastosRepository,
    private val ingresos: IngresosRepository,
    reloj: Clock,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val usuario = (sesion.estado.value as? EstadoSesion.Activa)?.usuario
    private val hoy = hoyEn(usuario?.zonaHoraria ?: "America/Santiago", reloj)
    private val mesActual = YearMonth.from(hoy)

    // Argumento de la ruta Movimientos(mes = "AAAA-MM"). El mes elegido se guarda
    // en el SavedStateHandle: sobrevive a rotaciones y a la muerte del proceso.
    private val mes: StateFlow<YearMonth> = savedStateHandle.getStateFlow(CLAVE_MES, savedStateHandle.get<String>(CLAVE_MES).orEmpty())
        .map { texto -> runCatching { YearMonth.parse(texto) }.getOrNull()?.takeIf { it <= mesActual } ?: mesActual }
        .stateIn(viewModelScope, SharingStarted.Eagerly, mesActual)

    private val actualizando = MutableStateFlow(false)
    private val aviso = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MovimientosUiState> = mes.flatMapLatest { m ->
        combine(gastos.gastosDelMes(m), ingresos.ingresosDelMes(m), actualizando, aviso) { lista, ingresosMes, cargando, aviso ->
            MovimientosUiState(
                mes = m,
                mesActual = mesActual,
                hoy = hoy,
                moneda = usuario?.moneda ?: "CLP",
                gastos = lista,
                porCategoria = totalesPorCategoria(lista),
                ingresos = ingresosMes,
                actualizando = cargando,
                aviso = aviso,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovimientosUiState(mesActual, mesActual, hoy, usuario?.moneda ?: "CLP"))

    init {
        viewModelScope.launch { mes.collect { actualizar(it) } }
    }

    fun cambiarMes(nuevo: YearMonth) {
        savedStateHandle[CLAVE_MES] = nuevo.toString()
    }

    fun actualizar(m: YearMonth = mes.value) {
        actualizando.value = true
        viewModelScope.launch {
            val error = gastos.refrescarMes(m).exceptionOrNull() as? ErrorApp
            aviso.value = error?.takeUnless { it.sesionExpirada }?.let { "${it.mensaje}. Mostrando datos guardados." }
            actualizando.value = false
        }
    }

    fun eliminarIngreso(id: Long) {
        viewModelScope.launch { ingresos.eliminar(id) }
    }

    private companion object {
        // Mismo nombre que la propiedad de la ruta Movimientos(mes).
        const val CLAVE_MES = "mes"
    }
}
