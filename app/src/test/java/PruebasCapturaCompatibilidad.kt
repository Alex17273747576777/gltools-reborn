import io.github.labandita.glreborn.core.Compatibilidad
import io.github.labandita.glreborn.shizuku.Shell

/** Regresiones de Screenshot_20261110-141956; no simula mediciones en el teléfono. */
fun pruebasCapturaCompatibilidad() {
    val c = Compatibilidad(Shell())
    val ayuda = "Power manager (power) commands:\n  help\n    Print this help text."
    val resultado = c.juzgarAyudaModoFijo(33, 255, ayuda)
    check("captura: ayuda con 255 no se llama servicio sin respuesta",
        resultado.resultado == Compatibilidad.Prueba.Estado.NO_APLICA &&
            !resultado.detalle.contains("no responde") && resultado.detalle.contains("respondió"))
    val rechazo = "Failed to set property 'persist.reborn.prueba' to '1'. See dmesg for error reason."
    val propiedad = c.juzgarEscritura("Escribir y deshacer una propiedad", "persist.reborn.prueba", 1, rechazo, "")
    check("captura: se preserva el rechazo de setprop sin decir sonó bien",
        propiedad.resultado == Compatibilidad.Prueba.Estado.FALLA &&
            propiedad.detalle.contains("Failed to set property") && !propiedad.detalle.contains("sonó bien"))
    check("un valor previo 1 no oculta un setprop rechazado",
        c.juzgarEscritura("prueba", "debug.reborn.prueba", 1, rechazo, "1").resultado == Compatibilidad.Prueba.Estado.FALLA)
    check("la prueba ordinaria no usa persist.reborn.prueba",
        Compatibilidad.CLAVE_PRUEBA == "debug.reborn.prueba")
    check("el diagnóstico de energía sólo consulta ayuda",
        Compatibilidad.SONDA_MODO_FIJO == "cmd power help")
    check("sin respuesta de ayuda se reconoce incertidumbre",
        c.juzgarAyudaModoFijo(33, null, "").resultado == Compatibilidad.Prueba.Estado.FALLA)
}
