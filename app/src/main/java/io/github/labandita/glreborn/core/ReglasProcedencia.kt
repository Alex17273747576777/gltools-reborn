package io.github.labandita.glreborn.core

import org.json.JSONObject

/**
 * Procedencia de los datos de terceros que viajan en el APK.
 *
 * Un catálogo del que no se puede decir de dónde salió, cuándo y con qué hash no es un dato: es un
 * rumor con formato JSON. Estas reglas convierten el bloque `verificacion` del manifiesto en texto
 * enseñable, y en nada más. Son puras (sólo `org.json`, que en las pruebas lo pone la dependencia
 * `pruebasJson`) para poder probarlas sin teléfono.
 */
object ReglasProcedencia {

    /** Qué ficheros de datos declaran su hash. */
    fun declarados(v: JSONObject?): List<String> {
        val arr = v?.optJSONArray("ficheros") ?: return emptyList()
        return (0 until arr.length()).mapNotNull {
            arr.optJSONObject(it)?.optString("archivo")?.takeIf { n -> n.isNotBlank() }
        }
    }

    /** El hash declarado de un fichero, o null si no está declarado. Nunca uno inventado. */
    fun shaDe(v: JSONObject?, archivo: String): String? {
        val arr = v?.optJSONArray("ficheros") ?: return null
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optString("archivo") == archivo) return o.optString("sha256").takeIf { it.length == 64 }
        }
        return null
    }

    /**
     * Lo que se enseña. Si falta el bloque, se dice que falta: callarlo sería exactamente el
     * problema que este bloque existe para resolver.
     */
    fun lineas(v: JSONObject?): List<String> {
        if (v == null) {
            return listOf("Procedencia: SIN bloque de verificación. No se puede decir de dónde viene cada dato ni cuándo se capturó.")
        }
        val out = mutableListOf<String>()
        out += "Procedencia verificable (comprobada el ${v.optString("verificado_el", "fecha no declarada")}):"
        out += "  · ${declarados(v).size} ficheros de datos con su sha256 y su tamaño; el CI los recalcula en cada push."
        val o = v.optJSONObject("origen_dispositivos")
        if (o != null) {
            out += "  · Catálogo de dispositivos: ${o.optString("url")}"
            out += "    origen descargado el ${o.optString("descargado_el")}: ${o.optInt("fichas_origen")} fichas, sha256 ${o.optString("sha256_origen").take(16)}…"
            out += "    copia incluida: ${o.optInt("fichas_versionado")} fichas, sha256 ${o.optString("sha256_versionado").take(16)}…"
            out += if (o.optBoolean("coincide")) "    coinciden byte a byte." else "    NO coinciden: " + o.optString("deriva")
            val fecha = o.optString("fecha_de_captura_original")
            out += if (o.isNull("fecha_de_captura_original") || fecha.isBlank())
                "    fecha de captura original: NO se registró. " + o.optString("por_que_no_hay_fecha")
            else "    fecha de captura original: $fecha"
        }
        return out
    }

    /** Vacío si todo lo enviado declara procedencia; si no, la lista de lo que no la declara. */
    fun sinDeclarar(v: JSONObject?, reales: List<String>): List<String> {
        val d = declarados(v).toSet()
        return reales.filter { it != "manifiesto.json" && it !in d }.sorted()
    }
}
