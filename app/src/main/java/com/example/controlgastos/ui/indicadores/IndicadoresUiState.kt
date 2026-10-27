package com.example.controlgastos.ui.indicadores

import com.example.controlgastos.domain.model.Indicador

/** Estado de la última petición a la API. */
sealed interface Refresco {
    data object EnCurso : Refresco
    data object Completado : Refresco
    data class Fallido(val mensaje: String) : Refresco
}

/**
 * Lo que la pantalla debe mostrar. Una `sealed interface` obliga a que el
 * `when` de la UI contemple TODOS los casos: el compilador avisa si falta uno.
 */
sealed interface IndicadoresUiState {
    /** Todavía no hay nada que mostrar. */
    data object Cargando : IndicadoresUiState

    /** Falló y no hay datos guardados. */
    data class Error(val mensaje: String) : IndicadoresUiState

    /**
     * Hay datos (de la red o de la caché).
     * @param actualizando se está pidiendo una versión más nueva.
     * @param aviso la última actualización falló: los datos pueden estar viejos.
     */
    data class Exito(
        val indicadores: List<Indicador>,
        val actualizando: Boolean = false,
        val aviso: String? = null,
    ) : IndicadoresUiState
}

/**
 * Combina la caché local con el estado de la petición.
 * @param cache null mientras Room todavía no respondió.
 */
fun calcularEstado(cache: List<Indicador>?, refresco: Refresco): IndicadoresUiState = when {
    cache.isNullOrEmpty() -> when (refresco) {
        Refresco.EnCurso -> IndicadoresUiState.Cargando
        is Refresco.Fallido -> IndicadoresUiState.Error(refresco.mensaje)
        // Terminó bien pero Room aún no emitió (o la API devolvió una lista vacía).
        Refresco.Completado -> if (cache == null) IndicadoresUiState.Cargando else IndicadoresUiState.Exito(emptyList())
    }
    else -> IndicadoresUiState.Exito(
        indicadores = cache,
        actualizando = refresco == Refresco.EnCurso,
        aviso = (refresco as? Refresco.Fallido)?.mensaje,
    )
}
