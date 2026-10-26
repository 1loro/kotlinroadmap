package com.example.controlgastos.ui

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val localeApp: Locale = Locale.forLanguageTag("es-AR")
private val formatoMoneda: NumberFormat = NumberFormat.getCurrencyInstance(localeApp)
private val formatoFecha: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", localeApp)

fun formatearMonto(monto: Double): String = formatoMoneda.format(monto)

fun formatearFecha(fecha: LocalDate): String = fecha.format(formatoFecha)
