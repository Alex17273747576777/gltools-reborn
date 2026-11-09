package io.github.labandita.glreborn.core

import android.content.Context
import java.io.File

/**
 * Quién puede escribir propiedades del sistema en este teléfono.
 *
 * Hay tres estados y los tres se muestran tal cual en la interfaz. El estado NINGUNO
 * no es un error que se esconde: es la respuesta honesta cuando no hay ni Shizuku ni
 * root, y sin uno de los dos NO se puede cambiar el modelo del dispositivo. Punto.
 * Una app que dice «aplicado» sin haber podido escribir nada es una app que miente.
 */
enum class Via { SHIZUKU, ROOT, NINGUNA }

data class Resultado(
    val ok: Boolean,
    val via: Via,
    val detalle: String,
    val lineas: List<String> = emptyList(),
)

interface Ejecutor {
    val via: Via
    fun disponible(): Boolean
    /** Ejecuta y devuelve la salida. `null` si no hay vía disponible. */
    fun ejecutar(comando: String): String?
}

/** Ejecuta con `su`. Sirve en un teléfono con root y sin Shizuku. */
class EjecutorRoot(private val context: Context) : Ejecutor {
    override val via = Via.ROOT

    override fun disponible(): Boolean = try {
        val p = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
        val salida = p.inputStream.bufferedReader().readText()
        p.waitFor()
        salida.contains("uid=0")
    } catch (t: Throwable) {
        false
    }

    override fun ejecutar(comando: String): String? = try {
        val p = ProcessBuilder("su", "-c", comando).redirectErrorStream(true).start()
        val salida = p.inputStream.bufferedReader().readText()
        p.waitFor()
        salida
    } catch (t: Throwable) {
        null
    }
}

/**
 * El «robot» del que hablábamos: no es un adorno ni un falso indicador.
 *
 * Hace tres cosas y las tres se pueden comprobar desde fuera:
 *   1. lee el estado REAL del teléfono antes de tocar nada;
 *   2. aplica el perfil y vuelve a leer para confirmar que quedó escrito;
 *   3. si algo no cuadra, lo dice con el valor que encontró, no con un «listo».
 */
class RobotVerificador(private val ejecutor: Ejecutor?) {

    private val props = listOf(
        "ro.product.brand", "ro.product.manufacturer", "ro.product.model",
        "ro.product.device", "ro.product.board", "ro.hardware", "ro.opengles.version",
    )

    fun leerEstado(): Map<String, String> {
        val e = ejecutor ?: return emptyMap()
        return props.associateWith { prop ->
            (e.ejecutar("getprop $prop") ?: "").trim()
        }
    }

    data class Comprobacion(val prop: String, val esperado: String, val obtenido: String) {
        val coincide: Boolean get() = esperado.isBlank() || esperado == obtenido
    }

    /**
     * Confirmación después de aplicar: relee y compara. Un perfil PARCIAL tiene campos
     * vacíos a propósito y ésos no se comprueban —no se escribieron—, pero se informan.
     */
    fun comprobar(perfil: PerfilDispositivo): List<Comprobacion> {
        val estado = leerEstado()
        return listOf(
            Comprobacion("ro.product.brand", perfil.marca, estado["ro.product.brand"] ?: ""),
            Comprobacion("ro.product.manufacturer", perfil.fabricante, estado["ro.product.manufacturer"] ?: ""),
            Comprobacion("ro.product.model", perfil.modelo, estado["ro.product.model"] ?: ""),
            Comprobacion("ro.product.device", perfil.device, estado["ro.product.device"] ?: ""),
            Comprobacion("ro.opengles.version", perfil.openglEs, estado["ro.opengles.version"] ?: ""),
        )
    }

    fun resumen(comprobaciones: List<Comprobacion>): String {
        val aplicadas = comprobaciones.count { it.esperado.isNotBlank() && it.coincide }
        val fallidas = comprobaciones.count { it.esperado.isNotBlank() && !it.coincide }
        val omitidas = comprobaciones.count { it.esperado.isBlank() }
        return when {
            aplicadas == 0 && fallidas == 0 -> "No se aplicó nada: el perfil no tiene campos escribibles."
            fallidas == 0 -> "Listo: $aplicadas propiedades escritas y confirmadas" +
                if (omitidas > 0) " · $omitidas omitidas por falta de dato" else ""
            else -> "Parcial: $aplicadas confirmadas, $fallidas NO coinciden · revisa antes de creerlo"
        }
    }
}

/** Elige la vía disponible, en este orden: Shizuku (no requiere root), luego root. */
object Vias {
    fun detectar(context: Context, shizukuListo: Boolean, ejecutorShizuku: Ejecutor?): Ejecutor? = when {
        shizukuListo && ejecutorShizuku != null && ejecutorShizuku.disponible() -> ejecutorShizuku
        EjecutorRoot(context).disponible() -> EjecutorRoot(context)
        else -> null
    }
}
