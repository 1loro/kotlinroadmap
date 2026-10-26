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
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.controlgastos.R
import com.example.controlgastos.ui.cotizaciones.CotizacionesRuta
import com.example.controlgastos.ui.detalle.DetalleRuta
import com.example.controlgastos.ui.formulario.FormularioRuta
import com.example.controlgastos.ui.inicio.InicioRuta
import com.example.controlgastos.ui.movimientos.MovimientosRuta

private data class Pestania(val etiqueta: String, val ruta: Any, val icono: @Composable () -> Unit)

private val pestanias = listOf(
    Pestania("Inicio", Inicio) { Icon(Icons.Default.Home, contentDescription = null) },
    Pestania("Movimientos", Movimientos) { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
    Pestania("Dólar", Cotizaciones) { Icon(painterResource(R.drawable.ic_dolar), contentDescription = null) },
)

/**
 * Solo navegación: qué pantalla se muestra y cómo se pasa de una a otra.
 * Los datos los obtiene cada pantalla de su propio ViewModel, que Hilt crea
 * y asocia a la entrada de navegación (vive mientras la pantalla esté en la
 * pila, y sobrevive a la rotación).
 */
@Composable
fun GastosApp(navController: NavHostController = rememberNavController()) {
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
                            icon = pestania.icono,
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
                InicioRuta(
                    onNuevoMovimiento = { navController.navigate(NuevoMovimiento) },
                    onVerTodos = { navController.irAPestania(Movimientos) },
                    onMovimientoClick = { id -> navController.navigate(Detalle(id)) },
                )
            }
            composable<Movimientos> {
                MovimientosRuta(
                    onNuevoMovimiento = { navController.navigate(NuevoMovimiento) },
                    onMovimientoClick = { id -> navController.navigate(Detalle(id)) },
                )
            }
            composable<Cotizaciones> {
                CotizacionesRuta()
            }
            // El id de Detalle(id) llega al DetalleViewModel por su SavedStateHandle.
            composable<Detalle> {
                DetalleRuta(onVolver = { navController.popBackStack() })
            }
            composable<NuevoMovimiento> {
                FormularioRuta(onCerrar = { navController.popBackStack() })
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
