package com.example.controlgastos.ui.navegacion

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.controlgastos.ui.GastosState
import com.example.controlgastos.ui.detalle.DetalleScreen
import com.example.controlgastos.ui.formulario.FormularioScreen
import com.example.controlgastos.ui.inicio.InicioScreen
import com.example.controlgastos.ui.movimientos.MovimientosScreen
import com.example.controlgastos.ui.rememberGastosState
import kotlinx.serialization.Serializable

// Rutas "type-safe": cada pantalla es un tipo. Los argumentos (como el id)
// son propiedades, así el compilador verifica que se pasen correctamente.
@Serializable object Inicio
@Serializable object Movimientos
@Serializable data class Detalle(val id: Long)
@Serializable object NuevoMovimiento

private data class Pestania(val etiqueta: String, val icono: ImageVector, val ruta: Any)

private val pestanias = listOf(
    Pestania("Inicio", Icons.Default.Home, Inicio),
    Pestania("Movimientos", Icons.AutoMirrored.Filled.List, Movimientos),
)

@Composable
fun GastosApp(
    navController: NavHostController = rememberNavController(),
    state: GastosState = rememberGastosState(),
) {
    val entradaActual by navController.currentBackStackEntryAsState()
    val destinoActual = entradaActual?.destination
    // La barra inferior solo se muestra en las pantallas principales.
    val mostrarBarra = pestanias.any { destinoActual?.hasRoute(it.ruta::class) == true }

    Scaffold(
        // Cada pantalla maneja sus propios márgenes del sistema con su Scaffold.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (mostrarBarra) {
                NavigationBar {
                    pestanias.forEach { pestania ->
                        NavigationBarItem(
                            selected = destinoActual?.hierarchy?.any { it.hasRoute(pestania.ruta::class) } == true,
                            onClick = { navController.irAPestania(pestania.ruta) },
                            icon = { Icon(pestania.icono, contentDescription = null) },
                            label = { Text(pestania.etiqueta) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Inicio,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable<Inicio> {
                InicioScreen(
                    movimientos = state.movimientos,
                    onNuevoMovimiento = { navController.navigate(NuevoMovimiento) },
                    onVerTodos = { navController.irAPestania(Movimientos) },
                    onMovimientoClick = { id -> navController.navigate(Detalle(id)) },
                )
            }
            composable<Movimientos> {
                MovimientosScreen(
                    movimientos = state.movimientos,
                    onNuevoMovimiento = { navController.navigate(NuevoMovimiento) },
                    onMovimientoClick = { id -> navController.navigate(Detalle(id)) },
                )
            }
            composable<Detalle> { entrada ->
                val detalle: Detalle = entrada.toRoute()
                DetalleScreen(
                    movimiento = state.buscar(detalle.id),
                    onVolver = { navController.popBackStack() },
                    onEliminar = { id ->
                        navController.popBackStack()
                        state.eliminar(id)
                    },
                )
            }
            composable<NuevoMovimiento> {
                FormularioScreen(
                    onGuardar = { movimiento ->
                        state.agregar(movimiento)
                        navController.popBackStack()
                    },
                    onCancelar = { navController.popBackStack() },
                )
            }
        }
    }
}

// Patrón estándar para pestañas: no apilar copias de la misma pantalla y
// conservar el estado (scroll, filtros) de cada pestaña al volver a ella.
private fun NavHostController.irAPestania(ruta: Any) {
    navigate(ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
