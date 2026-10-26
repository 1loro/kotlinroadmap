package com.example.controlgastos.ui

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

private val localeApp: Locale = Locale.forLanguageTag("es-AR")
private val formatoMoneda: NumberFormat = NumberFormat.getCurrencyInstance(localeApp)
private val formatoDolares: NumberFormat = NumberFormat.getCurrencyInstance(localeApp).apply {
    currency = Currency.getInstance("USD")
}
private val formatoFecha: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", localeApp)
private val formatoFechaHora: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm", localeApp)

fun formatearMonto(monto: Double): String = formatoMoneda.format(monto)

fun formatearDolares(monto: Double): String = formatoDolares.format(monto)

fun formatearFecha(fecha: LocalDate): String = fecha.format(formatoFecha)

// Instant es un momento en UTC: se convierte a la zona horaria del teléfono.
fun formatearFechaHora(instante: Instant, zona: ZoneId = ZoneId.systemDefault()): String =
    instante.atZone(zona).format(formatoFechaHora)
