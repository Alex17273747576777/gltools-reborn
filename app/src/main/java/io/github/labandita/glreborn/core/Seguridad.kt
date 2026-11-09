package io.github.labandita.glreborn.core

/**
 * El filtro que impide que esta app rompa un teléfono.
 *
 * No es un aviso ni una casilla: es una comprobación que se ejecuta ANTES de cada
 * escritura y que puede decir NO. Tres capas, de la más dura a la más blanda.
 */
enum class Modo(val titulo: String, val explicacion: String) {
    SEGURO(
        "Modo seguro",
        "Sólo se tocan las propiedades de identidad del dispositivo: marca, modelo, placa, " +
            "hardware y versión de OpenGL ES. Son las únicas que los juegos leen y las únicas " +
            "que no pueden dejar el teléfono sin arrancar. Si algo no cuadra al releer, se " +
            "devuelve todo como estaba, solo.",
    ),
    EXPERTO(
        "Modo experto",
        "Añade versión de Android, huella de compilación, kernel y número de API. Esto SÍ " +
            "puede romper apps —cambiar el API hace que apps viejas dejen de instalarse— y por " +
            "eso cada escritura se anota antes de hacerse y se deshace sola si la app muere a " +
            "mitad. Las propiedades de identidad del teléfono siguen bloqueadas: eso no es un " +
            "perfil, es una identidad.",
    );

    fun alternar(): Modo = if (this == SEGURO) EXPERTO else SEGURO
}

object Seguridad {

    /**
     * NUNCA. Ni en modo experto. Estas propiedades no cambian el aspecto del teléfono:
     * cambian si arranca. `persist.*` se relee en el arranque siguiente, `ro.boot.*` viene
     * del cargador de arranque y `ro.secure`/`ro.debuggable` deciden si el sistema confía
     * en sí mismo. Equivocarse aquí es un teléfono que no pasa del logo.
     */
    val NUNCA: List<String> = listOf(
        "persist.", "ro.boot.", "sys.", "ro.secure", "ro.debuggable",
        "ro.selinux", "selinux.", "ro.zygote", "init.", "ro.hardware.chipname",
    )

    /**
     * IDENTIDAD. Bloqueadas en los dos modos, siempre. Cambiar el IMEI, el número de serie,
     * la MAC o el Android ID no simula otro teléfono: falsifica la identidad única de un
     * aparato, que es lo que hace falta para cometer fraude y lo que la ley persigue.
     * Esta app no lo hace ni en modo experto, y la lista está a la vista para que se sepa.
     */
    val IDENTIDAD: List<String> = listOf(
        "imei", "meid", "serial", "android_id", "ro.serialno", "gsm.sim",
        "wifi.mac", "ro.wifi", "bluetooth.address", "ro.btmacaddr", "oem_id",
    )

    /** Sólo en modo experto: cambian el comportamiento del sistema, no su arranque. */
    val SOLO_EXPERTO: List<String> = listOf(
        "ro.build.version.sdk", "ro.build.version.release", "ro.build.fingerprint",
        "ro.build.tags", "ro.build.type", "ro.build.display.id", "ro.build.description",
        "ro.product.cpu.abi", "ro.kernel", "ro.build.id", "ro.build.date",
    )

    /** Las que el modo seguro permite. Es la lista corta y el corazón de la app. */
    val LIBRES: List<String> = listOf(
        "ro.product.brand", "ro.product.manufacturer", "ro.product.model",
        "ro.product.device", "ro.product.board", "ro.product.name",
        "ro.product.marketname", "ro.hardware", "ro.board.platform",
        "ro.opengles.version", "ro.sf.lcd_density",
    )

    data class Permiso(val permitido: Boolean, val motivo: String = "", val aviso: String = "")

    /**
     * Límite real de Android para el valor de una propiedad. No es una precaución mía:
     * `PROP_VALUE_MAX` es 92 en bionic. Un valor más largo se trunca o se rechaza, así que
     * escribirlo es garantizar que quede mal escrito.
     */
    const val MAX_VALOR = 92

    private val PERMITIDOS = Regex("^[A-Za-z0-9 ._\\-+()/:,]*$")

    fun revisar(clave: String, valor: String, modo: Modo): Permiso {
        val k = clave.trim()
        val v = valor.trim()

        if (k.isBlank()) return Permiso(false, "clave vacía")

        // 1 · Identidad: nunca, en ningún modo.
        if (IDENTIDAD.any { k.contains(it, ignoreCase = true) }) {
            return Permiso(false, "identidad del teléfono: bloqueada siempre, también en modo experto")
        }

        // 2 · Lo que puede impedir el arranque: nunca.
        if (NUNCA.any { k == it || k.startsWith(it) }) {
            return Permiso(false, "propiedad de arranque: cambiarla puede dejar el teléfono sin sistema")
        }

        // 3 · Inyección de shell y caracteres de control.
        if (!PERMITIDOS.matches(v)) {
            return Permiso(false, "el valor tiene caracteres que el sistema no admite (comillas, punto y coma, saltos de línea…)")
        }
        if (v.contains("\n") || v.contains("\u0000")) {
            return Permiso(false, "salto de línea o carácter nulo en el valor")
        }

        // 4 · Longitud real máxima del sistema.
        if (v.length > MAX_VALOR) {
            return Permiso(false, "el valor tiene ${v.length} caracteres y el máximo del sistema es $MAX_VALOR")
        }

        // 5 · Valor vacío: se omite, no se escribe (vaciar una propiedad no es lo mismo que no tocarla).
        if (v.isEmpty()) return Permiso(false, "valor vacío")

        val esLibre = LIBRES.any { k == it || k.startsWith("$it.") }
        val esExperto = SOLO_EXPERTO.any { k == it || k.startsWith(it) }

        return when {
            esLibre -> Permiso(true)
            esExperto && modo == Modo.EXPERTO -> Permiso(
                true, "",
                "propiedad de sistema: se anota antes de escribir para poder deshacerla",
            )
            esExperto && modo == Modo.SEGURO -> Permiso(
                false, "requiere modo experto ($k cambia el sistema, no el modelo)",
            )
            modo == Modo.SEGURO -> Permiso(
                false, "no está en la lista del modo seguro",
            )
            else -> Permiso(true, "", "clave fuera del catálogo conocido: se anota y se puede deshacer")
        }
    }

    /** Para la pantalla de diagnóstico: las listas, dichas, sin esconder nada. */
    fun resumenListas(): List<Pair<String, String>> = listOf(
        "Bloqueadas siempre (identidad)" to IDENTIDAD.joinToString(", "),
        "Bloqueadas siempre (arranque)" to NUNCA.joinToString(", "),
        "Sólo modo experto" to SOLO_EXPERTO.joinToString(", "),
        "Permitidas en modo seguro" to LIBRES.joinToString(", "),
    )
}
