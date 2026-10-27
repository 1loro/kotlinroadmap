package com.example.controlgastos.ui

import com.example.controlgastos.domain.LOCALE_APP
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import kotlin.math.abs

// Formatos en español de Chile (punto de miles, coma decimal), iguales a los
// de la web (formatMoney con Intl.NumberFormat "es-CL").

private fun formatoMoneda(moneda: String): NumberFormat =
    NumberFormat.getCurrencyInstance(LOCALE_APP).apply {
        // setCurrency ajusta los decimales a los de la moneda: CLP 0, USD 2.
        currency = runCatching { Currency.getInstance(moneda) }.getOrDefault(Currency.getInstance("CLP"))
    }

/** Símbolo de la moneda en es-CL: "$" para CLP, "US$" para USD, "€" para EUR. */
fun simboloMoneda(moneda: String): String =
    runCatching { Currency.getInstance(moneda).getSymbol(LOCALE_APP) }.getOrDefault(moneda)

/**
 * "$1.250.000" o, compacto, "$1,3 M" / "$12,5 mil" (para espacios chicos,
 * como `notation: "compact"` en la web).
 */
fun formatearDinero(valor: Double, moneda: String, compacto: Boolean = false): String {
    if (!compacto || abs(valor) < 1_000) return formatoMoneda(moneda).format(valor)
    val (cifra, sufijo) = if (abs(valor) >= 1_000_000) valor / 1_000_000 to "M" else valor / 1_000 to "mil"
    val numero = NumberFormat.getNumberInstance(LOCALE_APP).apply { maximumFractionDigits = 1 }.format(cifra)
    return "${simboloMoneda(moneda)}$numero $sufijo"
}

private val formatoDolares: NumberFormat = NumberFormat.getCurrencyInstance(LOCALE_APP).apply {
    currency = Currency.getInstance("USD")
    minimumFractionDigits = 2
}

fun formatearDolares(monto: Double): String = formatoDolares.format(monto)

// Valor de un indicador en pesos: aquí SÍ importan los decimales
// (dólar observado $990,35; UF $41.221,08).
private val formatoIndicador: NumberFormat = NumberFormat.getCurrencyInstance(LOCALE_APP).apply {
    currency = Currency.getInstance("CLP")
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

fun formatearValorIndicador(valor: Double): String = formatoIndicador.format(valor)

private val formatoFecha: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", LOCALE_APP)
private val formatoFechaNumerica: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE_APP)

fun formatearFecha(fecha: LocalDate): String = fecha.format(formatoFecha)

/** "26/10/2026" (como el botón "Otra fecha" de la web). */
fun formatearFechaNumerica(fecha: LocalDate): String = fecha.format(formatoFechaNumerica)

// Los indicadores corresponden a un DÍA de Chile (la API lo envía como la
// medianoche de Santiago en UTC). Se muestra en esa zona para que no aparezca
// el día anterior en un teléfono con otra zona horaria.
private val zonaChile: ZoneId = ZoneId.of("America/Santiago")

fun formatearDiaChile(instante: Instant): String = instante.atZone(zonaChile).format(formatoFecha)
