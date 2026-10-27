package com.example.controlgastos.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

// Etiquetas de fechas en español de Chile, iguales a las de la web.

val LOCALE_APP: Locale = Locale.forLanguageTag("es-CL")

private fun String.capitalizar() = replaceFirstChar { it.titlecase(LOCALE_APP) }

/** "Octubre". */
fun nombreMes(mes: YearMonth): String = mes.month.getDisplayName(TextStyle.FULL, LOCALE_APP).capitalizar()

/** "Oct". */
fun nombreMesCorto(mes: YearMonth): String =
    mes.month.getDisplayName(TextStyle.SHORT, LOCALE_APP).replace(".", "").capitalizar()

/** "Octubre de 2026". */
fun etiquetaMes(mes: YearMonth): String = "${nombreMes(mes)} de ${mes.year}"

/** "Hoy", "Ayer" o "Lunes, 20 oct". */
fun etiquetaDia(fecha: LocalDate, hoy: LocalDate): String {
    val dias = ChronoUnit.DAYS.between(fecha, hoy)
    return when (dias) {
        0L -> "Hoy"
        1L -> "Ayer"
        else -> {
            val semana = fecha.dayOfWeek.getDisplayName(TextStyle.FULL, LOCALE_APP).capitalizar()
            "$semana, ${fecha.dayOfMonth} ${nombreMesCorto(YearMonth.from(fecha)).lowercase()}"
        }
    }
}

/** Fecha de hoy en la zona horaria del usuario (todayIn de la web). */
fun hoyEn(zonaHoraria: String, reloj: java.time.Clock): LocalDate {
    val zona = runCatching { java.time.ZoneId.of(zonaHoraria) }.getOrDefault(java.time.ZoneId.of("America/Santiago"))
    return LocalDate.now(reloj.withZone(zona))
}
