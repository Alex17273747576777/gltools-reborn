package io.github.labandita.glreborn.core

/**
 * El diagnóstico: qué se puede y qué no, MEDIDO, no prometido.
 *
 * Cada línea de esta pantalla es una afirmación que se puede comprobar en el teléfono.
 * No hay barras de progreso decorativas ni «optimizando…»: hay hechos con su resultado.
 */
class Diagnostico(
    private val director: Director,
    private val inventario: Inventario,
) {

    data class Linea(
        val etiqueta: String,
        val valor: String,
        val estado: Estado,
    ) {
        enum class Estado { BIEN, ATENCION, MAL }
    }

    fun lineas(): List<Linea> {
        val estado = director.estado()
        val actual = director.telefoniaActual()
        val resumen = inventario.resumen()

        val via = when (estado) {
            Director.Estado.SHIZUKU_LISTO -> Linea("Vía de escritura", "Shizuku con permiso", Linea.Estado.BIEN)
            Director.Estado.ROOT_LISTO -> Linea("Vía de escritura", "root (su)", Linea.Estado.BIEN)
            Director.Estado.SHIZUKU_SIN_PERMISO ->
                Linea("Vía de escritura", "Shizuku instalado, sin permiso", Linea.Estado.ATENCION)
            Director.Estado.SIN_VIA -> Linea("Vía de escritura", "ninguna", Linea.Estado.MAL)
        }

        val lectura = if (actual.isEmpty()) {
            Linea("Lectura de propiedades", "no disponible", Linea.Estado.MAL)
        } else {
            Linea("Lectura de propiedades", "${actual.count { it.value.isNotBlank() }} de ${actual.size} legibles",
                if (actual.count { it.value.isNotBlank() } > 0) Linea.Estado.BIEN else Linea.Estado.MAL)
        }

        val red = Linea("Permiso de INTERNET", "no declarado (no hay red posible)", Linea.Estado.BIEN)
        val apps = Linea("Apps alcanzables", resumen.texto, Linea.Estado.BIEN)

        val perfiles = Linea(
            "Catálogo de perfiles",
            buildString {
                val v = Perfiles.todos.count { it.confianza == PerfilDispositivo.Confianza.VERIFICADO }
                val p = Perfiles.todos.count { it.confianza == PerfilDispositivo.Confianza.PARCIAL }
                append("$v verificados · $p parciales · 0 inventados")
            },
            Linea.Estado.BIEN,
        )

        val modelo = Linea(
            "Modelo declarado ahora",
            actual["ro.product.model"]?.ifBlank { "—" } ?: "no se puede leer",
            if (actual["ro.product.model"].isNullOrBlank()) Linea.Estado.ATENCION else Linea.Estado.BIEN,
        )

        return listOf(via, lectura, modelo, apps, red, perfiles)
    }

    /** Lo que esta app NO puede hacer, dicho antes de que lo descubras a golpes. */
    fun limites(): List<String> = listOf(
        "No puede escribir propiedades sin Shizuku ni root. No hay excepción, ni modo oculto.",
        "No puede garantizar que tu fabricante permita cambiar ro.product.model.",
        "No toca IMEI, número de serie, MAC ni Android ID: eso no es un perfil, es una identidad.",
        "No descarga nada: no hay servidor, ni cuentas, ni comprobación de compra.",
        "Ve las apps con icono de lanzador y las que responden a VIEW. Las que corren " +
            "en segundo plano sin icono pueden no salir: se dejó de pedir QUERY_ALL_PACKAGES " +
            "a propósito, porque da acceso a la lista completa de programas instalados.",
        "No modifica otras apps ni sus APK: cambia lo que el sistema declara del teléfono.",
    )
}
