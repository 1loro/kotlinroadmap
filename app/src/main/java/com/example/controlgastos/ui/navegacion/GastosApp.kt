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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.controlgastos.R
import com.example.controlgastos.data.AppContainer
import com.example.controlgastos.model.Cotizacion
import com.example.controlgastos.model.Movimiento
import com.example.controlgastos.ui.cotizaciones.CotizacionesRuta
import com.example.controlgastos.ui.detalle.DetalleScreen
import com.example.controlgastos.ui.formulario.FormularioScreen
import com.example.controlgastos.ui.inicio.InicioScreen
import com.example.controlgastos.ui.movimientos.MovimientosScreen
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// Rutas "type-safe": cada pantalla es un tipo. Los argumentos (como el id)
// son propiedades, así el compilador verifica que se pasen correctamente.
@Serializable object Inicio
@Serializable object Movimientos
@Serializable object Cotizaciones
@Serializable data class Detalle(val id: Long)
@Serializable object NuevoMovimiento

private data class Pestania(val etiqueta: String, val ruta: Any, val icono: @Composable () -> Unit)

private val pestanias = listOf(
    Pestania("Inicio", Inicio) { Icon(Icons.Default.Home, contentDescription = null) },
    Pestania("Movimientos", Movimientos) { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
    Pestania("Dólar", Cotizaciones) { Icon(painterResource(R.drawable.ic_dolar), contentDescription = null) },
)

// Distingue "Room todavía no respondió" (null) de "respondió que no existe".
private data class Consulta(val movimiento: Movimiento?)

@Composable
fun GastosApp(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    val movimientosRepo = container.movimientosRepository
    val cotizacionesRepo = container.cotizacionesRepository

    // Scope atado a GastosApp (no a una pantalla): una escritura en la BD no
    // se cancela si el usuario sale de la pantalla que la inició.
    val scope = rememberCoroutineScope()

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
            // Cada destino lee SUS datos dentro de su propio bloque. Si se leyeran
            // arriba del NavHost y se pasaran por estas lambdas, cada dato nuevo
            // cambiaría el grafo de navegación entero.
            //
            // collectAsStateWithLifecycle convierte un Flow en estado de Compose y
            // deja de escucharlo cuando la app pasa a segundo plano.
            composable<Inicio> {
                val movimientos by movimientosRepo.movimientos.collectAsStateWithLifecycle(initialValue = emptyList())
                val cotizaciones by cotizacionesRepo.cotizaciones.collectAsStateWithLifecycle(initialValue = emptyList())
                InicioScreen(
                    movimientos = movimientos,
                    dolarOficial = cotizaciones.find { it.casa == Cotizacion.OFICIAL },
                    onNuevoMovimiento = { navController.navigate(NuevoMovimiento) },
                    onVerTodos = { navController.irAPestania(Movimientos) },
                    onMovimientoClick = { id -> navController.navigate(Detalle(id)) },
                )
            }
            composable<Movimientos> {
                val movimientos by movimientosRepo.movimientos.collectAsStateWithLifecycle(initialValue = emptyList())
                MovimientosScreen(
                    movimientos = movimientos,
                    onNuevoMovimiento = { navController.navigate(NuevoMovimiento) },
                    onMovimientoClick = { id -> navController.navigate(Detalle(id)) },
                )
            }
            composable<Cotizaciones> {
                CotizacionesRuta(cotizacionesRepo)
            }
            composable<Detalle> { entrada ->
                val id = entrada.toRoute<Detalle>().id
                // remember(id): crear el Flow una sola vez, no en cada recomposición.
                val consulta by remember(id) { movimientosRepo.movimiento(id).map(::Consulta) }
                    .collectAsStateWithLifecycle(initialValue = null)
                DetalleScreen(
                    movimiento = consulta?.movimiento,
                    cargando = consulta == null,
                    onVolver = { navController.popBackStack() },
                    onEliminar = { idEliminar ->
                        navController.popBackStack()
                        scope.launch { movimientosRepo.eliminar(idEliminar) }
                    },
                )
            }
            composable<NuevoMovimiento> {
                FormularioScreen(
                    onGuardar = { movimiento ->
                        scope.launch { movimientosRepo.agregar(movimiento) }
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
