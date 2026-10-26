package com.example.controlgastos.fakes

import com.example.controlgastos.domain.model.Cotizacion
import com.example.controlgastos.domain.model.Movimiento
import com.example.controlgastos.domain.repository.CotizacionesRepository
import com.example.controlgastos.domain.repository.MovimientosRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// Implementaciones FALSAS de las interfaces del dominio, en memoria. Gracias
// a que los ViewModels dependen de interfaces (no de Room ni Retrofit), los
// tests corren en la JVM, en milisegundos, sin emulador.

class FakeMovimientosRepository(iniciales: List<Movimiento> = emptyList()) : MovimientosRepository {
    private val tabla = MutableStateFlow(iniciales)
    val guardados: List<Movimiento> get() = tabla.value

    override val movimientos: Flow<List<Movimiento>> =
        tabla.map { lista -> lista.sortedWith(compareByDescending<Movimiento> { it.fecha }.thenByDescending { it.id }) }

    override fun movimiento(id: Long): Flow<Movimiento?> = tabla.map { lista -> lista.find { it.id == id } }

    override suspend fun agregar(movimiento: Movimiento): Long {
        val id = (tabla.value.maxOfOrNull { it.id } ?: 0) + 1
        tabla.update { it + movimiento.copy(id = id) }
        return id
    }

    override suspend fun eliminar(id: Long) {
        tabla.update { lista -> lista.filterNot { it.id == id } }
    }
}

class FakeCotizacionesRepository(
    iniciales: List<Cotizacion> = emptyList(),
    /** Lo que devolverá la próxima llamada a actualizar(). */
    var resultado: Result<Unit> = Result.success(Unit),
    /** Cotizaciones que "llegan de la red" si la actualización tiene éxito. */
    var desdeLaRed: List<Cotizacion> = iniciales,
) : CotizacionesRepository {
    /** Si no es null, actualizar() espera a que se complete: simula una red lenta. */
    var pausa: CompletableDeferred<Unit>? = null

    private val cache = MutableStateFlow(iniciales)
    var llamadasActualizar = 0
        private set

    override val cotizaciones: Flow<List<Cotizacion>> = cache

    override suspend fun actualizar(): Result<Unit> {
        llamadasActualizar++
        pausa?.await()
        if (resultado.isSuccess) cache.value = desdeLaRed
        return resultado
    }
}
