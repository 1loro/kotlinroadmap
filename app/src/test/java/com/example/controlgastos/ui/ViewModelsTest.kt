package com.example.controlgastos.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.controlgastos.domain.model.Gasto
import com.example.controlgastos.domain.model.Indicador
import com.example.controlgastos.domain.model.TipoIngreso
import com.example.controlgastos.domain.repository.ErrorApp
import com.example.controlgastos.domain.repository.EstadoSesion
import com.example.controlgastos.domain.repository.FalloIndicadores
import com.example.controlgastos.fakes.FakeCategoriasRepository
import com.example.controlgastos.fakes.FakeGastosRepository
import com.example.controlgastos.fakes.FakeIndicadoresRepository
import com.example.controlgastos.fakes.FakeIngresosRepository
import com.example.controlgastos.fakes.FakePreferenciasRepository
import com.example.controlgastos.fakes.FakeSesionRepository
import com.example.controlgastos.fakes.MainDispatcherRule
import com.example.controlgastos.ui.ajustes.AjustesViewModel
import com.example.controlgastos.ui.auth.AuthViewModel
import com.example.controlgastos.ui.comparar.CompararViewModel
import com.example.controlgastos.ui.gasto.GastoFormViewModel
import com.example.controlgastos.ui.gasto.TipoMovimiento
import com.example.controlgastos.ui.indicadores.IndicadoresUiState
import com.example.controlgastos.ui.indicadores.IndicadoresViewModel
import com.example.controlgastos.ui.inicio.InicioUiState
import com.example.controlgastos.ui.inicio.InicioViewModel
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
import java.time.YearMonth
import java.time.ZoneOffset

/** 10 de octubre de 2026, 12:00 en Santiago (UTC-3). */
private val RELOJ = Clock.fixed(Instant.parse("2026-10-10T15:00:00Z"), ZoneOffset.UTC)
private val HOY = LocalDate.of(2026, 10, 10)
private val OCTUBRE = YearMonth.of(2026, 10)

private fun gasto(id: Long, monto: Double, fecha: LocalDate, categoria: Long = 1, nota: String? = null) =
    Gasto(id, monto, nota, fecha, "app", categoria, if (categoria == 1L) "Comida" else "Hogar", "utensils")

/** Los uiState con WhileSubscribed solo calculan si alguien los observa: este colector hace de "pantalla". */
private fun TestScope.observar(estado: StateFlow<*>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { estado.collect {} }
}

class InicioViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun vm(gastos: FakeGastosRepository, indicadores: FakeIndicadoresRepository = FakeIndicadoresRepository()) =
        InicioViewModel(FakeSesionRepository(), gastos, FakeCategoriasRepository(), FakeIngresosRepository(), indicadores, RELOJ)

    @Test fun `calcula el panel del mes y lo compara con el mismo tramo del anterior`() = runTest {
        val gastos = FakeGastosRepository(
            listOf(
                gasto(1, 6000.0, HOY),
                gasto(2, 4000.0, HOY.minusDays(3), categoria = 2),
                gasto(3, 8000.0, LocalDate.of(2026, 9, 5)),
            ),
        )
        val vm = vm(gastos)
        observar(vm.uiState)

        val panel = (vm.uiState.value as InicioUiState.Listo).panel
        assertEquals(10000.0, panel.resumen.total, 0.0)
        assertEquals(25.0, panel.resumen.cambio!!, 0.001)
        assertEquals(listOf("Comida", "Hogar"), panel.porCategoria.map { it.nombre })
        assertEquals(listOf(1L, 2L, 3L), panel.recientes.map { it.id })
        assertTrue("pide el mes actual y el anterior", gastos.mesesRefrescados.containsAll(listOf(OCTUBRE, OCTUBRE.minusMonths(1))))
    }

    @Test fun `sin conexion muestra lo guardado con un aviso`() = runTest {
        val gastos = FakeGastosRepository(listOf(gasto(1, 5000.0, HOY))).apply { error = ErrorApp("Sin conexión a internet") }
        val vm = vm(gastos)
        observar(vm.uiState)

        val estado = vm.uiState.value as InicioUiState.Listo
        assertEquals("Sin conexión a internet. Mostrando datos guardados.", estado.aviso)
        assertEquals(5000.0, estado.panel.resumen.total, 0.0)
    }
}

class MovimientosViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun `muestra el mes de la ruta y al cambiar de mes lo pide al servidor`() = runTest {
        val gastos = FakeGastosRepository(listOf(gasto(1, 100.0, HOY), gasto(2, 200.0, LocalDate.of(2026, 9, 1))))
        val handle = SavedStateHandle(mapOf("mes" to "2026-09"))
        val vm = MovimientosViewModel(FakeSesionRepository(), gastos, FakeIngresosRepository(), RELOJ, handle)
        observar(vm.uiState)
        assertEquals(listOf(2L), vm.uiState.value.gastos.map { it.id })

        vm.cambiarMes(OCTUBRE)

        assertEquals(listOf(1L), vm.uiState.value.gastos.map { it.id })
        assertEquals("2026-10", handle.get<String>("mes"))
        assertTrue(OCTUBRE in gastos.mesesRefrescados)
    }

    @Test fun `no deja pasar del mes actual`() = runTest {
        val vm = MovimientosViewModel(FakeSesionRepository(), FakeGastosRepository(), FakeIngresosRepository(), RELOJ, SavedStateHandle(mapOf("mes" to "2027-01")))
        observar(vm.uiState)
        assertEquals(OCTUBRE, vm.uiState.value.mes)
    }
}

class GastoFormViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val gastos = FakeGastosRepository(listOf(gasto(7, 12500.0, HOY.minusDays(1), nota = "Almuerzo")))
    private val ingresos = FakeIngresosRepository()

    private fun vm(vararg args: Pair<String, Any>) =
        GastoFormViewModel(FakeSesionRepository(), gastos, FakeCategoriasRepository(), ingresos, RELOJ, SavedStateHandle(mapOf(*args)))

    @Test fun `valida con los mismos mensajes que la web`() {
        val vm = vm()
        vm.guardar()
        assertEquals("Ingresa un monto válido", vm.uiState.value.error)
        vm.cambiarMonto("4990")
        vm.guardar()
        assertEquals("Elige una categoría", vm.uiState.value.error)
    }

    @Test fun `registra un gasto con el monto leido como en la web`() {
        val vm = vm()
        vm.cambiarMonto("12500,5")
        assertEquals("12.500,5", vm.uiState.value.montoTexto)
        vm.elegirCategoria(2)
        vm.elegirFecha(HOY.minusDays(1))
        vm.cambiarNota("  Feria  ")

        vm.guardar()

        assertTrue(vm.uiState.value.terminado)
        val nuevo = gastos.guardados.last()
        assertEquals(12500.5, nuevo.monto, 0.0)
        assertEquals(2L, nuevo.categoriaId)
        assertEquals(HOY.minusDays(1), nuevo.fecha)
        assertEquals("Feria", nuevo.nota)
    }

    @Test fun `solo ofrece categorias activas y no permite fechas futuras`() = runTest {
        val vm = vm()
        assertEquals(listOf("Comida", "Hogar"), vm.uiState.value.categorias.map { it.nombre })
        vm.elegirFecha(HOY.plusDays(5))
        assertEquals(HOY, vm.uiState.value.fecha)
    }

    @Test fun `un ingreso se guarda solo en el telefono`() {
        val vm = vm("ingreso" to true)
        assertEquals(TipoMovimiento.INGRESO, vm.uiState.value.tipo)
        vm.cambiarMonto("850000")
        vm.guardar()
        assertEquals("Elige un tipo de ingreso", vm.uiState.value.error)

        vm.elegirTipoIngreso(TipoIngreso.SUELDO)
        vm.guardar()

        assertTrue(vm.uiState.value.terminado)
        assertEquals(850000.0, ingresos.guardados.single().monto, 0.0)
        assertEquals(1, gastos.guardados.size) // no tocó los gastos de la cuenta
    }

    @Test fun `editar carga el gasto y eliminar lo borra`() = runTest {
        val vm = vm("id" to 7L)
        val e = vm.uiState.value
        assertTrue(e.edicion)
        assertEquals("12.500", e.montoTexto)
        assertEquals("Almuerzo", e.nota)

        vm.eliminar()

        assertTrue(vm.uiState.value.terminado)
        assertTrue(gastos.guardados.isEmpty())
    }

    @Test fun `si el servidor rechaza, muestra su mensaje y no cierra`() {
        gastos.error = ErrorApp("Categoría inválida")
        val vm = vm()
        vm.cambiarMonto("1000")
        vm.elegirCategoria(1)
        vm.guardar()
        assertEquals("Categoría inválida", vm.uiState.value.error)
        assertFalse(vm.uiState.value.terminado)
    }
}

class CompararViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun `por defecto compara este mes con el anterior`() = runTest {
        val gastos = FakeGastosRepository(listOf(gasto(1, 300.0, HOY), gasto(2, 100.0, LocalDate.of(2026, 9, 3))))
        val vm = CompararViewModel(FakeSesionRepository(), gastos, RELOJ, SavedStateHandle())
        observar(vm.uiState)

        val e = vm.uiState.value
        assertEquals(OCTUBRE.minusMonths(1), e.a)
        assertEquals(OCTUBRE, e.b)
        assertEquals(100.0, e.totalA, 0.0)
        assertEquals(300.0, e.totalB, 0.0)
        assertEquals(200.0, e.filas.single().cambio!!, 0.001)
    }
}

class AuthViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun `registro valida antes de llamar al servidor`() {
        val vm = AuthViewModel(FakeSesionRepository(usuario = null))
        vm.cambiarNombre("Lorenzo")
        vm.cambiarEmail("malo")
        vm.registrarse()
        assertEquals("Email inválido", vm.uiState.value.error)
        vm.cambiarEmail("lorenzo@ejemplo.cl")
        vm.cambiarContrasena("corta")
        vm.registrarse()
        assertEquals("La contraseña debe tener al menos 8 caracteres", vm.uiState.value.error)
    }

    @Test fun `login normaliza el email y muestra el error del servidor`() {
        val sesion = FakeSesionRepository(usuario = null)
        val vm = AuthViewModel(sesion)
        vm.cambiarEmail(" Lorenzo@Ejemplo.CL ")
        vm.cambiarContrasena("secreta123")
        vm.iniciarSesion()
        assertEquals("lorenzo@ejemplo.cl", sesion.ultimoLogin?.first)
        assertTrue(sesion.estado.value is EstadoSesion.Activa)

        sesion.error = ErrorApp("Email o contraseña incorrectos")
        vm.iniciarSesion()
        assertEquals("Email o contraseña incorrectos", vm.uiState.value.error)
    }
}

class AjustesViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun `guardar perfil actualiza la cuenta`() = runTest {
        val sesion = FakeSesionRepository()
        val vm = AjustesViewModel(sesion, FakeCategoriasRepository(), FakePreferenciasRepository())
        observar(vm.uiState)
        vm.cambiarMoneda("USD")
        vm.guardarPerfil()
        assertEquals("Guardado", vm.uiState.value.perfil.mensaje)
        assertEquals("USD", (sesion.estado.value as EstadoSesion.Activa).usuario.moneda)
    }

    @Test fun `una categoria nueva necesita nombre`() = runTest {
        val categorias = FakeCategoriasRepository()
        val vm = AjustesViewModel(FakeSesionRepository(), categorias, FakePreferenciasRepository())
        observar(vm.uiState)
        vm.nuevaCategoria()
        vm.guardarCategoria()
        assertEquals("Ponle un nombre", vm.uiState.value.editor?.error)

        vm.cambiarNombreCategoria("Mascotas")
        vm.elegirIcono("pet")
        vm.guardarCategoria()

        assertNull(vm.uiState.value.editor)
        assertTrue(vm.uiState.value.categorias.any { it.nombre == "Mascotas" && it.icono == "pet" })
    }

    @Test fun `eliminar una categoria con gastos la archiva`() = runTest {
        val vm = AjustesViewModel(FakeSesionRepository(), FakeCategoriasRepository(), FakePreferenciasRepository())
        observar(vm.uiState)
        vm.eliminarCategoria(FakeCategoriasRepository.CATEGORIAS[0]) // Comida tiene 3 gastos
        assertTrue(vm.uiState.value.categorias.first { it.id == 1L }.archivada)
    }
}

class IndicadoresViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val dolar = Indicador("dolar", "Dólar observado", 990.35, Instant.EPOCH)

    @Test fun `primera carga pasa de Cargando a Exito`() = runTest {
        val red = CompletableDeferred<Unit>()
        val repo = FakeIndicadoresRepository(desdeLaRed = listOf(dolar)).apply { pausa = red }
        val vm = IndicadoresViewModel(repo)
        vm.uiState.test {
            assertEquals(IndicadoresUiState.Cargando, awaitItem())
            red.complete(Unit)
            assertEquals(IndicadoresUiState.Exito(listOf(dolar)), expectMostRecentItem())
        }
    }

    @Test fun `con cache y sin conexion muestra los datos con aviso`() = runTest {
        val vm = IndicadoresViewModel(FakeIndicadoresRepository(listOf(dolar), resultado = Result.failure(FalloIndicadores.SinConexion())))
        observar(vm.uiState)
        assertEquals(IndicadoresUiState.Exito(listOf(dolar), aviso = "Sin conexión a internet"), vm.uiState.value)
    }
}
