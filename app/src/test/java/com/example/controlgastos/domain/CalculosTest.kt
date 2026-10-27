package com.example.controlgastos.domain

import com.example.controlgastos.domain.model.Gasto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

private fun gasto(id: Long, monto: Double, fecha: LocalDate, categoria: Long = 1, nombre: String = "Comida") =
    Gasto(id, monto, null, fecha, "app", categoria, nombre, "utensils")

class CalculosTest {

    private val hoy = LocalDate.of(2026, 10, 10)

    @Test fun `cambio porcentual como percentChange de la web`() {
        assertEquals(50.0, cambioPorcentual(150.0, 100.0)!!, 0.001)
        assertEquals(-25.0, cambioPorcentual(75.0, 100.0)!!, 0.001)
        assertEquals(0.0, cambioPorcentual(0.0, 0.0)!!, 0.0)
        assertNull("sin base no hay porcentaje", cambioPorcentual(10.0, 0.0))
    }

    @Test fun `totales por categoria de mayor a menor`() {
        val totales = totalesPorCategoria(
            listOf(
                gasto(1, 100.0, hoy, 1, "Comida"),
                gasto(2, 500.0, hoy, 2, "Hogar"),
                gasto(3, 50.0, hoy, 1, "Comida"),
            ),
        )
        assertEquals(listOf("Hogar", "Comida"), totales.map { it.nombre })
        assertEquals(150.0, totales[1].total, 0.0)
        assertEquals(2, totales[1].cantidad)
    }

    @Test fun `resumen compara con el mismo tramo del mes anterior`() {
        val r = resumenMes(
            hoy,
            gastosMes = listOf(gasto(1, 3000.0, hoy.withDayOfMonth(2)), gasto(2, 7000.0, hoy)),
            // Solo cuenta lo del 1 al 10 de septiembre: el gasto del 25 queda fuera.
            gastosMesAnterior = listOf(gasto(3, 8000.0, LocalDate.of(2026, 9, 5)), gasto(4, 99999.0, LocalDate.of(2026, 9, 25))),
        )
        assertEquals(10000.0, r.total, 0.0)
        assertEquals(2, r.cantidad)
        assertEquals(1000.0, r.promedioDiario, 0.0) // 10.000 / 10 días
        assertEquals(31000.0, r.proyeccion, 0.0) // octubre tiene 31 días
        assertEquals(25.0, r.cambio!!, 0.001) // 10.000 vs 8.000
    }

    @Test fun `frase del resumen con la misma redaccion que la web`() {
        val fmt: (Double) -> String = { "$" + it.toLong() }
        val sinGastos = resumenMes(hoy, emptyList(), emptyList())
        assertEquals("Aún no registras gastos en octubre.", fraseResumen(sinGastos, hoy, fmt))

        val conCambio = resumenMes(hoy, listOf(gasto(1, 10000.0, hoy)), listOf(gasto(2, 8000.0, LocalDate.of(2026, 9, 1))))
        assertEquals(
            "25% más que a esta altura de septiembre. A este ritmo cerrarías el mes en $31000.",
            fraseResumen(conCambio, hoy, fmt),
        )
    }

    @Test fun `comparar meses une categorias de ambos y ordena por el mayor`() {
        val a = listOf(gasto(1, 100.0, hoy, 1, "Comida"), gasto(2, 300.0, hoy, 2, "Hogar"))
        val b = listOf(gasto(3, 200.0, hoy, 1, "Comida"), gasto(4, 50.0, hoy, 3, "Ocio"))
        val filas = compararMeses(a, b)
        assertEquals(listOf("Hogar", "Comida", "Ocio"), filas.map { it.nombre })
        assertEquals(100.0, filas[1].cambio!!, 0.001) // Comida: 100 -> 200
        assertNull("categoría nueva en B", filas[2].cambio)
    }

    // Mismos casos que parseAmount (src/lib/format.ts).
    @Test fun `parsear monto acepta los formatos de la web`() {
        assertEquals(12500.0, parsearMonto("12.500")!!, 0.0)
        assertEquals(12.5, parsearMonto("12,50")!!, 0.0)
        assertEquals(4990.0, parsearMonto("$ 4.990")!!, 0.0)
        assertEquals(1234.56, parsearMonto("1.234,56")!!, 0.0)
        assertEquals(1234.56, parsearMonto("1,234.56")!!, 0.0)
        assertEquals(12.5, parsearMonto("12.5")!!, 0.0)
        assertEquals(1250000.0, parsearMonto("1.250.000")!!, 0.0)
        assertNull(parsearMonto(""))
        assertNull(parsearMonto("0"))
        assertNull(parsearMonto("abc"))
    }

    @Test fun `formatea mientras se escribe con punto de miles y coma decimal`() {
        assertEquals("12.500", formatearMientrasEscribe("12500"))
        assertEquals("1.250.000", formatearMientrasEscribe("1250000"))
        assertEquals("12.500,5", formatearMientrasEscribe("12500,5"))
        assertEquals("12,50", formatearMientrasEscribe("12,505"))
        assertEquals("5", formatearMientrasEscribe("005"))
        assertEquals("", formatearMientrasEscribe("abc"))
    }

    @Test fun `etiquetas de dia y mes en espanol de Chile`() {
        assertEquals("Hoy", etiquetaDia(hoy, hoy))
        assertEquals("Ayer", etiquetaDia(hoy.minusDays(1), hoy))
        assertEquals("Lunes, 5 oct", etiquetaDia(LocalDate.of(2026, 10, 5), hoy))
        assertEquals("Octubre de 2026", etiquetaMes(YearMonth.of(2026, 10)))
        assertEquals("Sept", nombreMesCorto(YearMonth.of(2026, 9)).take(4))
    }
}
