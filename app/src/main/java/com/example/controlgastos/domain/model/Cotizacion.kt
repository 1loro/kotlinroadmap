package com.example.controlgastos.domain.model

import java.time.Instant

// Cotización del dólar en Argentina (oficial, blue, MEP...), en pesos.
data class Cotizacion(
    val casa: String,
    val nombre: String,
    val compra: Double?,
    val venta: Double?,
    val actualizada: Instant,
) {
    companion object {
        const val OFICIAL = "oficial"
    }
}
