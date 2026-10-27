package com.example.controlgastos.domain.model

import java.time.LocalDate
import java.time.YearMonth

// Modelos de la app. Gastos, categorías y usuario viven en el backend de
// gastos. (la misma cuenta que la PWA del iPhone); los ingresos, solo en este
// teléfono.

data class Usuario(
    val id: Long,
    val nombre: String,
    val email: String,
    /** Código ISO 4217: "CLP", "USD"... Se elige en Ajustes. */
    val moneda: String,
    /** Zona IANA, p. ej. "America/Santiago": define qué día es "hoy". */
    val zonaHoraria: String,
    /** Cambia cada vez que se sube una foto; null si no hay foto. */
    val versionAvatar: Long?,
) {
    val primerNombre: String get() = nombre.trim().split(Regex("\\s+")).first()

    val iniciales: String
        get() = nombre.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
            .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
}

data class Categoria(
    val id: Long,
    val nombre: String,
    /** Clave del ícono, igual que en la web: "utensils", "cart", "car"... */
    val icono: String,
    val orden: Int = 0,
    val archivada: Boolean = false,
    /** Cuántos gastos la usan (solo se puede eliminar si es 0). */
    val usos: Int = 0,
)

data class Gasto(
    val id: Long,
    val monto: Double,
    val nota: String?,
    val fecha: LocalDate,
    /** "app" (web), "shortcut" (Atajo de iPhone) o "android". */
    val origen: String,
    val categoriaId: Long,
    val categoriaNombre: String,
    val categoriaIcono: String,
) {
    val mes: YearMonth get() = YearMonth.from(fecha)
}

data class TotalMes(val mes: YearMonth, val total: Double)

data class TotalCategoria(
    val categoriaId: Long,
    val nombre: String,
    val icono: String,
    val total: Double,
    val cantidad: Int,
)

/** Tipos de ingreso (locales). El ícono usa las mismas claves que las categorías. */
enum class TipoIngreso(val etiqueta: String, val icono: String) {
    SUELDO("Sueldo", "briefcase"),
    VENTA("Venta", "tag"),
    REGALO("Regalo", "gift"),
    OTRO("Otro ingreso", "piggy"),
}

data class Ingreso(
    val id: Long,
    val monto: Double,
    val tipo: TipoIngreso,
    val nota: String?,
    val fecha: LocalDate,
)

enum class Tema { CLARO, OSCURO, SISTEMA }
