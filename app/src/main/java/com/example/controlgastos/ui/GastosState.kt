package com.example.controlgastos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.example.controlgastos.data.DatosDeEjemplo
import com.example.controlgastos.model.Movimiento

/**
 * Fase 2: "state holder" simple que vive en la composición.
 *
 * `mutableStateListOf` es una lista observable: cuando cambia, Compose vuelve
 * a dibujar (recomposición) solo los composables que la leen.
 *
 * Limitación intencional: con `remember` el estado se pierde al rotar la
 * pantalla, porque la Activity se recrea (ver docs/FASE-1). En la Fase 4 esto
 * pasa a un ViewModel, que sobrevive a esos cambios.
 */
class GastosState(iniciales: List<Movimiento>) {
    private val _movimientos = mutableStateListOf<Movimiento>().apply { addAll(iniciales) }

    // Más recientes primero.
    val movimientos: List<Movimiento>
        get() = _movimientos.sortedWith(compareByDescending<Movimiento> { it.fecha }.thenByDescending { it.id })

    fun buscar(id: Long): Movimiento? = _movimientos.find { it.id == id }

    fun agregar(nuevo: Movimiento) {
        val siguienteId = (_movimientos.maxOfOrNull { it.id } ?: 0) + 1
        _movimientos.add(nuevo.copy(id = siguienteId))
    }

    fun eliminar(id: Long) {
        _movimientos.removeAll { it.id == id }
    }
}

@Composable
fun rememberGastosState(): GastosState = remember { GastosState(DatosDeEjemplo.movimientos()) }
