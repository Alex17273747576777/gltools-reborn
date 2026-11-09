package io.github.labandita.glreborn.core

import io.github.labandita.glreborn.shizuku.Shell

/**
 * Resolución, densidad y frecuencia de refresco — con vuelta atrás automática.
 *
 * Esto es lo más peligroso de toda la app y por eso es lo más protegido. Una resolución
 * o una densidad equivocada deja el teléfono con una interfaz inusable: la pantalla
 * responde, pero no hay dónde pulsar. La única defensa real es un temporizador: se
 * aplica, se cuenta atrás, y si nadie confirma, se revierte solo.
 *
 * Idea tomada de Benimaru Tool (magiskmodule.com/benimaru-tool), que hace lo mismo con
 * 6 segundos de margen. Aquí son 15 y se puede cancelar.
 */
class AjustesPantalla(private val shell: Shell) {

    data class Actual(
        val resolucion: String,
        val densidad: String,
        val refresco: String,
    )

    /** Lo que el teléfono tiene AHORA, leído del sistema. */
    fun leer(): Actual? {
        val r = shell.correr("wm size")?.texto ?: return null
        val d = shell.correr("wm density")?.texto ?: return null
        val f = shell.correr("settings get system peak_refresh_rate")?.texto ?: ""
        // "Physical size: 1080x2400" y, si hay override, una segunda línea "Override size: ..."
        val fisica = Regex("Physical size: (\\d+x\\d+)").find(r)?.groupValues?.get(1) ?: ""
        val dens = Regex("Physical density: (\\d+)").find(d)?.groupValues?.get(1) ?: ""
        return Actual(fisica, dens, f.ifBlank { "—" })
    }

    /**
     * Calcula la densidad que mantiene la MISMA escala visual al cambiar resolución.
     * Sin esto, bajar la resolución encoge todo y la interfaz queda inservible.
     */
    fun densidadEquivalente(anchoOriginal: Int, densidadOriginal: Int, anchoNuevo: Int): Int =
        Math.round(densidadOriginal.toFloat() * anchoNuevo / anchoOriginal)

    /**
     * Aplica resolución y densidad, y arranca la cuenta atrás.
     *
     * Devuelve el plan para que la interfaz pueda confirmar o revertir. La reversión
     * NO depende de que la app siga viva: el comando de vuelta se lanza con `sh` y un
     * `sleep`, así que si el sistema mata la app, el teléfono vuelve solo igual.
     */
    fun aplicar(ancho: Int, alto: Int, densidad: Int, segundos: Int = 15): Resultado {
        if (!shell.disponible()) return Resultado(false, Via.NINGUNA, "Shizuku no está activo.")
        val antes = leer() ?: return Resultado(false, Via.SHIZUKU, "No se pudo leer el estado actual.")

        val r = shell.correr("wm size ${ancho}x${alto}")
        if (r == null || !r.ok) {
            return Resultado(false, Via.SHIZUKU, "El sistema rechazó el cambio de resolución: ${r?.texto ?: "sin respuesta"}")
        }
        shell.correr("wm density $densidad")

        // Red de seguridad en segundo plano: si no se cancela, vuelve solo.
        shell.correr(
            "sh -c 'sleep $segundos; wm size reset; wm density reset' >/dev/null 2>&1 &",
            2000,
        )

        val despues = leer()
        return Resultado(
            ok = true,
            via = Via.SHIZUKU,
            detalle = "Resolución ${ancho}x${alto} y densidad $densidad aplicadas. " +
                "Si no confirmas en $segundos segundos, el teléfono vuelve solo a ${antes.resolucion}.",
            lineas = listOf("antes: ${antes.resolucion} @ ${antes.densidad} dpi",
                "ahora: ${despues?.resolucion ?: "?"} @ ${despues?.densidad ?: "?"} dpi"),
        )
    }

    /** Cancela la cuenta atrás: lo aplicado se queda. */
    fun confirmar(): Resultado {
        shell.correr("pkill -f 'wm size reset' ; pkill -f 'sleep 15'")
        return Resultado(true, Via.SHIZUKU, "Cambio confirmado y fijado.")
    }

    /** Vuelve a lo de fábrica sin esperar. */
    fun revertir(): Resultado {
        val r = shell.correr("wm size reset; wm density reset")
        return Resultado(
            ok = r?.ok == true,
            via = Via.SHIZUKU,
            detalle = if (r?.ok == true) "Resolución y densidad devueltas a las de fábrica."
            else "No se pudo revertir: ${r?.texto ?: "sin respuesta"}",
        )
    }

    /** Frecuencia de refresco máxima. No todas las pantallas la aceptan. */
    fun fijarRefresco(hz: Int): Resultado {
        if (!shell.disponible()) return Resultado(false, Via.NINGUNA, "Shizuku no está activo.")
        shell.correr("settings put system peak_refresh_rate $hz")
        shell.correr("settings put system min_refresh_rate $hz")
        val leido = shell.correr("settings get system peak_refresh_rate")?.texto
        return Resultado(
            ok = leido?.trim() == hz.toString(),
            via = Via.SHIZUKU,
            detalle = if (leido?.trim() == hz.toString()) "Refresco fijado a $hz Hz (confirmado por lectura)."
            else "Se pidió $hz Hz y el sistema lee $leido. Tu pantalla puede no admitirlo.",
        )
    }
}
