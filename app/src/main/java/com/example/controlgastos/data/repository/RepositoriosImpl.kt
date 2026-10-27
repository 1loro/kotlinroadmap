package com.example.controlgastos.data.repository

import com.example.controlgastos.data.local.IndicadorDao
import com.example.controlgastos.data.local.MovimientoDao
import com.example.controlgastos.data.local.aEntidad
import com.example.controlgastos.data.local.aModelo
import com.example.controlgastos.data.remote.IndicadoresApi
import com.example.controlgastos.data.remote.aEntidad
import com.example.controlgastos.domain.model.Indicador
import com.example.controlgastos.domain.model.Movimiento
import com.example.controlgastos.domain.repository.FalloIndicadores
import com.example.controlgastos.domain.repository.IndicadoresRepository
import com.example.controlgastos.domain.repository.MovimientosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.time.format.DateTimeParseException
import javax.inject.Inject

// Implementaciones de las interfaces del dominio con Room y Retrofit.
// `@Inject constructor`: Hilt sabe construirlas pidiendo sus dependencias.

class MovimientosRepositoryImpl @Inject constructor(
    private val dao: MovimientoDao,
) : MovimientosRepository {

    override val movimientos: Flow<List<Movimiento>> =
        dao.observarTodos().map { lista -> lista.map { it.aModelo() } }

    override fun movimiento(id: Long): Flow<Movimiento?> = dao.observarPorId(id).map { it?.aModelo() }

    override suspend fun agregar(movimiento: Movimiento): Long = dao.insertar(movimiento.aEntidad().copy(id = 0))

    override suspend fun eliminar(id: Long) = dao.eliminar(id)
}

/**
 * Estrategia "offline-first":
 * - La UI SIEMPRE lee de Room (`indicadores`), así funciona sin conexión.
 * - `actualizar()` descarga de la API y reemplaza la caché local. Como Room
 *   emite por el Flow, la UI se actualiza sola.
 */
class IndicadoresRepositoryImpl @Inject constructor(
    private val api: IndicadoresApi,
    private val dao: IndicadorDao,
) : IndicadoresRepository {

    override val indicadores: Flow<List<Indicador>> =
        dao.observarTodas().map { lista -> lista.map { it.aModelo() } }

    override suspend fun actualizar(): Result<Unit> {
        // No usamos runCatching: atraparía también CancellationException y
        // rompería la cancelación de corrutinas. Atrapamos solo lo esperable.
        return try {
            val dtos = api.obtenerIndicadores()
            // Una respuesta sin ningún indicador conocido significa que la API
            // cambió: no se borra la caché con una lista vacía.
            if (dtos.isEmpty()) return Result.failure(FalloIndicadores.DatosInvalidos())
            dao.reemplazarTodas(dtos.mapIndexed { i, dto -> dto.aEntidad(orden = i) })
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(FalloIndicadores.SinConexion())
        } catch (e: HttpException) {
            Result.failure(FalloIndicadores.Servidor(e.code()))
        } catch (e: SerializationException) {
            Result.failure(FalloIndicadores.DatosInvalidos())
        } catch (e: DateTimeParseException) {
            Result.failure(FalloIndicadores.DatosInvalidos())
        }
    }
}
