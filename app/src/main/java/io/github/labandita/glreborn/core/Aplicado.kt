package io.github.labandita.glreborn.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Qué se le aplicó a cada app, por separado.
 *
 * El usuario pidió un panel por app. Para poder enseñarlo hace falta recordar, por
 * paquete, qué se hizo y cuándo. Esto es ese cuaderno, y vive en un JSON local: no
 * sale del teléfono.
 */
data class Hecho(
    val cuando: Long,
    val accion: String,
    val detalle: String,
    val reversible: Boolean,
)

data class Ficha(
    val paquete: String,
    val nombre: String,
    val hechos: List<Hecho>,
) {
    val ultimo: Long get() = hechos.maxOfOrNull { it.cuando } ?: 0L
    val tieneIrreversible: Boolean get() = hechos.any { !it.reversible }
}

/**
 * Las reglas del cuaderno, sin Android de por medio.
 *
 * Están aparte para poder probarlas de verdad en la máquina de compilación: `Aplicado`
 * necesita un `Context` y un fichero, y aquí no hay emulador. Las reglas sí son lógica pura.
 */
object ReglasAplicado {
    /** Tope por app. Es un historial legible, no un registro de auditoría. */
    const val MAX_HECHOS = 40

    /** Se queda con los últimos [MAX_HECHOS], tirando por el principio. */
    fun recortar(hechos: List<Hecho>, max: Int = MAX_HECHOS): List<Hecho> =
        if (hechos.size <= max) hechos else hechos.takeLast(max)

    /** El más reciente primero: es el orden en que se lee un historial. */
    fun ordenar(hechos: List<Hecho>): List<Hecho> = hechos.sortedByDescending { it.cuando }

    fun tieneIrreversible(hechos: List<Hecho>): Boolean = hechos.any { !it.reversible }
}

class Aplicado(context: Context) {

    private val archivo = File(context.filesDir, "aplicado.json")

    private fun leerTodo(): MutableMap<String, MutableList<Hecho>> {
        val mapa = mutableMapOf<String, MutableList<Hecho>>()
        if (!archivo.exists()) return mapa
        return try {
            val raiz = JSONObject(archivo.readText())
            val paquetes = raiz.optJSONObject("paquetes") ?: return mapa
            for (p in paquetes.keys()) {
                val lista = paquetes.getJSONArray(p)
                val hechos = mutableListOf<Hecho>()
                for (i in 0 until lista.length()) {
                    val o = lista.getJSONObject(i)
                    hechos += Hecho(
                        cuando = o.optLong("cuando"),
                        accion = o.optString("accion"),
                        detalle = o.optString("detalle"),
                        reversible = o.optBoolean("reversible", true),
                    )
                }
                mapa[p] = hechos
            }
            mapa
        } catch (t: Throwable) {
            mapa
        }
    }

    private fun guardar(mapa: Map<String, List<Hecho>>) {
        val paquetes = JSONObject()
        for ((p, lista) in mapa) {
            val arr = JSONArray()
            for (h in lista) {
                arr.put(JSONObject().apply {
                    put("cuando", h.cuando)
                    put("accion", h.accion)
                    put("detalle", h.detalle)
                    put("reversible", h.reversible)
                })
            }
            paquetes.put(p, arr)
        }
        JSONObject().put("paquetes", paquetes).also { archivo.writeText(it.toString()) }
    }

    /** Anota un hecho sobre una app. Se llama DESPUÉS de que la acción saliera bien. */
    fun anotar(paquete: String, accion: String, detalle: String, reversible: Boolean = true) {
        val mapa = leerTodo()
        // Máximo 40 hechos por app: es un historial legible, no un archivo de auditoría.
        val lista = mapa.getOrPut(paquete) { mutableListOf() }
        lista += Hecho(System.currentTimeMillis(), accion, detalle, reversible)
        mapa[paquete] = ReglasAplicado.recortar(lista).toMutableList()
        guardar(mapa)
    }

    /** Todo lo anotado, listo para el panel. */
    fun fichas(nombres: Map<String, String>): List<Ficha> =
        leerTodo().map { (paquete, hechos) ->
            Ficha(paquete, nombres[paquete] ?: paquete, hechos.sortedByDescending { it.cuando })
        }.sortedByDescending { it.ultimo }

    fun de(paquete: String): Ficha? =
        leerTodo()[paquete]?.sortedByDescending { it.cuando }
            ?.let { Ficha(paquete, paquete, it) }

    /** Borra el historial de una app. No deshace nada: sólo olvida. */
    fun olvidar(paquete: String): Boolean {
        val mapa = leerTodo()
        val habia = mapa.remove(paquete) != null
        if (habia) guardar(mapa)
        return habia
    }
}
