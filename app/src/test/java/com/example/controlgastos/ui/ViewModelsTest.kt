package com.example.controlgastos.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.Cotizacion
import com.example.controlgastos.domain.model.Movimiento
import com.example.controlgastos.domain.model.TipoMovimiento
import com.example.controlgastos.domain.repository.FalloCotizaciones
import com.example.controlgastos.domain.usecase.AgregarMovimientoUseCase
import com.example.controlgastos.domain.usecase.ErrorValidacion
import com.example.controlgastos.domain.usecase.ObtenerPanelInicioUseCase
import com.example.controlgastos.fakes.FakeCotizacionesRepository
import com.example.controlgastos.fakes.FakeMovimientosRepository
import com.example.controlgastos.fakes.MainDispatcherRule
import com.example.controlgastos.ui.cotizaciones.CotizacionesUiState
import com.example.controlgastos.ui.cotizaciones.CotizacionesViewModel
import com.example.controlgastos.ui.detalle.DetalleUiState
import com.example.controlgastos.ui.detalle.DetalleViewModel
import com.example.controlgastos.ui.formulario.FormularioViewModel
import com.example.controlgastos.ui.inicio.InicioUiState
import com.example.controlgastos.ui.inicio.InicioViewModel
import com.example.controlgastos.ui.movimientos.Filtro
import com.example.controlgastos.ui.movimientos.MovimientosViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val HOY = LocalDate.of(2026, 10, 26)

private val EJEMPLO = listOf(
    Movimiento(1, "Sueldo", 1000.0, Categoria.SUELDO, HOY.minusDays(1)),
    Movimiento(2, "Alquiler", 400.0, Categoria.VIVIENDA, HOY),
)

/**
 * Los `uiState` usan WhileSubscribed: solo calculan mientras alguien los
 * observa (como la pantalla). En el test, este colector hace de "pantalla".
 */
private fun TestScope.observar(estado: StateFlow<*>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { estado.collect {} }
}

class InicioViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test fun `muestra el panel y pide cotizaciones al iniciar`() = runTest {
        val cotizaciones = FakeCotizacionesRepository(listOf(Cotizacion("oficial", "Oficial", 1.0, 200.0, Instant.EPOCH)))
        val vm = InicioViewModel(ObtenerPanelInicioUseCase(FakeMovimientosRepository(EJEMPLO), cotizaciones), cotizaciones)
        observar(vm.uiState)

        val panel = (vm.uiState.value as InicioUiState.Listo).panel
        assertEquals(600.0, panel.resumen.saldo, 0.0)
        assertEquals(3.0, panel.saldoEnDolares!!, 0.0)
        assertEquals(1, cotizaciones.llamadasActualizar)
    }

    @Test fun `si falla la red igual muestra el panel, sin saldo en dolares`() = runTest {
        val cotizaciones = FakeCotizacionesRepository(resultado = Result.failure(FalloCotizaciones.SinConexion()))
        val vm = InicioViewModel(ObtenerPanelInicioUseCase(FakeMovimientosRepository(EJEMPLO), cotizaciones), cotizaciones)
        observar(vm.uiState)

        val panel = (vm.uiState.value as InicioUiState.Listo).panel
        assertNull(panel.saldoEnDolares)
    }
}

class MovimientosViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test fun `filtrar muestra solo el tipo elegido y lo guarda en el SavedStateHandle`() = runTest {
        val handle = SavedStateHandle()
        val vm = MovimientosViewModel(FakeMovimientosRepository(EJEMPLO), handle)
        observar(vm.uiState)
        assertEquals(listOf("Alquiler", "Sueldo"), vm.uiState.value.movimientos.map { it.descripcion })

        vm.elegirFiltro(Filtro.INGRESOS)

        assertEquals(listOf("Sueldo"), vm.uiState.value.movimientos.map { it.descripcion })
        assertEquals(Filtro.INGRESOS, handle.get<Filtro>("filtro"))
    }

    @Test fun `el filtro se restaura si Android recrea el ViewModel`() = runTest {
        // Simula un SavedStateHandle restaurado tras la muerte del proceso.
        val vm = MovimientosViewModel(FakeMovimientosRepository(EJEMPLO), SavedStateHandle(mapOf("filtro" to Filtro.GASTOS)))
        observar(vm.uiState)

        assertEquals(Filtro.GASTOS, vm.uiState.value.filtro)
        assertEquals(listOf("Alquiler"), vm.uiState.value.movimientos.map { it.descripcion })
    }
}

class DetalleViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    // Así recibe el ViewModel el argumento de la ruta Detalle(id).
    private fun handle(id: Long) = SavedStateHandle(mapOf("id" to id))

    @Test fun `muestra el movimiento o avisa que no existe`() = runTest {
        val repo = FakeMovimientosRepository(EJEMPLO)

        val existe = DetalleViewModel(handle(2), repo).also { observar(it.uiState) }
        assertEquals(DetalleUiState.Listo(EJEMPLO[1]), existe.uiState.value)

        val noExiste = DetalleViewModel(handle(99), repo).also { observar(it.uiState) }
        assertEquals(DetalleUiState.NoExiste, noExiste.uiState.value)
    }

    @Test fun `eliminar borra, avisa y no muestra 'no existe' mientras tanto`() = runTest {
        val repo = FakeMovimientosRepository(EJEMPLO)
        val vm = DetalleViewModel(handle(2), repo)
        observar(vm.uiState)

        vm.eliminar()

        assertTrue(vm.eliminado.value)
        assertEquals(listOf(1L), repo.guardados.map { it.id })
        assertEquals(DetalleUiState.Listo(EJEMPLO[1]), vm.uiState.value)
    }
}

class FormularioViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val repo = FakeMovimientosRepository()
    private val relojFijo = Clock.fixed(Instant.parse("2026-10-26T15:00:00Z"), ZoneOffset.UTC)
    private val vm = FormularioViewModel(AgregarMovimientoUseCase(repo), relojFijo)

    @Test fun `los errores aparecen recien al intentar guardar y se corrigen en vivo`() {
        vm.cambiarDescripcion("Cine")
        assertTrue(vm.uiState.value.errores.isEmpty())

        vm.guardar()
        assertEquals(setOf(ErrorValidacion.MONTO_INVALIDO, ErrorValidacion.SIN_CATEGORIA), vm.uiState.value.errores)

        vm.cambiarMonto("1500")
        assertEquals(setOf(ErrorValidacion.SIN_CATEGORIA), vm.uiState.value.errores)
        assertFalse(vm.uiState.value.guardado)
    }

    @Test fun `el monto solo acepta numeros y un separador`() {
        vm.cambiarMonto("12,5")
        vm.cambiarMonto("12,5a")
        vm.cambiarMonto("12,5,3")
        assertEquals("12,5", vm.uiState.value.montoTexto)
    }

    @Test fun `cambiar el tipo descarta una categoria que ya no corresponde`() {
        vm.elegirCategoria(Categoria.COMIDA)
        vm.cambiarTipo(TipoMovimiento.INGRESO)
        assertNull(vm.uiState.value.categoria)
        assertTrue(Categoria.SUELDO in vm.uiState.value.categoriasDisponibles)
    }

    @Test fun `guardar datos validos persiste con la fecha del reloj y marca guardado`() {
        vm.cambiarDescripcion("Cine")
        vm.cambiarMonto("1500,5")
        vm.elegirCategoria(Categoria.OCIO)

        vm.guardar()

        assertTrue(vm.uiState.value.guardado)
        val guardado = repo.guardados.single()
        assertEquals(1500.5, guardado.monto, 0.0)
        assertEquals(HOY, guardado.fecha)
    }
}

class CotizacionesViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val oficial = Cotizacion("oficial", "Oficial", 1510.0, 1560.0, Instant.EPOCH)

    // Turbine: `test { }` recolecta el Flow y permite verificar la SECUENCIA
    // de estados, uno por uno, con awaitItem().
    @Test fun `primera carga pasa de Cargando a Exito`() = runTest {
        val red = CompletableDeferred<Unit>()
        val repo = FakeCotizacionesRepository(desdeLaRed = listOf(oficial)).apply { pausa = red }
        val vm = CotizacionesViewModel(repo)

        vm.uiState.test {
            assertEquals(CotizacionesUiState.Cargando, awaitItem())
            red.complete(Unit) // "llega" la respuesta
            assertEquals(CotizacionesUiState.Exito(listOf(oficial)), expectMostRecentItem())
        }
    }

    @Test fun `con cache y sin conexion muestra los datos con aviso`() = runTest {
        val repo = FakeCotizacionesRepository(listOf(oficial), resultado = Result.failure(FalloCotizaciones.SinConexion()))
        val vm = CotizacionesViewModel(repo)
        observar(vm.uiState)

        assertEquals(CotizacionesUiState.Exito(listOf(oficial), aviso = "Sin conexión a internet"), vm.uiState.value)
    }

    @Test fun `no lanza una segunda peticion si ya hay una en curso`() = runTest {
        val red = CompletableDeferred<Unit>()
        val repo = FakeCotizacionesRepository().apply { pausa = red }
        val vm = CotizacionesViewModel(repo) // init ya lanzó una

        vm.actualizar()
        vm.actualizar()
        assertEquals(1, repo.llamadasActualizar)

        red.complete(Unit)
        vm.actualizar()
        assertEquals(2, repo.llamadasActualizar)
    }
}
