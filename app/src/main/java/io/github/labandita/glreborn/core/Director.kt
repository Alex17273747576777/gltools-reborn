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
class Director(private val context: Context) {

    enum class Estado { SHIZUKU_LISTO, SHIZUKU_SIN_PERMISO, ROOT_LISTO, SIN_VIA }

    private var shizuku: EjecutorShizuku? = null
    private var ejecutor: Ejecutor? = null
    private var estado: Estado = Estado.SIN_VIA
    private var original: Map<String, String> = emptyMap()

    fun estado(): Estado = estado

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
        val root = EjecutorRoot(context)
        estado = when {
            shizukuListo && s.disponible() -> { ejecutor = s; shizuku = s; Estado.SHIZUKU_LISTO }
            root.disponible() -> { ejecutor = root; Estado.ROOT_LISTO }
            shizukuListo -> Estado.SHIZUKU_SIN_PERMISO
            else -> { ejecutor = null; Estado.SIN_VIA }
        }
        if (original.isEmpty()) original = RobotVerificador(ejecutor).leerEstado()
        return estado
    }

    /** Aplica un perfil y devuelve el resultado con su comprobación. */
    fun aplicar(perfil: PerfilDispositivo): Resultado {
        val e = ejecutor ?: return Resultado(
            false, Via.NINGUNA,
            "No se aplicó nada: no hay Shizuku con permiso ni root en este teléfono.",
        )
        val r = Escritor(e).aplicar(perfil)
        // Si el fabricante bloqueó las propiedades, el perfil no sirvió y se dice.
        return r
    }

    fun restaurar(): Resultado {
        val e = ejecutor ?: return Resultado(false, Via.NINGUNA, "No hay vía para restaurar.")
        return Escritor(e).restaurar(original)
    }

    /** Lo que el teléfono declara AHORA. Sin adornos. */
    fun telefoniaActual(): Map<String, String> = RobotVerificador(ejecutor).leerEstado()
}
