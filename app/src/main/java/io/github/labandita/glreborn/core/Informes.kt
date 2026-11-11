package io.github.labandita.glreborn.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

/**
 * Guardar los informes donde el usuario los pueda encontrar de verdad.
 *
 * Lo que hace: en Android 10 o más, escribe en la carpeta pública **Documentos** con
 * `MediaStore`, que es la vía soportada y no necesita permisos. En Android 8 y 9 (`minSdk 26`),
 * sin permiso de almacenamiento no se puede escribir ahí, así que se guarda en la carpeta de la
 * app y se **dice la ruta exacta**, en vez de fingir que se descargó.
 *
 * Nada de esto sube un byte a internet: es un `ContentResolver` local.
 */
/** Lo que se consiguió guardar y lo que no. Se enseña tal cual: una ruta real o el motivo. */
data class ResultadoInforme(
    val rutas: List<String> = emptyList(),
    val fallos: List<Pair<String, String>> = emptyList(),
) {
    val huboArchivos: Boolean get() = rutas.isNotEmpty()
}

object Informes {

    const val CANAL = "reborn-informes"

    /**
     * Las rutas del último informe generado, para poder compartirlo después sin volver a medir.
     *
     * Vive en memoria a propósito: un informe es de la sesión en la que se hizo. Si la app se
     * cierra del todo, el fichero sigue en Documentos —donde el usuario lo puede abrir— y esta
     * lista vacía sólo significa «no hay nada recién generado que compartir», que es la verdad.
     */
    @Volatile
    private var ultimasRutas: List<String> = emptyList()

    fun ultimas(): List<String> = ultimasRutas

    /** Guarda las rutas en la carpeta de la app para poder compartirlas por `content://`. */
    private fun aCarpetaPropia(context: Context, nombre: String, contenido: String): String? =
        runCatching {
            val archivo = File(context.filesDir, nombre)
            archivo.writeText(contenido)
            archivo.absolutePath
        }.getOrNull()
    private const val ETIQUETA = "GLTools Reborn"

    /**
     * Guarda un texto y devuelve la ruta legible, o el motivo del fallo.
     * Nunca lanza: se llama al cerrar la app, donde no hay nadie para ver una excepción.
     */
    fun guardar(context: Context, nombre: String, contenido: String, mime: String): Pair<String, String> =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val valores = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, nombre)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/GLTools Reborn")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
                    ?: return@runCatching "" to "el sistema no dio un destino en Documentos"
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(contenido.toByteArray())
                } ?: return@runCatching "" to "no se pudo abrir el archivo para escribir"
                // El `use` cierra el flujo y eso es lo que confirma la fila de MediaStore: sin
                // cerrar, en algunos teléfonos el archivo se queda en 0 bytes. Lo que se
                // devuelve es una ETIQUETA legible, no una ruta: en Android 10 o más la ruta
                // real no es accesible para la app, y tratarla como si lo fuera fue lo que
                // rompió el botón de compartir en la 1.12.2 y la 1.13.0.
                "Documentos/GLTools Reborn/$nombre" to ""
            } else {
                val carpeta = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                    ?: File(context.filesDir, "documentos").also { it.mkdirs() }
                val f = File(carpeta, nombre)
                f.writeText(contenido)
                f.absolutePath to ""
            }
        }.getOrElse { t ->
            "" to "no se pudo guardar: ${t.javaClass.simpleName}: ${t.message}"
        }

    /**
     * Avisa de que el informe está listo. Sin permiso de notificaciones no se pide nada raro:
     * simplemente no se avisa, y el archivo está igual.
     */
    fun avisar(context: Context, titulo: String, texto: String, sonido: Boolean) {
        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(CANAL, "Informes de la app", NotificationManager.IMPORTANCE_DEFAULT)
                        .apply { description = "Avisos cuando se genera un informe al cerrar" },
                )
            }
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, CANAL)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }
            builder
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setAutoCancel(true)
            if (!sonido) builder.setDefaults(0) else builder.setDefaults(Notification.DEFAULT_SOUND)
            nm.notify(4100, builder.build())
        }.onFailure { Log.w(ETIQUETA, "sin aviso: ${it.message}") }
    }

    /**
     * Lo que se genera al cerrar: el `.md` del diagnóstico y el `.txt` con todos los errores.
     * Devuelve las rutas conseguidas y lo que no se pudo hacer — se dice, no se calla.
     */
    fun alCerrar(
        context: Context,
        version: String,
        profundo: Boolean,
        conSonido: Boolean,
    ): ResultadoInforme {
        val fallos = mutableListOf<Pair<String, String>>()
        val rutas = mutableListOf<String>()
        val erroresPrevios: List<String> =
            io.github.labandita.glreborn.RegistroFallos.leer(context)?.lines()?.take(30) ?: emptyList()
        val diag = Autodiagnostico.ejecutar(context, profundo = profundo, erroresPrevios = erroresPrevios)
        val sello = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.US)
            .format(java.util.Date())
        val md = Informe.markdown(diag, version, RegistroErrores.FICHERO)
        val txt = buildString {
            appendLine("# Registro de errores · GLTools Reborn $version")
            appendLine("# Generado ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
            appendLine("# Formato: fecha<TAB>nivel<TAB>dónde<TAB>mensaje")
            appendLine()
            appendLine(RegistroErrores.leer(context).ifBlank { "(sin errores anotados)" })
        }
        val (rutaMd, falloMd) = guardar(context, "reborn-diagnostico-$sello.md", md, "text/markdown")
        val (rutaTxt, falloTxt) = guardar(context, "reborn-errores-$sello.txt", txt, "text/plain")
        if (falloMd.isNotBlank()) fallos += "informe .md" to falloMd
        if (falloTxt.isNotBlank()) fallos += "registro .txt" to falloTxt
        if (rutaMd.isNotBlank()) rutas += rutaMd
        if (rutaTxt.isNotBlank()) rutas += rutaTxt
        // Copia interna con el mismo contenido: en Android 8 y 9 Documentos puede fallar, y el
        // informe no debe quedarse sin poder salir del teléfono por eso.
        val internoMd = aCarpetaPropia(context, "reborn-diagnostico-$sello.md", md)
        val internoTxt = aCarpetaPropia(context, "reborn-errores-$sello.txt", txt)
        ultimasRutas = listOfNotNull(
            rutaMd.ifBlank { null }, rutaTxt.ifBlank { null }, internoMd, internoTxt,
        ).distinct()
        /*
          La copia interna se escribe siempre, así que sin límite la carpeta de la app se llena
          de informes viejos con datos del teléfono. Se conservan las cinco últimas tandas y el
          resto se borra. Fallar aquí no puede impedir que el informe salga: por eso va envuelto.
        */
        runCatching {
            val enCarpeta = context.filesDir.listFiles()?.toList() ?: emptyList()
            ReglasInformes.sobrantes(enCarpeta, conservar = 5).forEach { it.delete() }
        }

        if (rutaMd.isNotBlank() || rutaTxt.isNotBlank()) {
            avisar(
                context,
                "Informe guardado",
                listOf(rutaMd, rutaTxt).filter { it.isNotBlank() }.joinToString(" · "),
                conSonido,
            )
        }
        return ResultadoInforme(rutas = rutas, fallos = fallos)
    }
}
