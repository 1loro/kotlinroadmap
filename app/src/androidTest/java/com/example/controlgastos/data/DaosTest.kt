package com.example.controlgastos.data

import androidx.test.core.app.ApplicationProvider
import com.example.controlgastos.data.local.IndicadorEntity
import com.example.controlgastos.data.local.GastosDatabase
import com.example.controlgastos.data.local.MovimientoEntity
import com.example.controlgastos.domain.model.Categoria
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

    @Test fun laBdNuevaTraeDatosDeEjemplo() = runTest {
        val todos = db.movimientoDao().observarTodos().first()
        assertEquals(9, todos.size)
    }

    @Test fun insertarOrdenarPorFechaYEliminar() = runTest {
        val dao = db.movimientoDao()
        val futuro = LocalDate.now().plusDays(10)
        val id = dao.insertar(MovimientoEntity(descripcion = "Futuro", monto = 1.0, categoria = Categoria.OCIO, fecha = futuro))

        // Lo más reciente primero, y los tipos convertidos ida y vuelta.
        val primero = dao.observarTodos().first().first()
        assertEquals(id, primero.id)
        assertEquals(futuro, primero.fecha)
        assertEquals(Categoria.OCIO, primero.categoria)

        dao.eliminar(id)
        assertNull(dao.observarPorId(id).first())
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
