package fase0

// =====================================================================
// Tema: null safety — tipos anulables (?), llamada segura (?.),
// operador Elvis (?:) y por qué evitar !! .
// =====================================================================

// ---------------------------------------------------------------------
// Ejercicio 5 — Parseo seguro de montos
// El usuario escribe un monto como texto ("1.500,50", " 20 ", "abc").
// Devolvemos Double? : null significa "entrada inválida".
// ---------------------------------------------------------------------
fun parsearMonto(entrada: String?): Double? {
    // ?. : si entrada es null, toda la cadena da null sin lanzar excepción.
    val normalizado = entrada
        ?.trim()
        ?.replace(".", "")   // separador de miles
        ?.replace(",", ".")  // coma decimal -> punto
        ?: return null       // Elvis con return: salida temprana

    return normalizado.toDoubleOrNull()?.takeIf { it >= 0 }
}

// Uso del Elvis para dar un valor por defecto.
fun montoOCero(entrada: String?): Double = parsearMonto(entrada) ?: 0.0

// `!!` afirma "esto nunca es null" y lanza NullPointerException si lo es.
// Solo se justifica cuando el null es un error de programación. Aquí se
// muestra para entenderlo, no como práctica recomendada.
fun longitudForzada(texto: String?): Int = texto!!.length

// ---------------------------------------------------------------------
// Ejercicio 6 — Estadísticas de una lista
// Una lista vacía no tiene promedio ni máximo: el resultado es anulable.
// ---------------------------------------------------------------------
data class Estadisticas(val minimo: Double, val maximo: Double, val promedio: Double)

fun estadisticas(valores: List<Double>): Estadisticas? {
    if (valores.isEmpty()) return null
    return Estadisticas(
        minimo = valores.min(),
        maximo = valores.max(),
        promedio = valores.average(),
    )
}
