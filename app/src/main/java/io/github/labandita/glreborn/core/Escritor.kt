package io.github.labandita.glreborn.core

/**
 * Aplica un perfil: pasa cada escritura por el filtro de seguridad, la anota ANTES de
 * hacerla y la confirma DESPUÉS releyendo.
 *
 * El orden importa y es lo que separa esta app de una que dice «listo» y te deja el
 * teléfono raro: primero se comprueba si se puede, luego se apunta qué había, luego se
 * escribe, y al final se lee para ver si de verdad cambió.
 */
class Escritor(
    private val ejecutor: Ejecutor,
    private val registro: Registro? = null,
    private val modo: Modo = Modo.SEGURO,
) {

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
        var bloqueadas = 0

        for ((prop, sel) in propiedades) {
            val valor = sel(perfil)
            if (valor.isBlank()) {
                omitidas++
                lineas += "· $prop — omitida (el perfil no tiene dato verificado)"
                continue
            }

            // El filtro manda. Si dice no, no se escribe, y se dice por qué.
            val permiso = Seguridad.revisar(prop, valor, modo)
            if (!permiso.permitido) {
                bloqueadas++
                lineas += "⛔ $prop — bloqueada: ${permiso.motivo}"
                continue
            }

            if (escribirConRegistro(prop, valor)) {
                escritas++
                lineas += "✓ $prop = $valor" + if (permiso.aviso.isNotBlank()) "  (${permiso.aviso})" else ""
            } else {
                lineas += "✗ $prop — no se pudo ejecutar"
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

        // Modo seguro: si algo no cuadró, se devuelve solo. Es la diferencia entre
        // «no funcionó» y «no funcionó y además te lo dejé a medias».
        var autoDeshecho = false
        if (modo == Modo.SEGURO && noCuadran.isNotEmpty() && registro != null) {
            val r = registro.deshacerTodo(ejecutor)
            autoDeshecho = true
            lineas += "↩ modo seguro: deshecho automático — ${r.detalle}"
        }

        return Resultado(
            ok = noCuadran.isEmpty() && escritas > 0,
            via = ejecutor.via,
            detalle = when {
                escritas == 0 && bloqueadas > 0 -> "El modo seguro bloqueó todo lo que se intentó escribir."
                escritas == 0 -> "No se escribió nada."
                noCuadran.isNotEmpty() && autoDeshecho ->
                    "$escritas escritas, pero ${noCuadran.size} no cuadraron y el modo seguro las devolvió. " +
                        "Tu teléfono está como estaba."
                noCuadran.isNotEmpty() ->
                    "$escritas escritas, $confirmadas confirmadas, ${noCuadran.size} no cuadran. " +
                        "El fabricante bloquea esas propiedades: si no cuadran, no cambiaron."
                else -> "$escritas propiedades escritas y $confirmadas confirmadas por lectura. " +
                    "($omitidas sin dato, $bloqueadas bloqueadas por el filtro.)"
            },
            lineas = lineas,
        )
    }

    /** Anota el valor anterior, escribe, y sólo entonces da por hecho que se intentó. */
    private fun escribirConRegistro(clave: String, valor: String): Boolean {
        val antes = leerPropiedad(clave)
        // Se apunta ANTES de escribir: si el proceso muere aquí, queda dicho qué devolver.
        registro?.apuntar(clave, antes, valor, modo)
        return escribirPropiedad(clave, valor)
    }

    private fun leerPropiedad(clave: String): String = when (val e = ejecutor) {
        is io.github.labandita.glreborn.shizuku.EjecutorShizuku -> e.leer(clave) ?: ""
        else -> (e.ejecutar("getprop $clave") ?: "").trim()
    }

    private fun escribirPropiedad(prop: String, valor: String): Boolean =
        when (val e = ejecutor) {
            is io.github.labandita.glreborn.shizuku.EjecutorShizuku -> e.escribir(prop, valor)
            else -> e.ejecutar("setprop $prop '$valor'") != null
        }

    /** Escribe una propiedad suelta, con filtro. Para el modo experto. */
    fun escribirSuelta(clave: String, valor: String): Resultado {
        if (!ejecutor.disponible()) return Resultado(false, Via.NINGUNA, "Sin vía: no se escribió nada.")
        val permiso = Seguridad.revisar(clave, valor, modo)
        if (!permiso.permitido) {
            return Resultado(false, ejecutor.via, "Bloqueada: ${permiso.motivo}")
        }
        val ok = escribirConRegistro(clave, valor)
        val leido = leerPropiedad(clave)
        return Resultado(
            ok = ok && leido == valor,
            via = ejecutor.via,
            detalle = if (leido == valor) "$clave = $valor (confirmado)"
            else "Se intentó escribir «$valor» y el sistema lee «$leido».",
            lineas = listOfNotNull(permiso.aviso.takeIf { it.isNotBlank() }),
        )
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
        registro?.let { it.historial() }
        return Resultado(true, ejecutor.via, "$n propiedades devueltas a su valor original.")
    }
}
