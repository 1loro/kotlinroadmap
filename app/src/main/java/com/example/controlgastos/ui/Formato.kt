package com.example.controlgastos.ui

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

// Español de Chile: punto para miles y coma para decimales.
private val localeApp: Locale = Locale.forLanguageTag("es-CL")

// Pesos chilenos (CLP): la moneda no tiene decimales -> "$1.250.000".
private val formatoMoneda: NumberFormat = NumberFormat.getCurrencyInstance(localeApp).apply {
    currency = Currency.getInstance("CLP")
    maximumFractionDigits = 0
}

// Valor de un indicador en pesos: aquí SÍ importan los decimales
// (dólar observado $990,35; UF $41.221,08).
private val formatoIndicador: NumberFormat = NumberFormat.getCurrencyInstance(localeApp).apply {
    currency = Currency.getInstance("CLP")
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

private val formatoDolares: NumberFormat = NumberFormat.getCurrencyInstance(localeApp).apply {
    currency = Currency.getInstance("USD")
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}
private val formatoFecha: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", localeApp)
private val formatoFechaHora: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm", localeApp)

fun formatearMonto(monto: Double): String = formatoMoneda.format(monto)

fun formatearDolares(monto: Double): String = formatoDolares.format(monto)

fun formatearValorIndicador(valor: Double): String = formatoIndicador.format(valor)

fun formatearFecha(fecha: LocalDate): String = fecha.format(formatoFecha)

// Los indicadores corresponden a un DÍA de Chile (la API lo envía como la
// medianoche de Santiago en UTC). Se muestra en esa zona para que no aparezca
// el día anterior en un teléfono con otra zona horaria.
private val zonaChile: ZoneId = ZoneId.of("America/Santiago")

fun formatearDiaChile(instante: Instant): String = instante.atZone(zonaChile).format(formatoFecha)

// Instant es un momento en UTC: se convierte a la zona horaria del teléfono.
fun formatearFechaHora(instante: Instant, zona: ZoneId = ZoneId.systemDefault()): String =
    instante.atZone(zona).format(formatoFechaHora)
