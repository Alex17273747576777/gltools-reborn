package io.github.labandita.glreborn.core

import android.content.Context
import java.io.File

/**
 * El perfil del auto-diagnóstico: qué se mide, cuánto se mide y cuándo se avisa.
 *
 * Por qué es un perfil y no un montón de casillas: en un teléfono de 4 GB, medir cuesta batería y
 * tiempo. El usuario decide si quiere el diagnóstico rápido de dos segundos o el completo de diez,
 * y esa decisión se guarda. Se guarda en texto plano `clave=valor` en `filesDir`, no en JSON:
 * el mismo formato se puede leer y probar sin Android de por medio, y se puede abrir a mano.
 */
data class PerfilDiagnostico(
    val nombre: String = "Equilibrado",
    /** Si es `false`, sólo se mide lo barato: memoria, almacenamiento, batería y estado. */
    val profundo: Boolean = true,
    /** Pruebas a las que el usuario dijo que sí. Las que no estén, no se ejecutan. */
    val pruebas: Set<String> = TODAS,
    /** Cuántos milisegundos dura la prueba de CPU. Más tiempo, más estable el número. */
    val msCpu: Int = 400,
    /** Aviso si el uso de memoria pasa de este porcentaje. */
    val umbralMemoriaPct: Int = 90,
    /** Aviso si la batería pasa de esta temperatura. */
    val umbralBateriaC: Double = 42.0,
    /** Generar el informe al cerrar la app. */
    val alCerrar: Boolean = true,
    /** Sonido al terminar, además de la notificación. */
    val conSonido: Boolean = true,
) {
    companion object {
        const val MEMORIA = "memoria"
        const val ALMACENAMIENTO = "almacenamiento"
        const val BATERIA = "bateria"
        const val CPU = "cpu"
        const val VULKAN = "vulkan"
        const val RED = "red"
        const val RAM = "ram"
        const val IO = "io"

        /** Nuevo en la 1.14.0: estado térmico y margen térmico del sistema. */
        const val TERMICO = "termico"

        val TODAS = setOf(MEMORIA, ALMACENAMIENTO, BATERIA, CPU, TERMICO, VULKAN, RED, RAM, IO)

        /** Lo que era «todas» antes de existir la prueba térmica. Sirve para migrar, no para elegir. */
        val TODAS_ANTERIORES = setOf(MEMORIA, ALMACENAMIENTO, BATERIA, CPU, VULKAN, RED, RAM, IO)
    }
}

/** Las reglas del perfil, sin Android: validar, codificar y decodificar. */
object ReglasPerfilDiagnostico {

    val VALORES: Map<String, List<String>> = mapOf(
        "profundo" to listOf("sí", "no"),
        "msCpu" to listOf("150", "400", "1000"),
        "alCerrar" to listOf("sí", "no"),
        "conSonido" to listOf("sí", "no"),
    )

    /**
     * Un perfil a medio escribir no puede romper la app: se corrige lo imposible y se dice qué se
     * corrigió. Devolver un perfil válido y silencioso sería mentir sobre lo que el usuario pidió.
     */
    fun validar(p: PerfilDiagnostico): Pair<PerfilDiagnostico, List<String>> {
        val avisos = mutableListOf<String>()
        val ms = p.msCpu.coerceIn(50, 5000)
        if (ms != p.msCpu) avisos += "la duración de la prueba de CPU se ajustó a $ms ms (50–5000)"
        val mem = p.umbralMemoriaPct.coerceIn(50, 99)
        if (mem != p.umbralMemoriaPct) avisos += "el umbral de memoria se ajustó a $mem % (50–99)"
        val bat = p.umbralBateriaC.coerceIn(30.0, 60.0)
        if (bat != p.umbralBateriaC) avisos += "el umbral de batería se ajustó a $bat °C (30–60)"
        val migradas = migrar(p.pruebas)
        if (migradas.size != p.pruebas.size) {
            avisos += "el perfil se guardó antes de existir la prueba térmica: «todas» ahora la incluye"
        }
        val pruebas = migradas.intersect(PerfilDiagnostico.TODAS)
        if (pruebas.isEmpty()) avisos += "sin ninguna prueba marcada se mide lo mínimo: memoria y estado"
        if (pruebas.size != migradas.size) avisos += "se descartaron pruebas que no existen"
        val nombre = p.nombre.trim().ifBlank {
            avisos += "el perfil no tenía nombre: se llamó «Personalizado»"
            "Personalizado"
        }
        return p.copy(nombre = nombre, msCpu = ms, umbralMemoriaPct = mem,
            umbralBateriaC = bat, pruebas = pruebas.ifEmpty { setOf(PerfilDiagnostico.MEMORIA) }) to avisos
    }

    /** Codifica. Ordenado: el mismo perfil produce siempre el mismo texto (y se puede comparar). */
    fun codificar(p: PerfilDiagnostico): String = buildString {
        appendLine("# perfil del auto-diagnóstico · GLTools Reborn")
        appendLine("nombre=${p.nombre}")
        appendLine("profundo=${p.profundo}")
        appendLine("pruebas=${p.pruebas.sorted().joinToString(",")}")
        appendLine("msCpu=${p.msCpu}")
        appendLine("umbralMemoriaPct=${p.umbralMemoriaPct}")
        appendLine("umbralBateriaC=${p.umbralBateriaC}")
        appendLine("alCerrar=${p.alCerrar}")
        appendLine("conSonido=${p.conSonido}")
    }

    /** Decodifica sin lanzar: lo que no entienda se queda en el valor por defecto. */
    fun decodificar(texto: String): PerfilDiagnostico {
        val base = PerfilDiagnostico()
        val mapa = texto.lines()
            .filter { it.contains("=") && !it.trimStart().startsWith("#") }
            .associate { linea ->
                val i = linea.indexOf('=')
                linea.substring(0, i).trim() to linea.substring(i + 1).trim()
            }
        return base.copy(
            nombre = mapa["nombre"]?.takeIf { it.isNotBlank() } ?: base.nombre,
            profundo = mapa["profundo"]?.equals("true", true) ?: base.profundo,
            pruebas = mapa["pruebas"]?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
                ?.toSet()?.let { migrar(it) } ?: base.pruebas,
            msCpu = mapa["msCpu"]?.toIntOrNull() ?: base.msCpu,
            umbralMemoriaPct = mapa["umbralMemoriaPct"]?.toIntOrNull() ?: base.umbralMemoriaPct,
            umbralBateriaC = mapa["umbralBateriaC"]?.toDoubleOrNull() ?: base.umbralBateriaC,
            alCerrar = mapa["alCerrar"]?.equals("true", true) ?: base.alCerrar,
            conSonido = mapa["conSonido"]?.equals("true", true) ?: base.conSonido,
        )
    }

    /**
     * Un perfil guardado antes de la 1.14.0 que decía «todas» sigue queriendo decir «todas»: la
     * prueba térmica es nueva y el usuario no la desmarcó, simplemente no existía. Migrar no es
     * añadir pruebas que nadie pidió, es no quitarle al que pidió todo.
     */
    fun migrar(pruebas: Set<String>): Set<String> =
        if (pruebas == PerfilDiagnostico.TODAS_ANTERIORES) PerfilDiagnostico.TODAS else pruebas

    /** ¿Hay que avisar de algo con estas medidas? Devuelve las que se pasan del umbral. */
    fun avisos(d: DiagnosticoCompleto, p: PerfilDiagnostico): List<String> {
        val salida = mutableListOf<String>()
        d.medidas.firstOrNull { it.nombre == "Memoria disponible" }?.let { m ->
            val total = Regex("de (\\d+) MB").find(m.detalle)?.groupValues?.get(1)?.toDoubleOrNull()
            if (m.valor != null && total != null && total > 0) {
                val uso = 100.0 * (total - m.valor) / total
                if (uso >= p.umbralMemoriaPct) salida += "memoria al ${uso.toInt()} % (umbral ${p.umbralMemoriaPct} %)"
            }
        }
        d.medidas.firstOrNull { it.nombre == "Temperatura de batería" }?.let { m ->
            if (m.valor != null && m.valor >= p.umbralBateriaC) {
                salida += "batería a ${m.valor} °C (umbral ${p.umbralBateriaC} °C)"
            }
        }
        d.medidas.firstOrNull { it.nombre == "Almacenamiento libre" }?.let { m ->
            if (m.valor != null && m.valor < 500) salida += "quedan ${m.valor.toInt()} MB de almacenamiento"
        }
        // 1.15.1: leer el estado térmico y sólo pintarlo era leerlo a medias. Si el sistema declara
        // estrangulamiento serio o peor, se avisa, igual que con la memoria y la batería.
        d.medidas.firstOrNull { it.nombre == ReglasDiagnostico.NOMBRE_TERMICO }?.let { m ->
            val nivel = m.valor
            if (nivel != null && nivel >= ReglasDiagnostico.NIVEL_PREOCUPA) {
                salida += "el sistema declara estrangulamiento térmico (nivel ${nivel.toInt()} de 6)"
            }
        }
        return salida
    }
}

/** El perfil vive en `filesDir/ajustes-diagnostico.txt`, legible y borrable a mano. */
class PerfilesDiagnostico(context: Context) {

    private val archivo = File(context.filesDir, "ajustes-diagnostico.txt")

    fun leer(): PerfilDiagnostico {
        val texto = runCatching { archivo.takeIf { it.exists() }?.readText() }.getOrNull()
            ?: return PerfilDiagnostico()
        return ReglasPerfilDiagnostico.validar(ReglasPerfilDiagnostico.decodificar(texto)).first
    }

    fun guardar(p: PerfilDiagnostico): Pair<PerfilDiagnostico, List<String>> {
        val (valido, avisos) = ReglasPerfilDiagnostico.validar(p)
        runCatching { archivo.writeText(ReglasPerfilDiagnostico.codificar(valido)) }
        return valido to avisos
    }

    fun ruta(): File = archivo
}
