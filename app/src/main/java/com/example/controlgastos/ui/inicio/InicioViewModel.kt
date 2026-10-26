package com.example.controlgastos.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controlgastos.domain.repository.CotizacionesRepository
import com.example.controlgastos.domain.usecase.ObtenerPanelInicioUseCase
import com.example.controlgastos.domain.usecase.PanelInicio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface InicioUiState {
    data object Cargando : InicioUiState
    data class Listo(val panel: PanelInicio) : InicioUiState
}

/**
 * MVVM: el ViewModel prepara el estado de la pantalla y sobrevive a la
 * rotación (la Activity se recrea, el ViewModel no). La pantalla solo dibuja
 * `uiState` y le avisa de los eventos.
 */
@HiltViewModel
class InicioViewModel @Inject constructor(
    obtenerPanel: ObtenerPanelInicioUseCase,
    private val cotizaciones: CotizacionesRepository,
) : ViewModel() {

    // stateIn convierte el Flow en StateFlow (siempre tiene un valor actual).
    // WhileSubscribed(5_000): deja de escuchar la BD 5 s después de que nadie
    // mire la pantalla; los 5 s evitan reiniciar todo en una rotación.
    val uiState: StateFlow<InicioUiState> = obtenerPanel()
        .map<PanelInicio, InicioUiState> { InicioUiState.Listo(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState.Cargando)

    init {
        // Pide cotizaciones nuevas al abrir la app para mostrar el saldo en
        // dólares. Si falla no importa: se usa la caché, y la pestaña Dólar
        // es la que informa los errores.
        viewModelScope.launch { cotizaciones.actualizar() }
    }
}
