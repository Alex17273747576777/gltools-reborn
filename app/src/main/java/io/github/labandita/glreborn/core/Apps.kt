package io.github.labandita.glreborn.core

import io.github.labandita.glreborn.shizuku.Shell

/**
 * Congelar, detener y desinstalar apps — con vuelta atrás.
 *
 * Con la shell de Shizuku (los mismos permisos que un `adb shell`), `pm` permite hacer
 * esto sin root. Lo que se puede hacer sin vuelta atrás es lo que se marca como tal y
 * se pide dos veces; lo demás se guarda en el registro y se deshace con un botón.
 *
 * Idea tomada de ZKMNEXT GameSpace (magiskmodule.com/zkmnext-gamespace) y de Buge App
 * Manager: congelar apps para que no gasten batería sin desinstalarlas.
 */
class GestorApps(
    private val shell: Shell,
    private val registro: Registro?,
    private val aplicado: Aplicado? = null,
) {

    /** Paquetes que NO se tocan nunca: si se congelan, el teléfono deja de responder. */
    val intocables: List<String> = listOf(
        "android", "com.android.systemui", "com.android.settings", "com.android.phone",
        "com.android.shell", "com.google.android.gms", "com.android.vending",
        "moe.shizuku.privileged.api", "io.github.labandita.glreborn",
        "com.android.providers.settings", "com.android.permissioncontroller",
    )

    fun esIntocable(paquete: String): Boolean = intocables.any { paquete == it || paquete.startsWith("$it.") }

    private fun revisar(paquete: String): String? = when {
        !shell.disponible() -> "Shizuku no está activo."
        paquete.isBlank() -> "Paquete vacío."
        esIntocable(paquete) -> "$paquete es parte del sistema: congelarlo puede dejar el teléfono sin interfaz."
        else -> null
    }

    /** ¿Está congelada ahora mismo? Se pregunta al sistema, no se supone. */
    fun estaCongelada(paquete: String): Boolean = paquete in congeladas()

    /**
     * Todas las apps deshabilitadas en UNA llamada.
     *
     * Antes se preguntaba app por app: con 100 apps instaladas eso son 100 procesos de
     * shell, y en un teléfono modesto la interfaz se quedaba esperando hasta que Android
     * la mataba. Una sola consulta devuelve lo mismo.
     */
    fun congeladas(): Set<String> {
        val r = shell.correr("pm list packages -d")?.texto ?: return emptySet()
        return r.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:") }
            .toSet()
    }

    fun congelar(paquete: String): Resultado {
        revisar(paquete)?.let { return Resultado(false, Via.SHIZUKU, it) }
        val r = shell.correr("pm disable-user --user 0 $paquete")
        if (r == null || !r.ok) {
            return Resultado(false, Via.SHIZUKU, "No se pudo congelar: ${r?.texto ?: "sin respuesta"}")
        }
        registro?.apuntar("app:$paquete", "activa", "congelada", Modo.SEGURO)
        val confirmado = estaCongelada(paquete)
        if (confirmado) aplicado?.anotar(
            paquete, "Congelada",
            "Se deshabilitó con pm disable-user. Deja de gastar batería y no aparece " +
                "en el cajón de apps. Se puede volver a activar desde aquí.",
            reversible = true,
        )
        return Resultado(
            ok = confirmado,
            via = Via.SHIZUKU,
            detalle = if (confirmado) "$paquete congelada (confirmado en la lista del sistema)."
            else "Se pidió congelar y el sistema no la lista como deshabilitada.",
        )
    }

    fun descongelar(paquete: String): Resultado {
        if (!shell.disponible()) return Resultado(false, Via.NINGUNA, "Shizuku no está activo.")
        val r = shell.correr("pm enable $paquete")
        val ok = r?.ok == true && !estaCongelada(paquete)
        if (ok) aplicado?.anotar(
            paquete, "Activada",
            "Se volvió a habilitar con pm enable. Vuelve a funcionar con normalidad.",
            reversible = true,
        )
        return Resultado(ok, Via.SHIZUKU,
            if (ok) "$paquete activa de nuevo." else "No se pudo activar: ${r?.texto ?: "sin respuesta"}")
    }

    /** Cierra la app ahora. No la congela: vuelve a abrirse si la lanzas. */
    fun detener(paquete: String): Resultado {
        revisar(paquete)?.let { return Resultado(false, Via.SHIZUKU, it) }
        val r = shell.correr("am force-stop $paquete")
        if (r?.ok == true) aplicado?.anotar(
            paquete, "Detenida",
            "Se cerró con am force-stop. Se puede volver a abrir cuando quieras; " +
                "no queda deshabilitada.",
            reversible = true,
        )
        return Resultado(r?.ok == true, Via.SHIZUKU,
            if (r?.ok == true) "$paquete detenida." else "No se pudo detener.")
    }

    /** Congela todas las que se le pasen. Devuelve el recuento real, no el intentado. */
    fun congelarVarias(paquetes: List<String>): Resultado {
        var ok = 0
        val fallos = mutableListOf<String>()
        for (p in paquetes) {
            val r = congelar(p)
            if (r.ok) ok++ else fallos += "$p: ${r.detalle}"
        }
        return Resultado(
            ok = ok > 0,
            via = Via.SHIZUKU,
            detalle = "$ok de ${paquetes.size} congeladas.",
            lineas = fallos,
        )
    }

    /**
     * Desinstala. SIN vuelta atrás: por eso se exige confirmación explícita y no se
     * ofrece como acción de un solo toque en ninguna parte de la interfaz.
     */
    fun desinstalar(paquete: String, confirmado: Boolean): Resultado {
        revisar(paquete)?.let { return Resultado(false, Via.SHIZUKU, it) }
        if (!confirmado) return Resultado(false, Via.SHIZUKU, "Falta la confirmación: esto no se deshace.")
        val r = shell.correr("pm uninstall --user 0 $paquete")
        if (r?.ok == true) aplicado?.anotar(
            paquete, "Desinstalada",
            "Se desinstaló para este usuario con pm uninstall --user 0. " +
                "Los datos de la app se pierden. Desde aquí NO se puede deshacer.",
            reversible = false,
        )
        return Resultado(r?.ok == true, Via.SHIZUKU,
            if (r?.ok == true) "$paquete desinstalada para este usuario. No se puede deshacer desde aquí."
            else "No se pudo desinstalar: ${r?.texto ?: "sin respuesta"}")
    }
}
