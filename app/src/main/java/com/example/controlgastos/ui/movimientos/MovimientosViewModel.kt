package com.example.controlgastos.ui.movimientos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controlgastos.domain.model.Movimiento
import com.example.controlgastos.domain.model.TipoMovimiento
import com.example.controlgastos.domain.repository.MovimientosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class Filtro(val etiqueta: String, val tipo: TipoMovimiento?) {
    TODOS("Todos", null),
    INGRESOS("Ingresos", TipoMovimiento.INGRESO),
    GASTOS("Gastos", TipoMovimiento.GASTO),
}

data class MovimientosUiState(
    val filtro: Filtro = Filtro.TODOS,
    val movimientos: List<Movimiento> = emptyList(),
    val cargando: Boolean = false,
)

@HiltViewModel
class MovimientosViewModel @Inject constructor(
    repositorio: MovimientosRepository,
    // SavedStateHandle: almacenamiento que sobrevive incluso si Android mata
    // el proceso en segundo plano (el ViewModel solo sobrevive a la rotación).
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val filtro = savedStateHandle.getStateFlow(CLAVE_FILTRO, Filtro.TODOS)

    val uiState: StateFlow<MovimientosUiState> =
        combine(repositorio.movimientos, filtro) { lista, filtro ->
            MovimientosUiState(
                filtro = filtro,
                movimientos = lista.filter { filtro.tipo == null || it.tipo == filtro.tipo },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovimientosUiState(cargando = true))

    fun elegirFiltro(nuevo: Filtro) {
        savedStateHandle[CLAVE_FILTRO] = nuevo
    }

    private companion object {
        const val CLAVE_FILTRO = "filtro"
    }
}
