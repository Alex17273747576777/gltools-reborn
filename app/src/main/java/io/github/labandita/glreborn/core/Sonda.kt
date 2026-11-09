package io.github.labandita.glreborn.core

/**
 * Lo que el teléfono dice de sí mismo, leído de verdad.
 *
 * Todo lo de aquí sale de `getprop` o de la API de Android. Nada está inventado: si el
 * teléfono no contesta a una propiedad, el campo queda vacío y quien lo lea sabrá que
 * falta. Un perfil con huecos avisa; uno con datos inventados engaña.
 *
 * La sonda no decide nada: sólo recoge. Las decisiones (qué se puede copiar, qué no,
 * cómo se calcula un OpenGL ES coherente con la GPU) están en [Generador], que sí es
 * lógica pura y se prueba sin teléfono.
 */
object Sonda {

    /** Propiedades que interesan. El orden es el de la ficha del perfil. */
    val CLAVES = listOf(
        "ro.product.brand",
        "ro.product.manufacturer",
        "ro.product.model",
        "ro.product.device",
        "ro.product.board",
        "ro.hardware",
        "ro.board.platform",
        "ro.soc.manufacturer",
        "ro.soc.model",
        "ro.opengles.version",
        "ro.build.version.release",
        "ro.build.version.sdk",
        "ro.build.characteristics",
    )

    /** Propiedades que jamás se copian a un perfil, ni siquiera para leerlas. */
    val PROHIBIDAS = listOf(
        "ro.serialno", "ro.boot.serialno", "gsm.sim.operator", "ro.boot.mode",
        "persist.sys.locale", "ro.product.name", "net.hostname",
    )

    /**
     * Limpia la salida de `getprop`: una propiedad por línea, con su valor.
     *
     * Se acepta también el formato `[clave]: [valor]` que imprime `getprop` sin
     * argumentos, porque en algunas ROMs es lo único que responde.
     */
    fun interpretar(salida: String): Map<String, String> {
        val mapa = mutableMapOf<String, String>()
        salida.lineSequence().forEach { linea ->
            val l = linea.trim()
            if (l.isEmpty() || l.startsWith("#")) return@forEach
            when {
                l.startsWith("[") && l.contains("]:") -> {
                    val clave = l.substringAfter("[").substringBefore("]").trim()
                    val valor = l.substringAfter("]:", "").trim().trim('[', ']').trim()
                    if (clave.isNotEmpty()) mapa[clave] = valor
                }
                l.contains('=') -> {
                    val clave = l.substringBefore('=').trim()
                    val valor = l.substringAfter('=').trim()
                    if (clave.isNotEmpty()) mapa[clave] = valor
                }
            }
        }
        return mapa
    }

    /** El comando que se le pasa a la shell: leer sólo lo que interesa. */
    fun comando(): String = CLAVES.joinToString("; ") { "getprop $it" }

    /**
     * Une las claves leídas con los datos que ya tiene Android (versión, arquitectura).
     * Aquí sí hay `Build`... no: se recibe como parámetro para poder probarlo.
     */
    fun juntar(
        props: Map<String, String>,
        versionAndroid: String,
        arquitectura: List<String>,
        esHuawei: Boolean,
    ): Datos = Datos(
        marca = props["ro.product.brand"].orEmpty(),
        fabricante = props["ro.product.manufacturer"].orEmpty(),
        modelo = props["ro.product.model"].orEmpty(),
        device = props["ro.product.device"].orEmpty(),
        board = props["ro.product.board"].orEmpty(),
        hardware = props["ro.hardware"].orEmpty(),
        plataforma = props["ro.board.platform"].orEmpty()
            .ifEmpty { props["ro.hardware"].orEmpty() },
        socFabricante = props["ro.soc.manufacturer"].orEmpty(),
        socModelo = props["ro.soc.model"].orEmpty(),
        openglEs = props["ro.opengles.version"].orEmpty(),
        versionAndroid = props["ro.build.version.release"].orEmpty().ifEmpty { versionAndroid },
        sdk = props["ro.build.version.sdk"].orEmpty(),
        caracteristicas = props["ro.build.characteristics"].orEmpty(),
        arquitectura = arquitectura,
        esHuawei = esHuawei,
    )

    /** Lo leído, en un tipo con nombre para no pasear mapas por toda la app. */
    data class Datos(
        val marca: String,
        val fabricante: String,
        val modelo: String,
        val device: String,
        val board: String,
        val hardware: String,
        val plataforma: String,
        val socFabricante: String,
        val socModelo: String,
        val openglEs: String,
        val versionAndroid: String,
        val sdk: String,
        val caracteristicas: String,
        val arquitectura: List<String>,
        val esHuawei: Boolean,
    ) {
        val completa: Boolean
            get() = marca.isNotBlank() && modelo.isNotBlank() && device.isNotBlank()

        /** Cuántos campos trae de verdad, para no presentar un perfil hueco como lleno. */
        val camposLeidos: Int
            get() = listOf(
                marca, fabricante, modelo, device, board, hardware,
                plataforma, socFabricante, socModelo, openglEs, versionAndroid, sdk,
            ).count { it.isNotBlank() }

        val resumen: String
            get() = "${marca.ifBlank { "?" }} ${modelo.ifBlank { "?" }} · " +
                "device ${device.ifBlank { "?" }} · " +
                "Android ${versionAndroid.ifBlank { "?" }} · " +
                "$camposLeidos/12 campos leídos"
    }
}
