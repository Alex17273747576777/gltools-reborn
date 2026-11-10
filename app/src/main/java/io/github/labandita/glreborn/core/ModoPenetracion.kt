package io.github.labandita.glreborn.core

import io.github.labandita.glreborn.shizuku.Shell

/**
 * Modo penetración: qué se puede hacer en ESTE teléfono, comprobado uno a uno.
 *
 * La tentación, y lo que hacen muchas apps del género, es enseñar una lista de «capacidades» y
 * pintarlas todas en verde. Aquí cada línea se prueba con un comando que se ejecuta de verdad y
 * se clasifica según lo que responde el sistema:
 *
 *  · **COMPLETO** — el comando respondió y el ajuste quedó.
 *  · **LIMITADO** — responde, pero el fabricante ignora el ajuste (se comprueba leyéndolo).
 *  · **NO** — no hay vía, o el comando no existe en este Android.
 *  · **SIN_PERMISO** — hay vía pero este usuario no da el permiso que hace falta.
 *
 * Lo que NO es: un root. No se escala privilegios, no se explota nada y no se parchea el sistema.
 * Si el teléfono no da Shizuku, aquí no hay nada que hacer y se dice con esas palabras.
 */
data class Capacidad(
    val nombre: String,
    val estado: Estado,
    val detalle: String,
    /** El comando exacto que se lanzó, para que se pueda repetir a mano. */
    val comando: String,
) {
    enum class Estado { COMPLETO, LIMITADO, NO, SIN_PERMISO }
}

object ReglasPenetracion {

    /**
     * Clasifica sin ejecutar nada: recibe lo que devolvió el sistema y decide. Así se puede probar
     * la parte que más se equivoca —la interpretación— sin tener un teléfono delante.
     */
    fun clasificar(
        salida: String?,
        esperado: String?,
        existeComando: Boolean = true,
    ): Capacidad.Estado = when {
        salida == null -> Capacidad.Estado.NO
        !existeComando || salida.contains("Unknown command") || salida.contains("not found") ->
            Capacidad.Estado.NO
        salida.contains("Permission denial") || salida.contains("denied") || salida.contains("SecurityException") ->
            Capacidad.Estado.SIN_PERMISO
        esperado == null -> Capacidad.Estado.COMPLETO
        salida.trim() == esperado.trim() -> Capacidad.Estado.COMPLETO
        else -> Capacidad.Estado.LIMITADO
    }

    /** Las capacidades que tienen sentido en un Android sin root, con su comando de prueba. */
    val PRUEBAS: List<Triple<String, String, String?>> = listOf(
        Triple("Shell de sistema", "id", null),
        Triple("Congelar apps (pm disable-user)", "pm list packages -d", null),
        Triple("Leer ajustes globales", "settings get global device_provisioned", null),
        Triple("Escribir ajustes globales", "settings put global reborn_prueba 1 && settings get global reborn_prueba", "1"),
        Triple("Modo rendimiento fijo", "settings get global power_fixed_performance_mode_enabled", null),
        Triple("Escritura de propiedades", "setprop persist.reborn.prueba 1 && getprop persist.reborn.prueba", "1"),
        Triple("Leer dumpsys", "dumpsys -l", null),
        Triple("Modo root disponible", "su -c id", null),
    )
}

class ModoPenetracion(private val shell: Shell) {

    /** Ejecuta la batería de pruebas y devuelve lo que pasó de verdad, línea a línea. */
    fun diagnosticar(): List<Capacidad> {
        val disponible = shell.disponible()
        return ReglasPenetracion.PRUEBAS.map { (nombre, comando, esperado) ->
            if (!disponible) {
                return@map Capacidad(
                    nombre, Capacidad.Estado.SIN_PERMISO,
                    "Shizuku no está activo: sin vía no se puede comprobar ni hacer nada.", comando,
                )
            }
            val r = shell.correr(comando)
            val estado = ReglasPenetracion.clasificar(
                salida = r?.texto,
                esperado = esperado,
                existeComando = r?.texto?.contains("Unknown command") != true,
            )
            Capacidad(
                nombre = nombre,
                estado = estado,
                detalle = when (estado) {
                    Capacidad.Estado.COMPLETO -> "respondió y quedó como se pidió"
                    Capacidad.Estado.LIMITADO ->
                        "el comando respondió, pero el sistema no quedó con el valor pedido: " +
                            "este fabricante lo ignora"
                    Capacidad.Estado.NO ->
                        "este Android no tiene ese comando o no está disponible aquí: " +
                            (r?.texto?.lines()?.firstOrNull()?.take(120) ?: "sin respuesta")
                    Capacidad.Estado.SIN_PERMISO ->
                        "hay vía pero falta el permiso: " + (r?.texto?.lines()?.firstOrNull()?.take(120) ?: "")
                },
                comando = comando,
            )
        }
    }

    /** Resumen honesto: cuántas de cuántas, y ninguna medalla si no hay vía. */
    fun resumen(c: List<Capacidad>): String {
        if (c.isEmpty()) return "no se probó nada"
        val completas = c.count { it.estado == Capacidad.Estado.COMPLETO }
        val limitadas = c.count { it.estado == Capacidad.Estado.LIMITADO }
        val sinPermiso = c.count { it.estado == Capacidad.Estado.SIN_PERMISO }
        return buildString {
            append("$completas de ${c.size} capacidades funcionan de verdad")
            if (limitadas > 0) append(", $limitadas el fabricante las ignora")
            if (sinPermiso > 0) append(", $sinPermiso no tienen vía")
            append(". Ninguna se da por buena sin haberla ejecutado.")
        }
    }
}
