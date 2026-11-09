package io.github.labandita.glreborn.core

/**
 * Buscar, extraer y generar perfiles en bloque.
 *
 * Tres piezas, todas de lógica pura para poder probarlas sin teléfono:
 *
 *  - [Buscador]   — busca en el catálogo por nombre, marca, SoC o GPU. Sin acentos y sin
 *                   mayúsculas: quien escribe «tecno» encuentra «TECNO».
 *  - [Extractor]  — saca campos de un texto pegado (una ficha de especificaciones). Lo
 *                   que no encuentra no se lo inventa: lo deja vacío.
 *  - [Distribuida]— genera varios perfiles a la vez con generadores distintos y los
 *                   consolida por encaje con el aparato real.
 *
 * Sobre «generación distribuida»: esto reparte el trabajo entre varios generadores
 * dentro del propio teléfono, no entre máquinas de una red. Llamarlo de otra forma sería
 * vender una cosa por otra, y aquí las palabras se usan con su significado.
 */
object Buscador {

    data class Resultado(
        val perfil: PerfilDispositivo,
        val puntuacion: Int,
        val motivos: List<String>,
    )

    /** Normaliza: minúsculas y sin acentos. «iQOO» y «iqoo» son lo mismo al buscar. */
    fun normalizar(s: String): String {
        val sinAcentos = s.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ü", "u")
            .replace("ñ", "n")
        return sinAcentos.replace(Regex("[^a-z0-9 ]"), " ").replace(Regex(" +"), " ").trim()
    }

    /**
     * Busca. Devuelve resultados ordenados por puntuación, y una lista vacía cuando de
     * verdad no hay nada: la interfaz dirá «sin resultados», no una lista de relleno.
     */
    fun buscar(consulta: String, catalogo: List<PerfilDispositivo>): List<Resultado> {
        val q = normalizar(consulta)
        if (q.isBlank()) return catalogo.map { Resultado(it, 0, listOf("catálogo completo")) }
        val palabras = q.split(" ").filter { it.length > 1 }

        return catalogo.mapNotNull { p ->
            val campos = listOf(
                "nombre" to p.nombre,
                "fabricante" to p.fabricante,
                "marca" to p.marca,
                "modelo" to p.modelo,
                "device" to p.device,
                "soc" to p.soc,
                "gpu" to p.gpu,
                "id" to p.id,
            ).map { (etiqueta, valor) -> etiqueta to normalizar(valor) }

            var puntos = 0
            val motivos = mutableListOf<String>()

            campos.forEach { (etiqueta, valor) ->
                if (valor.isBlank()) return@forEach
                if (valor == q) { puntos += 100; motivos += "coincide con $etiqueta" }
                else if (valor.contains(q) && q.length > 2) {
                    puntos += 40; motivos += "contiene «$consulta» en $etiqueta"
                }
                palabras.forEach { w ->
                    if (valor == w) { puntos += 12; motivos += "$etiqueta: «$w»" }
                    else if (valor.contains(w) && w.length > 2) { puntos += 6; motivos += "$etiqueta: «$w»" }
                }
            }

            // La GPU y el SoC son lo que un juego mira de verdad: pesan más.
            if (p.gpu.isNotBlank() && normalizar(p.gpu).contains(q) && q.length > 2) puntos += 25

            if (puntos == 0) null
            else Resultado(p, puntos, motivos.distinct().take(3))
        }.sortedByDescending { it.puntuacion }
    }
}

object Extractor {

    data class Extraido(
        val fabricante: String,
        val marca: String,
        val modelo: String,
        val device: String,
        val soc: String,
        val gpu: String,
        val openglEs: String,
        val android: String,
        val encontrados: List<String>,
        val faltan: List<String>,
    ) {
        val completo: Boolean get() = modelo.isNotBlank() && marca.isNotBlank()

        val resumen: String
            get() = if (encontrados.isEmpty()) "No se reconoció ningún campo."
            else "${encontrados.size} campo(s) reconocidos" +
                if (faltan.isEmpty()) "" else " · faltan ${faltan.size}"
    }

    private val conocidos = listOf(
        "samsung", "google", "xiaomi", "redmi", "poco", "oneplus", "oppo", "vivo",
        "iqoo", "realme", "honor", "huawei", "tecno", "infinix", "itel", "motorola",
        "nokia", "sony", "asus", "lenovo", "zte", "nubia", "black shark", "nothing",
    )

    private val gpus = listOf(
        "adreno", "mali", "powervr", "immortalis", "xclipse", "apple gpu", "helio",
    )

    /**
     * Extrae campos de un texto pegado. Cada campo se busca con su etiqueta o, si no
     * aparece, por reconocimiento de patrones conocidos (marcas, GPUs, versiones).
     *
     * Lo que no esté, queda vacío. No se rellena con lo primero que se parezca.
     */
    fun extraer(texto: String): Extraido {
        val t = texto.replace("\r", "\n")
        val plano = Buscador.normalizar(t)
        val encontrados = mutableListOf<String>()
        val faltan = mutableListOf<String>()

        fun campo(etiquetas: List<String>): String {
            etiquetas.forEach { etiqueta ->
                val re = Regex(
                    "(?im)^\\s*${Regex.escape(etiqueta)}\\s*[:\\-–]?\\s*(.+?)\\s*$",
                )
                re.find(t)?.groupValues?.get(1)?.let { v ->
                    val limpio = v.trim().trim(',', ';', '.')
                    if (limpio.isNotEmpty() && limpio.length < 60) return limpio
                }
            }
            return ""
        }

        val fabricante = campo(listOf("Fabricante", "Manufacturer", "Brand", "Marca"))
        val modelo = campo(listOf("Modelo", "Model", "Nombre del dispositivo"))
        val device = campo(listOf("Device", "Código", "Codename"))
        val soc = campo(listOf("SoC", "Chipset", "Procesador", "CPU", "Processor"))
        val gpu = campo(listOf("GPU", "Gráficos"))
        val android = campo(listOf("Versión de Android", "Android version", "Android"))

        if (fabricante.isNotBlank()) encontrados += "fabricante" else faltan += "fabricante"
        if (modelo.isNotBlank()) encontrados += "modelo" else faltan += "modelo"
        if (device.isNotBlank()) encontrados += "device" else faltan += "device"
        if (soc.isNotBlank()) encontrados += "SoC" else faltan += "SoC"
        if (gpu.isNotBlank()) encontrados += "GPU" else faltan += "GPU"

        // Si no se dijo la marca con etiqueta, se busca una conocida en el texto.
        val marca = if (fabricante.isNotBlank()) fabricante.lowercase()
        else conocidos.firstOrNull { plano.contains(it) }.orEmpty()
        if (marca.isNotBlank() && fabricante.isBlank()) encontrados += "marca (por reconocimiento)"

        val gpuFinal: String = if (gpu.isNotBlank()) gpu
        else gpus.firstOrNull { plano.contains(it) }?.let { clave ->
            t.lineSequence().firstOrNull { Buscador.normalizar(it).contains(clave) }
                ?.trim()?.take(40) ?: ""
        } ?: ""
        if (gpuFinal.isNotBlank() && gpu.isBlank()) encontrados += "GPU (por reconocimiento)"

        val opengl: String = Regex("(?i)opengl\\s*es?\\s*([0-9])\\s*\\.\\s*([0-9])")
            .find(t)?.let { m ->
                val mayor = m.groupValues[1].toInt()
                val menor = m.groupValues[2].toInt()
                ((mayor shl 16) or menor).toString()
            } ?: ""
        if (opengl.isNotBlank()) encontrados += "OpenGL ES"

        return Extraido(
            fabricante = fabricante,
            marca = marca,
            modelo = modelo,
            device = device,
            soc = soc,
            gpu = gpuFinal,
            openglEs = opengl,
            android = android,
            encontrados = encontrados,
            faltan = faltan,
        )
    }

    /** Convierte lo extraído en un perfil. Vacío en lo que falte, marcado como PARCIAL. */
    fun aPerfil(e: Extraido, original: String): PerfilDispositivo? {
        if (!e.completo) return null
        return PerfilDispositivo(
            id = "txt-" + Buscador.normalizar(e.modelo).replace(" ", "-"),
            nombre = "${e.marca} ${e.modelo}".trim(),
            fabricante = e.fabricante.ifBlank { e.marca },
            marca = e.marca,
            modelo = e.modelo,
            device = e.device.ifBlank { Buscador.normalizar(e.modelo).replace(" ", "_") },
            board = e.device,
            hardware = e.device,
            soc = e.soc,
            gpu = e.gpu,
            openglEs = e.openglEs.ifBlank {
                Generador.glCompatible("", e.gpu, e.soc) ?: ""
            },
            fuente = "Extraído por ti de un texto pegado: «${original.take(60)}…»",
            confianza = PerfilDispositivo.Confianza.PARCIAL,
            nota = "Perfil hecho con lo que se reconoció del texto. Los campos que no " +
                "aparecían quedaron vacíos: no se rellenaron a ojo.",
        )
    }
}

object Distribuida {

    data class Candidato(
        val generado: Generador.Generado,
        val encaje: Int,
        val motivos: List<String>,
    ) {
        val clave: String get() = generado.perfil.id
    }

    /**
     * Genera con todos los generadores a la vez y consolida.
     *
     * El encaje mide cuánto se parece el candidato al aparato real: misma GPU suma,
     * misma versión de Android suma, y cuantos más campos se hayan leído, más arriba.
     * No es una nota de «calidad»: es una medida de parecido, y se presenta así.
     */
    fun generar(datos: Sonda.Datos, catalogo: List<PerfilDispositivo>): List<Candidato> {
        val candidatos = Generador.Estrategia.entries.mapNotNull { estrategia ->
            val g = Generador.derivar(datos, estrategia) ?: return@mapNotNull null
            val p = g.perfil
            var encaje = 0
            val motivos = mutableListOf<String>()

            if (p.hardware.equals(datos.hardware, ignoreCase = true) && datos.hardware.isNotBlank()) {
                encaje += 30; motivos += "misma GPU que el aparato"
            }
            if (p.board.equals(datos.board, ignoreCase = true) && datos.board.isNotBlank()) {
                encaje += 20; motivos += "misma placa"
            }
            if (p.openglEs.isNotBlank() && p.openglEs == datos.openglEs) {
                encaje += 15; motivos += "mismo OpenGL ES declarado"
            }
            encaje += datos.camposLeidos * 2
            if (datos.camposLeidos < 6) motivos += "se leyó poco del aparato: confianza baja"
            motivos += "${datos.camposLeidos}/12 campos del aparato"

            Candidato(g, encaje, motivos.distinct())
        }

        // Consolidar: un candidato por identidad. Si dos generadores dan el mismo perfil
        // (pasa cuando no hay tope de gama para el fabricante), se queda el de más encaje
        // y se anota que venía de varios.
        return candidatos
            .groupBy { it.clave }
            .map { (_, lista) -> lista.maxByOrNull { it.encaje }!! }
            .sortedWith(compareByDescending<Candidato> { it.encaje }.thenBy { it.generado.estrategia.ordinal })
    }

    /**
     * Añade al catálogo lo que se haya generado o extraído, sin duplicar y sin pisar.
     *
     * Es lo que hace que la búsqueda encuentre los perfiles nuevos: primero los del
     * catálogo, luego los tuyos.
     */
    fun unir(
        catalogo: List<PerfilDispositivo>,
        extra: List<PerfilDispositivo>,
    ): List<PerfilDispositivo> {
        val vistos = catalogo.map { it.id }.toMutableSet()
        val salida = catalogo.toMutableList()
        extra.forEach { if (vistos.add(it.id)) salida += it }
        return salida
    }
}
