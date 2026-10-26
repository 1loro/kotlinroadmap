package fase0

// =====================================================================
// Tema: sintaxis, variables (val/var), tipos, when, bucles y recursión.
// =====================================================================

// ---------------------------------------------------------------------
// Ejercicio 1 — Calculadora
// Operaciones básicas usando un enum y `when`. La división por cero
// devuelve null en vez de lanzar excepción (null safety desde el diseño).
// ---------------------------------------------------------------------
enum class Operacion(val simbolo: String) {
    SUMA("+"), RESTA("-"), MULTIPLICACION("*"), DIVISION("/");

    companion object {
        // Busca la operación por su símbolo; null si no existe.
        fun desdeSimbolo(simbolo: String): Operacion? = entries.find { it.simbolo == simbolo }
    }
}

fun calcular(a: Double, b: Double, op: Operacion): Double? = when (op) {
    Operacion.SUMA -> a + b
    Operacion.RESTA -> a - b
    Operacion.MULTIPLICACION -> a * b
    Operacion.DIVISION -> if (b == 0.0) null else a / b
}

// ---------------------------------------------------------------------
// Ejercicio 2 — FizzBuzz
// `when` sin argumento funciona como una cadena de if/else más legible.
// ---------------------------------------------------------------------
fun fizzBuzz(n: Int): String = when {
    n % 15 == 0 -> "FizzBuzz"
    n % 3 == 0 -> "Fizz"
    n % 5 == 0 -> "Buzz"
    else -> n.toString()
}

// ---------------------------------------------------------------------
// Ejercicio 3 — Palíndromo
// Ignora mayúsculas, espacios y signos: "Anita lava la tina" es palíndromo.
// ---------------------------------------------------------------------
fun esPalindromo(texto: String): Boolean {
    val limpio = texto.lowercase().filter { it.isLetterOrDigit() }
    return limpio == limpio.reversed()
}

// ---------------------------------------------------------------------
// Ejercicio 4 — Factorial, Fibonacci y primos
// `var` solo donde el valor cambia; `tailrec` permite recursión sin
// desbordar la pila; `..` y `until` son rangos.
// ---------------------------------------------------------------------
tailrec fun factorial(n: Int, acumulado: Long = 1): Long =
    if (n <= 1) acumulado else factorial(n - 1, acumulado * n)

fun fibonacci(cantidad: Int): List<Long> {
    val resultado = mutableListOf<Long>()
    var a = 0L
    var b = 1L
    repeat(cantidad) {
        resultado.add(a)
        val siguiente = a + b
        a = b
        b = siguiente
    }
    return resultado
}

fun esPrimo(n: Int): Boolean {
    if (n < 2) return false
    var i = 2
    while (i * i <= n) {
        if (n % i == 0) return false
        i++
    }
    return true
}

fun primosHasta(limite: Int): List<Int> = (2..limite).filter(::esPrimo)
