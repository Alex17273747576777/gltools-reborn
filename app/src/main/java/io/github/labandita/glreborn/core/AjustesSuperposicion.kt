package io.github.labandita.glreborn.core

import android.content.Context
import android.graphics.Color

/**
 * Los ajustes del punto de mira, con su modelo puro.
 *
 * Está en `core` a propósito: así se puede probar en JVM sin teléfono. Todo lo que decide
 * (qué modos hay, qué tamaños se permiten, qué color se acepta) es una función de datos, y el
 * `Context` sólo aparece en `leer`/`guardar`.
 *
 * Nada de esto necesita root. Es la API pública de superposiciones de Android.
 */
data class AjustesSuperposicion(
    val modo: String = "MIRA",
    val tamano: Float = 26f,
    val grosor: Float = 2.5f,
    val color: Int = COLOR_CIAN,
    val fps: Boolean = true,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
) {
    companion object {
        const val COLOR_CIAN = 0xFF5CE1E6.toInt()
        const val COLOR_VERDE = 0xFF4ADE80.toInt()
        const val COLOR_ROSA = 0xFFFF7AB6.toInt()
        const val COLOR_BLANCO = 0xFFFFFFFF.toInt()
        const val COLOR_ROJO = 0xFFFF6B81.toInt()

        val MODOS = listOf("MIRA", "CIRCULO", "CRUZ", "APAGADO")
        val TAMANOS = listOf(18f, 26f, 34f, 44f)
        val GROSORES = listOf(1.5f, 2.5f, 3.5f, 5f)
        val COLORES = listOf(COLOR_CIAN, COLOR_VERDE, COLOR_ROSA, COLOR_BLANCO, COLOR_ROJO)

        private const val PREFS = "superposicion"

        fun leer(c: Context): AjustesSuperposicion {
            val p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return AjustesSuperposicion(
                modo = p.getString("modo", "MIRA") ?: "MIRA",
                tamano = p.getFloat("tamano", 26f),
                grosor = p.getFloat("grosor", 2.5f),
                color = p.getInt("color", COLOR_CIAN),
                fps = p.getBoolean("fps", true),
                x = p.getFloat("x", 0.5f),
                y = p.getFloat("y", 0.5f),
            )
        }

        fun guardar(c: Context, a: AjustesSuperposicion) {
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("modo", a.modo)
                .putFloat("tamano", a.tamano)
                .putFloat("grosor", a.grosor)
                .putInt("color", a.color)
                .putBoolean("fps", a.fps)
                .putFloat("x", a.x)
                .putFloat("y", a.y)
                .apply()
        }

        fun guardarPosicion(c: Context, x: Float, y: Float) {
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putFloat("x", x).putFloat("y", y).apply()
        }
    }

    /** Lo que el ajuste hace, en una línea, sin promesas. Se usa en la interfaz. */
    fun resumen(): String = when (modo.uppercase()) {
        "APAGADO" -> if (fps) "Sin punto de mira · contador de FPS activo"
                    else "Apagado del todo: no se dibuja nada"
        "CIRCULO" -> "Círculo ${tamano.toInt()} px" + if (fps) " · con FPS" else ""
        "CRUZ" -> "Cruz ${tamano.toInt()} px" + if (fps) " · con FPS" else ""
        else -> "Punto de mira ${tamano.toInt()} px" + if (fps) " · con FPS" else ""
    }

    /**
     * ¿Vale la pena tener el servicio encendido? Si no se dibuja nada, no: gastar una
     * notificación fija para nada es molestar.
     */
    fun debeEstarActivo(): Boolean = modo.uppercase() != "APAGADO" || fps

    /** Sanea lo que venga de fuera: modo, tamaño, grosor y color sólo de las listas. */
    fun saneado(): AjustesSuperposicion = copy(
        modo = if (modo.uppercase() in MODOS) modo.uppercase() else "MIRA",
        tamano = if (tamano in TAMANOS) tamano else 26f,
        grosor = if (grosor in GROSORES) grosor else 2.5f,
        color = if (color in COLORES) color else COLOR_CIAN,
        x = x.coerceIn(0.04f, 0.96f),
        y = y.coerceIn(0.04f, 0.96f),
    )

    fun colorLegible(): String = when (color) {
        COLOR_CIAN -> "cian"
        COLOR_VERDE -> "verde"
        COLOR_ROSA -> "rosa"
        COLOR_ROJO -> "rojo"
        else -> "blanco"
    }

    /** Para la vista previa en la interfaz: el color como `Color` de Compose. */
    fun colorCompose() = androidx.compose.ui.graphics.Color(color)
}
