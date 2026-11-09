package io.github.labandita.glreborn.core

/**
 * Aplica un perfil: escribe las propiedades y COMPRUEBA que quedaron.
 *
 * Se escriben sólo las propiedades que el perfil declara con valor. Las vacías se
 * saltan a propósito —son las que no se pudieron verificar— y se informa cuántas se
 * saltaron, porque un perfil a medias aplicado en silencio es peor que un error.
 */
class Escritor(private val ejecutor: Ejecutor) {

    private val propiedades = listOf(
        "ro.product.brand" to { p: PerfilDispositivo -> p.marca },
        "ro.product.manufacturer" to { p: PerfilDispositivo -> p.fabricante },
        "ro.product.model" to { p: PerfilDispositivo -> p.modelo },
        "ro.product.device" to { p: PerfilDispositivo -> p.device },
        "ro.product.board" to { p: PerfilDispositivo -> p.board },
        "ro.hardware" to { p: PerfilDispositivo -> p.hardware },
        "ro.opengles.version" to { p: PerfilDispositivo -> p.openglEs },
    )

    fun aplicar(perfil: PerfilDispositivo): Resultado {
        if (!ejecutor.disponible()) {
            return Resultado(
                ok = false,
                via = Via.NINGUNA,
                detalle = "Sin Shizuku activo y sin root no se puede escribir el modelo. " +
                    "El teléfono sigue siendo el que es: no se aplicó nada.",
            )
        }

        val lineas = mutableListOf<String>()
        var escritas = 0
        var omitidas = 0
        var errores = 0

        for ((prop, sel) in propiedades) {
            val valor = sel(perfil)
            if (valor.isBlank()) {
                omitidas++
                lineas += "· $prop — omitida (el perfil no tiene dato verificado)"
                continue
            }
            val ok = escribirPropiedad(prop, valor)
            if (!ok) {
                errores++
                lineas += "✗ $prop — no se pudo ejecutar"
            } else {
                escritas++
                lineas += "✓ $prop = $valor"
            }
        }

        // Comprobar no es opcional: es lo que separa «lo apliqué» de «creo que lo apliqué».
        val robot = RobotVerificador(ejecutor)
        val comprobaciones = robot.comprobar(perfil)
        val confirmadas = comprobaciones.count { it.esperado.isNotBlank() && it.coincide }
        val noCuadran = comprobaciones.filter { it.esperado.isNotBlank() && !it.coincide }

        for (c in noCuadran) {
            lineas += "⚠ ${c.prop}: se escribió «${c.esperado}» y el sistema lee «${c.obtenido}»"
        }

        return Resultado(
            ok = errores == 0 && noCuadran.isEmpty() && escritas > 0,
            via = ejecutor.via,
            detalle = when {
                escritas == 0 -> "No se escribió nada."
                noCuadran.isNotEmpty() ->
                    "$escritas escritas, $confirmadas confirmadas, ${noCuadran.size} no cuadran. " +
                        "Algunos fabricantes bloquean estas propiedades: si no cuadran, no cambiaron."
                else -> "$escritas propiedades escritas y $confirmadas confirmadas por lectura. " +
                    "($omitidas omitidas por falta de dato verificado.)"
            },
            lineas = lineas,
        )
    }

    /**
     * Escribe por la vía que toque. Con Shizuku se usa su API de propiedades —no un
     * shell, que es lo que esa API resuelve—; con root, `setprop` por `su`.
     */
    private fun escribirPropiedad(prop: String, valor: String): Boolean =
        when (val e = ejecutor) {
            is io.github.labandita.glreborn.shizuku.EjecutorShizuku -> e.escribir(prop, valor)
            else -> e.ejecutar("setprop $prop '$valor'") != null
        }

    /** Devuelve el teléfono a lo que declaró al abrir la app. */
    fun restaurar(original: Map<String, String>): Resultado {
        if (!ejecutor.disponible()) return Resultado(false, Via.NINGUNA, "Sin vía para restaurar.")
        var n = 0
        for ((prop, valor) in original) {
            if (valor.isBlank()) continue
            escribirPropiedad(prop, valor)
            n++
        }
        return Resultado(true, ejecutor.via, "$n propiedades devueltas a su valor original.")
    }
}
