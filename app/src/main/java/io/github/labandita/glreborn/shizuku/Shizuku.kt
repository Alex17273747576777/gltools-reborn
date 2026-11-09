package io.github.labandita.glreborn.shizuku

import android.content.pm.PackageManager
import io.github.labandita.glreborn.core.Ejecutor
import io.github.labandita.glreborn.core.Via
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuSystemProperties

/**
 * Ejecutor por Shizuku: lee y escribe propiedades del sistema SIN root.
 *
 * Usa `ShizukuSystemProperties`, que es la API pública de Shizuku para esto, en vez de
 * lanzar un shell: se comprobó la firma real leyendo el AAR de la 13.1.5 con javap —
 * `public static void set(String, String)` y `public static String get(String)`— y no
 * la memoria. La primera versión de este archivo llamaba a `Shizuku.newProcess`, que
 * NO es público en la API: el compilador lo rechazó, y tenía razón.
 *
 * `ro.*` son propiedades de sólo lectura en Android moderno: `set` puede fallar aunque
 * Shizuku tenga permiso. Eso no se esconde: se captura y se devuelve al llamador para
 * que la pantalla diga «no cuadra» en vez de «listo».
 */
class EjecutorShizuku : Ejecutor {

    override val via = Via.SHIZUKU

    fun shizukuVivo(): Boolean = try {
        Shizuku.pingBinder()
    } catch (t: Throwable) {
        false
    }

    fun permisoConcedido(): Boolean = try {
        shizukuVivo() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (t: Throwable) {
        false
    }

    override fun disponible(): Boolean = permisoConcedido()

    /** Lee una propiedad. Devuelve null si no hay vía o si el binder falla. */
    fun leer(prop: String): String? = try {
        if (!disponible()) null else ShizukuSystemProperties.get(prop) ?: ""
    } catch (t: Throwable) {
        null
    }

    /**
     * Escribe una propiedad. Devuelve null si no se pudo —y el motivo no se inventa:
     * el llamador lo verá como «no cuadra» al releer, que es la verdad comprobable.
     */
    fun escribir(prop: String, valor: String): Boolean = try {
        if (!disponible()) false
        else {
            ShizukuSystemProperties.set(prop, valor)
            true
        }
    } catch (t: Throwable) {
        false
    }

    /**
     * El contrato de [Ejecutor] es «dame la salida de este comando», que es lo que
     * necesita el ejecutor por `su`. Shizuku no ejecuta comandos con esta API: lee y
     * escribe propiedades. Por eso aquí se responde sólo a las dos formas que la app
     * usa de verdad y se devuelve null para lo demás, en vez de fingir un shell que
     * no existe.
     */
    override fun ejecutar(comando: String): String? {
        val limpio = comando.trim()
        return when {
            limpio.startsWith("getprop ") -> leer(limpio.removePrefix("getprop ").trim())
            limpio == "id" -> if (disponible()) "uid=2000(shell)" else null
            else -> null
        }
    }

    companion object {
        /** Pide el permiso una sola vez. Si Shizuku no está, no hace nada. */
        fun pedirPermiso(requestCode: Int) {
            try {
                Shizuku.requestPermission(requestCode)
            } catch (t: Throwable) {
                // Sin Shizuku instalado esto lanza; la interfaz lo informa por su estado.
            }
        }
    }
}
