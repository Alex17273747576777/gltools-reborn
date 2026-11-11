package io.github.labandita.glreborn.core

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * Entrega los informes de la app a quien el usuario elija, y nada más.
 *
 * Existe por un motivo concreto: en Android 8 y 9 los informes no se pueden escribir en
 * Documentos sin permiso de almacenamiento, así que se guardan en la carpeta privada. Para
 * mandarlos por correo o por WhatsApp hace falta una `content://` que la otra app pueda abrir,
 * y eso pide un proveedor. La alternativa —pedir permiso de almacenamiento para todo— sería
 * peor: se daría acceso al almacenamiento entero para compartir un fichero de texto.
 *
 * La regla que hace esto seguro es simple: **éste no es un proveedor de ficheros**. No sirve
 * una carpeta; sirve un fichero concreto cuyo nombre empieza por `reborn-` y termina en `.md`
 * o `.txt`, dentro de `filesDir`. Si alguien pide otra cosa, se lanza `FileNotFoundException`
 * y no se entrega nada. Un proveedor genérico de ficheros habría que confiar en que nadie
 * construya la URL de otra cosa; aquí no hay nada más que servir.
 */
object ReglasInformes {

    /** Prefijo y extensiones que se permiten. Corto y cerrado a propósito. */
    val PERMITIDOS = Regex("^reborn-[a-z0-9_-]+\\.(md|txt)$")

    /**
     * La tanda de un informe: `reborn-diagnostico-2026-11-10_20-47.md` y
     * `reborn-errores-2026-11-10_20-47.txt` son la misma medición, y comparten sello.
     */
    private val TANDA = Regex("^reborn-(?:diagnostico|errores)-(.+)\\.(?:md|txt)$")

    /** El sello de una tanda, o `null` si el nombre no es de informe. */
    fun selloDe(nombre: String): String? = TANDA.matchEntire(nombre)?.groupValues?.get(1)

    /** La regla: es un informe si existe, es un fichero y se llama como uno. */
    fun esInforme(archivo: File): Boolean =
        archivo.isFile && PERMITIDOS.matches(archivo.name)

    /**
     * La tanda más reciente de los informes que hay en una carpeta: su `.md` y su `.txt`.
     *
     * Existe por un fallo real de la 1.12.2 y la 1.13.0: en Android 10 o más lo que devuelve
     * `Informes.guardar` NO es una ruta, es la etiqueta «Documentos/GLTools Reborn/…», y
     * `File(etiqueta).isFile` da falso. Compartir filtraba por eso y concluía «no hay ningún
     * informe en disco» con el informe recién escrito. Mirar la carpeta de la app en vez de
     * fiarse de la lista que llega es lo que hace que el botón funcione siempre.
     */
    fun ultimos(archivos: List<File>): List<File> {
        val informes = archivos.filter { PERMITIDOS.matches(it.name) && it.isFile }
        if (informes.isEmpty()) return emptyList()
        val reciente = informes.maxByOrNull { it.lastModified() } ?: return emptyList()
        val sello = selloDe(reciente.name) ?: return listOf(reciente)
        return informes.filter { selloDe(it.name) == sello }.sortedBy { it.name }
    }

    /**
     * Los ficheros que sobran si sólo se conservan [conservar] tandas.
     *
     * La copia interna se escribe siempre, y sin límite la carpeta de la app se llena de
     * informes viejos con datos del teléfono que nadie va a volver a mirar. Borrarlos es una
     * decisión de privacidad, no de espacio. Lo que no es informe no se toca nunca.
     */
    fun sobrantes(archivos: List<File>, conservar: Int): List<File> {
        val informes = archivos.filter { PERMITIDOS.matches(it.name) }
        val tandas = informes.groupBy { selloDe(it.name) ?: it.name }.values
            .sortedByDescending { grupo -> grupo.maxOf { it.lastModified() } }
        return tandas.drop(conservar.coerceAtLeast(1)).flatten().sortedBy { it.name }
    }

    /**
     * El nombre servible que hay dentro de una ruta o de una etiqueta; `null` si no es informe.
     *
     * Se queda sólo con el nombre a propósito: lo único que el proveedor sabe servir es un
     * fichero de la carpeta de la app con nombre de informe, así que ni `../` ni una ruta
     * absoluta pueden llevar a otro sitio. Y decidir eso no necesita Android: se puede probar.
     */
    fun servible(ruta: String): String? =
        File(ruta).name.takeIf { PERMITIDOS.matches(it) }

    /** El texto que acompaña al envío: qué es, sin un solo dato del teléfono. */
    fun asuntoDe(nombre: String): String =
        "Informe de GLTools Reborn ($nombre). Generado en el teléfono; no se subió a " +
            "ningún servidor."
}

class ProveedorInformes : ContentProvider() {

    companion object {
        private const val AUTORIDAD = "io.github.labandita.glreborn.informes"

        private val PERMITIDOS = ReglasInformes.PERMITIDOS

        /** La URI de un informe interno, para poder compartirlo. */
        fun uri(context: Context, ruta: String): Uri? {
            val archivo = File(ruta)
            if (!esInforme(archivo)) return null
            return Uri.Builder()
                .scheme("content")
                .authority(AUTORIDAD)
                .appendPath(archivo.name)
                .build()
        }

        /** La regla vive en [ReglasInformes], que sí se puede probar sin Android delante. */
        fun esInforme(archivo: File): Boolean = ReglasInformes.esInforme(archivo)
    }

    override fun onCreate(): Boolean = true

    private fun archivoDe(uri: Uri): File? {
        val nombre = uri.lastPathSegment ?: return null
        if (!PERMITIDOS.matches(nombre)) return null
        val ctx = context ?: return null
        val archivo = File(ctx.filesDir, nombre)
        return archivo.takeIf { esInforme(it) }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw SecurityException("Los informes son de sólo lectura.")
        val archivo = archivoDe(uri) ?: throw java.io.FileNotFoundException("No es un informe: $uri")
        return ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String = when {
        uri.lastPathSegment?.endsWith(".md") == true -> "text/markdown"
        else -> "text/plain"
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?,
    ): Int = 0
}

/**
 * Armar el «compartir» sin que la pantalla sepa cómo funciona.
 *
 * Va aquí y no en la interfaz porque es lógica pura de intents: se puede probar sin emulador
 * comprobando qué se pone en el intent, y la pantalla sólo tiene que llamar a [para].
 */
object Compartir {

    /** La tanda más reciente que hay en la carpeta de la app, se haya generado cuando sea. */
    fun ultimosInformes(context: Context): List<File> {
        val enCarpeta = context.filesDir.listFiles()?.toList() ?: emptyList()
        return ReglasInformes.ultimos(enCarpeta)
    }

    /** Para el botón: hay algo que compartir aunque no se haya generado nada en esta sesión. */
    fun hayInformes(context: Context): Boolean = ultimosInformes(context).isNotEmpty()

    /**
     * Los ficheros que se van a compartir: los que se pidieron por su ruta o etiqueta y, si
     * ésos no están en disco, la tanda más reciente de la carpeta de la app.
     */
    private fun elegibles(context: Context, rutas: List<String>): List<File> {
        val pedidos = rutas.mapNotNull { ruta ->
            if (ruta.isBlank()) return@mapNotNull null
            val nombre = ReglasInformes.servible(ruta) ?: return@mapNotNull null
            File(context.filesDir, nombre).takeIf { ReglasInformes.esInforme(it) }
        }
        return (pedidos + ultimosInformes(context)).distinctBy { it.name }
    }

    /**
     * Un intent para enviar los informes, siempre por el `content://` del proveedor propio.
     *
     * Devuelve `null` si no hay nada que compartir: la pantalla lo dice en vez de abrir un
     * selector vacío.
     */
    fun para(context: Context, rutas: List<String>): Intent? {
        val archivos = elegibles(context, rutas)
        if (archivos.isEmpty()) return null
        val intents = archivos.mapNotNull { archivo ->
            /*
              Nunca un file:// : con targetSdk 35 un ACTION_SEND con una URI de fichero lanza
              FileUriExposedException en Android 7 o más, o sea, la app se cierra al pulsar
              compartir. Lo que el proveedor no puede servir se descarta y ya está.
            */
            val uri = ProveedorInformes.uri(context, archivo.absolutePath) ?: return@mapNotNull null
            val mime = if (archivo.extension == "md") "text/markdown" else "text/plain"
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Informe de GLTools Reborn")
                putExtra(Intent.EXTRA_TEXT, asuntoDe(archivo))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        if (intents.isEmpty()) return null
        val primero = intents.first()
        if (intents.size == 1) return primero
        return Intent.createChooser(primero, "Compartir informe").apply {
            // Los dos ficheros a la vez: el .md y el .txt van juntos o no sirven de nada.
            val varios = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    ArrayList(intents.mapNotNull { it.getParcelableExtra(Intent.EXTRA_STREAM) }),
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(varios))
        }
    }

    fun asuntoDe(archivo: File): String = ReglasInformes.asuntoDe(archivo.name)
}
