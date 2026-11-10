package io.github.labandita.glreborn.core

import io.github.labandita.glreborn.shizuku.Shell

/**
 * Ajustes del sistema que no requieren root: caché, DNS privado, escala de animación.
 *
 * Cada función dice qué toca y cómo se deshace. Si una no se puede deshacer, lo dice
 * en su texto en vez de esconderlo.
 *
 * Ideas de: Cache Cleaner (magiskmodule.com/cache-cleaner), Benimaru Tool
 * (DNS privado) y los tweaks de animación habituales.
 */
class Sistema(private val shell: Shell, private val aplicado: Aplicado? = null) {

    /** Tamaño de la caché de una app, en bytes. -1 si no se pudo leer. */
    fun tamanoCache(paquete: String): Long {
        val r = shell.correr("du -sb /data/data/$paquete/cache 2>/dev/null")?.texto ?: return -1
        return r.split("\t").firstOrNull()?.trim()?.toLongOrNull() ?: -1
    }

    /**
     * Borra la caché de una app con la shell.
     *
     * Se prefiere esto a la técnica de Cache Cleaner (servicio de accesibilidad que
     * pulsa por ti en Ajustes): son 3 pasos menos, no requiere permiso de accesibilidad
     * —que puede leer TODA la pantalla— y no deja un servicio corriendo siempre.
     * La caché no contiene datos de usuario: se regenera sola.
     */
    fun limpiarCache(paquete: String): Resultado {
        if (!shell.disponible()) return Resultado(false, Via.NINGUNA, "Shizuku no está activo.")
        val antes = tamanoCache(paquete)
        val r = shell.correr("rm -rf /data/data/$paquete/cache/* 2>/dev/null; true")
        val despues = tamanoCache(paquete)
        if (despues >= 0 && (antes < 0 || despues < antes)) aplicado?.anotar(
            paquete, "Caché borrada",
            "Se borraron ${legible(if (antes > 0) antes - despues else 0)} de caché. " +
                "No se pierde nada: la caché se regenera sola.",
            reversible = true,
        )
        return Resultado(
            ok = despues >= 0 && (antes < 0 || despues < antes),
            via = Via.SHIZUKU,
            detalle = buildString {
                append("$paquete: caché ")
                if (antes >= 0 && despues >= 0) append("de ${legible(antes)} a ${legible(despues)}")
                else append("borrada (no se pudo medir antes/después)")
                append(". No se pierde nada: la caché se regenera sola.")
            },
        )
    }

    /**
     * DNS privado del sistema. Afecta a TODAS las apps, y se deshace poniendo «off».
     * No es magia: cambia a quién le pregunta el teléfono para resolver nombres.
     */
    fun dnsPrivado(host: String): Resultado {
        if (!shell.disponible()) return Resultado(false, Via.NINGUNA, "Shizuku no está activo.")
        val valor = if (host.isBlank() || host == "off") "off" else host
        shell.correr("settings put global private_dns_mode ${if (valor == "off") "off" else "hostname"}")
        if (valor != "off") shell.correr("settings put global private_dns_specifier $valor")
        val modo = shell.correr("settings get global private_dns_mode")?.texto?.trim()
        val espec = shell.correr("settings get global private_dns_specifier")?.texto?.trim()
        val ok = if (valor == "off") modo == "off" else modo == "hostname" && espec == valor
        return Resultado(
            ok = ok,
            via = Via.SHIZUKU,
            detalle = if (ok) "DNS privado: ${if (valor == "off") "desactivado" else valor} (confirmado)."
            else "Se pidió «$valor» y el sistema lee modo=$modo servidor=$espec.",
        )
    }

    /** Escalas de animación. 1.0 es lo normal; 0.5 va más rápido; 0 las quita. */
    fun escalasAnimacion(escala: String): Resultado {
        if (!shell.disponible()) return Resultado(false, Via.NINGUNA, "Shizuku no está activo.")
        val v = escala.toFloatOrNull()
            ?: return Resultado(false, Via.SHIZUKU, "«$escala» no es un número.")
        if (v < 0f || v > 2f) return Resultado(false, Via.SHIZUKU, "Fuera de rango razonable (0 a 2).")
        shell.correr("settings put global window_animation_scale $escala")
        shell.correr("settings put global transition_animation_scale $escala")
        shell.correr("settings put global animator_duration_scale $escala")
        val leido = shell.correr("settings get global window_animation_scale")?.texto?.trim()
        return Resultado(
            ok = leido == escala,
            via = Via.SHIZUKU,
            detalle = if (leido == escala) "Animaciones a $escala (confirmado)."
            else "Se pidió $escala y el sistema lee $leido.",
        )
    }

    /**
     * Modo rendimiento del sistema. Sin root sólo se puede pedir; el sistema decide.
     *
     * Lo que estaba mal: se comprobaba el resultado con `r.ok == true`, que es **el código de
     * salida**, no el efecto. En Android 13 el comando existe
     * (`cmd power set-fixed-performance-mode-enabled true`) pero muchos teléfonos lo aceptan y
     * **lo ignoran**, así que la app decía «hecho» sin que pasara nada.
     *
     * Ahora se comprueba el ajuste que el sistema deja escrito, y si no cambia se dice tal cual.
     * Y si el comando no existe en este teléfono —pasó con la consulta que la app usaba, que no
     * existe en ninguna versión de Android— se dice **qué comando** y **qué contestó**, en vez
     * de «sin detalle».
     */
    fun modoRendimiento(activar: Boolean): Resultado {
        if (!shell.disponible()) return Resultado(false, Via.NINGUNA, "Shizuku no está activo.")
        val ajuste = "power_fixed_performance_mode_enabled"
        val antes = shell.correr("settings get global $ajuste")?.texto?.trim().orEmpty()
        val r = shell.correr("cmd power set-fixed-performance-mode-enabled $activar")
        if (r == null) {
            return Resultado(false, Via.SHIZUKU, "El servicio de energía no respondió.")
        }
        if (r.codigo != 0) {
            // El mensaje del sistema, tal cual: es lo único que permite arreglarlo.
            return Resultado(
                ok = false,
                via = Via.SHIZUKU,
                detalle = "Este teléfono rechazó el modo rendimiento (código ${r.codigo}): " +
                    r.motivo + ". No se ha cambiado nada.",
                lineas = listOf(
                    "pedido: cmd power set-fixed-performance-mode-enabled $activar",
                    "respuesta: ${r.texto.ifBlank { "(vacía)" }}",
                ),
            )
        }
        val despues = shell.correr("settings get global $ajuste")?.texto?.trim().orEmpty()
        val esperado = if (activar) "1" else "0"
        val cambio = despues == esperado && despues != antes
        return Resultado(
            ok = true,
            via = Via.SHIZUKU,
            detalle = if (cambio)
                "Modo rendimiento fijo ${if (activar) "activado" else "desactivado"}, y el " +
                    "sistema lo ha registrado ($ajuste = $despues). Sube el consumo y el calor."
            else
                "El comando se aceptó, pero el sistema no ha cambiado el ajuste " +
                    "($ajuste sigue en «${despues.ifBlank { "vacío" }}»). En este teléfono el " +
                    "fabricante no lo aplica: no se puede prometer más.",
            lineas = listOf(
                "ajuste antes: ${antes.ifBlank { "(vacío)" }}",
                "ajuste después: ${despues.ifBlank { "(vacío)" }}",
            ),
        )
    }

    companion object {
        fun legible(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024)
            else -> "%.2f GB".format(bytes / 1024.0 / 1024 / 1024)
        }
    }
}
