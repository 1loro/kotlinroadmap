package fase0

import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

// Ejecuta una demostración de cada ejercicio: ./gradlew :fase0-kotlin:run
fun main() = runBlocking {
    seccion("1. Calculadora")
    println("10 / 4 = ${calcular(10.0, 4.0, Operacion.DIVISION)}")
    println("10 / 0 = ${calcular(10.0, 0.0, Operacion.DIVISION) ?: "error: división por cero"}")
    println("Símbolo '*' -> ${Operacion.desdeSimbolo("*")}")

    seccion("2. FizzBuzz")
    println((1..15).joinToString(" ") { fizzBuzz(it) })

    seccion("3. Palíndromo")
    listOf("Anita lava la tina", "Kotlin").forEach { println("\"$it\" -> ${esPalindromo(it)}") }

    seccion("4. Factorial, Fibonacci y primos")
    println("10! = ${factorial(10)}")
    println("Fibonacci(10) = ${fibonacci(10)}")
    println("Primos hasta 30 = ${primosHasta(30)}")

    seccion("5. Parseo seguro")
    listOf("1.500,50", " 20 ", "abc", null, "-5").forEach { println("'$it' -> ${parsearMonto(it)}") }

    seccion("6. Estadísticas")
    println(estadisticas(listOf(4.0, 8.0, 15.0, 16.0, 23.0, 42.0)))
    println("Lista vacía -> ${estadisticas(emptyList())}")

    seccion("7. Funciones de orden superior")
    println("Duplicar 3 veces el 1 = ${aplicarNVeces(1, 3) { it * 2 }}")
    val longitudPar = componer<String, Int, Boolean>({ it.length }, { it % 2 == 0 })
    println("¿\"Kotlin\" tiene longitud par? ${longitudPar("Kotlin")}")
    val descuento20 = crearDescuento(20.0)
    println("1000 con 20% de descuento = ${descuento20(1000.0)}")

    seccion("8. Extensiones")
    println(1234.5.comoMoneda())
    println("hola   mundo kotlin".capitalizarPalabras())
    println("Pares en 1..10: ${(1..10).toList().contarSi { it % 2 == 0 }}")

    seccion("9. Figuras")
    val figuras = listOf(Circulo(1.0), Rectangulo(2.0, 3.0), Cuadrado(2.0))
    figuras.forEach { println(it.describir()) }
    println("Área total = %.2f".format(areaTotal(figuras)))

    seccion("10. Carrito")
    val carrito = Carrito()
    val cafe = Producto("Café", 3.5, "Alimentos")
    carrito.agregar(cafe, 2)
    carrito.agregar(Producto("Cuaderno", 5.0, "Librería"))
    carrito.agregar(cafe)
    carrito.items.forEach { println("${it.producto.nombre} x${it.cantidad} = ${it.subtotal.comoMoneda()}") }
    println("Total = ${carrito.total().comoMoneda()}  Por categoría = ${carrito.totalPorCategoria()}")

    seccion("11. object y companion object")
    val c1 = Cuenta.abrir("Ana")
    val c2 = Cuenta.abrir("Luis")
    println("Cuenta ${c1.id} de ${c1.titular}, cuenta ${c2.id} de ${c2.titular}")

    seccion("12. Frecuencia de palabras")
    println(palabrasMasFrecuentes("el banco y el cliente; el cliente paga al banco", 3))

    seccion("13. Conjuntos")
    println(compararCursos(listOf("Ana", "Luis", "Eva", "Ana"), listOf("Eva", "Juan")))

    seccion("14. Resumen de gastos")
    imprimirResumen(
        resumir(
            listOf(
                Movimiento("Sueldo", 2500.0, "Trabajo", TipoMovimiento.INGRESO),
                Movimiento("Alquiler", 900.0, "Vivienda", TipoMovimiento.GASTO),
                Movimiento("Supermercado", 320.0, "Comida", TipoMovimiento.GASTO),
                Movimiento("Restaurante", 80.0, "Comida", TipoMovimiento.GASTO),
            ),
        ),
    )

    seccion("15. Corrutinas")
    val cuentas = mapOf("ahorros" to 300L, "corriente" to 200L, "inversion" to 100L)
    val tSec = measureTimeMillis { println("Secuencial: ${saldoTotalSecuencial(cuentas)}") }
    val tPar = measureTimeMillis { println("Paralelo:   ${saldoTotalParalelo(cuentas)}") }
    println("Tiempo secuencial ≈ $tSec ms, paralelo ≈ $tPar ms")
    val registro = mutableListOf<String>()
    notificarCuentas(cuentas.keys.toList(), registro)
    println(registro.sorted())
}

private fun seccion(titulo: String) = println("\n=== $titulo ===")
