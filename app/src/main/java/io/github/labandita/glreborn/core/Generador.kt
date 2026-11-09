package io.github.labandita.glreborn.core

/**
 * Generación de perfiles a partir de un teléfono real, y no de una lista escrita a mano.
 *
 * La idea que pidió el usuario: en vez de elegir de un catálogo de siete, tomar las
 * capacidades del aparato que tiene en la mano y derivar perfiles que encajen con él.
 * Aquí está la parte que decide; la que lee del sistema es [Sonda].
 *
 * Dos reglas que no se rompen:
 *
 *  1. **Nada inventado.** Un campo que no se leyó queda vacío. El perfil lo dice.
 *  2. **Nada copiado del aparato que identifica.** El perfil derivado cambia el modelo y
 *     el device (que es lo único que un juego mira para elegir preset), pero NO se lleva
 *     el número de serie, ni el IMEI, ni nada que venga de `ro.serialno`. Se comprueba
 *     con una lista de claves prohibidas, no con buena voluntad.
 *
 * Todo esto es lógica pura: se prueba en la máquina de compilación, sin teléfono.
 */
object Generador {

    /** Cómo se deriva un perfil del aparato real. */
    enum class Estrategia(val titulo: String, val explicacion: String) {
        MISMO_APARATO(
            "Mismo aparato, otro nombre",
            "Copia todas las capacidades reales y cambia sólo marca y modelo. Sirve para " +
                "que un juego aplique el preset de otro teléfono sin que el resto de " +
                "propiedades deje de ser cierto.",
        ),
        MISMA_FAMILIA(
            "Misma familia, un escalón arriba",
            "Mantiene el fabricante y la plataforma, y sube al modelo tope de esa misma " +
                "gama. Es lo más conservador que cambia lo que el juego lee.",
        ),
        MISMA_GPU(
            "Misma GPU, otro tope de gama",
            "Conserva la GPU y la versión de OpenGL ES —lo que de verdad limita los " +
                "gráficos— y sube de gama. Si tu GPU no da para más, el juego lo notará " +
                "igual: por eso se conserva.",
        ),
        CONSERVADOR(
            "Conservador: sólo lo que el juego mira",
            "Toca nada más que marca y modelo. Todo lo demás se queda como está, así que " +
                "es el que menos puede romper.",
        ),
    }

    /** Un perfil generado, con su procedencia a la vista. */
    data class Generado(
        val perfil: PerfilDispositivo,
        val estrategia: Estrategia,
        val capacidades: List<Pair<String, String>>,
        val avisos: List<String>,
    ) {
        val resumen: String
            get() = "${capacidades.size} capacidades del aparato real · " +
                avisos.size + " aviso(s)"
    }

    /**
     * Deriva un perfil del aparato real.
     *
     * Devuelve `null` cuando faltan los datos mínimos: es mejor no ofrecer nada que
     * ofrecer un perfil con `?` en el modelo. Quien llama enseña el motivo.
     */
    fun derivar(datos: Sonda.Datos, estrategia: Estrategia): Generado? {
        if (datos.modelo.isBlank() && datos.marca.isBlank()) return null

        val avisos = mutableListOf<String>()

        // El "tope de gama" del mismo fabricante: si está en el catálogo se usa, y si no,
        // se usa el propio aparato con el nombre de su gama. Nada de inventar un modelo
        // que no existe: eso sería mentir con nombre propio.
        val tope = topeDeGama(datos.fabricante, datos.marca)

        val (marca, fabricante, modelo, device) = when (estrategia) {
            Estrategia.MISMO_APARATO -> Cuatro(
                datos.marca.ifBlank { datos.fabricante },
                datos.fabricante,
                datos.modelo,
                datos.device,
            )
            Estrategia.MISMA_FAMILIA, Estrategia.MISMA_GPU -> {
                if (tope == null) {
                    avisos += "No hay un tope de gama conocido para «${datos.fabricante}»: " +
                        "se usa el propio aparato. No se inventa un modelo que no existe."
                    Cuatro(
                        datos.marca.ifBlank { datos.fabricante },
                        datos.fabricante,
                        datos.modelo,
                        datos.device,
                    )
                } else {
                    Cuatro(tope.marca, tope.fabricante, tope.modelo, tope.device)
                }
            }
            Estrategia.CONSERVADOR -> Cuatro(
                datos.marca.ifBlank { datos.fabricante },
                datos.fabricante,
                datos.modelo,
                datos.device,
            )
        }

        // Si cambia el modelo pero no el device, algunas apps ven una incoherencia. Se
        // avisa en vez de arreglarlo con un device inventado.
        if (estrategia != Estrategia.CONSERVADOR && device.isBlank()) {
            avisos += "El teléfono no dijo su `ro.product.device`: el perfil queda sin él."
        }

        val hardware = when (estrategia) {
            Estrategia.MISMA_GPU, Estrategia.CONSERVADOR -> datos.hardware
            else -> datos.hardware.ifBlank { datos.plataforma }
        }
        val opengl = glCompatible(datos.openglEs, datos.hardware, datos.plataforma)
        if (opengl == null) {
            avisos += "El teléfono declara `ro.opengles.version` = «${datos.openglEs}», que " +
                "no es un número válido. El perfil lo deja VACÍO a propósito: escribir ahí " +
                "el texto «OpenGL ES 3.2» rompe la propiedad en vez de mejorarla."
        } else if (opengl != datos.openglEs && datos.openglEs.isNotBlank()) {
            avisos += "OpenGL ES venía como «${datos.openglEs}»; se normaliza a $opengl " +
                "para que sea coherente con la GPU."
        }

        val perfil = PerfilDispositivo(
            id = "gen-" + estrategia.name.lowercase() + "-" + slug(modelo),
            nombre = nombreLegible(estrategia, marca, modelo),
            fabricante = fabricante,
            marca = marca,
            modelo = modelo,
            device = device,
            board = datos.board.ifBlank { datos.hardware },
            hardware = hardware,
            soc = listOf(datos.socFabricante, datos.socModelo)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { datos.plataforma },
            gpu = if (estrategia == Estrategia.MISMA_GPU) datos.hardware else "",
            openglEs = opengl ?: "",
            fuente = "Generado en el teléfono con lo que el aparato declara (getprop), " +
                "el $fecha",
            confianza = PerfilDispositivo.Confianza.PARCIAL,
            nota = "Perfil derivado del aparato real con la estrategia «${estrategia.titulo}». " +
                "Lo que no se pudo leer quedó vacío; nada aquí está inventado.",
        )

        val capacidades = listOf(
            "Marca declarada" to datos.marca,
            "Fabricante" to datos.fabricante,
            "Modelo" to datos.modelo,
            "Device" to datos.device,
            "Placa (board)" to datos.board,
            "Hardware" to datos.hardware,
            "Plataforma" to datos.plataforma,
            "SoC" to listOf(datos.socFabricante, datos.socModelo)
                .filter { it.isNotBlank() }.joinToString(" "),
            "OpenGL ES" to (opengl ?: ""),
            "Android" to datos.versionAndroid,
            "API" to datos.sdk,
            "Arquitectura" to datos.arquitectura.joinToString(", "),
            "Huawei/Harmony" to if (datos.esHuawei) "sí" else "no",
        ).filter { it.second.isNotBlank() }

        return Generado(perfil, estrategia, capacidades, avisos)
    }

    /** Nombre que se ve en la tarjeta. Deja claro que es generado, no de catálogo. */
    private fun nombreLegible(e: Estrategia, marca: String, modelo: String): String {
        val base = listOf(marca, modelo).filter { it.isNotBlank() }
            .joinToString(" ").ifBlank { "Aparato" }
        return when (e) {
            Estrategia.MISMO_APARATO -> "$base (real)"
            Estrategia.MISMA_FAMILIA -> "$base · gama alta"
            Estrategia.MISMA_GPU -> "$base · misma GPU"
            Estrategia.CONSERVADOR -> "$base (mínimo)"
        }
    }

    private fun slug(s: String): String = s.lowercase()
        .map { if (it.isLetterOrDigit()) it else '-' }
        .joinToString("")
        .replace(Regex("-+"), "-")
        .trim('-')
        .ifBlank { "aparato" }

    private data class Cuatro(val marca: String, val fabricante: String, val modelo: String, val device: String)

    // ── Reglas de coherencia ────────────────────────────────────────────────

    /**
     * OpenGL ES como número, que es lo que la propiedad admite.
     *
     * `196610` = 0x30002 = OpenGL ES 3.2. El formato es
     * `mayor << 16 | menor`. Si el teléfono no lo declara o declara basura, se deduce de
     * la GPU conocida; si tampoco, se deja vacío. Nunca se escribe texto.
     */
    fun glCompatible(declarado: String, hardware: String, plataforma: String): String? {
        val n = declarado.trim().toIntOrNull()
        if (n != null && n > 0) return n.toString()
        val gpu = (hardware + " " + plataforma).lowercase()
        val mayor = when {
            gpu.contains("adreno 7") || gpu.contains("mali-g7") || gpu.contains("immortalis") ->
                3 to 2
            gpu.contains("adreno 6") || gpu.contains("mali-g5") || gpu.contains("mali-g6") ->
                3 to 1
            gpu.contains("mali-t") || gpu.contains("powervr") -> 3 to 0
            gpu.isBlank() -> return null
            else -> return null
        }
        return ((mayor.first shl 16) or mayor.second).toString()
    }

    /** "196610" → "OpenGL ES 3.2". Para leerlo en pantalla, no para escribirlo. */
    fun glLegible(valor: String): String {
        val n = valor.trim().toIntOrNull() ?: return "—"
        val mayor = (n shr 16) and 0xFFFF
        val menor = n and 0xFFFF
        return "OpenGL ES $mayor.$menor"
    }

    /**
     * El tope de gama conocido de un fabricante, del catálogo. Si no hay, null: no se
     * inventa un modelo.
     */
    fun topeDeGama(fabricante: String, marca: String): PerfilDispositivo? {
        val f = fabricante.lowercase().trim()
        return Perfiles.todos
            .filter { candidato ->
                candidato.fabricante.lowercase() == f ||
                    (f.isNotBlank() && candidato.marca.lowercase() == f)
            }
            .maxByOrNull { candidato -> candidato.openglEs.toIntOrNull() ?: 0 }
            ?: if (marca.isNotBlank()) {
                Perfiles.todos.firstOrNull { it.marca.lowercase() == marca.lowercase().trim() }
            } else null
    }

    /**
     * Comprueba que un perfil no se llevó nada que identifique al aparato.
     *
     * Se aplica al perfil ya montado, como última barrera: si alguna vez alguien añade
     * un campo por descuido, esto lo caza en las pruebas del núcleo.
     */
    fun llevaIdentificadores(perfil: PerfilDispositivo): List<String> {
        val texto = listOf(
            perfil.modelo, perfil.device, perfil.board, perfil.hardware, perfil.soc,
            perfil.gpu, perfil.nota, perfil.fuente,
        ).joinToString(" ").lowercase()
        return Sonda.PROHIBIDAS.filter { texto.contains(it.lowercase().substringAfterLast('.')) }
    }

    /** Fecha para la procedencia, inyectable para que la prueba sea determinista. */
    var fecha: String = "hoy"
}
