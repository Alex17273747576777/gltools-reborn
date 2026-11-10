package io.github.labandita.glreborn.core

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Todo lo que la app CAMBIÓ en el teléfono, en una sola línea de tiempo.
 *
 * Por qué existe: hasta la 1.10.0 había dos cuadernos separados y ninguno de los dos contestaba
 * la pregunta que de verdad se hace uno: «¿qué le he hecho yo a este teléfono?». `Aplicado` sabía
 * lo de cada app; `Registro` sabía las propiedades escritas, pero sólo se veía dentro del panel de
 * compatibilidad. Una propiedad global (`power_fixed_performance_mode_enabled`, un DNS, una
 * animación) no aparecía en ninguna lista: se aplicaba y se olvidaba.
 *
 * Esto no guarda nada nuevo que no se guardara ya: **junta**. Las dos fuentes son las de siempre
 * (`registro.tsv` y `aplicado.json`), y aquí se leen y se ordenan por fecha. Se lee de los dos de
 * cuyo formato ya está probado en `ReglasAplicado` y en `Registro`; no se duplica el estado.
 *
 * Lo que NO hace: no inventa entradas, no rellena huecos con «sin datos», y no dice que algo se
 * deshizo si no consta. Cada línea es un hecho con su hora y su vuelta atrás.
 */
data class Movimiento(
    /** 0 = propiedad del sistema (va por [Registro]); 1 = algo de una app (va por [Aplicado]). */
    val fuente: Int,
    val cuando: Long,
    val clave: String,
    val antes: String,
    val despues: String,
    val reversible: Boolean,
) {
    /** El sitio de donde salió: una app, o «sistema» cuando es un ajuste global. */
    val donde: String get() = when (fuente) {
        FUENTE_REGISTRO -> "sistema"
        else -> if (clave.startsWith("app:")) clave.removePrefix("app:") else clave
    }

    /** Qué se tocó, en palabras: el nombre de la propiedad o el de la acción. */
    val que: String get() = if (fuente == FUENTE_REGISTRO) propiedad else despues

    val detalle: String get() = when (fuente) {
        FUENTE_REGISTRO ->
            if (antes.isBlank()) "estaba vacío → ${despues.ifBlank { "vacío" }}"
            else "$antes → ${despues.ifBlank { "vacío" }}"
        else -> antes.ifBlank { "—" }
    }

    val propiedad: String get() = clave.removePrefix("app:")

    companion object {
        const val FUENTE_REGISTRO = 0
        const val FUENTE_APLICADO = 1
    }
}

/**
 * Las reglas de la línea de tiempo, sin Android de por medio, para poder probarlas aquí.
 */
object ReglasMovimientos {

    /** Tope de la lista que se pinta. Es un historial para mirar, no un archivo de auditoría. */
    const val MAX_VISIBLES = 200

    /** Junta las dos fuentes y las ordena: lo último arriba, como se lee un historial. */
    fun juntar(apuntes: List<Registro.Apunte>, hechos: List<Movimiento>): List<Movimiento> {
        val todos = ArrayList<Movimiento>(apuntes.size + hechos.size)
        apuntes.forEach { a ->
            todos += Movimiento(
                fuente = Movimiento.FUENTE_REGISTRO,
                cuando = a.cuando,
                clave = a.clave,
                antes = a.antes,
                despues = a.despues,
                // Lo escrito en el registro siempre se puede devolver: para eso está el cuaderno.
                reversible = true,
            )
        }
        todos += hechos
        return todos.sortedByDescending { it.cuando }.take(MAX_VISIBLES)
    }

    /**
     * Qué claves se pueden deshacer de verdad: las del registro. Lo de las apps se deshace desde
     * el panel de cada app, no desde aquí, y decirlo mal sería peor que no decirlo.
     */
    fun deshacibles(lista: List<Movimiento>): List<Movimiento> =
        lista.filter { it.fuente == Movimiento.FUENTE_REGISTRO }

    /**
     * Agrupa por día en texto («hoy», «ayer», «7 nov»). Sirve para separar la lista sin dibujar
     * una cabecera por cada fila.
     */
    fun dia(cuando: Long, ahora: Long, zona: java.util.TimeZone = java.util.TimeZone.getDefault()): String {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = zona }
        val a = fmt.format(Date(ahora))
        val c = fmt.format(Date(cuando))
        if (a == c) return "hoy"
        val ayer = fmt.format(Date(ahora - 24L * 60 * 60 * 1000))
        if (ayer == c) return "ayer"
        return SimpleDateFormat("d MMM", Locale.forLanguageTag("es")).apply { timeZone = zona }
            .format(Date(cuando))
    }

    /** La hora, corta: en una lista de historial los segundos no aportan nada. */
    fun hora(cuando: Long): String =
        SimpleDateFormat("HH:mm", Locale.US).format(Date(cuando))

    /** Cuántos cambios se hicieron en total, para poder decirlo en la portada. */
    fun resumen(lista: List<Movimiento>): String = when {
        lista.isEmpty() -> "no se cambió nada todavía"
        lista.size == 1 -> "1 cambio"
        else -> "${lista.size} cambios"
    }
}

/**
 * Lee las dos fuentes y las junta. Sólo lee: el que escribe es `Registro` y `Aplicado`.
 */
class Movimientos(private val context: Context, private val registro: Registro, private val aplicado: Aplicado) {

    fun todos(nombres: Map<String, String> = emptyMap()): List<Movimiento> {
        val deApps = runCatching {
            aplicado.fichas(nombres).flatMap { f ->
                f.hechos.map { h ->
                    Movimiento(
                        fuente = Movimiento.FUENTE_APLICADO,
                        cuando = h.cuando,
                        clave = "app:${f.paquete}",
                        antes = f.nombre,
                        despues = h.accion,
                        reversible = h.reversible,
                    )
                }
            }
        }.getOrDefault(emptyList())
        return ReglasMovimientos.juntar(registro.historial(), deApps)
    }

    /** El archivo donde vive el registro, para poder enseñar su tamaño sin mentir. */
    fun archivoDelRegistro(): File = File(context.filesDir, "registro.tsv")
}
