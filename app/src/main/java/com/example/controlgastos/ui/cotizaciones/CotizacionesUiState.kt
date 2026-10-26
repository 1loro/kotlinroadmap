package com.example.controlgastos.ui.cotizaciones

import com.example.controlgastos.model.Cotizacion

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
sealed interface CotizacionesUiState {
    /** Todavía no hay nada que mostrar. */
    data object Cargando : CotizacionesUiState

    /** Falló y no hay datos guardados. */
    data class Error(val mensaje: String) : CotizacionesUiState

    /**
     * Hay datos (de la red o de la caché).
     * @param actualizando se está pidiendo una versión más nueva.
     * @param aviso la última actualización falló: los datos pueden estar viejos.
     */
    data class Exito(
        val cotizaciones: List<Cotizacion>,
        val actualizando: Boolean = false,
        val aviso: String? = null,
    ) : CotizacionesUiState
}

/**
 * Combina la caché local con el estado de la petición.
 * @param cache null mientras Room todavía no respondió.
 */
fun calcularEstado(cache: List<Cotizacion>?, refresco: Refresco): CotizacionesUiState = when {
    cache.isNullOrEmpty() -> when (refresco) {
        Refresco.EnCurso -> CotizacionesUiState.Cargando
        is Refresco.Fallido -> CotizacionesUiState.Error(refresco.mensaje)
        // Terminó bien pero Room aún no emitió (o la API devolvió una lista vacía).
        Refresco.Completado -> if (cache == null) CotizacionesUiState.Cargando else CotizacionesUiState.Exito(emptyList())
    }
    else -> CotizacionesUiState.Exito(
        cotizaciones = cache,
        actualizando = refresco == Refresco.EnCurso,
        aviso = (refresco as? Refresco.Fallido)?.mensaje,
    )
}
