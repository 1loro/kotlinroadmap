package fase0

import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EjerciciosTest {

    @Test fun `1 calculadora`() {
        assertEquals(7.0, calcular(3.0, 4.0, Operacion.SUMA)!!, 0.0)
        assertEquals(2.5, calcular(10.0, 4.0, Operacion.DIVISION)!!, 0.0)
        assertNull(calcular(1.0, 0.0, Operacion.DIVISION))
        assertEquals(Operacion.RESTA, Operacion.desdeSimbolo("-"))
        assertNull(Operacion.desdeSimbolo("%"))
    }

    @Test fun `2 fizzbuzz`() {
        assertEquals(listOf("1", "2", "Fizz", "4", "Buzz"), (1..5).map(::fizzBuzz))
        assertEquals("FizzBuzz", fizzBuzz(30))
    }

    @Test fun `3 palindromo`() {
        assertTrue(esPalindromo("Anita lava la tina"))
        assertTrue(esPalindromo("¿Acaso hubo búhos acá?".replace("ú", "u").replace("á", "a")))
        assertFalse(esPalindromo("Kotlin"))
    }

    @Test fun `4 factorial fibonacci y primos`() {
        assertEquals(1L, factorial(0))
        assertEquals(3_628_800L, factorial(10))
        assertEquals(listOf(0L, 1L, 1L, 2L, 3L, 5L, 8L), fibonacci(7))
        assertEquals(listOf(2, 3, 5, 7, 11, 13), primosHasta(13))
        assertFalse(esPrimo(1))
    }

    @Test fun `5 parseo seguro`() {
        assertEquals(1500.5, parsearMonto("1.500,50")!!, 0.0)
        assertEquals(20.0, parsearMonto(" 20 ")!!, 0.0)
        assertNull(parsearMonto("abc"))
        assertNull(parsearMonto(null))
        assertNull(parsearMonto("-5"))
        assertEquals(0.0, montoOCero("xyz"), 0.0)
    }

    @Test(expected = NullPointerException::class)
    fun `5 doble exclamacion lanza excepcion con null`() {
        longitudForzada(null)
    }

    @Test fun `6 estadisticas`() {
        assertEquals(Estadisticas(1.0, 3.0, 2.0), estadisticas(listOf(3.0, 1.0, 2.0)))
        assertNull(estadisticas(emptyList()))
    }

    @Test fun `7 orden superior`() {
        assertEquals(8, aplicarNVeces(1, 3) { it * 2 })
        assertEquals("6", componer<Int, Int, String>({ it * 2 }, { it.toString() })(3))
        assertEquals(800.0, crearDescuento(20.0)(1000.0), 0.001)
    }

    @Test fun `8 extensiones`() {
        assertEquals("$1234.50", 1234.5.comoMoneda())
        assertEquals("Hola Mundo", "  hOLA   mundo ".capitalizarPalabras())
        assertEquals(5, (1..10).toList().contarSi { it % 2 == 0 })
    }

    @Test fun `9 figuras`() {
        assertEquals(4.0, Cuadrado(2.0).area(), 0.0)
        assertEquals(8.0, Cuadrado(2.0).perimetro(), 0.0)
        assertEquals(Math.PI, Circulo(1.0).area(), 1e-9)
        assertEquals(10.0, areaTotal(listOf(Rectangulo(2.0, 3.0), Cuadrado(2.0))), 0.0)
    }

    @Test fun `10 carrito`() {
        val cafe = Producto("Café", 3.5, "Alimentos")
        val carrito = Carrito().apply {
            agregar(cafe, 2)
            agregar(cafe)
            agregar(Producto("Cuaderno", 5.0, "Librería"))
        }
        assertEquals(2, carrito.items.size)
        assertEquals(3, carrito.items.first().cantidad)
        assertEquals(15.5, carrito.total(), 0.0)
        assertEquals(mapOf("Alimentos" to 10.5, "Librería" to 5.0), carrito.totalPorCategoria())
        carrito.quitar(cafe)
        assertEquals(5.0, carrito.total(), 0.0)
    }

    @Test fun `11 object y companion`() {
        GeneradorId.reiniciar()
        assertEquals(1, Cuenta.abrir("Ana").id)
        assertEquals(2, Cuenta.abrir("Luis").id)
    }

    @Test fun `12 frecuencia de palabras`() {
        val texto = "el banco y el cliente; el cliente paga"
        assertEquals(3, frecuenciaPalabras(texto)["el"])
        assertEquals(listOf("el" to 3, "cliente" to 2), palabrasMasFrecuentes(texto, 2))
    }

    @Test fun `13 conjuntos`() {
        val r = compararCursos(listOf("Ana", "Luis", "Ana"), listOf("Luis", "Eva"))
        assertEquals(setOf("Luis"), r.enAmbos)
        assertEquals(setOf("Ana"), r.soloEnA)
        assertEquals(setOf("Ana", "Luis", "Eva"), r.todos)
    }

    @Test fun `14 resumen de gastos`() {
        val alquiler = Movimiento("Alquiler", 900.0, "Vivienda", TipoMovimiento.GASTO)
        val resumen = resumir(
            listOf(
                Movimiento("Sueldo", 2000.0, "Trabajo", TipoMovimiento.INGRESO),
                alquiler,
                Movimiento("Super", 300.0, "Comida", TipoMovimiento.GASTO),
                Movimiento("Cena", 100.0, "Comida", TipoMovimiento.GASTO),
            ),
        )
        assertEquals(700.0, resumen.saldo, 0.0)
        assertEquals(listOf("Vivienda", "Comida"), resumen.gastoPorCategoria.keys.toList())
        assertEquals(400.0, resumen.gastoPorCategoria["Comida"]!!, 0.0)
        assertEquals(alquiler, resumen.mayorGasto)
        assertNull(resumir(emptyList()).mayorGasto)
    }

    // runTest usa un reloj virtual: delay() no espera de verdad, así que
    // podemos comprobar cuánto "tiempo" tardó cada versión.
    @Test fun `15 corrutinas en paralelo tardan lo que la mas lenta`() = runTest {
        val cuentas = mapOf("a" to 300L, "bb" to 200L, "ccc" to 100L)

        val inicioSec = currentTime
        assertEquals(6000.0, saldoTotalSecuencial(cuentas), 0.0)
        assertEquals(600L, currentTime - inicioSec)

        val inicioPar = currentTime
        assertEquals(6000.0, saldoTotalParalelo(cuentas), 0.0)
        assertEquals(300L, currentTime - inicioPar)

        val registro = mutableListOf<String>()
        notificarCuentas(listOf("a", "b"), registro)
        assertEquals(2, registro.size)
    }
}
