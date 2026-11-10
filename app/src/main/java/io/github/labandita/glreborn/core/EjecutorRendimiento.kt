package io.github.labandita.glreborn.core

import io.github.labandita.glreborn.shizuku.Shell

/** Ejecuta órdenes y conserva errores. Una orden aceptada no equivale a restaurar el estado original. */
class EjecutorRendimiento internal constructor(
    private val disponible: () -> Boolean,
    private val correr: (String) -> Shell.Salida?,
    private val sdk: Int,
    private val aplicado: Aplicado?,
    private val modoFijo: (Boolean) -> Resultado,
) {
    constructor(shell: Shell, aplicado: Aplicado?) : this(
        shell::disponible, { shell.correr(it) }, android.os.Build.VERSION.SDK_INT,
        aplicado, { Sistema(shell).modoRendimiento(it) },
    )

    /** La misma barrera rige aplicar y revertir, también si se invocan fuera de la UI. */
    private fun validar(accion: Rendimiento.Accion, paquete: String?): Resultado? = when {
        !disponible() -> Resultado(false, Via.NINGUNA, "Sin Shizuku disponible no se ejecutó ninguna orden.")
        accion.necesitaRoot -> Resultado(false, Via.NINGUNA, "Esta acción necesita root; esta vía no lo presupone.")
        sdk < accion.androidMinimo -> Resultado(false, Via.SHIZUKU,
            "Esta acción requiere API ${accion.androidMinimo}; el dispositivo declara API $sdk.")
        Rendimiento.necesitaPaquete(accion) && !Rendimiento.paqueteValido(paquete) ->
            Resultado(false, Via.SHIZUKU, "Elige un nombre de paquete válido; no se ejecutó ninguna orden.")
        else -> null
    }

    private fun ejecutar(comando: String, lineas: MutableList<String>): Shell.Salida? {
        lineas += "$ $comando"
        val r = correr(comando)
        lineas += "código: ${r?.codigo ?: "sin respuesta"}"
        lineas += "→ ${r?.texto?.take(2000)?.ifBlank { "sin salida" } ?: "sin respuesta"}"
        if (r?.seAgotoElTiempo == true) lineas += "Tiempo agotado: no se conoce el estado final."
        return r
    }

    fun aplicar(accion: Rendimiento.Accion, paquete: String? = null): Resultado {
        validar(accion, paquete)?.let { return it }
        if (accion.id == "rendimiento-fijo") return modoFijo(true)
        val lineas = mutableListOf<String>()
        val ordenes = Rendimiento.comando(accion, paquete)
        if (ordenes.isEmpty()) return Resultado(false, Via.SHIZUKU, "No hay órdenes que aplicar.")
        for (comando in ordenes) {
            val r = ejecutar(comando, lineas)
            if (r?.ok != true || r.seAgotoElTiempo) return Resultado(false, Via.SHIZUKU,
                "Falló o no terminó una orden. Se detuvo la secuencia; puede haber cambios parciales. " +
                    "No se declara aplicada ni se infiere que el fabricante la ignore.", lineas)
        }
        val comprobacion = accion.comprobar.replace("%PAQUETE%", paquete.orEmpty())
        val respuesta = ejecutar(comprobacion, lineas)
        val ok = respuesta?.ok == true && !respuesta.seAgotoElTiempo &&
            Rendimiento.comprueba(respuesta.texto, accion.esperado)
        if (ok) aplicado?.anotar(
            paquete ?: "sistema", accion.nombre,
            "Lectura confirmada con `$comprobacion`: ${accion.esperado}. No es una medición de rendimiento.",
            reversible = accion.revertir.isNotEmpty(),
        )
        return Resultado(ok, Via.SHIZUKU,
            if (ok) "Órdenes aceptadas y lectura esperada confirmada. Esto no mide una mejora física."
            else "No se confirmó la lectura esperada. Puede haber cambios parciales; revisa código y salida.",
            lineas)
    }

    fun revertir(accion: Rendimiento.Accion, paquete: String? = null): Resultado {
        validar(accion, paquete)?.let { return it }
        if (accion.id == "rendimiento-fijo") return modoFijo(false)
        val lineas = mutableListOf<String>()
        val ordenes = Rendimiento.revertir(accion, paquete)
        if (ordenes.isEmpty()) return Resultado(false, Via.SHIZUKU, "No hay órdenes de reversión.")
        for (comando in ordenes) {
            val r = ejecutar(comando, lineas)
            if (r?.ok != true || r.seAgotoElTiempo) return Resultado(false, Via.SHIZUKU,
                "Falló o no terminó una orden de reversión. Puede haber cambios parciales; " +
                    "no se declara restaurado el dispositivo.", lineas)
        }
        return Resultado(true, Via.SHIZUKU,
            "Órdenes de reversión aceptadas. No se verificó el estado original: los valores " +
                "predeterminados del catálogo no equivalen necesariamente a tus valores anteriores.", lineas)
    }
}
