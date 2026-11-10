package io.github.labandita.glreborn.core

/**
 * Ejecuta las acciones de [Rendimiento] con la shell y **comprueba** el resultado.
 *
 * Contrato, igual que en el resto de la app: no se dice «aplicado» por haberlo pedido.
 * Se ejecuta, se lee el estado con el comando de comprobación, y se informa de lo que
 * devolvió el sistema. Si no coincide con lo esperado, se dice que no se pudo —aunque la
 * orden no hubiera dado error.
 */
class EjecutorRendimiento(
    private val shell: io.github.labandita.glreborn.shizuku.Shell,
    private val aplicado: Aplicado?,
) {
    fun aplicar(accion: Rendimiento.Accion, paquete: String? = null): Resultado {
        if (!shell.disponible()) {
            return Resultado(false, Via.NINGUNA,
                "Sin Shizuku con permiso no se puede ejecutar nada de esto.")
        }
        if (accion.necesitaRoot) {
            return Resultado(false, Via.NINGUNA,
                "«${accion.nombre}» necesita root. Esta app no finge tenerlo.")
        }
        if (Rendimiento.necesitaPaquete(accion) && paquete.isNullOrBlank()) {
            return Resultado(false, Via.NINGUNA,
                "«${accion.nombre}» actúa sobre una app concreta: elige una primero.")
        }

        if (accion.id == "rendimiento-fijo") return Sistema(shell).modoRendimiento(true)

        val lineas = mutableListOf<String>()
        Rendimiento.comando(accion, paquete).forEach { comando ->
            val r = shell.correr(comando)
            lineas += "$ $comando"
            lineas += "→ ${r?.texto?.take(200)?.ifBlank { "sin salida" } ?: "sin respuesta"}"
        }

        val leido = shell.correr(accion.comprobar)?.texto
        lineas += "$ ${accion.comprobar}"
        lineas += "→ ${leido?.take(200)?.ifBlank { "sin salida" } ?: "sin respuesta"}"

        val ok = Rendimiento.comprueba(leido, accion.esperado)
        if (ok) {
            aplicado?.anotar(
                paquete ?: "sistema",
                accion.nombre,
                "${accion.queHace} Se comprobó con `${
                    accion.comprobar.replace("%PAQUETE%", paquete.orEmpty())
                }` y devolvió ${accion.esperado}.",
                reversible = true,
            )
        }
        return Resultado(
            ok = ok,
            via = Via.SHIZUKU,
            detalle = if (ok)
                "«${accion.nombre}» aplicado y comprobado: el sistema devolvió " +
                    "${accion.esperado}."
            else
                "Se ejecutó «${accion.nombre}», pero la comprobación no dio " +
                    "${accion.esperado}: el teléfono puede no admitirlo. Mira las líneas " +
                    "de abajo antes de dar nada por hecho.",
            lineas = lineas,
        )
    }

    fun revertir(accion: Rendimiento.Accion, paquete: String? = null): Resultado {
        if (!shell.disponible()) {
            return Resultado(false, Via.NINGUNA, "Sin Shizuku no se puede deshacer.")
        }
        if (accion.id == "rendimiento-fijo") return Sistema(shell).modoRendimiento(false)

        val lineas = mutableListOf<String>()
        Rendimiento.revertir(accion, paquete).forEach { comando ->
            val r = shell.correr(comando)
            lineas += "$ $comando"
            lineas += "→ ${r?.texto?.take(200)?.ifBlank { "sin salida" } ?: "sin respuesta"}"
        }
        return Resultado(
            ok = true,
            via = Via.SHIZUKU,
            detalle = "«${accion.nombre}» revertido. Los comandos ejecutados están abajo: " +
                "si alguno falló, lo verás ahí.",
            lineas = lineas,
        )
    }
}
