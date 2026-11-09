package io.github.labandita.glreborn.core

import android.content.Context
import java.io.File

/**
 * El cuaderno de lo que se escribió, para poder volver atrás.
 *
 * Se anota el valor ANTERIOR antes de escribir el nuevo, no después: si la app muere
 * entre las dos cosas, queda apuntado lo que hay que devolver. Ese es el único orden
 * que sirve de algo cuando algo sale mal.
 *
 * En modo experto, al abrir la app se mira si el cierre anterior quedó a medias; si
 * quedó, se devuelve solo. Un teléfono que arranca con una huella de compilación
 * inventada es un teléfono que puede no arrancar.
 */
class Registro(context: Context) {

    private val archivo = File(context.filesDir, "registro.tsv")

    data class Apunte(
        val cuando: Long,
        val clave: String,
        val antes: String,
        val despues: String,
        val modo: Modo,
    )

    /** Lee el archivo como texto y lo cierra. Nunca se lee después de un pipe. */
    private fun leer(): List<Apunte> {
        if (!archivo.exists()) return emptyList()
        val texto = archivo.readText()
        return texto.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { linea ->
                val p = linea.split("\t")
                if (p.size < 5) return@mapNotNull null
                Apunte(
                    cuando = p[0].toLongOrNull() ?: 0L,
                    clave = p[1],
                    antes = p[2].replace("\\t", "\t"),
                    despues = p[3],
                    modo = if (p[4] == "EXPERTO") Modo.EXPERTO else Modo.SEGURO,
                )
            }
            .toList()
    }

    fun apuntar(clave: String, antes: String, despues: String, modo: Modo) {
        val linea = listOf(
            System.currentTimeMillis().toString(),
            clave,
            antes.replace("\t", "\\t").replace("\n", " "),
            despues.replace("\n", " "),
            modo.name,
        ).joinToString("\t")
        archivo.appendText(linea + "\n")
    }

    fun historial(): List<Apunte> = leer()

    /** Último valor conocido de cada clave, del más reciente al más viejo. */
    fun valorAnterior(clave: String): String? =
        leer().lastOrNull { it.clave == clave }?.antes

    /**
     * Devuelve el teléfono a como estaba según el cuaderno.
     *
     * Recorre el historial AL REVÉS —lo último escrito es lo primero que hay que
     * deshacer— y deja cada clave en el valor que tenía antes de que la app la tocara.
     */
    fun deshacerTodo(ejecutor: Ejecutor): Resultado {
        val apuntes = leer()
        if (apuntes.isEmpty()) return Resultado(true, ejecutor.via, "No hay nada que deshacer.")
        if (!ejecutor.disponible()) {
            return Resultado(false, Via.NINGUNA, "Sin vía: no se puede deshacer ahora.")
        }

        // El valor original de cada clave es el "antes" del PRIMER apunte, no del último.
        val originales = mutableMapOf<String, String>()
        for (a in apuntes) {
            if (!originales.containsKey(a.clave)) originales[a.clave] = a.antes
        }

        val lineas = mutableListOf<String>()
        var ok = 0
        for ((clave, valor) in originales.entries.reversed()) {
            val escrito = when (ejecutor) {
                is io.github.labandita.glreborn.shizuku.EjecutorShizuku -> ejecutor.escribir(clave, valor)
                else -> ejecutor.ejecutar("setprop $clave '$valor'") != null
            }
            if (escrito) { ok++; lineas += "← $clave = $valor" }
            else lineas += "✗ $clave no se pudo devolver"
        }

        archivo.writeText("")  // cuaderno limpio: ya se deshizo todo
        return Resultado(
            ok = ok == originales.size,
            via = ejecutor.via,
            detalle = "$ok de ${originales.size} propiedades devueltas a su valor original.",
            lineas = lineas,
        )
    }

    /** ¿Quedó una sesión a medias? Sólo en modo experto hay riesgo de que importe. */
    fun huboCambiosExpertos(): Boolean = leer().any { it.modo == Modo.EXPERTO }
}
