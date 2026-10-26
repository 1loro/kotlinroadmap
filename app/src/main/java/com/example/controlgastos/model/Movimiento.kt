package com.example.controlgastos.model

import java.time.LocalDate

enum class TipoMovimiento { INGRESO, GASTO }

// Cada categoría sabe a qué tipo de movimiento pertenece y cómo mostrarse.
enum class Categoria(val etiqueta: String, val emoji: String, val tipo: TipoMovimiento) {
    COMIDA("Comida", "🍔", TipoMovimiento.GASTO),
    VIVIENDA("Vivienda", "🏠", TipoMovimiento.GASTO),
    TRANSPORTE("Transporte", "🚌", TipoMovimiento.GASTO),
    SERVICIOS("Servicios", "🧾", TipoMovimiento.GASTO),
    SALUD("Salud", "💊", TipoMovimiento.GASTO),
    OCIO("Ocio", "🎬", TipoMovimiento.GASTO),
    OTROS_GASTOS("Otros", "📦", TipoMovimiento.GASTO),
    SUELDO("Sueldo", "💼", TipoMovimiento.INGRESO),
    EXTRA("Extra", "💰", TipoMovimiento.INGRESO);

    companion object {
        fun de(tipo: TipoMovimiento): List<Categoria> = entries.filter { it.tipo == tipo }
    }
}

data class Movimiento(
    val id: Long,
    val descripcion: String,
    // Siempre positivo; el signo lo da la categoría (ingreso o gasto).
    val monto: Double,
    val categoria: Categoria,
    val fecha: LocalDate,
) {
    val tipo: TipoMovimiento get() = categoria.tipo
    val esGasto: Boolean get() = tipo == TipoMovimiento.GASTO
}

data class Resumen(
    val ingresos: Double,
    val gastos: Double,
    // Ordenado de mayor a menor gasto.
    val gastoPorCategoria: List<Pair<Categoria, Double>>,
) {
    val saldo: Double get() = ingresos - gastos
}

fun List<Movimiento>.resumen(): Resumen {
    val (gastos, ingresos) = partition { it.esGasto }
    return Resumen(
        ingresos = ingresos.sumOf { it.monto },
        gastos = gastos.sumOf { it.monto },
        gastoPorCategoria = gastos
            .groupBy { it.categoria }
            .map { (categoria, lista) -> categoria to lista.sumOf { it.monto } }
            .sortedByDescending { it.second },
    )
}
