package com.example.controlgastos.domain.repository

import com.example.controlgastos.domain.model.Indicador
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

interface IndicadoresRepository {
    /** Últimos indicadores guardados localmente (funciona sin conexión). */
    val indicadores: Flow<List<Indicador>>

    /** Descarga indicadores nuevos. En caso de error, la falla es un [FalloIndicadores]. */
    suspend fun actualizar(): Result<Unit>
}

/** Motivos por los que puede fallar la actualización, con un mensaje para el usuario. */
sealed class FalloIndicadores(val mensaje: String) : Exception(mensaje) {
    class SinConexion : FalloIndicadores("Sin conexión a internet")
    class Servidor(val codigo: Int) : FalloIndicadores("El servidor respondió con error ($codigo)")
    class DatosInvalidos : FalloIndicadores("La respuesta del servidor no es válida")
}
