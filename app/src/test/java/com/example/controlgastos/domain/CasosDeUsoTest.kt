package com.example.controlgastos.domain

import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.Indicador
import com.example.controlgastos.domain.model.Movimiento
import com.example.controlgastos.domain.usecase.AgregarMovimientoUseCase
import com.example.controlgastos.domain.usecase.ErrorValidacion
import com.example.controlgastos.domain.usecase.NuevoMovimiento
import com.example.controlgastos.domain.usecase.ObtenerPanelInicioUseCase
import com.example.controlgastos.fakes.FakeIndicadoresRepository
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
        val errores = agregar(NuevoMovimiento("  Cine  ", "9.800", Categoria.OCIO, hoy))

        assertTrue(errores.isEmpty())
        val guardado = repositorio.guardados.single()
        assertEquals("Cine", guardado.descripcion)
        assertEquals(9800.0, guardado.monto, 0.0)
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

    private fun indicador(codigo: String, valor: Double) = Indicador(codigo, codigo, valor, Instant.EPOCH)

    @Test fun `calcula resumen, ultimos y saldo en dolares al dolar observado`() = runTest {
        val indicadores = FakeIndicadoresRepository(listOf(indicador("euro", 2000.0), indicador("dolar", 1200.0)))

        val panel = ObtenerPanelInicioUseCase(movimientos, indicadores)(cantidadUltimos = 2).first()

        assertEquals(2400.0, panel.resumen.saldo, 0.0)
        assertEquals(listOf("Cine", "Super"), panel.ultimos.map { it.descripcion })
        assertEquals(2.0, panel.saldoEnDolares!!, 0.0) // 2400 / 1200, no usa el euro
    }

    @Test fun `sin dolar o con valor invalido no hay saldo en dolares`() = runTest {
        val sinDolar = FakeIndicadoresRepository(listOf(indicador("uf", 41000.0)))
        assertNull(ObtenerPanelInicioUseCase(movimientos, sinDolar)().first().saldoEnDolares)

        val valorCero = FakeIndicadoresRepository(listOf(indicador("dolar", 0.0)))
        assertNull(ObtenerPanelInicioUseCase(movimientos, valorCero)().first().saldoEnDolares)
    }
}
