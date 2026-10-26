package com.example.controlgastos.ui.detalle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controlgastos.domain.model.Movimiento
import com.example.controlgastos.domain.repository.MovimientosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetalleUiState {
    data object Cargando : DetalleUiState
    data object NoExiste : DetalleUiState
    data class Listo(val movimiento: Movimiento) : DetalleUiState
}

@HiltViewModel
class DetalleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repositorio: MovimientosRepository,
) : ViewModel() {

    // Navigation guarda los argumentos de la ruta en el SavedStateHandle con el
    // nombre de cada propiedad: Detalle(val id: Long) -> clave "id".
    private val id: Long = checkNotNull(savedStateHandle["id"]) { "Falta el argumento id" }

    // Mientras se borra, se ignora el "null" que emite Room: si no, la pantalla
    // mostraría "no existe" un instante antes de cerrarse.
    @Volatile private var eliminando = false

    val uiState: StateFlow<DetalleUiState> = repositorio.movimiento(id)
        .filter { it != null || !eliminando }
        .map { it?.let(DetalleUiState::Listo) ?: DetalleUiState.NoExiste }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState.Cargando)

    // Evento de una sola vez modelado como ESTADO: la UI lo observa y navega.
    private val _eliminado = MutableStateFlow(false)
    val eliminado: StateFlow<Boolean> = _eliminado.asStateFlow()

    fun eliminar() {
        if (eliminando) return
        eliminando = true
        viewModelScope.launch {
            repositorio.eliminar(id)
            _eliminado.value = true
        }
    }
}
