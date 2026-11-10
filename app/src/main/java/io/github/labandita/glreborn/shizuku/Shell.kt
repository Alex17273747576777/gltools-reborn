package io.github.labandita.glreborn.shizuku

import android.os.IBinder
import android.os.ParcelFileDescriptor
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.io.FileInputStream

/**
 * Ejecuta comandos de shell con los permisos de Shizuku, SIN root.
 *
 * Por qué no se usa `Shizuku.newProcess`: se comprobó con `javap` sobre el AAR 13.1.5
 * y **es `private`**. Llamarlo desde otra clase no compila, aunque esté ahí. Lo que sí
 * es público es el servicio del binder (`IShizukuService.newProcess`) y su clase
 * generada `Stub.asInterface`. Ese es el camino: el binder de Shizuku, sin reflexión
 * sobre métodos privados.
 *
 * Alcance real de lo que esto permite: lo mismo que un `adb shell` conectado al
 * teléfono. Ni más, ni menos. NO es root: no toca /system, ni el kernel, ni SELinux.
 */
class Shell {

    private fun servicio(): IShizukuService? = try {
        if (!Shizuku.pingBinder()) null
        else IShizukuService.Stub.asInterface(Shizuku.getBinder())
    } catch (t: Throwable) {
        null
    }

    fun disponible(): Boolean = servicio() != null

    /**
     * Lo que devolvió un comando.
     *
     * `codigo` es el del proceso. **`-1` no es un código de shell**: significa que se agotó el
     * tiempo y se mató el proceso, o que no se pudo ni lanzar. Antes se devolvía `-1` como si
     * fuera un código cualquiera y la pantalla decía «código -1», que no existe en ningún shell.
     */
    data class Salida(val codigo: Int, val texto: String, val seAgotoElTiempo: Boolean = false) {
        val ok: Boolean get() = codigo == 0
        /** Qué contar cuando algo falló, sin inventarse nada. */
        val motivo: String
            get() = when {
                seAgotoElTiempo -> "el comando no terminó en 8 s y se canceló"
                codigo == -1 -> "el comando no llegó a ejecutarse"
                texto.isNotBlank() -> texto.lineSequence().take(4).joinToString(" · ").take(160)
                // Aquí estaba el «sin detalle»: se lanzaba con `sh -c` y los mensajes de error van
                // por la salida de error, que no se estaba leyendo. Se tiraban a la basura.
                else -> "salió con código $codigo pero no dijo por qué"
            }
    }

    /**
     * Ejecuta y espera. Se envuelve en `sh -c` para que las tuberías y comillas
     * funcionen, igual que haría `adb shell`.
     */
    fun correr(comando: String, tiempoMaxMs: Long = 8000): Salida? {
        val s = servicio() ?: return null
        var pfd: ParcelFileDescriptor? = null
        return try {
            // `2>&1` no es un adorno: sin él, un comando que falla no dice nada. La salida de
            // error es justo donde Android explica por qué se negó.
            val proceso = s.newProcess(arrayOf("sh", "-c", "$comando 2>&1"), null, null)
            val codigo = try {
                val hilo = Thread {
                    try { proceso.waitFor() } catch (_: Throwable) {}
                }
                hilo.start()
                hilo.join(tiempoMaxMs)
                if (hilo.isAlive) { proceso.destroy(); -1 } else proceso.exitValue()
            } catch (t: Throwable) {
                -1
            }
            pfd = proceso.inputStream
            val texto = pfd?.let {
                FileInputStream(it.fileDescriptor).bufferedReader().readText()
            } ?: ""
            pfd?.close()
            Salida(codigo, texto.trim(), seAgotoElTiempo = codigo == -1)
        } catch (t: Throwable) {
            try { pfd?.close() } catch (_: Throwable) {}
            null
        }
    }

    /** Sólo la primera línea, que es lo que suele hacer falta. */
    fun linea(comando: String): String? = correr(comando)?.texto?.lineSequence()?.firstOrNull()?.trim()
}
