package com.example.controlgastos.ui.componentes

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.controlgastos.R
import com.example.controlgastos.ui.theme.Gastos

/**
 * Íconos de categoría: las MISMAS claves que guarda la web (src/lib/icons.ts),
 * así una categoría creada en el iPhone se ve igual aquí. Los drawables
 * lucide_* se generaron desde lucide-react (licencia ISC).
 */
val ICONOS_CATEGORIA: Map<String, Int> = linkedMapOf(
    "utensils" to R.drawable.lucide_utensils,
    "cart" to R.drawable.lucide_shopping_cart,
    "car" to R.drawable.lucide_car,
    "fuel" to R.drawable.lucide_fuel,
    "house" to R.drawable.lucide_house,
    "zap" to R.drawable.lucide_zap,
    "wifi" to R.drawable.lucide_wifi,
    "health" to R.drawable.lucide_heart_pulse,
    "shirt" to R.drawable.lucide_shirt,
    "bag" to R.drawable.lucide_shopping_bag,
    "film" to R.drawable.lucide_film,
    "music" to R.drawable.lucide_music,
    "plane" to R.drawable.lucide_plane,
    "gift" to R.drawable.lucide_gift,
    "education" to R.drawable.lucide_graduation_cap,
    "book" to R.drawable.lucide_book_open,
    "coffee" to R.drawable.lucide_coffee,
    "gym" to R.drawable.lucide_dumbbell,
    "phone" to R.drawable.lucide_smartphone,
    "pet" to R.drawable.lucide_paw_print,
    "baby" to R.drawable.lucide_baby,
    "tools" to R.drawable.lucide_wrench,
    "beauty" to R.drawable.lucide_sparkles,
    "receipt" to R.drawable.lucide_receipt,
)

/** Íconos extra para los tipos de ingreso (solo existen en Android). */
private val ICONOS_INGRESO = mapOf(
    "briefcase" to R.drawable.lucide_briefcase,
    "tag" to R.drawable.lucide_tag,
    "piggy" to R.drawable.lucide_piggy_bank,
)

/** Ícono de una clave; si no se conoce (versión más nueva de la web), el recibo. */
@DrawableRes
fun iconoCategoria(clave: String): Int = ICONOS_CATEGORIA[clave] ?: ICONOS_INGRESO[clave] ?: R.drawable.lucide_receipt

/** Ícono Lucide con el color del texto actual (o el indicado). */
@Composable
fun GIcono(
    @DrawableRes icono: Int,
    modifier: Modifier = Modifier,
    tamano: Dp = 20.dp,
    color: Color = Gastos.colores.fg,
    descripcion: String? = null,
) {
    Icon(painterResource(icono), contentDescription = descripcion, modifier = modifier.size(tamano), tint = color)
}
