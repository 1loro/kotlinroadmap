package com.example.controlgastos.ui

import com.example.controlgastos.ui.seguridad.BloqueoViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Reloj que el test puede adelantar a mano. */
private class RelojManual(var ahora: Instant = Instant.parse("2026-10-26T12:00:00Z")) : Clock() {
    override fun instant(): Instant = ahora
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    fun adelantar(ms: Long) {
        ahora = ahora.plusMillis(ms)
    }
}

class BloqueoViewModelTest {

    private val reloj = RelojManual()
    private val vm = BloqueoViewModel(reloj)

    @Test fun `arranca bloqueada y se desbloquea al autenticarse`() {
        assertTrue(vm.bloqueada.value)
        vm.desbloqueada()
        assertFalse(vm.bloqueada.value)
    }

    @Test fun `una salida breve no vuelve a bloquear`() {
        vm.desbloqueada()
        vm.pasoASegundoPlano()
        reloj.adelantar(BloqueoViewModel.TIEMPO_GRACIA_MS - 1)
        vm.volvioAPrimerPlano()
        assertFalse(vm.bloqueada.value)
    }

    @Test fun `volver despues del tiempo de gracia bloquea de nuevo`() {
        vm.desbloqueada()
        vm.pasoASegundoPlano()
        reloj.adelantar(BloqueoViewModel.TIEMPO_GRACIA_MS)
        vm.volvioAPrimerPlano()
        assertTrue(vm.bloqueada.value)
    }

    @Test fun `el error se muestra y se limpia al desbloquear`() {
        vm.falloAutenticacion("Demasiados intentos")
        assertEquals("Demasiados intentos", vm.error.value)
        assertTrue(vm.bloqueada.value)

        vm.desbloqueada()
        assertNull(vm.error.value)
    }

    @Test fun `sin metodo de bloqueo en el dispositivo no se puede exigir`() {
        vm.sinMetodoDisponible()
        assertFalse(vm.bloqueada.value)
    }
}
