package com.example.controlgastos

import com.example.controlgastos.model.Categoria
import com.example.controlgastos.model.Movimiento
import com.example.controlgastos.model.resumen
import com.example.controlgastos.ui.GastosState
import com.example.controlgastos.ui.formulario.parsearMonto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class LogicaTest {

    private val hoy = LocalDate.of(2026, 10, 26)

    @Test fun `resumen calcula saldo y ordena categorias por gasto`() {
        val r = listOf(
            Movimiento(1, "Sueldo", 1000.0, Categoria.SUELDO, hoy),
            Movimiento(2, "Super", 100.0, Categoria.COMIDA, hoy),
            Movimiento(3, "Alquiler", 400.0, Categoria.VIVIENDA, hoy),
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

    @Test fun `parsearMonto acepta coma o punto y rechaza invalidos`() {
        assertEquals(1500.5, parsearMonto("1500,50")!!, 0.0)
        assertEquals(1500.5, parsearMonto(" 1500.50 ")!!, 0.0)
        assertNull(parsearMonto(""))
        assertNull(parsearMonto("0"))
        assertNull(parsearMonto("abc"))
    }

    @Test fun `estado agrega con id nuevo, ordena por fecha y elimina`() {
        val state = GastosState(listOf(Movimiento(7, "Viejo", 10.0, Categoria.OCIO, hoy.minusDays(3))))

        state.agregar(Movimiento(0, "Nuevo", 20.0, Categoria.COMIDA, hoy))

        assertEquals(listOf("Nuevo", "Viejo"), state.movimientos.map { it.descripcion })
        assertEquals(8L, state.movimientos.first().id)

        state.eliminar(7)
        assertEquals(listOf("Nuevo"), state.movimientos.map { it.descripcion })
        assertNull(state.buscar(7))
    }
}
