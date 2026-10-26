package fase0

// =====================================================================
// Tema: colecciones (List, Map, Set) y operadores funcionales.
// =====================================================================

// ---------------------------------------------------------------------
// Ejercicio 12 — Frecuencia de palabras (Map)
// Devuelve las `top` palabras más frecuentes, de mayor a menor.
// ---------------------------------------------------------------------
fun frecuenciaPalabras(texto: String): Map<String, Int> =
    texto.lowercase()
        .split(Regex("[^\\p{L}\\p{N}]+"))  // separa por todo lo que no sea letra o número
        .filter { it.isNotEmpty() }
        .groupingBy { it }
        .eachCount()

fun palabrasMasFrecuentes(texto: String, top: Int): List<Pair<String, Int>> =
    frecuenciaPalabras(texto)
        .toList()
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
        .take(top)

// ---------------------------------------------------------------------
// Ejercicio 13 — Conjuntos (Set)
// Un Set no admite duplicados y ofrece operaciones de conjuntos.
// ---------------------------------------------------------------------
data class ComparacionCursos(
    val enAmbos: Set<String>,
    val soloEnA: Set<String>,
    val todos: Set<String>,
)

fun compararCursos(cursoA: List<String>, cursoB: List<String>): ComparacionCursos {
    val a = cursoA.toSet()
    val b = cursoB.toSet()
    return ComparacionCursos(
        enAmbos = a intersect b,
        soloEnA = a subtract b,
        todos = a union b,
    )
}

// ---------------------------------------------------------------------
// Ejercicio 14 — Resumen de gastos (puente hacia el proyecto integrador)
// Combina map, filter, groupBy, sumOf, sortedByDescending y forEach.
// ---------------------------------------------------------------------
enum class TipoMovimiento { INGRESO, GASTO }

data class Movimiento(
    val descripcion: String,
    val monto: Double,
    val categoria: String,
    val tipo: TipoMovimiento,
)

data class ResumenGastos(
    val totalIngresos: Double,
    val totalGastos: Double,
    val gastoPorCategoria: Map<String, Double>,
    val mayorGasto: Movimiento?,
) {
    val saldo: Double get() = totalIngresos - totalGastos
}

fun resumir(movimientos: List<Movimiento>): ResumenGastos {
    // partition divide en dos listas según el predicado.
    val (ingresos, gastos) = movimientos.partition { it.tipo == TipoMovimiento.INGRESO }

    val porCategoria = gastos
        .groupBy { it.categoria }
        .mapValues { (_, lista) -> lista.sumOf { it.monto } }
        .toList()
        .sortedByDescending { it.second }
        .toMap()  // LinkedHashMap: conserva el orden de mayor a menor

    return ResumenGastos(
        totalIngresos = ingresos.sumOf { it.monto },
        totalGastos = gastos.sumOf { it.monto },
        gastoPorCategoria = porCategoria,
        mayorGasto = gastos.maxByOrNull { it.monto },
    )
}

fun imprimirResumen(resumen: ResumenGastos) {
    println("Ingresos: ${resumen.totalIngresos.comoMoneda()}")
    println("Gastos:   ${resumen.totalGastos.comoMoneda()}")
    println("Saldo:    ${resumen.saldo.comoMoneda()}")
    resumen.gastoPorCategoria.forEach { (categoria, total) ->
        println("  - $categoria: ${total.comoMoneda()}")
    }
}
