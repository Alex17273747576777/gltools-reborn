package io.github.labandita.glreborn

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * El parte de un fallo, guardado en disco para que la app pueda enseñarlo en vez de morirse.
 *
 * Por qué existe esto: la app se cerraba y no decía por qué. Sin un registro, un cierre es
 * indistinguible de «el teléfono lo mató», y eso obliga a adivinar. Con esto, la próxima vez
 * la propia app te da el archivo y la línea.
 *
 * Lo que NO se guarda: ni un dato personal. Sólo la excepción, la versión y el modelo. Nada de
 * identificadores, ni de la lista de apps, ni de las propiedades del teléfono.
 */
object RegistroFallos {

    private const val FICHERO = "ultimo-fallo.txt"

    fun fichero(c: Context): File = File(c.filesDir, FICHERO)

    /** Guarda el fallo. Se llama desde el manejador global, así que no puede fallar él. */
    fun guardar(c: Context, t: Throwable, contexto: String) {
        runCatching {
            val pila = StringWriter().also { t.printStackTrace(PrintWriter(it)) }.toString()
            val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val texto = buildString {
                appendLine("CUÁNDO   $fecha")
                appendLine("DÓNDE    $contexto")
                appendLine("VERSIÓN  ${Build.VERSION_REBORN}")
                appendLine("ANDROID  API ${android.os.Build.VERSION.SDK_INT} · ${android.os.Build.MODEL}")
                appendLine("EXCEPCIÓN ${t.javaClass.name}: ${t.message}")
                appendLine()
                appendLine(pila.take(6000))
            }
            fichero(c).writeText(texto)
        }
    }

    /** Lo que hay guardado, o null si no hubo fallo. */
    fun leer(c: Context): String? =
        runCatching { fichero(c).takeIf { it.exists() }?.readText() }.getOrNull()

    fun borrar(c: Context) {
        runCatching { fichero(c).delete() }
    }

    /** Extracto para enseñar en pantalla: las cuatro primeras líneas, que son las que importan. */
    fun extracto(texto: String?): String {
        if (texto.isNullOrBlank()) return ""
        val lineas = texto.lines().filter { it.isNotBlank() }
        val interesantes = lineas.filter {
            it.startsWith("CUÁNDO") || it.startsWith("DÓNDE") ||
                it.startsWith("EXCEPCIÓN") || it.startsWith("ANDROID")
        }
        // Una línea del stack trace que sea del propio paquete: dice la clase y el número.
        val nuestra = lineas.firstOrNull { it.contains("io.github.labandita.glreborn") }
        return (interesantes + listOfNotNull(nuestra?.trim())).joinToString("\n")
    }
}

/** La versión, en un sitio, para que el registro no mienta. */
object Build {
    const val VERSION_REBORN = "1.8.1"
}
