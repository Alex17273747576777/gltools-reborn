package io.github.labandita.glreborn.core

/**
 * Búsqueda única: una caja, y todo lo que la app sabe.
 *
 * Por qué existe: hasta la 1.9.9 había que saber **dónde** estaba cada cosa para encontrarla.
 * El modelo de un teléfono estaba en Perfiles, una acción de rendimiento en Herramientas, una
 * ficha en Datos, un ajuste en Configurar. Saber qué buscar y no saber dónde es exactamente el
 * problema que resuelve una búsqueda global.
 *
 * Lo que busca, en este orden de importancia:
 *  1. **Acciones de la app** («congelar», «dns», «refresco»): llevan a la pantalla que las hace.
 *  2. **Perfiles** (del catálogo y los propios): llevan al catálogo con ese perfil.
 *  3. **Ajustes y secciones** («mira», «respaldo», «compatibilidad»).
 *  4. **Dispositivos del catálogo** (54.000 fichas): contestan «¿existe este teléfono?».
 *
 * **Lo que NO hace:** no pide nada a internet, no busca en apps ajenas, no lee tus ficheros. Y no
 * inventa resultados: si no encuentra, dice que no encuentra y enseña qué sí hay.
 */
object BusquedaGlobal {

    enum class Tipo { ACCION, PERFIL, SECCION, DISPOSITIVO }

    data class Destino(val destino: String, val pestana: String?)

    data class Hallazgo(
        val titulo: String,
        val detalle: String,
        val tipo: Tipo,
        val ir: Destino,
        /** Cuánto casa: se usa para ordenar. */
        val puntos: Int,
    )

    /** Una sección de la app, para que se pueda llegar a ella escribiendo su nombre. */
    private data class Seccion(
        val titulo: String,
        val detalle: String,
        val palabras: List<String>,
        val ir: Destino,
    )

    private val secciones = listOf(
        Seccion("Identidad en uso", "qué dispositivo cree el teléfono que es",
            listOf("identidad", "perfil", "aplicar", "modelo"), Destino("PERFILES", "CATALOGO")),
        Seccion("Catálogo de perfiles", "elegir y filtrar por confianza",
            listOf("catalogo", "perfiles", "verificados", "filtrar"), Destino("PERFILES", "CATALOGO")),
        Seccion("Generar un perfil", "a partir de lo que tu teléfono declara",
            listOf("generar", "crear", "sonda", "propio"), Destino("PERFILES", "GENERAR")),
        Seccion("Llevarte los perfiles", "exportar e importar sin cuenta",
            listOf("respaldo", "exportar", "importar", "backup", "json"), Destino("PERFILES", "GENERAR")),
        Seccion("Punto de mira y FPS", "encima del juego, sin tapar los toques",
            listOf("mira", "crosshair", "fps", "superposicion", "overlay", "juego"),
            Destino("HERRAMIENTAS", "SUPERPOSICION")),
        Seccion("Configurar el sistema", "animaciones, DNS, resolución, refresco",
            listOf("configurar", "ajustes", "animaciones", "dns", "resolucion", "refresco"),
            Destino("HERRAMIENTAS", "CONFIGURAR")),
        Seccion("Rendimiento", "acciones sin root, una a una",
            listOf("rendimiento", "congelar", "cache", "fps", "acciones"), Destino("HERRAMIENTAS", "RENDIMIENTO")),
        Seccion("Apps instaladas", "todas, sin lista blanca ni cuota",
            listOf("apps", "instaladas", "congeladas", "paquetes"), Destino("HERRAMIENTAS", "APPS")),
        Seccion("Pantalla", "resolución, densidad y refresco",
            listOf("pantalla", "resolucion", "densidad", "hz", "refresco"), Destino("HERRAMIENTAS", "PANTALLA")),
        Seccion("Red y DNS", "privacidad de nombres",
            listOf("red", "dns", "privado", "doh"), Destino("HERRAMIENTAS", "RED")),
        Seccion("Compatibilidad", "qué funciona en este teléfono y qué no",
            listOf("compatibilidad", "probado", "pruebas", "shizuku"), Destino("AYUDA", "COMPATIBILIDAD")),
        Seccion("Datos y fuentes", "de dónde sale cada dato",
            listOf("datos", "fuentes", "top500", "dispositivos", "catalogo"), Destino("DATOS", "TOP500")),
        Seccion("Cambios aplicados", "qué se cambió y cómo se deshace",
            listOf("aplicado", "registro", "deshacer", "restaurar", "historial"), Destino("DATOS", "APLICADO")),
    )

    /**
     * Busca. `limite` corta por arriba para que la lista no se vuelva infinita en un teléfono
     * lento; los resultados ya vienen ordenados por cuánto casan.
     */
    fun buscar(
        consulta: String,
        perfiles: List<PerfilDispositivo>,
        biblioteca: List<PerfilDispositivo>,
        dispositivos: List<Datos.Dispositivo>,
        limite: Int = 40,
    ): List<Hallazgo> {
        val q = Buscador.normalizar(consulta)
        if (q.length < 2) return emptyList()
        val palabras = q.split(" ").filter { it.isNotBlank() }
        val salida = ArrayList<Hallazgo>()

        // 1. Secciones y acciones.
        secciones.forEach { s ->
            val texto = Buscador.normalizar("${s.titulo} ${s.detalle} ${s.palabras.joinToString(" ")}")
            val puntos = puntosDe(texto, palabras, s.titulo)
            if (puntos > 0) {
                salida += Hallazgo(s.titulo, s.detalle, Tipo.SECCION, s.ir, puntos + 30)
            }
        }

        // 2. Perfiles propios primero (son tuyos), después el catálogo.
        fun buscarPerfil(p: PerfilDispositivo, propio: Boolean) {
            val texto = Buscador.normalizar(
                "${p.nombre} ${p.fabricante} ${p.marca} ${p.modelo} ${p.device} ${p.soc} ${p.gpu}",
            )
            val puntos = puntosDe(texto, palabras, p.nombre)
            if (puntos > 0) {
                salida += Hallazgo(
                    p.nombre,
                    listOf(p.fabricante, p.soc, p.gpu).filter { it.isNotBlank() }.joinToString(" · ") +
                        if (propio) " · tu perfil" else "",
                    Tipo.PERFIL,
                    Destino("PERFILES", "CATALOGO"),
                    puntos + if (propio) 20 else 0,
                )
            }
        }
        biblioteca.forEach { buscarPerfil(it, propio = true) }
        perfiles.forEach { buscarPerfil(it, propio = false) }

        // 3. Dispositivos del catálogo: sólo si lo escrito puede ser un modelo.
        dispositivos.forEach { d ->
            val texto = Buscador.normalizar("${d.marca} ${d.comercial} ${d.modelo} ${d.device}")
            val puntos = puntosDe(texto, palabras, d.modelo)
            if (puntos > 0) {
                salida += Hallazgo(
                    d.legible,
                    "en el catálogo de dispositivos",
                    Tipo.DISPOSITIVO,
                    Destino("DATOS", "DISPOSITIVOS"),
                    puntos - 10,
                )
            }
        }

        // Sin repetidos: un perfil puede estar en el catálogo y en tu biblioteca.
        val vistos = HashSet<String>()
        return salida
            .sortedByDescending { it.puntos }
            .filter { vistos.add("${it.tipo}|${it.titulo}|${it.detalle}") }
            .take(limite)
    }

    private fun puntosDe(texto: String, palabras: List<String>, titulo: String): Int {
        val t = Buscador.normalizar(titulo)
        var puntos = 0
        palabras.forEach { p ->
            puntos += when {
                t.startsWith(p) -> 12
                t.contains(p) -> 8
                texto.contains(p) -> 4
                else -> 0
            }
        }
        // Todas las palabras tienen que aparecer en algún sitio: «pixel gpu» no debe devolver
        // todo lo que tenga «pixel».
        return if (palabras.all { texto.contains(it) }) puntos else 0
    }

    /** Qué enseñar cuando no hay nada: no un vacío, sino lo que sí se puede buscar. */
    fun sugerencias(): List<String> = listOf(
        "mira", "congelar", "dns", "pixel", "exportar", "refresco", "compatibilidad",
    )
}
