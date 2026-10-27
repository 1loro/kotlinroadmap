package com.example.controlgastos.ui.componentes

import android.graphics.BitmapFactory
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.controlgastos.R
import com.example.controlgastos.domain.model.Usuario
import com.example.controlgastos.domain.nombreMesCorto
import com.example.controlgastos.domain.etiquetaMes
import com.example.controlgastos.ui.theme.Gastos
import com.example.controlgastos.ui.theme.Geist
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

// Componentes de gastos. — equivalentes a src/components/ui.tsx de la web.

/** Estilo de texto con Geist. `tabular`: cifras de ancho fijo (clase `tabular`). */
fun estilo(
    tamano: TextUnit,
    peso: FontWeight = FontWeight.Normal,
    tracking: TextUnit = TextUnit.Unspecified,
    color: Color = Color.Unspecified,
    tabular: Boolean = false,
    interlineado: TextUnit = TextUnit.Unspecified,
) = TextStyle(
    fontFamily = Geist,
    fontSize = tamano,
    fontWeight = peso,
    letterSpacing = tracking,
    color = color,
    lineHeight = interlineado,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

/** Ícono de la app + "gastos." con el punto rojo. */
@Composable
fun Logo(grande: Boolean = false, conNombre: Boolean = true) {
    val c = Gastos.colores
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val tam = if (grande) 48.dp else 36.dp
        val forma = RoundedCornerShape(if (grande) 14.dp else 11.dp)
        Image(
            painterResource(R.drawable.icono_app),
            contentDescription = null,
            modifier = Modifier.size(tam).clip(forma).border(1.dp, c.line, forma),
        )
        if (conNombre) {
            Text(
                buildAnnotatedString {
                    append("gastos")
                    withStyle(SpanStyle(color = c.accent)) { append(".") }
                },
                style = estilo(if (grande) 24.sp else 19.sp, FontWeight.SemiBold, (-0.04).em, c.fg),
            )
        }
    }
}

/** Foto de perfil o, si no hay, las iniciales sobre rosado. */
@Composable
fun Avatar(usuario: Usuario, foto: ByteArray?, modifier: Modifier = Modifier, tamano: Dp = 40.dp) {
    val c = Gastos.colores
    val bitmap = remember(foto) { foto?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    val base = modifier.size(tamano).clip(CircleShape).border(1.dp, c.line, CircleShape)
    if (bitmap != null) {
        Image(bitmap, contentDescription = usuario.nombre, modifier = base, contentScale = ContentScale.Crop)
    } else {
        Box(base.background(c.accentSoft).semantics { contentDescription = usuario.nombre }, Alignment.Center) {
            Text(usuario.iniciales, style = estilo((tamano.value * 0.38f).sp, FontWeight.SemiBold, color = c.accent))
        }
    }
}

/** Titular en dos líneas: la segunda en rojo ("Este mes llevas / $120.000."). */
@Composable
fun Titular(texto: String, modifier: Modifier = Modifier, acento: String? = null, grande: Boolean = false) {
    val c = Gastos.colores
    val tam = if (grande) 44.sp else 34.sp
    Text(
        buildAnnotatedString {
            append(texto)
            if (acento != null) {
                append("\n")
                withStyle(SpanStyle(color = c.accent, fontFeatureSettings = "tnum")) { append(acento) }
            }
        },
        modifier = modifier,
        style = estilo(tam, FontWeight.SemiBold, (-0.055).em, c.fg, interlineado = if (grande) 42.sp else 34.sp),
    )
}

/** Etiqueta redondeada con punto rojo. */
@Composable
fun Pastilla(texto: String, punto: Boolean = true) {
    val c = Gastos.colores
    Row(
        Modifier.height(32.dp).clip(CircleShape).background(c.surface).border(1.dp, c.line, CircleShape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (punto) Box(Modifier.size(6.dp).clip(CircleShape).background(c.accent))
        Text(texto, style = estilo(13.sp, FontWeight.Medium, color = c.fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Encabezado de página: pastilla opcional, titular de dos líneas, descripción y acción. */
@Composable
fun EncabezadoPagina(
    titulo: String,
    acento: String? = null,
    pastilla: String? = null,
    descripcion: String? = null,
    accion: (@Composable () -> Unit)? = null,
) {
    val c = Gastos.colores
    Column(Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 32.dp)) {
        if (pastilla != null) {
            Pastilla(pastilla)
            Spacer(Modifier.height(20.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Titular(titulo, Modifier.weight(1f), acento = acento)
            if (accion != null) {
                Spacer(Modifier.width(16.dp))
                accion()
            }
        }
        if (descripcion != null) {
            Text(
                descripcion,
                modifier = Modifier.padding(top = 16.dp),
                style = estilo(16.sp, color = c.muted, interlineado = 26.sp),
            )
        }
    }
}

/** "Tu evolución / Últimos 6 meses" con acción a la derecha. */
@Composable
fun TituloSeccion(titulo: String, kicker: String? = null, accion: (@Composable () -> Unit)? = null) {
    val c = Gastos.colores
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            if (kicker != null) Text(kicker, style = estilo(13.sp, color = c.muted), modifier = Modifier.padding(bottom = 4.dp))
            Text(titulo, style = estilo(22.sp, FontWeight.SemiBold, (-0.04).em, c.fg))
        }
        accion?.invoke()
    }
}

/** Tarjeta (`.card`): superficie, borde y radio de 24 dp. */
@Composable
fun Tarjeta(modifier: Modifier = Modifier, relleno: Dp = 20.dp, contenido: @Composable ColumnScope.() -> Unit) {
    val c = Gastos.colores
    val forma = RoundedCornerShape(24.dp)
    Column(
        modifier.fillMaxWidth().clip(forma).background(c.surface).border(1.dp, c.line, forma).padding(relleno),
        content = contenido,
    )
}

/** Tarjeta elevada (`.card-raised`): radio de 28 dp y sombra con tinte rojo. */
@Composable
fun TarjetaElevada(modifier: Modifier = Modifier, relleno: Dp = 8.dp, contenido: @Composable ColumnScope.() -> Unit) {
    val c = Gastos.colores
    val forma = RoundedCornerShape(28.dp)
    Column(
        modifier.fillMaxWidth()
            .shadow(24.dp, forma, ambientColor = c.sombra, spotColor = c.sombra)
            .clip(forma).background(c.surface).border(1.dp, c.line, forma).padding(relleno),
        content = contenido,
    )
}

/** Botón principal (`.btn-primary`): píldora negra (blanca en oscuro). */
@Composable
fun BotonPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alto: Dp = 48.dp,
    icono: Int? = null,
    cargando: Boolean = false,
    habilitado: Boolean = true,
) {
    val c = Gastos.colores
    Row(
        modifier.height(alto).clip(CircleShape).background(c.primary)
            .alpha(if (habilitado && !cargando) 1f else 0.6f)
            .clickable(enabled = habilitado && !cargando, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cargando) {
            CircularProgressIndicator(Modifier.size(18.dp), color = c.primaryFg, strokeWidth = 2.dp)
        } else if (icono != null) {
            GIcono(icono, tamano = 18.dp, color = c.primaryFg)
        }
        Text(texto, style = estilo(if (alto >= 52.dp) 16.sp else 15.sp, FontWeight.SemiBold, color = c.primaryFg))
    }
}

/** Botón secundario (`.btn-outline`). */
@Composable
fun BotonContorno(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alto: Dp = 36.dp,
    icono: Int? = null,
    iconoFinal: Int? = null,
    colorTexto: Color = Gastos.colores.fg,
) {
    val c = Gastos.colores
    Row(
        modifier.height(alto).clip(CircleShape).background(c.surface).border(1.dp, c.line, CircleShape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icono != null) GIcono(icono, tamano = 15.dp, color = colorTexto)
        Text(texto, style = estilo(if (alto >= 44.dp) 15.sp else 13.sp, FontWeight.Medium, color = colorTexto))
        if (iconoFinal != null) GIcono(iconoFinal, tamano = 14.dp, color = colorTexto)
    }
}

/** Botón redondo con borde (cerrar, tema...). */
@Composable
fun BotonIcono(icono: Int, descripcion: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Gastos.colores
    Box(
        modifier.size(40.dp).clip(CircleShape).background(c.surface).border(1.dp, c.line, CircleShape)
            .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = descripcion },
        Alignment.Center,
    ) { GIcono(icono, tamano = 18.dp) }
}

/** Ícono de categoría sobre rosado (CategoryIcon). */
@Composable
fun CajaCategoria(icono: String, chica: Boolean = false, activa: Boolean = false, colorAcento: Color = Gastos.colores.accent) {
    val c = Gastos.colores
    val forma = RoundedCornerShape(if (chica) 12.dp else 16.dp)
    Box(
        Modifier.size(if (chica) 36.dp else 44.dp).clip(forma)
            .background(if (activa) colorAcento else if (colorAcento == c.accent) c.accentSoft else colorAcento.copy(alpha = 0.12f)),
        Alignment.Center,
    ) {
        GIcono(iconoCategoria(icono), tamano = if (chica) 17.dp else 20.dp, color = if (activa) Color.White else colorAcento)
    }
}

/** Selector de mes: ‹ Octubre de 2026 ›. No deja pasar del mes actual. */
@Composable
fun SelectorMes(mes: YearMonth, maximo: YearMonth, onCambiar: (YearMonth) -> Unit, compacto: Boolean = false) {
    val c = Gastos.colores
    val puedeAvanzar = mes < maximo
    Row(
        Modifier.height(40.dp).clip(CircleShape).background(c.surface).border(1.dp, c.line, CircleShape).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).clickable { onCambiar(mes.minusMonths(1)) }
                .semantics { contentDescription = "Mes anterior" },
            Alignment.Center,
        ) { GIcono(R.drawable.lucide_chevron_left, tamano = 16.dp) }
        Text(
            if (compacto) "${nombreMesCorto(mes)} ${mes.year % 100}" else etiquetaMes(mes),
            modifier = Modifier.defaultMinSize(minWidth = if (compacto) 64.dp else 128.dp),
            style = estilo(14.sp, FontWeight.Medium, color = c.fg).copy(textAlign = TextAlign.Center),
        )
        Box(
            Modifier.size(32.dp).clip(CircleShape).clickable(enabled = puedeAvanzar) { onCambiar(mes.plusMonths(1)) }
                .semantics { contentDescription = "Mes siguiente" },
            Alignment.Center,
        ) { GIcono(R.drawable.lucide_chevron_right, tamano = 16.dp, color = if (puedeAvanzar) c.fg else c.muted.copy(alpha = 0.4f)) }
    }
}

/** Estado vacío con ícono, título, texto y acción. */
@Composable
fun EstadoVacio(icono: Int, titulo: String, texto: String, accion: (@Composable () -> Unit)? = null) {
    val c = Gastos.colores
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(c.accentSoft), Alignment.Center) {
            GIcono(icono, tamano = 24.dp, color = c.accent)
        }
        Spacer(Modifier.height(16.dp))
        Text(titulo, style = estilo(17.sp, FontWeight.SemiBold, (-0.02).em, c.fg))
        Text(
            texto,
            modifier = Modifier.padding(top = 4.dp).width(288.dp),
            style = estilo(14.sp, color = c.muted).copy(textAlign = TextAlign.Center),
        )
        if (accion != null) {
            Spacer(Modifier.height(24.dp))
            accion()
        }
    }
}

/** Bloque gris que late mientras carga (Skeleton). */
@Composable
fun Esqueleto(modifier: Modifier = Modifier, radio: Dp = 24.dp) {
    val pulso by rememberInfiniteTransition(label = "esqueleto")
        .animateFloat(1f, 0.5f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "alpha")
    Box(modifier.alpha(pulso).clip(RoundedCornerShape(radio)).background(Gastos.colores.subtle))
}

/** Mensaje de error del formulario (borde y fondo rojizos). */
@Composable
fun MensajeError(texto: String, modifier: Modifier = Modifier) {
    val c = Gastos.colores
    val forma = RoundedCornerShape(16.dp)
    Text(
        texto,
        modifier = modifier.fillMaxWidth().clip(forma).background(c.accentSoft)
            .border(1.dp, c.accent.copy(alpha = 0.3f), forma).padding(horizontal = 16.dp, vertical = 12.dp),
        style = estilo(14.sp, FontWeight.Medium, color = c.accent),
    )
}

/** Variación porcentual: ↗ rojo si subió el gasto, ↘ verde si bajó, "Nuevo" sin base. */
@Composable
fun Delta(cambio: Double?) {
    val c = Gastos.colores
    if (cambio == null) {
        Text(
            "Nuevo",
            modifier = Modifier.clip(CircleShape).background(c.subtle).padding(horizontal = 8.dp, vertical = 2.dp),
            style = estilo(12.sp, FontWeight.Medium, color = c.muted),
        )
        return
    }
    val redondeado = cambio.roundToInt()
    val (fondo, texto, icono) = when {
        redondeado > 0 -> Triple(c.accentSoft, c.accent, R.drawable.lucide_arrow_up_right)
        redondeado < 0 -> Triple(c.positive.copy(alpha = 0.12f), c.positive, R.drawable.lucide_arrow_down_right)
        else -> Triple(c.subtle, c.muted, R.drawable.lucide_minus)
    }
    Row(
        Modifier.clip(CircleShape).background(fondo).padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        GIcono(icono, tamano = 14.dp, color = texto)
        Text("${abs(redondeado)}%", style = estilo(12.sp, FontWeight.SemiBold, color = texto, tabular = true))
    }
}

/** Selector segmentado (como Claro / Oscuro / Sistema en Ajustes). */
@Composable
fun <T> Segmentado(opciones: List<Pair<T, String>>, elegido: T, onElegir: (T) -> Unit, iconos: Map<T, Int> = emptyMap()) {
    val c = Gastos.colores
    Row(
        Modifier.fillMaxWidth().clip(CircleShape).background(c.subtle).border(1.dp, c.line, CircleShape).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        opciones.forEach { (valor, etiqueta) ->
            val activo = valor == elegido
            Surface(
                onClick = { onElegir(valor) },
                modifier = Modifier.weight(1f).height(40.dp),
                shape = CircleShape,
                color = if (activo) c.surface else Color.Transparent,
                shadowElevation = if (activo) 1.dp else 0.dp,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    iconos[valor]?.let { GIcono(it, tamano = 16.dp, color = if (activo) c.fg else c.muted) }
                    Text(etiqueta, style = estilo(14.sp, FontWeight.Medium, color = if (activo) c.fg else c.muted))
                }
            }
        }
    }
}

/** Fila de contenido con paddings uniformes de la app (px-5). */
@Composable
fun FilaContenido(modifier: Modifier = Modifier, contenido: @Composable RowScope.() -> Unit) =
    Row(modifier.fillMaxWidth().padding(PaddingValues(horizontal = 20.dp)), content = contenido)

/** Borde superior a todo el ancho (divisor entre secciones). */
@Composable
fun Divisor(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).background(Gastos.colores.line))

/** Contorno de 1 dp con el color de línea. */
@Composable
fun bordeLinea() = BorderStroke(1.dp, Gastos.colores.line)
