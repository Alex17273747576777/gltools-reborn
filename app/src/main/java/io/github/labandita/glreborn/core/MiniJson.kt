package io.github.labandita.glreborn.core

/**
 * Lo justo de JSON para guardar perfiles: leer y escribir.
 *
 * Por qué escrito a mano y no con una librería: los perfiles tienen que poder leerse y probarse
 * en la máquina de compilación, donde **no hay Android**. `org.json` es del sistema Android (y
 * por eso no se puede usar en `pruebasNucleo`), y una librería más sería otro trozo ajeno dentro
 * del APK para guardar ocho campos. Un objeto, una lista, textos, números y booleanos: eso es
 * todo lo que hace falta, y eso es todo lo que esto entiende.
 *
 * Lo que **no** hace: no acepta comentarios, ni comas finales, ni `NaN`. Un fichero que no es
 * JSON válido se rechaza con un mensaje que dice en qué línea, en vez de devolver `null` y que
 * el perfil del usuario desaparezca sin explicación.
 */
object MiniJson {

    class ErrorJson(mensaje: String) : Exception(mensaje)

    /** Un error de sintaxis, con la línea y la columna para poder arreglar el fichero a mano. */
    private fun fallo(texto: String, i: Int, que: String): Nothing {
        val linea = texto.substring(0, i.coerceAtMost(texto.length)).count { it == '\n' } + 1
        val columna = i - (texto.lastIndexOf('\n', (i - 1).coerceAtLeast(0)) + 1) + 1
        throw ErrorJson("$que en la línea $linea, columna $columna")
    }

    /** Lee un valor JSON. Devuelve `Map<String, Any?>`, `List<Any?>`, `String`, `Double`, `Boolean` o `null`. */
    fun leer(texto: String): Any? {
        val i = intArrayOf(0)
        saltar(texto, i)
        val v = valor(texto, i)
        saltar(texto, i)
        if (i[0] < texto.length) fallo(texto, i[0], "sobra contenido después del JSON")
        return v
    }

    private fun saltar(t: String, i: IntArray) {
        while (i[0] < t.length && t[i[0]].isWhitespace()) i[0]++
    }

    private fun valor(t: String, i: IntArray): Any? {
        if (i[0] >= t.length) fallo(t, i[0], "el JSON termina antes de tiempo")
        return when (t[i[0]]) {
            '{' -> objeto(t, i)
            '[' -> lista(t, i)
            '"' -> cadena(t, i)
            't' -> literal(t, i, "true", true)
            'f' -> literal(t, i, "false", false)
            'n' -> literal(t, i, "null", null)
            else -> numero(t, i)
        }
    }

    private fun literal(t: String, i: IntArray, palabra: String, valor: Any?): Any? {
        if (!t.startsWith(palabra, i[0])) fallo(t, i[0], "se esperaba «$palabra»")
        i[0] += palabra.length
        return valor
    }

    private fun objeto(t: String, i: IntArray): Map<String, Any?> {
        val salida = LinkedHashMap<String, Any?>()
        i[0]++ // {
        saltar(t, i)
        if (i[0] < t.length && t[i[0]] == '}') { i[0]++; return salida }
        while (true) {
            saltar(t, i)
            if (i[0] >= t.length || t[i[0]] != '"') fallo(t, i[0], "se esperaba el nombre de un campo entre comillas")
            val clave = cadena(t, i)
            saltar(t, i)
            if (i[0] >= t.length || t[i[0]] != ':') fallo(t, i[0], "se esperaban dos puntos después de «$clave»")
            i[0]++
            saltar(t, i)
            salida[clave] = valor(t, i)
            saltar(t, i)
            if (i[0] >= t.length) fallo(t, i[0], "falta cerrar la llave")
            when (t[i[0]]) {
                ',' -> { i[0]++; if (i[0] < t.length && t[i[0]] == '}') fallo(t, i[0], "hay una coma de más antes de «}»") }
                '}' -> { i[0]++; return salida }
                else -> fallo(t, i[0], "se esperaba una coma o «}»")
            }
        }
    }

    private fun lista(t: String, i: IntArray): List<Any?> {
        val salida = ArrayList<Any?>()
        i[0]++ // [
        saltar(t, i)
        if (i[0] < t.length && t[i[0]] == ']') { i[0]++; return salida }
        while (true) {
            saltar(t, i)
            salida += valor(t, i)
            saltar(t, i)
            if (i[0] >= t.length) fallo(t, i[0], "falta cerrar el corchete")
            when (t[i[0]]) {
                ',' -> { i[0]++; if (i[0] < t.length && t[i[0]] == ']') fallo(t, i[0], "hay una coma de más antes de «]»") }
                ']' -> { i[0]++; return salida }
                else -> fallo(t, i[0], "se esperaba una coma o «]»")
            }
        }
    }

    private fun cadena(t: String, i: IntArray): String {
        i[0]++ // comilla
        val sb = StringBuilder()
        while (true) {
            if (i[0] >= t.length) fallo(t, i[0], "falta cerrar las comillas")
            when (val c = t[i[0]]) {
                '"' -> { i[0]++; return sb.toString() }
                '\\' -> {
                    i[0]++
                    if (i[0] >= t.length) fallo(t, i[0], "escape incompleto")
                    when (val e = t[i[0]]) {
                        '"' -> sb.append('"'); '\\' -> sb.append('\\'); '/' -> sb.append('/')
                        'b' -> sb.append('\b'); 'f' -> sb.append('\u000C'); 'n' -> sb.append('\n')
                        'r' -> sb.append('\r'); 't' -> sb.append('\t')
                        'u' -> {
                            if (i[0] + 4 >= t.length) fallo(t, i[0], "escape \\u incompleto")
                            val hex = t.substring(i[0] + 1, i[0] + 5)
                            val n = hex.toIntOrNull(16) ?: fallo(t, i[0], "escape \\u no válido: $hex")
                            sb.append(n.toChar()); i[0] += 4
                        }
                        else -> fallo(t, i[0], "escape desconocido: \\$e")
                    }
                    i[0]++
                }
                // Un salto de línea crudo dentro de una cadena no es JSON válido: casi siempre
                // es un fichero editado a mano con un editor que no respeta el formato.
                '\n', '\r' -> fallo(t, i[0], "hay un salto de línea dentro de las comillas")
                else -> { sb.append(c); i[0]++ }
            }
        }
    }

    private fun numero(t: String, i: IntArray): Double {
        val inicio = i[0]
        if (i[0] < t.length && (t[i[0]] == '-' || t[i[0]] == '+')) i[0]++
        while (i[0] < t.length && (t[i[0]].isDigit() || t[i[0]] in ".eE+-")) i[0]++
        val trozo = t.substring(inicio, i[0])
        return trozo.toDoubleOrNull() ?: fallo(t, inicio, "se esperaba un número y hay «$trozo»")
    }

    /** Escribe un valor. Los `Map` salen con sus claves en el orden en que se metieron. */
    fun escribir(v: Any?): String = buildString { escribirEn(this, v, 0) }

    private fun escribirEn(sb: StringBuilder, v: Any?, nivel: Int) {
        when (v) {
            null -> sb.append("null")
            is String -> cadenaEscrita(sb, v)
            is Boolean -> sb.append(v)
            is Int, is Long -> sb.append(v)
            is Double -> sb.append(
                if (v.isFinite() && v == v.toLong().toDouble()) v.toLong().toString() else v.toString(),
            )
            is Float -> sb.append(v)
            is Map<*, *> -> {
                sb.append('{')
                var primero = true
                v.forEach { (k, x) ->
                    if (!primero) sb.append(',')
                    primero = false
                    sb.append('\n'); sangria(sb, nivel + 1)
                    cadenaEscrita(sb, k.toString()); sb.append(": ")
                    escribirEn(sb, x, nivel + 1)
                }
                if (!primero) { sb.append('\n'); sangria(sb, nivel) }
                sb.append('}')
            }
            is Iterable<*> -> {
                sb.append('[')
                var primero = true
                v.forEach { x ->
                    if (!primero) sb.append(',')
                    primero = false
                    sb.append('\n'); sangria(sb, nivel + 1)
                    escribirEn(sb, x, nivel + 1)
                }
                if (!primero) { sb.append('\n'); sangria(sb, nivel) }
                sb.append(']')
            }
            else -> throw ErrorJson("no se puede escribir un ${v.javaClass.simpleName} en JSON")
        }
    }

    private fun sangria(sb: StringBuilder, nivel: Int) { repeat(nivel) { sb.append("  ") } }

    private fun cadenaEscrita(sb: StringBuilder, s: String) {
        sb.append('"')
        s.forEach { c ->
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        sb.append('"')
    }

    // ── Ayudantes con tipo, para no llenar el código de casts ─────────────────

    fun objeto(v: Any?): Map<String, Any?> =
        v as? Map<String, Any?> ?: throw ErrorJson("se esperaba un objeto y hay ${v?.javaClass?.simpleName}")

    fun lista(v: Any?): List<Any?> =
        v as? List<Any?> ?: throw ErrorJson("se esperaba una lista y hay ${v?.javaClass?.simpleName}")

    fun texto(m: Map<String, Any?>, clave: String): String? = m[clave] as? String
    fun numero(m: Map<String, Any?>, clave: String): Double? = m[clave] as? Double
    fun entero(m: Map<String, Any?>, clave: String): Int? = (m[clave] as? Double)?.toInt()
    fun booleano(m: Map<String, Any?>, clave: String): Boolean? = m[clave] as? Boolean
