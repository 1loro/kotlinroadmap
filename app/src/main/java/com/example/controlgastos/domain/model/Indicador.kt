package com.example.controlgastos.domain.model

import java.time.Instant

/**
 * Indicador económico de Chile expresado en pesos: dólar observado, euro,
 * Unidad de Fomento (UF) o Unidad Tributaria Mensual (UTM).
 */
data class Indicador(
    val codigo: String,
    val nombre: String,
    /** Valor en pesos chilenos. */
    val valor: Double,
    /** Día al que corresponde el valor (lo publica el Banco Central). */
    val fecha: Instant,
) {
    companion object {
        const val DOLAR = "dolar"
    }
}
