package io.github.labandita.glreborn.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Los datos reales que viajan dentro del APK.
 *
 * Aquí no hay nada generado: son catálogos de terceros, con su fuente escrita al lado.
 *
 *  · **TOP500** — las 14 ediciones oficiales de 2019 a 2025: 500 sistemas por edición,
 *    con núcleos, Rmax, Rpeak, potencia y país. Los XML originales van incluidos.
 *  · **Dispositivos** — el catálogo oficial de Google Play: 54.434 modelos reales con su
 *    marca, su nombre comercial, su `device` y su modelo. De aquí salen perfiles que
 *    existen de verdad, no inventados.
 *  · **SoC y GPU** — extraídos de las tablas de Wikipedia: Snapdragon y MediaTek con su
 *    GPU, MHz y GFLOPS; Adreno con sus ALUs.
 *  · **Android** — versión, API y año.
 *
 * Se lee del APK, sin red. Las funciones de parseo son puras y se prueban en la máquina
 * de compilación; las que tocan `AssetManager` van al final y no se prueban aquí.
 */
object Datos {

    // ── Tipos ──────────────────────────────────────────────────────────────

    data class Sistema(
        val rank: Int,
        val nombre: String,
        val fabricante: String,
        val configuracion: String,
        val nucleos: Int,
        val rmaxPflops: Double?,
        val rpeakPflops: Double?,
        val potenciaKw: Int?,
        val pais: String,
        val ciudad: String,
        val sitio: String,
        val anio: Int,
    ) {
        val resumen: String
            get() = "$nombre · ${rmaxPflops ?: "?"} PFlop/s · ${nucleos} núcleos · $pais"
    }

    data class Edicion(val edicion: String, val fichero: String, val sistemas: Int)

    data class Dispositivo(
        val marca: String,
        val comercial: String,
        val device: String,
        val modelo: String,
    ) {
        val legible: String
            get() = listOf(marca, comercial).filter { it.isNotBlank() }
                .joinToString(" ").ifBlank { modelo }
    }

    /**
     * Una ficha de mobolist.net: el modelo con su SoC, su GPU y su versión de Android.
     * Es lo que faltaba en el catálogo de Google, que no trae ni chip ni GPU.
     */
    data class Ficha(
        val nombre: String,
        val chipset: String,
        val gpu: String,
        val nucleos: String,
        val android: String,
        val ram: String,
        val pantalla: String,
        val resolucion: String,
        val interfaz: String,
    ) {
        val resumen: String
            get() = listOfNotNull(
                chipset.takeIf { it.isNotBlank() },
                gpu.takeIf { it.isNotBlank() },
                android.takeIf { it.isNotBlank() }?.let { "Android $it" },
                ram.takeIf { it.isNotBlank() }?.let { "RAM $it" },
            ).joinToString(" · ")
    }

    data class Soc(
        val soc: String,
        val gpu: String,
        val cpu: String,
        val nucleos: Int?,
        val gpuMhz: Int?,
        val gflopsFp32: Double?,
        val fabricacion: String,
        val fuente: String,
    ) {
        val resumen: String
            get() = listOfNotNull(
                gpu.takeIf { it.isNotBlank() },
                gpuMhz?.let { "$it MHz" },
                gflopsFp32?.let { "$it GFLOPS FP32" },
                fabricacion.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
    }

    // ── Parseo puro (se prueba sin Android) ────────────────────────────────

    /** Una fila del índice del TOP500 → un sistema. Nada se rellena a ojo. */
    fun sistemaDeFila(fila: List<Any?>): Sistema? {
        if (fila.size < 12) return null
        fun texto(i: Int) = (fila.getOrNull(i) as? String).orEmpty()
        fun entero(i: Int) = (fila.getOrNull(i) as? Number)?.toInt()
        fun doble(i: Int) = (fila.getOrNull(i) as? Number)?.toDouble()
        val rank = entero(0) ?: return null
        return Sistema(
            rank = rank,
            nombre = texto(1),
            fabricante = texto(2),
            configuracion = texto(3),
            nucleos = entero(4) ?: 0,
            rmaxPflops = doble(5),
            rpeakPflops = doble(6),
            potenciaKw = entero(7),
            pais = texto(8),
            ciudad = texto(9),
            sitio = texto(10),
            anio = entero(11) ?: 0,
        )
    }

    /**
     * Una fila del catálogo de Google Play → un perfil de dispositivo.
     *
     * Es la conversión más útil de toda la app: el catálogo trae marca, nombre comercial,
     * `device` y modelo de 54.434 aparatos que existen. Lo que NO trae es el SoC ni la
     * GPU, así que el perfil sale con esos campos vacíos y marcado como PARCIAL. Un
     * perfil con huecos avisa; uno relleno a ojo engaña.
     */
    fun perfilDeDispositivo(d: Dispositivo): PerfilDispositivo? {
        if (d.modelo.isBlank() && d.comercial.isBlank()) return null
        val marca = d.marca.ifBlank { d.comercial.substringBefore(' ').trim() }
        val modelo = d.modelo.ifBlank { d.comercial }
        return PerfilDispositivo(
            id = "cat-" + d.device.ifBlank { modelo }.lowercase()
                .map { if (it.isLetterOrDigit()) it else '-' }
                .joinToString("").trim('-'),
            nombre = d.legible,
            fabricante = marca,
            marca = marca,
            modelo = modelo,
            device = d.device,
            board = d.device,
            hardware = d.device,
            soc = "",
            gpu = "",
            openglEs = "",
            fuente = "Catálogo oficial de dispositivos de Google Play (supported_devices.csv)",
            confianza = PerfilDispositivo.Confianza.PARCIAL,
            nota = "Modelo real del catálogo de Google. El SoC, la GPU y OpenGL ES no " +
                "vienen en ese catálogo, así que van vacíos a propósito.",
        )
    }

    /**
     * Busca en el catálogo por marca, nombre comercial, device o modelo.
     * Sin acentos y sin mayúsculas, igual que el buscador de perfiles.
     */
    fun buscarDispositivos(
        dispositivos: List<Dispositivo>,
        consulta: String,
        limite: Int = 40,
    ): List<Dispositivo> {
        val q = Buscador.normalizar(consulta)
        if (q.isBlank()) return emptyList()
        val palabras = q.split(" ").filter { it.isNotBlank() }
        return dispositivos.asSequence()
            .filter { d ->
                val heno = Buscador.normalizar("${d.marca} ${d.comercial} ${d.device} ${d.modelo}")
                palabras.all { heno.contains(it) }
            }
            .take(limite)
            .toList()
    }

    // ── Lectura desde los assets (necesita Android) ────────────────────────

    /** Para los lectores de datos que viven en otro archivo (`Antutu`). */
    internal fun leerTexto(context: Context, ruta: String): String? = texto(context, ruta)

    private fun texto(context: Context, ruta: String): String? = try {
        context.assets.open(ruta).bufferedReader().use { it.readText() }
    } catch (t: Throwable) {
        null
    }

    /** Qué hay dentro del APK y de dónde viene. Es lo que se enseña en Diagnóstico. */
    fun manifiesto(context: Context): JSONObject? = texto(context, "datos/manifiesto.json")
        ?.let { runCatching { JSONObject(it) }.getOrNull() }

    fun ediciones(context: Context): List<Edicion> {
        val json = texto(context, "datos/top500/indice.json") ?: return emptyList()
        return runCatching {
            val arr = JSONObject(json).optJSONArray("ediciones") ?: JSONArray()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                Edicion(o.optString("edicion"), o.optString("fichero"), o.optInt("sistemas"))
            }
        }.getOrDefault(emptyList())
    }

    /** Los 500 sistemas de una edición. */
    fun top500(context: Context, edicion: String): List<Sistema> {
        val ruta = "datos/top500/" + edicion.replace("-", "") + ".json"
        val json = texto(context, ruta) ?: return emptyList()
        return runCatching {
            val arr = JSONObject(json).optJSONArray("sistemas") ?: return emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val fila = arr.optJSONArray(i) ?: return@mapNotNull null
                sistemaDeFila((0 until fila.length()).map { fila.opt(it) })
            }
        }.getOrDefault(emptyList())
    }

    fun dispositivos(context: Context): List<Dispositivo> {
        val json = texto(context, "datos/dispositivos.json") ?: return emptyList()
        return runCatching {
            val arr = JSONObject(json).optJSONArray("dispositivos") ?: JSONArray()
            (0 until arr.length()).mapNotNull { i ->
                val fila = arr.optJSONArray(i) ?: return@mapNotNull null
                Dispositivo(
                    marca = fila.optString(0),
                    comercial = fila.optString(1),
                    device = fila.optString(2),
                    modelo = fila.optString(3),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun socs(context: Context): List<Soc> {
        val json = texto(context, "datos/soc.json") ?: return emptyList()
        return runCatching {
            val arr = JSONObject(json).optJSONArray("socs") ?: JSONArray()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                Soc(
                    soc = o.optString("soc"),
                    gpu = o.optString("gpu"),
                    cpu = o.optString("cpu"),
                    nucleos = if (o.has("nucleos") && !o.isNull("nucleos")) o.optInt("nucleos") else null,
                    gpuMhz = if (o.has("gpuMhz") && !o.isNull("gpuMhz")) o.optInt("gpuMhz") else null,
                    gflopsFp32 = if (o.has("gflopsFp32") && !o.isNull("gflopsFp32"))
                        o.optDouble("gflopsFp32") else null,
                    fabricacion = o.optString("fabricacion"),
                    fuente = o.optString("fuente"),
                )
            }
        }.getOrDefault(emptyList())
    }

    /** Las fichas de mobolist: 1.386 modelos con SoC y GPU reales. */
    fun fichas(context: Context): List<Ficha> {
        val json = texto(context, "datos/mobolist.json") ?: return emptyList()
        return runCatching {
            val arr = JSONObject(json).optJSONArray("dispositivos") ?: return emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                Ficha(
                    nombre = o.optString("nombre"),
                    chipset = o.optString("chipset"),
                    gpu = o.optString("gpu"),
                    nucleos = o.optString("nucleos"),
                    android = o.optString("android"),
                    ram = o.optString("ram"),
                    pantalla = o.optString("tamano_pantalla"),
                    resolucion = o.optString("resolucion"),
                    interfaz = o.optString("interfaz"),
                )
            }
        }.getOrDefault(emptyList())
    }

    /** Busca en las fichas por modelo, chipset o GPU. */
    fun buscarFichas(lista: List<Ficha>, consulta: String, limite: Int = 30): List<Ficha> {
        val q = Buscador.normalizar(consulta)
        if (q.isBlank()) return emptyList()
        val palabras = q.split(" ").filter { it.isNotBlank() }
        return lista.asSequence().filter { f ->
            val heno = Buscador.normalizar("${f.nombre} ${f.chipset} ${f.gpu}")
            palabras.all { heno.contains(it) }
        }.take(limite).toList()
    }

    /**
     * Convierte una ficha en perfil. Aquí sí hay GPU y versión de Android, así que el
     * perfil sale más completo que el de Google — y con OpenGL ES calculado de la GPU.
     * El `device` no viene en la ficha: se deja vacío en vez de inventarlo.
     */
    fun perfilDeFicha(f: Ficha): PerfilDispositivo? {
        if (f.nombre.isBlank()) return null
        val partes = f.nombre.split(" ").filter { it.isNotBlank() }
        val marca = partes.firstOrNull().orEmpty()
        return PerfilDispositivo(
            id = "mob-" + f.nombre.lowercase()
                .map { if (it.isLetterOrDigit()) it else '-' }.joinToString("")
                .trim('-').replace(Regex("-+"), "-"),
            nombre = f.nombre,
            fabricante = marca,
            marca = marca,
            modelo = f.nombre,
            device = "",
            board = "",
            hardware = "",
            soc = f.chipset,
            gpu = f.gpu,
            openglEs = openglDe(f.gpu) ?: "",
            fuente = "Ficha técnica de mobolist.net",
            confianza = PerfilDispositivo.Confianza.PARCIAL,
            nota = if (f.android.isNotBlank())
                "El modelo declara Android ${f.android}. El `device` no viene en la ficha: " +
                    "queda vacío a propósito, porque no se sabe."
            else "El `device` no viene en la ficha: queda vacío a propósito.",
        )
    }

    /** Busca un SoC por nombre de GPU o de chip: «Adreno 610», «Helio G36». */
    fun buscarSoc(lista: List<Soc>, consulta: String, limite: Int = 25): List<Soc> {
        val q = Buscador.normalizar(consulta)
        if (q.isBlank()) return emptyList()
        return lista.filter {
            Buscador.normalizar(it.soc).contains(q) || Buscador.normalizar(it.gpu).contains(q)
        }.take(limite)
    }

    /**
     * El dato que hace falta para escribir `ro.opengles.version` sin romperlo.
     *
     * Devuelve el número (`mayor << 16 | menor`), no el texto. Escribir «OpenGL ES 3.2»
     * en esa propiedad no la mejora: la deja ilegible para el sistema.
     */
    fun openglDe(gpu: String): String? = when {
        gpu.contains("Adreno 8", true) || gpu.contains("Adreno 7", true) -> 196611.toString()
        gpu.contains("Adreno 6", true) || gpu.contains("Mali-G7", true) -> 196610.toString()
        gpu.contains("Adreno 5", true) || gpu.contains("Mali-G5", true) ||
            gpu.contains("Mali-G6", true) -> 196609.toString()
        gpu.contains("Adreno 4", true) || gpu.contains("Mali-T", true) -> 196608.toString()
        gpu.contains("Adreno 3", true) -> 196608.toString()
        else -> null
    }
}
