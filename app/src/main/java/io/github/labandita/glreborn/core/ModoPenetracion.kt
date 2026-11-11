package io.github.labandita.glreborn.core

import io.github.labandita.glreborn.shizuku.Shell

/**
 * Modo penetración: qué se puede hacer en ESTE teléfono, comprobado uno a uno.
 *
 * La tentación, y lo que hacen muchas apps del género, es enseñar una lista de «capacidades» y
 * pintarlas todas en verde. Aquí cada línea se prueba con un comando que se ejecuta de verdad y
 * se clasifica según lo que responde el sistema:
 *
 *  · **COMPLETO** — respondió, terminó con código 0 y, si se esperaba un valor, la lectura
 *    posterior dio ése. Sin código de salida no se afirma nada: hasta la 1.13.1 no se miraba, y
 *    un comando que terminaba mal pero imprimía algo inocente salía pintado en verde.
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
     *
     * `codigo` es obligatorio a propósito. Hasta la 1.13.1 no se miraba: `Shell.Salida` ya lo
     * traía, pero nadie se lo pasaba a esta función, y entonces cualquier comando que terminaba
     * mal imprimiendo algo inocente salía como COMPLETO. Una capacidad que no funciona, enseñada
     * en verde, es peor que no enseñarla. Sin código de salida no se afirma nada.
     */
    fun clasificar(
        salida: String?,
        esperado: String?,
        existeComando: Boolean = true,
        codigo: Int?,
    ): Capacidad.Estado = when {
        // Sin respuesta, o sin código que interpretar: no hay nada que afirmar.
        salida == null || codigo == null -> Capacidad.Estado.NO
        !existeComando || salida.contains("Unknown command") || salida.contains("not found") ->
            Capacidad.Estado.NO
        // El permiso se mira ANTES que el código: lo denegado también termina mal, y «te falta el
        // permiso» es útil; «no funciona», no.
        salida.contains("Permission denial") || salida.contains("denied") || salida.contains("SecurityException") ->
            Capacidad.Estado.SIN_PERMISO
        // -1 no es un código de shell: es el tiempo agotado (8 s) o un proceso que no llegó a
        // lanzarse. Ninguna de las dos cosas es una capacidad que funcione.
        codigo != 0 -> Capacidad.Estado.NO
        esperado == null -> Capacidad.Estado.COMPLETO
        salida.trim() == esperado.trim() -> Capacidad.Estado.COMPLETO
        else -> Capacidad.Estado.LIMITADO
    }

    /*
     * Las capacidades que tienen sentido en un Android sin root, con su comando de prueba.

     * Las dos que ESCRIBEN dejan el teléfono como estaba: leen el valor previo, escriben, leen de
     * vuelta y restauran —o borran la clave si antes no existía— todo en el mismo comando, porque
     * entre dos llamadas puede pasar cualquier cosa. Hasta la 1.13.1 la de ajustes globales se
     * quedaba dentro del sistema: `reborn_prueba=1` seguía ahí para siempre en el teléfono de
     * cualquiera que hubiera pulsado el botón. Una sonda que deja basura no es una sonda.

     * El `exit $rc` del final no es decorativo. `Shell.correr` envuelve con `sh -c "… 2>&1"`, así
     * que sin él el código de salida sería siempre el del último `echo`: 0, y la clasificación
     * diría COMPLETO aunque el `put` o el `setprop` hubiera fallado. Se comprobó con `sh` de
     * verdad que `exit $rc 2>&1` conserva el código.
     */
    val PRUEBAS: List<Triple<String, String, String?>> = listOf(
        Triple("Shell de sistema", "id", null),
        // Sólo se LISTA. Llamar a esto «congelar apps» era prometer una capacidad que el comando
        // no prueba: `pm list packages -d` no congela nada, dice qué está ya congelado.
        Triple("Listar apps congeladas (pm list packages -d)", "pm list packages -d", null),
        Triple("Leer ajustes globales", "settings get global device_provisioned", null),
        Triple("Escribir ajustes globales (se restaura lo que había)",
            "prev=\$(settings get global reborn_prueba); " +
                "settings put global reborn_prueba 1; rc=\$?; " +
                "leido=\$(settings get global reborn_prueba); " +
                "if [ -z \"\$prev\" ] || [ \"\$prev\" = null ]; then settings delete global reborn_prueba; " +
                "else settings put global reborn_prueba \"\$prev\"; fi; " +
                "echo \"\$leido\"; exit \$rc",
            "1"),
        // Sólo lectura, igual que el diagnóstico: se mira si la ayuda de `cmd power` anuncia la
        // orden. (`help` devuelve 255 siempre, así que su código no dice nada: con «echo $?» y
        // esperando 0, esta línea salía «el fabricante lo ignora» en todos los teléfonos.)
        // Que la AYUDA anuncie la orden no es que el fabricante la obedezca: el nombre lo dice.
        Triple("Modo rendimiento fijo (sólo se mira si la ayuda lo anuncia)",
            Compatibilidad.SONDA_MODO_FIJO + " 2>&1 | grep -q set-fixed-performance-mode-enabled && echo si || echo no", "si"),
        // `debug.*` es lo que la vía ADB puede escribir; `persist.reborn.*` lo prohíbe SELinux
        // en todo teléfono sin root. Se lee y se deja vacía en el mismo comando.
        // `debug.*` es lo que la vía ADB puede escribir; `persist.reborn.*` lo prohíbe SELinux en
        // todo teléfono sin root. Se escribe, se lee de vuelta y se devuelve la propiedad a lo que
        // había antes —que normalmente es nada, y `setprop` con valor vacío la deja vacía.
        Triple("Escritura de propiedades (debug.*, se restaura lo que había)",
            "prev=\$(getprop ${Compatibilidad.CLAVE_PRUEBA}); " +
                "setprop ${Compatibilidad.CLAVE_PRUEBA} 1; rc=\$?; " +
                "leido=\$(getprop ${Compatibilidad.CLAVE_PRUEBA}); " +
                "setprop ${Compatibilidad.CLAVE_PRUEBA} \"\$prev\"; " +
                "echo \"\$leido\"; exit \$rc",
            "1"),
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
                codigo = r?.codigo,
            )
            val leido = r?.texto?.trim().orEmpty()
            Capacidad(
                nombre = nombre,
                estado = estado,
                detalle = when (estado) {
                    Capacidad.Estado.COMPLETO ->
                        if (esperado == null) "respondió y terminó con código 0"
                        else "respondió con código 0 y la lectura posterior dio «$leido»"
                    Capacidad.Estado.LIMITADO ->
                        "el comando terminó con código 0, pero la lectura posterior dio «$leido» " +
                            "en vez de «$esperado»: el sistema no quedó como se pidió (este " +
                            "fabricante lo ignora, o la orden no hace nada aquí)"
                    // El motivo lo pone `Shell.Salida`, que distingue tiempo agotado, proceso que
                    // no llegó a lanzarse y código distinto de cero con su propia explicación.
                    Capacidad.Estado.NO ->
                        "no se puede afirmar que funcione: " + (r?.motivo ?: "sin respuesta")
                    Capacidad.Estado.SIN_PERMISO ->
                        "hay vía pero falta el permiso: " + (r?.motivo ?: "")
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
