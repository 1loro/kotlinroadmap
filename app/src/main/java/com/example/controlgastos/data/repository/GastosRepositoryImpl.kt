package com.example.controlgastos.data.repository

import com.example.controlgastos.data.local.CategoriaDao
import com.example.controlgastos.data.local.GastoDao
import com.example.controlgastos.data.local.TotalMesDao
import com.example.controlgastos.data.local.TotalMesEntity
import com.example.controlgastos.data.local.aEntidad
import com.example.controlgastos.data.local.aModelo
import com.example.controlgastos.data.remote.CategoriaBody
import com.example.controlgastos.data.remote.GastoBody
import com.example.controlgastos.data.remote.GastosApi
import com.example.controlgastos.data.remote.aModelo
import com.example.controlgastos.data.remote.llamarApi
import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.Gasto
import com.example.controlgastos.domain.model.TotalMes
import com.example.controlgastos.domain.repository.CategoriasRepository
import com.example.controlgastos.domain.repository.GastosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * "Caché primero": la UI siempre lee de Room (funciona sin conexión) y las
 * escrituras van primero al servidor; solo si el servidor acepta se
 * actualiza la copia local, así nunca muestra algo que no quedó guardado.
 */
class GastosRepositoryImpl @Inject constructor(
    private val api: GastosApi,
    private val dao: GastoDao,
    private val totalDao: TotalMesDao,
    private val reloj: Clock,
) : GastosRepository {

    override fun gastosDelMes(mes: YearMonth): Flow<List<Gasto>> =
        dao.observarMes(mes.toString()).map { lista -> lista.map { it.aModelo() } }

    override fun recientes(cantidad: Int): Flow<List<Gasto>> =
        dao.observarRecientes(cantidad).map { lista -> lista.map { it.aModelo() } }

    override fun gasto(id: Long): Flow<Gasto?> = dao.observarPorId(id).map { it?.aModelo() }

    override fun totales(hasta: YearMonth, meses: Int): Flow<List<TotalMes>> {
        val desde = hasta.minusMonths((meses - 1).toLong())
        // Se completan los meses sin datos con 0 (como getMonthlyTotals en la web).
        return totalDao.observarEntre(desde.toString(), hasta.toString()).map { filas ->
            val porMes = filas.associate { it.mes to it.total }
            (0 until meses).map { i ->
                val mes = desde.plusMonths(i.toLong())
                TotalMes(mes, porMes[mes.toString()] ?: 0.0)
            }
        }
    }

    override suspend fun refrescarMes(mes: YearMonth) = llamarApi {
        val respuesta = api.gastos(mes.toString())
        dao.reemplazarMes(mes.toString(), respuesta.expenses.map { it.aModelo().aEntidad() })
    }

    override suspend fun refrescarTotales(hasta: YearMonth, meses: Int) = llamarApi {
        val respuesta = api.totales(hasta.toString(), meses)
        totalDao.guardarTodos(respuesta.months.map { TotalMesEntity(it.month, it.total) })
    }

    override suspend fun crear(monto: Double, categoriaId: Long, fecha: LocalDate, nota: String?) = llamarApi {
        api.crearGasto(GastoBody(monto, categoriaId, fecha.toString(), nota)).aModelo()
    }.onSuccess { guardarYRefrescarTotal(it) }

    override suspend fun editar(id: Long, monto: Double, categoriaId: Long, fecha: LocalDate, nota: String?) = llamarApi {
        api.editarGasto(id, GastoBody(monto, categoriaId, fecha.toString(), nota)).aModelo()
    }.onSuccess { guardarYRefrescarTotal(it) }

    override suspend fun eliminar(id: Long) = llamarApi {
        api.eliminarGasto(id)
        dao.borrar(id)
    }

    private suspend fun guardarYRefrescarTotal(gasto: Gasto) {
        dao.guardar(gasto.aEntidad())
        // Los totales por mes se recalculan en el servidor; si falla, se
        // corrigen en el próximo refresco.
        refrescarTotales(YearMonth.now(reloj), 12)
    }
}

class CategoriasRepositoryImpl @Inject constructor(
    private val api: GastosApi,
    private val dao: CategoriaDao,
) : CategoriasRepository {

    override val categorias: Flow<List<Categoria>> =
        dao.observarTodas().map { lista -> lista.map { it.aModelo() } }

    override suspend fun refrescar() = llamarApi {
        dao.reemplazarTodas(api.categorias().map { it.aModelo().aEntidad() })
    }

    override suspend fun crear(nombre: String, icono: String) = guardarConRespuesta {
        api.crearCategoria(CategoriaBody(name = nombre.trim(), icon = icono)).aModelo()
    }

    override suspend fun editar(id: Long, nombre: String, icono: String) = guardarConRespuesta {
        api.editarCategoria(id, CategoriaBody(name = nombre.trim(), icon = icono)).aModelo()
    }

    override suspend fun archivar(id: Long, archivada: Boolean) = guardarConRespuesta {
        api.editarCategoria(id, CategoriaBody(archived = archivada)).aModelo()
    }

    override suspend fun eliminar(id: Long) = llamarApi {
        val respuesta = api.eliminarCategoria(id)
        // Con gastos se archiva en el servidor: se vuelve a leer la lista completa.
        if (respuesta.archived) refrescar() else dao.borrar(id)
        respuesta.archived
    }

    private suspend fun guardarConRespuesta(bloque: suspend () -> Categoria) = llamarApi {
        val categoria = bloque()
        // PATCH no devuelve `usos`: se conserva el valor conocido.
        refrescar().getOrNull() ?: dao.guardar(categoria.aEntidad())
        categoria
    }
}
