package com.example.controlgastos.ui.comparar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controlgastos.domain.FilaComparacion
import com.example.controlgastos.domain.compararMeses
import com.example.controlgastos.domain.hoyEn
import com.example.controlgastos.domain.model.TotalMes
import com.example.controlgastos.domain.repository.ErrorApp
import com.example.controlgastos.domain.repository.EstadoSesion
import com.example.controlgastos.domain.repository.GastosRepository
import com.example.controlgastos.domain.repository.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

data class CompararUiState(
    val a: YearMonth,
    val b: YearMonth,
    val mesActual: YearMonth,
    val moneda: String,
    val totalA: Double = 0.0,
    val totalB: Double = 0.0,
    val filas: List<FilaComparacion> = emptyList(),
    val tendencia: List<TotalMes> = emptyList(),
    val actualizando: Boolean = false,
    val aviso: String? = null,
)

@HiltViewModel
class CompararViewModel @Inject constructor(
    sesion: SesionRepository,
    private val gastos: GastosRepository,
    reloj: Clock,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val usuario = (sesion.estado.value as? EstadoSesion.Activa)?.usuario
    private val mesActual = YearMonth.from(hoyEn(usuario?.zonaHoraria ?: "America/Santiago", reloj))

    // Por defecto, como la web: B = este mes, A = el anterior. Se guardan en el
    // SavedStateHandle para no perderlos al rotar.
    private val meses = MutableStateFlow(
        (savedStateHandle.get<String>(CLAVE_A)?.let(YearMonth::parse) ?: mesActual.minusMonths(1)) to
            (savedStateHandle.get<String>(CLAVE_B)?.let(YearMonth::parse) ?: mesActual),
    )
    private val actualizando = MutableStateFlow(false)
    private val aviso = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CompararUiState> = meses.flatMapLatest { (a, b) ->
        combine(
            gastos.gastosDelMes(a),
            gastos.gastosDelMes(b),
            gastos.totales(mesActual, 12),
            actualizando,
            aviso,
        ) { gastosA, gastosB, tendencia, cargando, aviso ->
            CompararUiState(
                a = a,
                b = b,
                mesActual = mesActual,
                moneda = usuario?.moneda ?: "CLP",
                totalA = gastosA.sumOf { it.monto },
                totalB = gastosB.sumOf { it.monto },
                filas = compararMeses(gastosA, gastosB),
                tendencia = tendencia,
                actualizando = cargando,
                aviso = aviso,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CompararUiState(meses.value.first, meses.value.second, mesActual, usuario?.moneda ?: "CLP"),
    )

    init {
        actualizar()
    }

    fun cambiarA(mes: YearMonth) = cambiar(mes, meses.value.second)
    fun cambiarB(mes: YearMonth) = cambiar(meses.value.first, mes)

    private fun cambiar(a: YearMonth, b: YearMonth) {
        savedStateHandle[CLAVE_A] = a.toString()
        savedStateHandle[CLAVE_B] = b.toString()
        meses.value = a to b
        actualizar()
    }

    fun actualizar() {
        val (a, b) = meses.value
        actualizando.value = true
        viewModelScope.launch {
            val resultados = listOf(
                async { gastos.refrescarMes(a) },
                async { gastos.refrescarMes(b) },
                async { gastos.refrescarTotales(mesActual, 12) },
            ).awaitAll()
            val error = resultados.firstNotNullOfOrNull { it.exceptionOrNull() as? ErrorApp }
            aviso.value = error?.takeUnless { it.sesionExpirada }?.let { "${it.mensaje}. Mostrando datos guardados." }
            actualizando.value = false
        }
    }

    private companion object {
        const val CLAVE_A = "a"
        const val CLAVE_B = "b"
    }
}
