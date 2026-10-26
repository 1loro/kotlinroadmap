package com.example.controlgastos.data

import androidx.test.core.app.ApplicationProvider
import com.example.controlgastos.data.local.CotizacionEntity
import com.example.controlgastos.data.local.GastosDatabase
import com.example.controlgastos.data.local.MovimientoEntity
import com.example.controlgastos.model.Categoria
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
        db = GastosDatabase.construir(ApplicationProvider.getApplicationContext(), enMemoria = true)
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

    @Test fun reemplazarCotizacionesBorraLasAnteriores() = runTest {
        val dao = db.cotizacionDao()
        dao.reemplazarTodas(listOf(cotizacion("blue", 1), cotizacion("oficial", 0)))
        assertEquals(listOf("oficial", "blue"), dao.observarTodas().first().map { it.casa })

        dao.reemplazarTodas(listOf(cotizacion("mep", 0)))
        assertEquals(listOf("mep"), dao.observarTodas().first().map { it.casa })
    }

    private fun cotizacion(casa: String, orden: Int) =
        CotizacionEntity(casa, casa, 1.0, 2.0, Instant.parse("2026-10-26T16:00:00Z"), orden)
}
