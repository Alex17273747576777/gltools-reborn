package io.github.labandita.glreborn.core

import android.content.Context
import io.github.labandita.glreborn.shizuku.EjecutorShizuku

/**
 * El director de la casa: decide qué se puede aplicar y con qué, y no deja que la
 * interfaz prometa más de lo que el teléfono permite.
 *
 * Este archivo existe porque la tentación de una app así es decir «aplicado» siempre.
 * Aquí el estado es un dato medido, no una intención: si no hay Shizuku ni root, el
 * estado es SIN_VIA y la pantalla lo dice con esas dos palabras.
 */
class Director(context: Context) {

    enum class Estado { SHIZUKU_LISTO, SHIZUKU_SIN_PERMISO, ROOT_LISTO, SIN_VIA }

    private val app = context.applicationContext
    private val registro = Registro(app)

    private var ejecutor: Ejecutor? = null
    private var estado: Estado = Estado.SIN_VIA
    private var original: Map<String, String> = emptyMap()

    /** El modo se guarda en disco: si lo dejaste en experto, sigue en experto al volver. */
    var modo: Modo = cargarModo()
        private set

    private val prefs = app.getSharedPreferences("reborn", Context.MODE_PRIVATE)

    private fun cargarModo(): Modo =
        if (prefs.getString("modo", "SEGURO") == "EXPERTO") Modo.EXPERTO else Modo.SEGURO

    fun cambiarModo(): Modo {
        modo = modo.alternar()
        prefs.edit().putString("modo", modo.name).apply()
        return modo
    }

    fun estado(): Estado = estado
    fun registro(): Registro = registro

    fun descripcion(): String = when (estado) {
        Estado.SHIZUKU_LISTO -> "Shizuku activo y con permiso: se puede escribir sin root."
        Estado.SHIZUKU_SIN_PERMISO ->
            "Shizuku está instalado pero no dio el permiso. Ábrelo y autoriza esta app, o usa root."
        Estado.ROOT_LISTO -> "Root disponible: se escribe con su. Shizuku no hace falta."
        Estado.SIN_VIA ->
            "Sin Shizuku con permiso y sin root. NO se puede cambiar el modelo: " +
                "esta app no va a fingir que sí."
    }

    fun refrescar(shizukuListo: Boolean): Estado {
        val s = EjecutorShizuku()
        val root = EjecutorRoot(app)
        estado = when {
            shizukuListo && s.disponible() -> { ejecutor = s; Estado.SHIZUKU_LISTO }
            root.disponible() -> { ejecutor = root; Estado.ROOT_LISTO }
            shizukuListo -> Estado.SHIZUKU_SIN_PERMISO
            else -> { ejecutor = null; Estado.SIN_VIA }
        }
        if (original.isEmpty()) original = RobotVerificador(ejecutor).leerEstado()

        // Auto-reparación al abrir: si la sesión anterior escribió en modo experto y
        // quedó a medias, se devuelve sola. Un teléfono no debe arrancar con datos falsos.
        if (modo == Modo.EXPERTO && registro.huboCambiosExpertos() && ejecutor != null) {
            registro.deshacerTodo(ejecutor!!)
        }
        return estado
    }

    /** Aplica un perfil pasando por el filtro de seguridad del modo actual. */
    fun aplicar(perfil: PerfilDispositivo): Resultado {
        val e = ejecutor ?: return Resultado(
            false, Via.NINGUNA,
            "No se aplicó nada: no hay Shizuku con permiso ni root en este teléfono.",
        )
        return Escritor(e, registro, modo).aplicar(perfil)
    }

    /** Aplica una ficha de supercomputadora: sólo las claves que existen de verdad. */
    fun simular(s: Supercomputadora): Resultado {
        val e = ejecutor ?: return Resultado(
            false, Via.NINGUNA, "Sin vía: la simulación no se aplicó.",
        )
        val escritor = Escritor(e, registro, modo)
        val props = Supercomputadoras.propiedadesDe(s)
        val lineas = mutableListOf<String>()
        var ok = 0
        for ((k, v) in props) {
            val r = escritor.escribirSuelta(k, v)
            if (r.ok) ok++ else lineas += "⛔ $k — ${r.detalle}"
        }
        // Lo que no tiene propiedad donde meterse se dice, no se simula.
        val info = Supercomputadoras.datosSoloInformativos(s)
            .joinToString(" · ") { "${it.first}: ${it.second}" }
        return Resultado(
            ok = ok > 0,
            via = e.via,
            detalle = if (ok > 0)
                "$ok propiedades escritas. El teléfono DECLARA ser «${s.nombre}»; " +
                    "no tiene sus ${"%,d".format(s.nucleos)} núcleos ni sus ${s.rmaxPFlops} PFlop/s."
            else "No se pudo escribir la simulación.",
            lineas = lineas + "· sin propiedad equivalente → $info",
        )
    }

    fun escribirSuelta(clave: String, valor: String): Resultado {
        val e = ejecutor ?: return Resultado(false, Via.NINGUNA, "Sin vía: no se escribió nada.")
        return Escritor(e, registro, modo).escribirSuelta(clave, valor)
    }

    fun restaurar(): Resultado {
        val e = ejecutor ?: return Resultado(false, Via.NINGUNA, "No hay vía para restaurar.")
        val r = registro.deshacerTodo(e)
        if (r.ok) return r
        return Escritor(e, registro, modo).restaurar(original)
    }

    fun telefoniaActual(): Map<String, String> = RobotVerificador(ejecutor).leerEstado()
}
