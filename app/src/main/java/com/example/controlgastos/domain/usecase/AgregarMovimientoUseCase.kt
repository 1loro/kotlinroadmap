package com.example.controlgastos.domain.usecase

import com.example.controlgastos.domain.model.Categoria
import com.example.controlgastos.domain.model.Movimiento
import com.example.controlgastos.domain.repository.MovimientosRepository
import java.time.LocalDate
import javax.inject.Inject

/** Datos tal como los escribe el usuario en el formulario. */
data class NuevoMovimiento(
    val descripcion: String,
    val montoTexto: String,
    val categoria: Categoria?,
    val fecha: LocalDate,
)

enum class ErrorValidacion { DESCRIPCION_VACIA, MONTO_INVALIDO, SIN_CATEGORIA }

/**
 * Caso de uso: una acción de negocio con nombre propio. Contiene las REGLAS
 * (qué es un movimiento válido), que antes estaban mezcladas en la pantalla.
 * Al vivir aquí se pueden testear sin UI y reutilizar (por ejemplo, desde una
 * futura pantalla de edición o una importación de datos).
 *
 * `@Inject constructor`: Hilt sabe crearlo solo, pidiendo el repositorio.
 */
class AgregarMovimientoUseCase @Inject constructor(
    private val repositorio: MovimientosRepository,
) {
    fun validar(nuevo: NuevoMovimiento): Set<ErrorValidacion> = buildSet {
        if (nuevo.descripcion.isBlank()) add(ErrorValidacion.DESCRIPCION_VACIA)
        if (parsearMonto(nuevo.montoTexto) == null) add(ErrorValidacion.MONTO_INVALIDO)
        if (nuevo.categoria == null) add(ErrorValidacion.SIN_CATEGORIA)
    }

    /**
     * Valida y, si todo está bien, guarda.
     * @return los errores encontrados; vacío significa que se guardó.
     */
    // `operator fun invoke` permite llamar al caso de uso como una función:
    // agregarMovimiento(nuevo)
    suspend operator fun invoke(nuevo: NuevoMovimiento): Set<ErrorValidacion> {
        val errores = validar(nuevo)
        val monto = parsearMonto(nuevo.montoTexto)
        val categoria = nuevo.categoria
        if (errores.isNotEmpty() || monto == null || categoria == null) return errores

        repositorio.agregar(
            Movimiento(
                id = 0,
                descripcion = nuevo.descripcion.trim(),
                monto = monto,
                categoria = categoria,
                fecha = nuevo.fecha,
            ),
        )
        return emptySet()
    }
}

/**
 * Convierte el texto del monto en pesos chilenos a número. El peso chileno no
 * usa decimales y en Chile el punto separa los miles: "15.000" es quince mil.
 * Devuelve null si no es un monto válido y mayor que cero.
 */
fun parsearMonto(texto: String): Double? {
    val digitos = texto.trim().replace(".", "")
    if (digitos.isEmpty() || !digitos.all { it.isDigit() }) return null
    return digitos.toLongOrNull()?.takeIf { it > 0 }?.toDouble()
}
