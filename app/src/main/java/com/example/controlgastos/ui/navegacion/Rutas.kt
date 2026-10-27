package com.example.controlgastos.ui.navegacion

import kotlinx.serialization.Serializable

// Rutas "type-safe": cada pantalla es un tipo. Los argumentos (como el id)
// son propiedades, así el compilador verifica que se pasen correctamente.
// Son las mismas pantallas que la web: /, /movimientos, /nuevo, /comparar...

// Sin sesión
@Serializable object Login
@Serializable object Registro

// Con sesión
@Serializable object Inicio
/** @param mes "AAAA-MM", o vacío para el mes actual. */
@Serializable data class Movimientos(val mes: String = "")
/** @param ingreso true: abre el formulario en modo ingreso. */
@Serializable data class NuevoGasto(val ingreso: Boolean = false)
@Serializable data class EditarGasto(val id: Long)
@Serializable object Comparar
@Serializable object Ajustes
@Serializable object Indicadores
