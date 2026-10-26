package fase0

import kotlin.math.PI

// =====================================================================
// Tema: clases, data class, object, herencia e interfaces.
// =====================================================================

// ---------------------------------------------------------------------
// Ejercicio 9 — Figuras: interfaces, clases abstractas y herencia
// En Kotlin las clases son `final` por defecto: hay que marcarlas
// `open` o `abstract` para poder heredar.
// ---------------------------------------------------------------------
interface Describible {
    fun describir(): String  // sin implementación: obligatoria
}

abstract class Figura(val nombre: String) : Describible {
    abstract fun area(): Double
    abstract fun perimetro(): Double

    // Implementación por defecto que usan todas las subclases.
    override fun describir() = "$nombre con área %.2f".format(area())
}

class Circulo(val radio: Double) : Figura("Círculo") {
    override fun area() = PI * radio * radio
    override fun perimetro() = 2 * PI * radio
}

open class Rectangulo(val ancho: Double, val alto: Double) : Figura("Rectángulo") {
    override fun area() = ancho * alto
    override fun perimetro() = 2 * (ancho + alto)
}

// Un cuadrado ES UN rectángulo: hereda area() y perimetro().
class Cuadrado(lado: Double) : Rectangulo(lado, lado) {
    override fun describir() = "Cuadrado de lado $ancho"
}

fun areaTotal(figuras: List<Figura>): Double = figuras.sumOf { it.area() }

// ---------------------------------------------------------------------
// Ejercicio 10 — Carrito de compras con data class
// data class genera equals, hashCode, toString y copy automáticamente.
// ---------------------------------------------------------------------
data class Producto(val nombre: String, val precio: Double, val categoria: String)

data class ItemCarrito(val producto: Producto, val cantidad: Int) {
    val subtotal: Double get() = producto.precio * cantidad
}

class Carrito {
    // Lista mutable privada; hacia afuera se expone como solo lectura.
    private val _items = mutableListOf<ItemCarrito>()
    val items: List<ItemCarrito> get() = _items

    fun agregar(producto: Producto, cantidad: Int = 1) {
        require(cantidad > 0) { "La cantidad debe ser positiva" }
        val indice = _items.indexOfFirst { it.producto == producto }
        if (indice >= 0) {
            // copy() crea una copia cambiando solo lo indicado (inmutabilidad).
            _items[indice] = _items[indice].copy(cantidad = _items[indice].cantidad + cantidad)
        } else {
            _items.add(ItemCarrito(producto, cantidad))
        }
    }

    fun quitar(producto: Producto) {
        _items.removeAll { it.producto == producto }
    }

    fun total(): Double = _items.sumOf { it.subtotal }

    fun totalPorCategoria(): Map<String, Double> =
        _items.groupBy { it.producto.categoria }
            .mapValues { (_, items) -> items.sumOf { it.subtotal } }
}

// ---------------------------------------------------------------------
// Ejercicio 11 — object (singleton) y companion object
// `object` declara una clase con una única instancia.
// ---------------------------------------------------------------------
object GeneradorId {
    private var ultimo = 0

    fun siguiente(): Int = ++ultimo
    fun reiniciar() {
        ultimo = 0
    }
}

class Cuenta private constructor(val id: Int, val titular: String) {
    // companion object: miembros "estáticos" asociados a la clase.
    // Aquí actúa como fábrica, ya que el constructor es privado.
    companion object {
        fun abrir(titular: String) = Cuenta(GeneradorId.siguiente(), titular)
    }
}
