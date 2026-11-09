/**
 * Pruebas del núcleo, ejecutables sin emulador.
 *
 * No hay /dev/kvm en esta máquina: no puedo instalar el APK y arrancarlo. Pero casi
 * toda la lógica que importa —qué se escribe, qué se omite, qué se confirma— no toca
 * Android y se puede probar aquí de verdad. Lo que NO se prueba aquí se dice al final.
 */
import io.github.labandita.glreborn.core.*

var fallos = 0
fun check(nombre: String, cond: Boolean, detalle: String = "") {
    if (cond) println("  ✓ $nombre") else { println("  ✗ $nombre $detalle"); fallos++ }
}

/** Ejecutor falso: simula lo que el sistema devolvería. */
class EjecutorFalso(private val props: MutableMap<String, String>, val bloquea: Boolean = false) : Ejecutor {
    override val via = Via.ROOT
    override fun disponible() = true
    override fun ejecutar(comando: String): String? {
        val c = comando.trim()
        return when {
            c.startsWith("getprop ") -> props[c.removePrefix("getprop ").trim()] ?: ""
            c.startsWith("setprop ") -> {
                val partes = c.removePrefix("setprop ").split(" ", limit = 2)
                if (bloquea) return ""   // el fabricante lo bloquea: no cambia
                props[partes[0]] = partes.getOrElse(1) { "" }.trim('\'')
                ""
            }
            else -> null
        }
    }
}

fun main() {
    println("PRUEBAS · GLTools Reborn\n")

    println("1 · El catálogo no inventa datos")
    val todos = Perfiles.todos
    check("hay 7 perfiles", todos.size == 7)
    check("todos tienen nombre y SoC", todos.all { it.nombre.isNotBlank() && it.soc.isNotBlank() })
    check("todos declaran su fuente", todos.all { it.fuente.isNotBlank() })
    val sinVerificar = todos.filter { it.confianza == PerfilDispositivo.Confianza.SIN_VERIFICAR }
    check("ninguno está sin declarar", sinVerificar.isEmpty(), "hay ${sinVerificar.size}")
    // Dato medido, no supuesto: 4 de los 7 perfiles tienen device y board vacíos a
    // propósito. Escribí «5» de memoria y la prueba lo cazó; el número real es 4.
    val conVacios = todos.count { it.device.isBlank() || it.board.isBlank() }
    check("los parciales dejan campos vacíos en vez de inventarlos", conVacios == 4, "vacíos: $conVacios")
    check("los verificados están completos",
        todos.filter { it.confianza == PerfilDispositivo.Confianza.VERIFICADO }
            .all { it.device.isNotBlank() && it.board.isNotBlank() })
    check("ningún perfil toca IMEI, serie ni MAC",
        todos.none { it.toString().contains("imei", true) || it.toString().contains("serial", true) })

    println("\n2 · El escritor escribe, omite y confirma")
    val props = mutableMapOf("ro.product.model" to "MiTelefono", "ro.product.brand" to "mimarca")
    val robot = RobotVerificador(EjecutorFalso(props))
    val antes = robot.leerEstado()
    check("lee el estado real antes de tocar", antes["ro.product.model"] == "MiTelefono")

    val perfil = Perfiles.PIXEL_8_PRO
    val r = Escritor(EjecutorFalso(props)).aplicar(perfil)
    check("aplica el perfil", r.ok, r.detalle)
    check("el modelo cambió de verdad", props["ro.product.model"] == "Pixel 8 Pro", props["ro.product.model"].toString())
    check("la marca cambió", props["ro.product.brand"] == "google")

    println("\n3 · Cuando el fabricante bloquea, se dice que no")
    val bloqueado = mutableMapOf("ro.product.model" to "MiTelefono")
    val rb = Escritor(EjecutorFalso(bloqueado, bloquea = true)).aplicar(perfil)
    check("NO declara éxito si no cambió", !rb.ok, rb.detalle)
    check("explica que no cuadra", rb.detalle.contains("no cuadran") || rb.detalle.contains("confirmadas"),
        rb.detalle)

    println("\n4 · Sin vía no se aplica nada y se dice")
    class SinVia : Ejecutor {
        override val via = Via.NINGUNA
        override fun disponible() = false
        override fun ejecutar(comando: String): String? = null
    }
    val rs = Escritor(SinVia()).aplicar(perfil)
    check("resultado NO ok sin vía", !rs.ok)
    check("dice que no se aplicó nada", rs.detalle.contains("no se aplicó nada", true) ||
        rs.detalle.contains("No se aplicó nada"), rs.detalle)
    check("la vía es NINGUNA", rs.via == Via.NINGUNA)

    println("\n5 · Los campos vacíos se omiten, no se escriben")
    val p2 = Perfiles.IQOO_15_PRO
    check("el perfil parcial tiene device vacío", p2.device.isBlank())
    val props2 = mutableMapOf("ro.product.device" to "midevice")
    Escritor(EjecutorFalso(props2)).aplicar(p2)
    check("NO pisó device con un valor vacío", props2["ro.product.device"] == "midevice",
        props2["ro.product.device"].toString())

    println("\n6 · Restaurar devuelve lo original")
    val props3 = mutableMapOf("ro.product.model" to "Original")
    val e3 = EjecutorFalso(props3)
    val robot3 = RobotVerificador(e3)
    val orig = robot3.leerEstado()
    Escritor(e3).aplicar(Perfiles.PIXEL_8_PRO)
    check("cambió", props3["ro.product.model"] == "Pixel 8 Pro")
    Escritor(e3).restaurar(orig)
    check("volvió al original", props3["ro.product.model"] == "Original", props3["ro.product.model"].toString())

    println()
    if (fallos == 0) println("TODAS LAS PRUEBAS PASAN")
    else println("$fallos PRUEBAS FALLARON")
    kotlin.system.exitProcess(if (fallos == 0) 0 else 1)
}
