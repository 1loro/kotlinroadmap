package com.example.controlgastos.ui.navegacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.controlgastos.domain.model.Usuario
import com.example.controlgastos.domain.repository.EstadoSesion
import com.example.controlgastos.ui.ajustes.AjustesRuta
import com.example.controlgastos.ui.auth.LoginScreen
import com.example.controlgastos.ui.auth.RegistroScreen
import com.example.controlgastos.ui.comparar.CompararRuta
import com.example.controlgastos.ui.gasto.GastoFormRuta
import com.example.controlgastos.ui.indicadores.IndicadoresRuta
import com.example.controlgastos.ui.inicio.InicioRuta
import com.example.controlgastos.ui.movimientos.MovimientosRuta
import com.example.controlgastos.ui.theme.Gastos
import kotlin.reflect.KClass

/**
 * Raíz de la interfaz: sin sesión, login/registro; con sesión, la app con
 * barra superior y navegación flotante (como el layout (app) de la web).
 * Cuando la sesión cambia (login, logout, token vencido) se cambia sola.
 */
@Composable
fun GastosApp(app: AppViewModel, modifier: Modifier = Modifier) {
    val estado by app.estado.collectAsStateWithLifecycle()
    val avatar by app.avatar.collectAsStateWithLifecycle()
    val c = Gastos.colores
    val alternarTema = { app.alternarTema(c.esOscuro) }

    Box(modifier.fillMaxSize().background(c.canvas)) {
        when (val e = estado) {
            is EstadoSesion.Activa -> AppConSesion(e.usuario, avatar, alternarTema)
            else -> AppSinSesion(alternarTema)
        }
    }
}

@Composable
private fun AppSinSesion(onTema: () -> Unit) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Login) {
        composable<Login> { LoginScreen(onIrARegistro = { nav.navigate(Registro) }, onTema = onTema) }
        composable<Registro> { RegistroScreen(onIrALogin = { nav.popBackStack() }, onTema = onTema) }
    }
}

@Composable
private fun AppConSesion(usuario: Usuario, avatar: ByteArray?, onTema: () -> Unit) {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            BarraSuperior(
                usuario = usuario,
                avatar = avatar,
                onLogo = { nav.irAPestania(Inicio) },
                onTema = onTema,
                onPerfil = { nav.irAPestania(Ajustes) },
            )
            NavHost(nav, startDestination = Inicio, modifier = Modifier.weight(1f)) {
                composable<Inicio> {
                    InicioRuta(
                        onNuevoGasto = { nav.navigate(NuevoGasto()) },
                        onNuevoIngreso = { nav.navigate(NuevoGasto(ingreso = true)) },
                        onVerMovimientos = { nav.irAPestania(Movimientos()) },
                        onComparar = { nav.irAPestania(Comparar) },
                        onIndicadores = { nav.navigate(Indicadores) },
                        onGasto = { nav.navigate(EditarGasto(it)) },
                    )
                }
                composable<Movimientos> {
                    MovimientosRuta(
                        onNuevoGasto = { nav.navigate(NuevoGasto()) },
                        onNuevoIngreso = { nav.navigate(NuevoGasto(ingreso = true)) },
                        onGasto = { nav.navigate(EditarGasto(it)) },
                    )
                }
                // Los argumentos de estas rutas llegan al ViewModel por su SavedStateHandle.
                composable<NuevoGasto> { GastoFormRuta(onCerrar = { nav.cerrar() }) }
                composable<EditarGasto> { GastoFormRuta(onCerrar = { nav.cerrar() }) }
                composable<Comparar> { CompararRuta() }
                composable<Ajustes> { AjustesRuta(avatar) }
                composable<Indicadores> { IndicadoresRuta(onVolver = { nav.cerrar() }) }
            }
        }
        BarraInferior(
            activo = entrada?.destination?.pestania(),
            onNavegar = { ruta -> nav.irAPestania(ruta) },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp),
        )
    }
}

/** Qué ícono de la barra se resalta: el detalle de un gasto cuenta como Movimientos. */
private fun NavDestination.pestania(): KClass<*>? = when {
    hasRoute(EditarGasto::class) -> Movimientos::class
    hasRoute(Indicadores::class) -> Inicio::class
    else -> ITEMS_NAVEGACION.map { it.ruta::class }.firstOrNull { hasRoute(it) }
}

// Patrón estándar para pestañas: no apilar copias de la misma pantalla y
// conservar el estado (scroll, mes elegido) de cada pestaña al volver a ella.
private fun NavHostController.irAPestania(ruta: Any) {
    navigate(ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Cierra la pantalla actual; si no hay a dónde volver, va al Inicio. */
private fun NavHostController.cerrar() {
    if (!popBackStack()) irAPestania(Inicio)
}
