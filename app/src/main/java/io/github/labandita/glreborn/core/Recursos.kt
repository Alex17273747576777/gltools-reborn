package io.github.labandita.glreborn.core

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

/**
 * Memoria y almacenamiento: lo que le queda al teléfono para trabajar.
 *
 * Por qué existe: la portada enseñaba la identidad, la sonda y las apps, y **no decía lo más
 * básico para decidir si aplicar algo**: cuánta memoria libre hay y cuánto espacio queda. Era el
 * dato que faltaba y el que más se mira en un teléfono de 4 GB.
 *
 * **Todo se lee con APIs públicas**, sin root ni Shizuku:
 *  - memoria: `ActivityManager.MemoryInfo` (la misma cifra que enseña Ajustes),
 *  - almacenamiento: `StatFs` sobre el directorio de datos y sobre el almacenamiento externo.
 *
 * **Lo que NO se puede leer sin root, y se dice:** la memoria que ocupa cada proceso, la memoria
 * de la GPU y la temperatura. Eso está en `/proc` y `/sys`, que no son legibles para una app
 * normal. No se inventan ni se estiman: no aparecen.
 */
object Recursos {

    data class Memoria(
        val totalBytes: Long,
        val libreBytes: Long,
        val baja: Boolean,
        val umbralBytes: Long,
    ) {
        val usadaBytes: Long get() = totalBytes - libreBytes
        val porcentajeUsado: Int
            get() = if (totalBytes <= 0) 0 else ((usadaBytes * 100) / totalBytes).toInt()
        /** Cuánto es «poco» según el propio sistema, no según nosotros. */
        val aviso: String?
            get() = when {
                totalBytes <= 0 -> null
                baja -> "El sistema marca la memoria como baja: va a empezar a cerrar apps de fondo."
                porcentajeUsado >= 90 -> "Queda menos del 10 % de memoria libre."
                else -> null
            }
    }

    data class Almacenamiento(
        val totalBytes: Long,
        val libreBytes: Long,
        val esExterno: Boolean,
    ) {
        val porcentajeUsado: Int
            get() = if (totalBytes <= 0) 0 else (((totalBytes - libreBytes) * 100) / totalBytes).toInt()
        val aviso: String?
            get() = when {
                totalBytes <= 0 -> null
                libreBytes < 500L * 1024 * 1024 -> "Quedan menos de 500 MB: no se puede instalar casi nada."
                libreBytes < 2L * 1024 * 1024 * 1024 -> "Quedan menos de 2 GB de espacio."
                else -> null
            }
    }

    /** Memoria del teléfono. `null` si el sistema no la da (raro, pero se contempla). */
    fun memoria(contexto: Context): Memoria? = runCatching {
        val am = contexto.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        Memoria(
            totalBytes = info.totalMem,
            libreBytes = info.availMem,
            baja = info.lowMemory,
            umbralBytes = info.threshold,
        )
    }.getOrNull()

    /**
     * Almacenamiento interno. Se mide sobre el directorio de datos de la app, que está en la
     * misma partición que el resto: es la cifra que importa para instalar y para cachés.
     */
    fun almacenamiento(contexto: Context): Almacenamiento? = runCatching {
        val stat = StatFs(contexto.filesDir.absolutePath)
        Almacenamiento(
            totalBytes = stat.totalBytes,
            libreBytes = stat.availableBytes,
            esExterno = false,
        )
    }.getOrNull()

    /** Almacenamiento externo (la tarjeta o la partición de usuario). Puede no existir. */
    fun almacenamientoExterno(): Almacenamiento? = runCatching {
        val dir: File = Environment.getExternalStorageDirectory()
        if (!dir.exists()) return null
        val stat = StatFs(dir.absolutePath)
        Almacenamiento(
            totalBytes = stat.totalBytes,
            libreBytes = stat.availableBytes,
            esExterno = true,
        )
    }.getOrNull()

    /** Lo que ocupa esta app, para que se pueda decidir si borrar sus cachés. */
    fun propio(contexto: Context): Long = runCatching {
        fun tamano(f: File): Long = when {
            !f.exists() -> 0L
            f.isFile -> f.length()
            else -> f.listFiles()?.sumOf { tamano(it) } ?: 0L
        }
        tamano(contexto.cacheDir) + tamano(contexto.filesDir)
    }.getOrDefault(0L)

    /** Bytes en algo que se lee de un vistazo: «3,1 GB», «812 MB». */
    fun legible(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024L * 1024 -> "%.0f MB".format(bytes / 1024.0 / 1024.0)
        bytes < 1024L * 1024 * 1024 -> "%.0f MB".format(bytes / 1024.0 / 1024.0)
        else -> "%.1f GB".format(bytes / 1024.0 / 1024.0 / 1024.0)
    }
}
