package com.example.controlgastos.domain

import com.example.controlgastos.domain.model.Gasto
import com.example.controlgastos.domain.model.TotalCategoria
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.round

// Cálculos portados de la web (src/lib/format.ts y las páginas): Kotlin
// puro, sin Android, para que la app muestre exactamente lo mismo y se pueda
// testear.

/** Variación porcentual. null = no hay base (el mes anterior fue 0 y este no). */
fun cambioPorcentual(actual: Double, anterior: Double): Double? {
    if (anterior == 0.0) return if (actual == 0.0) 0.0 else null
    return (actual - anterior) / anterior * 100
}

/** Total por categoría, de mayor a menor (como getCategoryTotals). */
fun totalesPorCategoria(gastos: List<Gasto>): List<TotalCategoria> =
    gastos.groupBy { it.categoriaId }
        .map { (id, lista) ->
            val primero = lista.first()
            TotalCategoria(id, primero.categoriaNombre, primero.categoriaIcono, lista.sumOf { it.monto }, lista.size)
        }
        .sortedByDescending { it.total }

/** Resumen del mes en curso, como el Inicio de la web. */
data class ResumenMes(
    val total: Double,
    val cantidad: Int,
    val promedioDiario: Double,
    val proyeccion: Double,
    /** vs el mismo tramo del mes anterior (del 1 al día de hoy). */
    val cambio: Double?,
)

/**
 * @param gastosMes gastos del mes de [hoy].
 * @param gastosMesAnterior gastos de todo el mes anterior (se recorta al mismo tramo).
 */
fun resumenMes(hoy: LocalDate, gastosMes: List<Gasto>, gastosMesAnterior: List<Gasto>): ResumenMes {
    val total = gastosMes.sumOf { it.monto }
    val dia = hoy.dayOfMonth
    // Mismo tramo del mes anterior para comparar parejo: si hoy es 10, del 1 al 10.
    val anteriorHastaHoy = gastosMesAnterior.filter { it.fecha.dayOfMonth <= dia }.sumOf { it.monto }
    val promedio = total / dia
    return ResumenMes(
        total = total,
        cantidad = gastosMes.size,
        promedioDiario = promedio,
        proyeccion = promedio * YearMonth.from(hoy).lengthOfMonth(),
        cambio = cambioPorcentual(total, anteriorHastaHoy),
    )
}

/** Frase bajo el total del Inicio (misma redacción que la web). */
fun fraseResumen(resumen: ResumenMes, hoy: LocalDate, formatear: (Double) -> String): String {
    val mes = nombreMes(YearMonth.from(hoy)).lowercase()
    if (resumen.total <= 0) return "Aún no registras gastos en $mes."
    val ritmo = "A este ritmo cerrarías el mes en ${formatear(round(resumen.proyeccion / 1000) * 1000)}."
    val cambio = resumen.cambio
    if (cambio == null || Math.round(cambio) == 0L) return ritmo
    val anterior = nombreMes(YearMonth.from(hoy).minusMonths(1)).lowercase()
    val direccion = if (cambio > 0) "más" else "menos"
    return "${Math.abs(Math.round(cambio))}% $direccion que a esta altura de $anterior. $ritmo"
}

/** Fila de la comparación entre dos meses, por categoría. */
data class FilaComparacion(val nombre: String, val icono: String, val a: Double, val b: Double) {
    val cambio: Double? get() = cambioPorcentual(b, a)
}

fun compararMeses(gastosA: List<Gasto>, gastosB: List<Gasto>): List<FilaComparacion> {
    val porA = totalesPorCategoria(gastosA).associateBy { it.categoriaId }
    val porB = totalesPorCategoria(gastosB).associateBy { it.categoriaId }
    return (porA.keys + porB.keys).map { id ->
        val c = porA[id] ?: porB.getValue(id)
        FilaComparacion(c.nombre, c.icono, porA[id]?.total ?: 0.0, porB[id]?.total ?: 0.0)
    }.sortedByDescending { maxOf(it.a, it.b) }
}

/**
 * Lee un monto escrito a mano: "12.500", "12,50", "$ 4.990"... (parseAmount de
 * la web). El último separador decide: con dos tipos, el último es el decimal;
 * con solo punto, "12.500" es miles y "12.5" decimal.
 */
fun parsearMonto(texto: String): Double? {
    var s = texto.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }
    if (s.isEmpty()) return null
    val ultimaComa = s.lastIndexOf(',')
    val ultimoPunto = s.lastIndexOf('.')
    s = when {
        ultimaComa > -1 && ultimoPunto > -1 ->
            if (ultimaComa > ultimoPunto) s.replace(".", "").replace(",", ".") else s.replace(",", "")
        ultimaComa > -1 ->
            if (Regex(",\\d{1,2}$").containsMatchIn(s)) s.replace(",", ".") else s.replace(",", "")
        ultimoPunto > -1 ->
            if (Regex("\\.\\d{3}$").containsMatchIn(s) || s.count { it == '.' } > 1) s.replace(".", "") else s
        else -> s
    }
    val n = s.toDoubleOrNull() ?: return null
    return if (n.isFinite() && n > 0) round(n * 100) / 100 else null
}

/** Formatea mientras se escribe: miles con punto, hasta 2 decimales con coma. */
fun formatearMientrasEscribe(texto: String): String {
    val limpio = texto.filter { it.isDigit() || it == ',' }
    val partes = limpio.split(',')
    val entero = partes.first().trimStart('0').ifEmpty { if (partes.first().isNotEmpty()) "0" else "" }
    val conMiles = entero.reversed().chunked(3).joinToString(".").reversed()
    return if (partes.size > 1) "$conMiles,${partes.drop(1).joinToString("").take(2)}" else conMiles
}
