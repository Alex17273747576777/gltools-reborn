package io.github.labandita.glreborn.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Los perfiles que no vienen de fábrica: los generados del aparato real y los que se
 * extrajeron de un texto. Viven en el teléfono (`filesDir/perfiles.json`) y de ahí
 * salen a la búsqueda.
 *
 * Se guardan como texto plano a propósito: se pueden abrir, leer y borrar a mano. Un
 * perfil que no puedes inspeccionar es un perfil en el que no puedes confiar.
 */
class Biblioteca(context: Context) {

    private val archivo = File(context.filesDir, "perfiles.json")

    fun leer(): List<PerfilDispositivo> {
        if (!archivo.exists()) return emptyList()
        return try {
            val raiz = JSONObject(archivo.readText())
            val arr = raiz.optJSONArray("perfiles") ?: return emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                PerfilDispositivo(
                    id = o.optString("id"),
                    nombre = o.optString("nombre"),
                    fabricante = o.optString("fabricante"),
                    marca = o.optString("marca"),
                    modelo = o.optString("modelo"),
                    device = o.optString("device"),
                    board = o.optString("board"),
                    hardware = o.optString("hardware"),
                    soc = o.optString("soc"),
                    gpu = o.optString("gpu"),
                    openglEs = o.optString("openglEs"),
                    fuente = o.optString("fuente"),
                    confianza = try {
                        PerfilDispositivo.Confianza.valueOf(o.optString("confianza"))
                    } catch (t: Throwable) {
                        PerfilDispositivo.Confianza.SIN_VERIFICAR
                    },
                    nota = o.optString("nota"),
                )
            }.filter { it.id.isNotBlank() }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    private fun escribir(lista: List<PerfilDispositivo>) {
        val arr = JSONArray()
        lista.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id); put("nombre", p.nombre)
                put("fabricante", p.fabricante); put("marca", p.marca)
                put("modelo", p.modelo); put("device", p.device)
                put("board", p.board); put("hardware", p.hardware)
                put("soc", p.soc); put("gpu", p.gpu)
                put("openglEs", p.openglEs); put("fuente", p.fuente)
                put("confianza", p.confianza.name); put("nota", p.nota)
            })
        }
        archivo.writeText(JSONObject().put("perfiles", arr).toString())
    }

    /** Guarda uno. Si ya existe ese id, lo sustituye: el último que generaste manda. */
    fun guardar(p: PerfilDispositivo) {
        val actual = leer().toMutableList()
        actual.removeAll { it.id == p.id }
        actual += p
        escribir(actual)
    }

    fun guardarVarios(lista: List<PerfilDispositivo>) {
        val actual = leer().toMutableList()
        lista.forEach { p ->
            actual.removeAll { it.id == p.id }
            actual += p
        }
        escribir(actual)
    }

    fun borrar(id: String): Boolean {
        val actual = leer().toMutableList()
        val habia = actual.removeAll { it.id == id }
        if (habia) escribir(actual)
        return habia
    }

    fun borrarTodo() {
        if (archivo.exists()) archivo.delete()
    }

    /** Catálogo de fábrica + los tuyos. Es lo que ve la búsqueda. */
    fun catalogoCompleto(): List<PerfilDispositivo> = Perfiles.conExtra(leer())
}
