package com.example.controlgastos.domain

import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.Cotizacion
import com.example.controlgastos.domain.model.Movimiento
import com.example.controlgastos.domain.usecase.AgregarMovimientoUseCase
import com.example.controlgastos.domain.usecase.ErrorValidacion
import com.example.controlgastos.domain.usecase.NuevoMovimiento
import com.example.controlgastos.domain.usecase.ObtenerPanelInicioUseCase
import com.example.controlgastos.fakes.FakeCotizacionesRepository
import com.example.controlgastos.fakes.FakeMovimientosRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class AgregarMovimientoUseCaseTest {

    private val hoy = LocalDate.of(2026, 10, 26)
    private val repositorio = FakeMovimientosRepository()
    private val agregar = AgregarMovimientoUseCase(repositorio)

    @Test fun `formulario vacio tiene los tres errores`() {
        assertEquals(
            setOf(ErrorValidacion.DESCRIPCION_VACIA, ErrorValidacion.MONTO_INVALIDO, ErrorValidacion.SIN_CATEGORIA),
            agregar.validar(NuevoMovimiento("  ", "", null, hoy)),
        )
    }

    @Test fun `datos validos se guardan normalizados`() = runTest {
        val errores = agregar(NuevoMovimiento("  Cine  ", "1500,50", Categoria.OCIO, hoy))

        assertTrue(errores.isEmpty())
        val guardado = repositorio.guardados.single()
        assertEquals("Cine", guardado.descripcion)
        assertEquals(1500.5, guardado.monto, 0.0)
        assertEquals(Categoria.OCIO, guardado.categoria)
        assertEquals(hoy, guardado.fecha)
    }

    @Test fun `datos invalidos no se guardan`() = runTest {
        val errores = agregar(NuevoMovimiento("Cine", "0", Categoria.OCIO, hoy))

        assertEquals(setOf(ErrorValidacion.MONTO_INVALIDO), errores)
        assertTrue(repositorio.guardados.isEmpty())
    }
}

class ObtenerPanelInicioUseCaseTest {

    private val hoy = LocalDate.of(2026, 10, 26)
    private val movimientos = FakeMovimientosRepository(
        listOf(
            Movimiento(1, "Sueldo", 3000.0, Categoria.SUELDO, hoy.minusDays(2)),
            Movimiento(2, "Super", 500.0, Categoria.COMIDA, hoy.minusDays(1)),
            Movimiento(3, "Cine", 100.0, Categoria.OCIO, hoy),
        ),
    )

    private fun cotizacion(casa: String, venta: Double?) = Cotizacion(casa, casa, 1.0, venta, Instant.EPOCH)

    @Test fun `calcula resumen, ultimos y saldo en dolares oficial`() = runTest {
        val cotizaciones = FakeCotizacionesRepository(listOf(cotizacion("blue", 2000.0), cotizacion("oficial", 1200.0)))

        val panel = ObtenerPanelInicioUseCase(movimientos, cotizaciones)(cantidadUltimos = 2).first()

        assertEquals(2400.0, panel.resumen.saldo, 0.0)
        assertEquals(listOf("Cine", "Super"), panel.ultimos.map { it.descripcion })
        assertEquals(2.0, panel.saldoEnDolares!!, 0.0) // 2400 / 1200, no usa el blue
    }

    @Test fun `sin cotizacion oficial o con venta invalida no hay saldo en dolares`() = runTest {
        val sinOficial = FakeCotizacionesRepository(listOf(cotizacion("blue", 2000.0)))
        assertNull(ObtenerPanelInicioUseCase(movimientos, sinOficial)().first().saldoEnDolares)

        val ventaCero = FakeCotizacionesRepository(listOf(cotizacion("oficial", 0.0)))
        assertNull(ObtenerPanelInicioUseCase(movimientos, ventaCero)().first().saldoEnDolares)
    }
}
