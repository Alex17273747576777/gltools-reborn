package io.github.labandita.glreborn.ui

import io.github.labandita.glreborn.core.Modo

/**
 * La arquitectura de navegación, en un solo sitio.
 *
 * Antes había diez secciones planas en una píldora: cabían de milagro, no había jerarquía
 * y la primera pantalla era un catálogo de perfiles, no un resumen de lo que está pasando.
 *
 * Ahora hay **cinco destinos** —los mismos que el GLTools original: Home, Perfiles,
 * Herramientas, Datos y Ayuda— y cada uno tiene sus subpestañas. Se ve dónde estás sin
 * leer nada, y ninguna píldora pasa de cinco.
 *
 * Esto es lógica pura: se prueba sin teléfono.
 */
enum class Destino(val titulo: String) {
    HOME("Home"),
    PERFILES("Perfiles"),
    HERRAMIENTAS("Herram."),
    DATOS("Datos"),
    AYUDA("Ayuda"),
}

/** Subpestañas de cada destino. El orden es el que se ve. */
enum class Pestana(val destino: Destino, val titulo: String) {
    // Perfiles
    CATALOGO(Destino.PERFILES, "Catálogo"),
    APLICAR(Destino.PERFILES, "Aplicar"),
    GENERAR(Destino.PERFILES, "Generar"),

    // Herramientas
    CONFIGURAR(Destino.HERRAMIENTAS, "Configurar"),
    SUPERPOSICION(Destino.HERRAMIENTAS, "Mira"),
    RENDIMIENTO(Destino.HERRAMIENTAS, "Rendimiento"),
    APPS(Destino.HERRAMIENTAS, "Apps"),
    PANTALLA(Destino.HERRAMIENTAS, "Pantalla"),
    RED(Destino.HERRAMIENTAS, "Red"),

    // Datos
    TOP500(Destino.DATOS, "TOP500"),
    HISTORIAL(Destino.DATOS, "Historial"),
    APLICADO(Destino.DATOS, "Aplicado"),
    DISPOSITIVOS(Destino.DATOS, "Dispositivos"),
    RANKING(Destino.DATOS, "Ranking"),

    // Ayuda
    COMPATIBILIDAD(Destino.AYUDA, "Compat."),
    DIAGNOSTICO(Destino.AYUDA, "Diagnóstico"),
    LIMITES(Destino.AYUDA, "Límites"),
    FUENTES(Destino.AYUDA, "Fuentes"),
    DIAGNOSTICO_AUTO(Destino.AYUDA, "Autom."),
    PENETRACION(Destino.AYUDA, "Penetración"),
}

object Navegacion {

    /** Las subpestañas de un destino, en orden. */
    fun pestanas(destino: Destino): List<Pestana> =
        Pestana.entries.filter { it.destino == destino }

    /** La primera subpestaña, la que se abre al entrar. */
    fun inicial(destino: Destino): Pestana? = pestanas(destino).firstOrNull()

    /** ¿Ese destino usa subpestañas? Home no: es un resumen. */
    fun tienePestanas(destino: Destino): Boolean = pestanas(destino).isNotEmpty()

    /**
     * Dónde estaba antes el contenido de la app, para no perder nada en el cambio:
     * cada sección antigua tiene ahora su sitio.
     */
    val mapaDesdeLaVersionAnterior: Map<String, Pestana> = mapOf(
        "Perfiles" to Pestana.CATALOGO,
        "Super" to Pestana.TOP500,
        "Aplicaciones" to Pestana.APPS,
        "Útil" to Pestana.RENDIMIENTO,
        "GPU" to Pestana.DIAGNOSTICO,
        "Diag." to Pestana.DIAGNOSTICO,
        "Compat." to Pestana.COMPATIBILIDAD,
        "Historial" to Pestana.HISTORIAL,
        "Aplicado" to Pestana.APLICADO,
        "Aprender" to Pestana.GENERAR,
        "Rendi." to Pestana.RENDIMIENTO,
        "Configurar" to Pestana.CONFIGURAR,
        "Punto de mira" to Pestana.SUPERPOSICION,
    )

    fun destinoDe(pestana: Pestana): Destino = pestana.destino
}

/**
 * El texto del banner de estado, igual que el del original pero sin mentir.
 *
 * El GLTools original pone un cartel verde enorme: «Rooted mode — GLTools is running in
 * the best mode possible, congratulations!». Es lo primero que se ve, y aquí hace falta
 * lo mismo: un estado grande y legible. Lo que cambia es el contenido: aquí dice lo que
 * hay de verdad, y cuando no hay nada dice que no hay nada.
 */
data class Banner(
    val titulo: String,
    val detalle: String,
    val tono: Tono,
) {
    enum class Tono { BIEN, AVISO, MAL }

    val esBueno: Boolean get() = tono == Tono.BIEN
}

object Estado {

    /**
     * Construye el banner con datos medidos. [conRoot] y [conShizuku] son hechos
     * comprobados, no deseos: si ninguno es cierto, el banner lo dice en rojo.
     */
    fun banner(conRoot: Boolean, conShizuku: Boolean, conPermiso: Boolean, modo: Modo): Banner =
        when {
            conRoot -> Banner(
                "Modo root",
                "Root disponible: se puede escribir con su. Shizuku no hace falta.",
                Banner.Tono.BIEN,
            )
            conShizuku && conPermiso -> Banner(
                "Modo Shizuku",
                "Shizuku activo y con permiso: los mismos permisos que un `adb shell`. " +
                    "No es root: no toca /system, ni el kernel, ni SELinux.",
                Banner.Tono.BIEN,
            )
            conShizuku -> Banner(
                "Shizuku sin permiso",
                "Shizuku está instalado pero no autorizó esta app. Ábrelo y dale permiso, " +
                    "o la app no podrá cambiar nada.",
                Banner.Tono.AVISO,
            )
            else -> Banner(
                "Sin vía",
                "Ni Shizuku con permiso ni root. No se puede escribir: esta app no va a " +
                    "fingir que sí.",
                Banner.Tono.MAL,
            )
        }

    /** Lo que se puede hacer con ese estado, en una línea, sin promesas. */
    fun puede(conRoot: Boolean, conShizuku: Boolean, conPermiso: Boolean): String = when {
        conRoot -> "Puede: aplicar perfiles, escribir propiedades y usar la shell."
        conShizuku && conPermiso -> "Puede: aplicar perfiles, escribir propiedades y usar la shell."
        else -> "Puede: leer el teléfono, buscar en los datos y generar perfiles. " +
            "NO puede escribir."
    }

    /**
     * Avisos del Home. Se construyen con datos y cada uno dice qué hacer.
     * Si no hay nada que avisar, la lista va vacía y no se pinta.
     */
    fun avisos(
        conVia: Boolean,
        modo: Modo,
        resumenCompat: String,
        tienePerfil: Boolean,
        irreversibleAplicado: Boolean,
        sinApps: Boolean,
    ): List<Par> {
        val lista = mutableListOf<Par>()
        if (!conVia) lista += Par(
            "Sin vía para escribir",
            "Activa Shizuku por depuración inalámbrica y dale permiso. Mientras tanto, " +
                "todo lo de leer y buscar funciona.",
            Banner.Tono.MAL,
        )
        if (modo == Modo.EXPERTO) lista += Par(
            "Modo experto activo",
            io.github.labandita.glreborn.core.ReglasPromesas.recuperacionExperto(conVia),
            Banner.Tono.AVISO,
        )
        if (resumenCompat.contains("no funciona", true)) lista += Par(
            "Hay pruebas de compatibilidad que fallan",
            "Mira Ayuda → Compat.: dice exactamente qué no responde en tu teléfono.",
            Banner.Tono.AVISO,
        )
        if (!tienePerfil) lista += Par(
            "Ningún perfil elegido",
            "Elige uno en Perfiles → Catálogo, o genera uno con lo que tu teléfono declara.",
            Banner.Tono.AVISO,
        )
        if (irreversibleAplicado) lista += Par(
            "Hay algo aplicado sin vuelta atrás",
            "En Datos → Aplicado está el detalle por app. Lo irreversible se marca en rojo.",
            Banner.Tono.AVISO,
        )
        if (sinApps) lista += Par(
            "No se pudo leer la lista de apps",
            "Android puede estar ocultando el catálogo. Está explicado en Ayuda → Límites.",
            Banner.Tono.AVISO,
        )
        return lista
    }

    data class Par(val titulo: String, val detalle: String, val tono: Banner.Tono)
}
