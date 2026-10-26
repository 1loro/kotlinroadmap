package com.example.controlgastos.ui.navegacion

import kotlinx.serialization.Serializable

// Rutas "type-safe": cada pantalla es un tipo. Los argumentos (como el id)
// son propiedades, así el compilador verifica que se pasen correctamente.
@Serializable object Inicio
@Serializable object Movimientos
@Serializable object Cotizaciones
@Serializable data class Detalle(val id: Long)
@Serializable object NuevoMovimiento
