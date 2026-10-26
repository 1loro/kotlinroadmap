package com.example.controlgastos.data

import com.example.controlgastos.data.local.CotizacionDao
import com.example.controlgastos.data.local.MovimientoDao
import com.example.controlgastos.data.local.aEntidad
import com.example.controlgastos.data.local.aModelo
import com.example.controlgastos.data.remote.DolarApi
import com.example.controlgastos.data.remote.aEntidad
import com.example.controlgastos.model.Cotizacion
import com.example.controlgastos.model.Movimiento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.time.format.DateTimeParseException

// Un repositorio es la ÚNICA puerta de entrada a los datos para la UI.
// La UI no sabe si los datos vienen de Room, de la red o de ambos.

class MovimientosRepository(private val dao: MovimientoDao) {

    val movimientos: Flow<List<Movimiento>> =
        dao.observarTodos().map { lista -> lista.map { it.aModelo() } }

    fun movimiento(id: Long): Flow<Movimiento?> = dao.observarPorId(id).map { it?.aModelo() }

    suspend fun agregar(movimiento: Movimiento): Long = dao.insertar(movimiento.aEntidad().copy(id = 0))

    suspend fun eliminar(id: Long) = dao.eliminar(id)
}

/** Motivos por los que puede fallar la actualización, con un mensaje para el usuario. */
sealed class FalloCotizaciones(val mensaje: String) : Exception(mensaje) {
    class SinConexion : FalloCotizaciones("Sin conexión a internet")
    class Servidor(val codigo: Int) : FalloCotizaciones("El servidor respondió con error ($codigo)")
    class DatosInvalidos : FalloCotizaciones("La respuesta del servidor no es válida")
}

/**
 * Estrategia "offline-first":
 * - La UI SIEMPRE lee de Room (`cotizaciones`), así funciona sin conexión.
 * - `actualizar()` descarga de la API y reemplaza la caché local. Como Room
 *   emite por el Flow, la UI se actualiza sola.
 */
class CotizacionesRepository(
    private val api: DolarApi,
    private val dao: CotizacionDao,
) {
    val cotizaciones: Flow<List<Cotizacion>> =
        dao.observarTodas().map { lista -> lista.map { it.aModelo() } }

    suspend fun actualizar(): Result<Unit> {
        // No usamos runCatching: atraparía también CancellationException y
        // rompería la cancelación de corrutinas. Atrapamos solo lo esperable.
        return try {
            val entidades = api.obtenerDolares().mapIndexed { i, dto -> dto.aEntidad(orden = i) }
            dao.reemplazarTodas(entidades)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(FalloCotizaciones.SinConexion())
        } catch (e: HttpException) {
            Result.failure(FalloCotizaciones.Servidor(e.code()))
        } catch (e: SerializationException) {
            Result.failure(FalloCotizaciones.DatosInvalidos())
        } catch (e: DateTimeParseException) {
            Result.failure(FalloCotizaciones.DatosInvalidos())
        }
    }
}
