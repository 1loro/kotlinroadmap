package fase0

import java.util.Locale

// =====================================================================
// Tema: funciones, lambdas, funciones de orden superior y extensiones.
// =====================================================================

// ---------------------------------------------------------------------
// Ejercicio 7 — Funciones de orden superior
// Una función de orden superior recibe o devuelve otra función.
// El tipo (Int) -> Int describe "función que recibe Int y devuelve Int".
// ---------------------------------------------------------------------
fun aplicarNVeces(valor: Int, veces: Int, f: (Int) -> Int): Int {
    var resultado = valor
    repeat(veces) { resultado = f(resultado) }
    return resultado
}

// Devuelve una nueva función que aplica primero `f` y luego `g`.
fun <A, B, C> componer(f: (A) -> B, g: (B) -> C): (A) -> C = { x -> g(f(x)) }

// Una función que "fabrica" funciones: captura `porcentaje` (closure).
fun crearDescuento(porcentaje: Double): (Double) -> Double =
    { precio -> precio * (1 - porcentaje / 100) }

// ---------------------------------------------------------------------
// Ejercicio 8 — Funciones de extensión y lambdas con receptor
// Las extensiones "agregan" métodos a tipos existentes sin heredar.
// ---------------------------------------------------------------------
// Locale.ROOT: el resultado no depende del idioma del sistema (siempre punto decimal).
fun Double.comoMoneda(simbolo: String = "$"): String = String.format(Locale.ROOT, "%s%.2f", simbolo, this)

fun String.capitalizarPalabras(): String =
    split(" ").filter { it.isNotBlank() }.joinToString(" ") { palabra ->
        palabra.lowercase().replaceFirstChar { it.uppercase() }
    }

// Extensión genérica que recibe un predicado: cuenta los elementos que cumplen.
fun <T> List<T>.contarSi(predicado: (T) -> Boolean): Int {
    var cuenta = 0
    for (elemento in this) if (predicado(elemento)) cuenta++
    return cuenta
}
