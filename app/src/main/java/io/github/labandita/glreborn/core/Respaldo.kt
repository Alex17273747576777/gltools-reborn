package io.github.labandita.glreborn.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Llevarte tus perfiles a otro teléfono, y traerlos de vuelta.
 *
 * Es lo que hace que la app no sea una jaula: hasta la 1.9.4, un perfil que te habías construido
 * a mano —generado de tu propio teléfono, o afinado a partir de una ficha— vivía y moría en ese
 * teléfono. Si cambiabas de móvil, o reinstalabas, se perdía. Todas las apps que se miraron para
 * el rediseño tienen esto; aquí no estaba.
 *
 * **Formato propio, versionado y legible.** No es magia ni un formato binario opaco: es JSON, y
 * se puede abrir con un editor de texto para ver qué hay dentro. Lleva:
 *
 *  - `formato`: el nombre y la versión del formato. Si un día cambia, un fichero viejo se puede
 *    seguir leyendo, y si no se puede, **se dice** en vez de cargar cosas a medias.
 *  - `exportado`: cuándo.
 *  - `origen`: versión de la app y modelo del teléfono, para saber de dónde salió.
 *  - `perfiles`: la lista.
 *
 * **Nada de lo que se escribe aquí es inventado:** los campos son exactamente los del perfil, con
 * su nivel de confianza (`VERIFICADO` / `PARCIAL` / `SIN_VERIFICAR`). Un fichero de otra persona
 * se importa con la confianza que traiga, no con la que nos gustaría.
 */
object Respaldo {

    const val FORMATO = "glreborn-perfiles"
    const val VERSION_FORMATO = 1

    /** Lo que se ha podido leer de un fichero, y lo que no. */
    data class Lectura(
        val perfiles: List<PerfilDispositivo>,
        val descartados: Int,
        val motivoDescarte: String = "",
        val origen: String = "",
        val formatoDesconocido: Boolean = false,
    ) {
        val ok: Boolean get() = perfiles.isNotEmpty()
        val resumen: String
            get() = when {
                formatoDesconocido -> "El fichero no es un respaldo de GLTools Reborn."
                perfiles.isEmpty() -> "No hay ni un perfil válido en el fichero."
                descartados > 0 -> "${perfiles.size} perfiles leídos · $descartados descartados ($motivoDescarte)"
                else -> "${perfiles.size} perfiles leídos"
            }
    }

    /** Escribe el respaldo. Devuelve el texto JSON listo para guardar. */
    fun escribir(
        perfiles: List<PerfilDispositivo>,
        versionApp: String,
        modeloTelefono: String,
        ahora: Long = System.currentTimeMillis(),
    ): String {
        val raiz = JSONObject()
        raiz.put("formato", FORMATO)
        raiz.put("version", VERSION_FORMATO)
        raiz.put("exportado", ahora)
        raiz.put("exportado_legible", java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm", java.util.Locale.US,
        ).format(java.util.Date(ahora)))
        raiz.put("origen", JSONObject().apply {
            put("app", versionApp)
            put("telefono", modeloTelefono)
        })
        raiz.put("perfiles", JSONArray().apply {
            perfiles.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("nombre", p.nombre)
                    put("fabricante", p.fabricante)
                    put("marca", p.marca)
                    put("modelo", p.modelo)
                    put("device", p.device)
                    put("board", p.board)
                    put("hardware", p.hardware)
                    put("soc", p.soc)
                    put("gpu", p.gpu)
                    put("openglEs", p.openglEs)
                    put("fuente", p.fuente)
                    put("confianza", p.confianza.name)
                    put("nota", p.nota)
                })
            }
        })
        return raiz.toString(2)
    }

    /** Lee un respaldo. Nunca lanza: lo que no puede leer, lo cuenta. */
    fun leer(texto: String): Lectura {
        val raiz = try {
            JSONObject(texto)
        } catch (e: Exception) {
            return Lectura(emptyList(), 0, formatoDesconocido = true)
        }
        if (raiz.optString("formato") != FORMATO) {
            return Lectura(emptyList(), 0, formatoDesconocido = true)
        }
        val version = raiz.optInt("version", 0)
        if (version > VERSION_FORMATO) {
            // Un fichero de una versión futura: se puede intentar, pero se avisa.
            val leidos = leerLista(raiz)
            return leidos.copy(
                motivoDescarte = "El fichero es de una versión más nueva ($version). " +
                    "Puede que falten campos.",
            )
        }
        val origen = raiz.optJSONObject("origen")
            ?.let { "${it.optString("app")} en ${it.optString("telefono")}" } ?: ""
        return leerLista(raiz).copy(origen = origen)
    }

    private fun leerLista(raiz: JSONObject): Lectura {
        val array = raiz.optJSONArray("perfiles") ?: return Lectura(
            emptyList(), 0, "El respaldo no tiene lista de perfiles.",
        )
        val buenos = ArrayList<PerfilDispositivo>(array.length())
        var malos = 0
        var primerMotivo = ""
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i)
            if (o == null) {
                malos++
                continue
            }
            val id = o.optString("id").ifBlank { null }
            val nombre = o.optString("nombre").ifBlank { null }
            // Un perfil sin id o sin nombre no se puede ni distinguir ni enseñar: no entra.
            if (id == null || nombre == null) {
                malos++
                if (primerMotivo.isBlank()) primerMotivo = "hay perfiles sin id o sin nombre"
                continue
            }
            val confianza = runCatching {
                PerfilDispositivo.Confianza.valueOf(
                    o.optString("confianza", "SIN_VERIFICAR").uppercase(),
                )
            }.getOrElse { PerfilDispositivo.Confianza.SIN_VERIFICAR }
            buenos.add(
                PerfilDispositivo(
                    id = id,
                    nombre = nombre,
                    fabricante = o.optString("fabricante"),
                    marca = o.optString("marca"),
                    modelo = o.optString("modelo"),
                    device = o.optString("device"),
                    board = o.optString("board"),
                    hardware = o.optString("hardware"),
                    soc = o.optString("soc"),
                    gpu = o.optString("gpu"),
                    openglEs = o.optString("openglEs"),
                    fuente = o.optString("fuente").ifBlank { "importado de un respaldo" },
                    confianza = confianza,
                    nota = o.optString("nota"),
                ),
            )
        }
        return Lectura(buenos, malos, primerMotivo)
    }

    /** Nombre de fichero con fecha, para no sobrescribir respaldos anteriores. */
    fun nombreDeFichero(ahora: Long = System.currentTimeMillis()): String =
        "glreborn-perfiles-" + java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US)
            .format(java.util.Date(ahora)) + ".json"
}
