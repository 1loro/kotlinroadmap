package com.example.controlgastos.domain.repository

import com.example.controlgastos.domain.model.Cotizacion
import com.example.controlgastos.domain.model.Movimiento
import kotlinx.coroutines.flow.Flow

// La capa domain define QUÉ datos necesita la app, con interfaces. La capa
// data decide CÓMO obtenerlos (Room, Retrofit). Así el dominio y los
// ViewModels no dependen de ninguna librería de Android y se testean con
// implementaciones falsas.

interface MovimientosRepository {
    /** Todos los movimientos, del más reciente al más antiguo. Se actualiza solo. */
    val movimientos: Flow<List<Movimiento>>

    /** Un movimiento por id; emite null si no existe (o se elimina). */
    fun movimiento(id: Long): Flow<Movimiento?>

    /** Guarda un movimiento nuevo (se ignora su id) y devuelve el id asignado. */
    suspend fun agregar(movimiento: Movimiento): Long

    suspend fun eliminar(id: Long)
}

interface CotizacionesRepository {
    /** Últimas cotizaciones guardadas localmente (funciona sin conexión). */
    val cotizaciones: Flow<List<Cotizacion>>

    /** Descarga cotizaciones nuevas. En caso de error, la falla es un [FalloCotizaciones]. */
    suspend fun actualizar(): Result<Unit>
}

/** Motivos por los que puede fallar la actualización, con un mensaje para el usuario. */
sealed class FalloCotizaciones(val mensaje: String) : Exception(mensaje) {
    class SinConexion : FalloCotizaciones("Sin conexión a internet")
    class Servidor(val codigo: Int) : FalloCotizaciones("El servidor respondió con error ($codigo)")
    class DatosInvalidos : FalloCotizaciones("La respuesta del servidor no es válida")
}
