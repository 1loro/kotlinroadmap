package com.example.controlgastos.data

import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.Movimiento
import java.time.LocalDate

// Datos con los que arranca la app la primera vez (ver GastosDatabase), y
// que usan los @Preview. Los ids se ignoran al insertarlos en Room.
object DatosDeEjemplo {
    fun movimientos(hoy: LocalDate = LocalDate.now()): List<Movimiento> = listOf(
        Movimiento(1, "Sueldo de octubre", 850_000.0, Categoria.SUELDO, hoy.withDayOfMonth(1)),
        Movimiento(2, "Alquiler", 320_000.0, Categoria.VIVIENDA, hoy.withDayOfMonth(2)),
        Movimiento(3, "Supermercado", 95_400.0, Categoria.COMIDA, hoy.minusDays(6)),
        Movimiento(4, "Luz y gas", 48_900.0, Categoria.SERVICIOS, hoy.minusDays(5)),
        Movimiento(5, "Carga SUBE", 15_000.0, Categoria.TRANSPORTE, hoy.minusDays(4)),
        Movimiento(6, "Venta de bicicleta", 120_000.0, Categoria.EXTRA, hoy.minusDays(3)),
        Movimiento(7, "Cine", 12_500.0, Categoria.OCIO, hoy.minusDays(2)),
        Movimiento(8, "Farmacia", 22_300.0, Categoria.SALUD, hoy.minusDays(1)),
        Movimiento(9, "Verdulería", 18_700.0, Categoria.COMIDA, hoy),
    )
}
