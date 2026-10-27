package com.example.controlgastos.data

import androidx.test.core.app.ApplicationProvider
import com.example.controlgastos.data.local.CategoriaEntity
import com.example.controlgastos.data.local.GastoEntity
import com.example.controlgastos.data.local.GastosDatabase
import com.example.controlgastos.data.local.IndicadorEntity
import com.example.controlgastos.data.local.IngresoEntity
import com.example.controlgastos.domain.model.TipoIngreso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

// Prueba Room con SQLite real (en el emulador), en una BD en memoria.
class DaosTest {

    private lateinit var db: GastosDatabase

    @Before fun abrir() {
        db = GastosDatabase.construirEnMemoria(ApplicationProvider.getApplicationContext())
    }

    @After fun cerrar() = db.close()

    private fun gasto(id: Long, fecha: LocalDate) =
        GastoEntity(id, fecha.toString().take(7), 1000.0 * id, null, fecha, "app", 1, "Comida", "utensils")

    @Test fun reemplazarUnMesNoTocaLosDemas() = runTest {
        val dao = db.gastoDao()
        dao.insertarTodos(listOf(gasto(1, LocalDate.of(2026, 9, 5)), gasto(2, LocalDate.of(2026, 10, 2)), gasto(3, LocalDate.of(2026, 10, 9))))

        // El servidor dice que en octubre solo queda el 3 (el 2 se borró desde el iPhone).
        dao.reemplazarMes("2026-10", listOf(gasto(3, LocalDate.of(2026, 10, 9))))

        assertEquals(listOf(3L), dao.observarMes("2026-10").first().map { it.id })
        assertEquals(listOf(1L), dao.observarMes("2026-09").first().map { it.id })
    }

    @Test fun recientesOrdenadosPorFechaYTraduceLasFechas() = runTest {
        val dao = db.gastoDao()
        dao.insertarTodos(listOf(gasto(1, LocalDate.of(2026, 9, 5)), gasto(2, LocalDate.of(2026, 10, 9)), gasto(3, LocalDate.of(2026, 10, 1))))

        val recientes = dao.observarRecientes(2).first()
        assertEquals(listOf(2L, 3L), recientes.map { it.id })
        assertEquals(LocalDate.of(2026, 10, 9), recientes.first().fecha)

        dao.borrar(2)
        assertNull(dao.observarPorId(2).first())
    }

    @Test fun categoriasEnElOrdenDeLaWeb() = runTest {
        val dao = db.categoriaDao()
        dao.reemplazarTodas(
            listOf(
                CategoriaEntity(5, "Otros", "receipt", orden = 9, archivada = false, usos = 0),
                CategoriaEntity(1, "Comida", "utensils", orden = 0, archivada = false, usos = 4),
            ),
        )
        assertEquals(listOf("Comida", "Otros"), dao.observarTodas().first().map { it.nombre })
    }

    @Test fun ingresosDeUnRangoDeFechas() = runTest {
        val dao = db.ingresoDao()
        dao.insertar(IngresoEntity(monto = 1.0, tipo = TipoIngreso.SUELDO, nota = null, fecha = LocalDate.of(2026, 10, 1)))
        dao.insertar(IngresoEntity(monto = 2.0, tipo = TipoIngreso.VENTA, nota = "Bici", fecha = LocalDate.of(2026, 9, 30)))

        val octubre = dao.observarEntre(LocalDate.of(2026, 10, 1).toEpochDay(), LocalDate.of(2026, 10, 31).toEpochDay()).first()
        assertEquals(listOf(TipoIngreso.SUELDO), octubre.map { it.tipo })
    }

    @Test fun reemplazarIndicadoresBorraLosAnteriores() = runTest {
        val dao = db.indicadorDao()
        dao.reemplazarTodas(listOf(indicador("euro", 1), indicador("dolar", 0)))
        assertEquals(listOf("dolar", "euro"), dao.observarTodas().first().map { it.codigo })

        dao.reemplazarTodas(listOf(indicador("uf", 0)))
        assertEquals(listOf("uf"), dao.observarTodas().first().map { it.codigo })
    }

    private fun indicador(codigo: String, orden: Int) =
        IndicadorEntity(codigo, codigo, 1.0, Instant.parse("2026-10-26T03:00:00Z"), orden)
}
