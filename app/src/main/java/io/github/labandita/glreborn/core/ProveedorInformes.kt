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
class ProveedorInformes : ContentProvider() {

    companion object {
        private const val AUTORIDAD = "io.github.labandita.glreborn.informes"

        /** Prefijos y extensiones que se permiten. Corto y cerrado a propósito. */
        private val PERMITIDOS = Regex("^reborn-[a-z0-9_-]+\\.(md|txt)$")

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

        /** La regla, aparte para poder probarla sin Android delante. */
        fun esInforme(archivo: File): Boolean =
            archivo.isFile && PERMITIDOS.matches(archivo.name)
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

    /**
     * Un intent para enviar los informes. Los que estén en la carpeta pública se mandan como
     * texto; los que estén dentro de la app, por su `content://`.
     *
     * Devuelve `null` si no hay nada que compartir: la pantalla lo dice en vez de abrir un
     * selector vacío.
     */
    fun para(context: Context, rutas: List<String>): Intent? {
        val existentes = rutas.filter { it.isNotBlank() && File(it).isFile }
        if (existentes.isEmpty()) return null
        val intents = existentes.map { ruta ->
            val archivo = File(ruta)
            val uri = ProveedorInformes.uri(context, ruta)
                ?: android.net.Uri.fromFile(archivo) // Documentos: ruta real, el selector la abre
            val mime = if (archivo.extension == "md") "text/markdown" else "text/plain"
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Informe de GLTools Reborn")
                putExtra(Intent.EXTRA_TEXT, asuntoDe(archivo))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
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

    /** El texto que acompaña al envío: qué es, sin datos del teléfono. */
    fun asuntoDe(archivo: File): String =
        "Informe de GLTools Reborn (${archivo.name}). Generado en el teléfono; no se " +
            "subió a ningún servidor."
}
