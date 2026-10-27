package com.example.controlgastos.ui.ajustes

import android.content.Intent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controlgastos.R
import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.Tema
import com.example.controlgastos.ui.componentes.Avatar
import com.example.controlgastos.ui.componentes.BotonContorno
import com.example.controlgastos.ui.componentes.BotonPrimario
import com.example.controlgastos.ui.componentes.CampoSeleccion
import com.example.controlgastos.ui.componentes.CampoTexto
import com.example.controlgastos.ui.componentes.EncabezadoPagina
import com.example.controlgastos.ui.componentes.EspacioSeccion
import com.example.controlgastos.ui.componentes.Etiqueta
import com.example.controlgastos.ui.componentes.GIcono
import com.example.controlgastos.ui.componentes.ICONOS_CATEGORIA
import com.example.controlgastos.ui.componentes.Pantalla
import com.example.controlgastos.ui.componentes.Segmentado
import com.example.controlgastos.ui.componentes.Tarjeta
import com.example.controlgastos.ui.componentes.TarjetaElevada
import com.example.controlgastos.ui.componentes.TituloSeccion
import com.example.controlgastos.ui.componentes.estilo
import com.example.controlgastos.ui.componentes.iconoCategoria
import com.example.controlgastos.ui.theme.Gastos

// Ajustes de la web (src/app/(app)/ajustes/page.tsx). La sección del Atajo de
// iPhone no aplica en Android; en su lugar, la de seguridad.

@Composable
fun AjustesRuta(avatar: ByteArray?, viewModel: AjustesViewModel = hiltViewModel()) {
    val e by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmarSalida by remember { mutableStateOf(false) }
    val usuario = e.usuario ?: return

    Pantalla {
        EncabezadoPagina("Tu cuenta,", "a tu manera.")

        TituloSeccion("Perfil", usuario.email)
        Tarjeta {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Avatar(usuario, avatar, tamano = 64.dp)
                Column {
                    Text(usuario.nombre, style = estilo(17.sp, FontWeight.SemiBold, (-0.02).em, Gastos.colores.fg))
                    Text("La foto se cambia desde la web", style = estilo(13.sp, color = Gastos.colores.muted))
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 24.dp), color = Gastos.colores.line)
            FormularioPerfil(e.perfil, viewModel)
        }

        EspacioSeccion()
        TituloSeccion("Apariencia", "Claro, oscuro o según tu equipo")
        Segmentado(
            listOf(Tema.CLARO to "Claro", Tema.OSCURO to "Oscuro", Tema.SISTEMA to "Sistema"),
            e.tema,
            viewModel::elegirTema,
            iconos = mapOf(Tema.CLARO to R.drawable.lucide_sun, Tema.OSCURO to R.drawable.lucide_moon, Tema.SISTEMA to R.drawable.lucide_monitor),
        )

        EspacioSeccion()
        TituloSeccion("Categorías", "Ordena tus gastos")
        Tarjeta { GestorCategorias(e, viewModel) }

        EspacioSeccion()
        TituloSeccion("Seguridad", "Tus datos, protegidos")
        TarjetaElevada(relleno = 20.dp) { Seguridad() }

        EspacioSeccion()
        BotonContorno("Cerrar sesión", { confirmarSalida = true }, Modifier.fillMaxWidth(), alto = 48.dp, icono = R.drawable.lucide_log_out, colorTexto = Gastos.colores.muted)

        Creditos()
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Se borra la copia local de tu cuenta. Tus ingresos quedan guardados en este teléfono.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    viewModel.cerrarSesion()
                }) { Text("Cerrar sesión", color = Gastos.colores.accent) }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun FormularioPerfil(p: FormPerfil, vm: AjustesViewModel) {
    val c = Gastos.colores
    val monedas = if (MONEDAS.any { it.first == p.moneda }) MONEDAS else listOf(p.moneda to p.moneda) + MONEDAS
    val zonas = (if (p.zona in ZONAS) ZONAS else listOf(p.zona) + ZONAS).map { it to it.replace('_', ' ') }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Etiqueta("Nombre")
            CampoTexto(p.nombre, vm::cambiarNombre, "Nombre", largoMaximo = 60)
        }
        Column {
            Etiqueta("Moneda")
            CampoSeleccion(p.moneda, monedas.map { (codigo, nombre) -> codigo to "$codigo · $nombre" }, vm::cambiarMoneda, "Moneda")
        }
        Column {
            Etiqueta("Zona horaria")
            CampoSeleccion(p.zona, zonas, vm::cambiarZona, "Zona horaria")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                p.mensaje.orEmpty(),
                modifier = Modifier.weight(1f),
                style = estilo(14.sp, if (p.esError) FontWeight.Medium else FontWeight.Normal, color = if (p.esError) c.accent else c.muted),
            )
            BotonPrimario("Guardar", vm::guardarPerfil, alto = 44.dp, cargando = p.guardando)
        }
    }
}

@Composable
private fun GestorCategorias(e: AjustesUiState, vm: AjustesViewModel) {
    val c = Gastos.colores
    val activas = e.categorias.filter { !it.archivada }
    val archivadas = e.categorias.filter { it.archivada }
    var verArchivadas by remember { mutableStateOf(false) }
    var aEliminar by remember { mutableStateOf<Categoria?>(null) }

    Column {
        e.avisoCategorias?.let { Text(it, modifier = Modifier.padding(bottom = 12.dp), style = estilo(14.sp, FontWeight.Medium, color = c.accent)) }
        activas.forEachIndexed { i, cat ->
            if (i > 0) HorizontalDivider(color = c.line)
            if (e.editor?.id == cat.id) {
                Editor(e.editor, vm)
            } else {
                FilaCategoria(cat, vm::editarCategoria, vm::alternarArchivada) { aEliminar = it }
            }
        }
        Spacer(Modifier.height(20.dp))
        if (e.editor != null && e.editor.id == null) {
            Editor(e.editor, vm)
        } else {
            Row(
                Modifier.fillMaxWidth().height(44.dp).clip(CircleShape).border(1.dp, c.line, CircleShape)
                    .clickable(onClick = vm::nuevaCategoria),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GIcono(R.drawable.lucide_plus, tamano = 16.dp, color = c.muted)
                Text("Nueva categoría", style = estilo(14.sp, FontWeight.Medium, color = c.muted))
            }
        }
        if (archivadas.isNotEmpty()) {
            Text(
                "Archivadas (${archivadas.size})",
                modifier = Modifier.padding(top = 20.dp).clickable { verArchivadas = !verArchivadas },
                style = estilo(14.sp, FontWeight.Medium, color = c.muted),
            )
            if (verArchivadas) {
                archivadas.forEach { FilaCategoria(it, vm::editarCategoria, vm::alternarArchivada) { cat -> aEliminar = cat } }
            }
        }
    }

    aEliminar?.let { cat ->
        AlertDialog(
            onDismissRequest = { aEliminar = null },
            title = { Text("¿Eliminar \"${cat.nombre}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.eliminarCategoria(cat)
                    aEliminar = null
                }) { Text("Eliminar", color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { aEliminar = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun FilaCategoria(cat: Categoria, onEditar: (Categoria) -> Unit, onArchivar: (Categoria) -> Unit, onEliminar: (Categoria) -> Unit) {
    val c = Gastos.colores
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (cat.archivada) c.subtle else c.accentSoft),
            Alignment.Center,
        ) { GIcono(iconoCategoria(cat.icono), tamano = 18.dp, color = if (cat.archivada) c.muted else c.accent) }
        Column(Modifier.weight(1f)) {
            Text(cat.nombre, style = estilo(15.sp, FontWeight.Medium, color = if (cat.archivada) c.muted else c.fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${cat.usos} ${if (cat.usos == 1) "gasto" else "gastos"}", style = estilo(13.sp, color = c.muted))
        }
        if (!cat.archivada) BotonChico(R.drawable.lucide_pencil, "Editar ${cat.nombre}") { onEditar(cat) }
        BotonChico(
            if (cat.archivada) R.drawable.lucide_archive_restore else R.drawable.lucide_archive,
            if (cat.archivada) "Restaurar ${cat.nombre}" else "Archivar ${cat.nombre}",
        ) { onArchivar(cat) }
        if (cat.usos == 0) BotonChico(R.drawable.lucide_trash_2, "Eliminar ${cat.nombre}") { onEliminar(cat) }
    }
}

@Composable
private fun BotonChico(icono: Int, descripcion: String, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClick).semantics { contentDescription = descripcion },
        Alignment.Center,
    ) { GIcono(icono, tamano = 16.dp, color = Gastos.colores.muted) }
}

/** Nombre + cuadrícula con los 24 íconos de la web. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Editor(editor: EditorCategoria, vm: AjustesViewModel) {
    val c = Gastos.colores
    val forma = RoundedCornerShape(16.dp)
    Column(Modifier.padding(vertical = 8.dp).clip(forma).background(c.subtle).border(1.dp, c.line, forma).padding(16.dp)) {
        CampoTexto(editor.nombre, vm::cambiarNombreCategoria, "Nombre de la categoría", placeholder = "Nombre de la categoría", largoMaximo = 40)
        FlowRow(Modifier.padding(top = 12.dp), maxItemsInEachRow = 6, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ICONOS_CATEGORIA.forEach { (clave, icono) ->
                val activo = clave == editor.icono
                Box(
                    Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (activo) c.accent else Color.Transparent)
                        .clickable { vm.elegirIcono(clave) }
                        .semantics {
                            contentDescription = clave
                            selected = activo
                        },
                    Alignment.Center,
                ) { GIcono(icono, tamano = 18.dp, color = if (activo) Color.White else c.muted) }
            }
        }
        editor.error?.let { Text(it, modifier = Modifier.padding(top = 12.dp), style = estilo(14.sp, FontWeight.Medium, color = c.accent)) }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            BotonContorno("Cancelar", vm::cancelarEdicion, alto = 40.dp, icono = R.drawable.lucide_x)
            BotonPrimario("Guardar", vm::guardarCategoria, alto = 40.dp, icono = R.drawable.lucide_check, cargando = editor.guardando)
        }
    }
}

@Composable
private fun Seguridad() {
    val c = Gastos.colores
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(
            R.drawable.lucide_fingerprint_pattern to "Bloqueo con huella o PIN al abrir la app y al volver después de 30 segundos.",
            R.drawable.lucide_lock to "Tu sesión y la copia local están cifradas con el Android Keystore.",
            R.drawable.lucide_eye_off to "Sin capturas de pantalla ni vista previa en Recientes (versión publicada).",
        ).forEach { (icono, texto) ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(c.accentSoft), Alignment.Center) {
                    GIcono(icono, tamano = 20.dp, color = c.accent)
                }
                Text(texto, modifier = Modifier.weight(1f), style = estilo(14.sp, color = c.muted, interlineado = 19.sp))
            }
        }
    }
}

/** "Hecho con ♥ por Lorenzo" (con el latido de la web) y enlaces. */
@Composable
private fun Creditos() {
    val c = Gastos.colores
    val contexto = LocalContext.current
    // Latido: dos pulsos seguidos y una pausa, como en globals.css.
    val escala by rememberInfiniteTransition(label = "latido").animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 1300
                1f at 0
                1.25f at 130
                1.05f at 260
                1.18f at 390
                1f at 520
            },
            RepeatMode.Restart,
        ),
        label = "escala",
    )
    Column(Modifier.fillMaxWidth().padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalDivider(color = c.line)
        Row(Modifier.padding(top = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Hecho con", style = estilo(17.sp, FontWeight.Medium, (-0.02).em, c.fg))
            GIcono(R.drawable.lucide_heart, Modifier.scale(escala), tamano = 18.dp, color = c.accent, descripcion = "amor")
            Text("por", style = estilo(17.sp, FontWeight.Medium, (-0.02).em, c.fg))
            Text("Lorenzo", style = estilo(17.sp, FontWeight.SemiBold, (-0.02).em, c.fg))
        }
        Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("LinkedIn" to "https://www.linkedin.com/in/lorenzoteixido/", "GitHub" to "https://github.com/1loro").forEach { (nombre, url) ->
                BotonContorno(nombre, { contexto.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }, alto = 40.dp)
            }
        }
    }
}
