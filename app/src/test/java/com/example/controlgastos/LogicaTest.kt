package com.example.controlgastos

import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.Indicador
import com.example.controlgastos.domain.model.Movimiento
import com.example.controlgastos.domain.model.resumen
import com.example.controlgastos.ui.indicadores.IndicadoresUiState
import com.example.controlgastos.ui.indicadores.Refresco
import com.example.controlgastos.ui.indicadores.calcularEstado
import com.example.controlgastos.domain.usecase.parsearMonto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class LogicaTest {

    private val hoy = LocalDate.of(2026, 10, 26)

    @Test fun `resumen calcula saldo y ordena categorias por gasto`() {
        val r = listOf(
            Movimiento(1, "Sueldo", 1000.0, Categoria.SUELDO, hoy),
            Movimiento(2, "Super", 100.0, Categoria.COMIDA, hoy),
            Movimiento(3, "Arriendo", 400.0, Categoria.VIVIENDA, hoy),
            Movimiento(4, "Cena", 50.0, Categoria.COMIDA, hoy),
        ).resumen()

        assertEquals(1000.0, r.ingresos, 0.0)
        assertEquals(550.0, r.gastos, 0.0)
        assertEquals(450.0, r.saldo, 0.0)
        assertEquals(listOf(Categoria.VIVIENDA to 400.0, Categoria.COMIDA to 150.0), r.gastoPorCategoria)
    }

    @Test fun `resumen de lista vacia es cero`() {
        val r = emptyList<Movimiento>().resumen()
        assertEquals(0.0, r.saldo, 0.0)
        assertEquals(emptyList<Pair<Categoria, Double>>(), r.gastoPorCategoria)
    }

    @Test fun `parsearMonto lee pesos chilenos con punto de miles`() {
        assertEquals(15_000.0, parsearMonto("15.000")!!, 0.0)
        assertEquals(1_250_000.0, parsearMonto(" 1.250.000 ")!!, 0.0)
        assertEquals(1500.0, parsearMonto("1500")!!, 0.0)
        assertNull(parsearMonto("1500,5")) // el peso chileno no tiene decimales
        assertNull(parsearMonto(""))
        assertNull(parsearMonto("."))
        assertNull(parsearMonto("0"))
        assertNull(parsearMonto("abc"))
    }

    // --- Estados de la pantalla de indicadores (carga / éxito / error) ---

    private val dolar = Indicador("dolar", "Dólar observado", 990.35, Instant.EPOCH)

    @Test fun `sin cache y pidiendo muestra cargando`() {
        assertEquals(IndicadoresUiState.Cargando, calcularEstado(null, Refresco.EnCurso))
        assertEquals(IndicadoresUiState.Cargando, calcularEstado(emptyList(), Refresco.EnCurso))
    }

    @Test fun `sin cache y con fallo muestra error`() {
        assertEquals(
            IndicadoresUiState.Error("Sin conexión"),
            calcularEstado(emptyList(), Refresco.Fallido("Sin conexión")),
        )
    }

    @Test fun `con cache muestra datos aunque falle, con aviso`() {
        assertEquals(
            IndicadoresUiState.Exito(listOf(dolar), actualizando = false, aviso = "Sin conexión"),
            calcularEstado(listOf(dolar), Refresco.Fallido("Sin conexión")),
        )
    }

    @Test fun `con cache y pidiendo muestra datos e indicador de actualizacion`() {
        assertEquals(
            IndicadoresUiState.Exito(listOf(dolar), actualizando = true),
            calcularEstado(listOf(dolar), Refresco.EnCurso),
        )
    }
}
