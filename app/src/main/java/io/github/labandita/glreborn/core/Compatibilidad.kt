package io.github.labandita.glreborn.core

import android.os.Build
import io.github.labandita.glreborn.shizuku.Shell

/**
 * Prueba de compatibilidad que corre EN EL TELÉFONO del usuario.
 *
 * Existe porque desde aquí no puedo probar el APK en cada Android ni en cada Huawei:
 * la máquina de compilación no tiene emulador. Lo que sí puedo hacer es que la app
 * compruebe lo suyo y lo cuente, en vez de que el usuario descubra los fallos a ciegas.
 *
 * Cada línea es una prueba que se ejecuta de verdad, no una casilla marcada:
 * se lanza el comando y se mira si respondió.
 */
class Compatibilidad(private val shell: Shell) {

    data class Prueba(
        val nombre: String,
        val resultado: Estado,
        val detalle: String,
    ) {
        enum class Estado { PASA, FALLA, NO_APLICA }
    }

    /**
     * La detección es lógica pura a propósito: así se puede probar sin un teléfono.
     * `esHuawei()` y `esHarmony()` sólo leen los datos del sistema y llaman aquí.
     */
    fun esHuaweiDe(fabricante: String?, marca: String?): Boolean {
        val m = (fabricante ?: "").lowercase()
        val b = (marca ?: "").lowercase()
        return listOf("huawei", "honor").any { m.contains(it) || b.contains(it) }
    }

    fun esHarmonyDe(campos: String?): Boolean = (campos ?: "").lowercase().contains("harmony")

    /** Fabricantes cuyo Android se aparta más de lo estándar. */
    fun esHuawei(): Boolean = esHuaweiDe(Build.MANUFACTURER, Build.BRAND)

    fun esHarmony(): Boolean = esHarmonyDe(
        listOf(
            runCatching { Build.DISPLAY }.getOrDefault(""),
            runCatching { Build.VERSION.INCREMENTAL }.getOrDefault(""),
            runCatching { System.getProperty("os.version") }.getOrDefault(""),
        ).joinToString(" ")
    )

    /**
     * Lee del teléfono lo que hace falta para generar perfiles.
     *
     * Se pide en UNA llamada a la shell, no una por propiedad: cada `correr()` abre un
     * proceso, y en un teléfono modesto eso se nota.
     */
    fun sondar(): Sonda.Datos {
        val props = try {
            val salida = shell.correr(Sonda.comando())?.texto.orEmpty()
            // Se pide una por línea, así que se numera la respuesta con las claves.
            val valores = salida.lineSequence().map { it.trim() }.toList()
            Sonda.CLAVES.mapIndexedNotNull { i, clave ->
                valores.getOrNull(i)?.takeIf { it.isNotBlank() }?.let { clave to it }
            }.toMap()
        } catch (t: Throwable) {
            emptyMap()
        }
        return Sonda.juntar(
            props = props,
            versionAndroid = Build.VERSION.RELEASE,
            arquitectura = Build.SUPPORTED_ABIS.toList(),
            esHuawei = esHuawei(),
        )
    }

    fun datosBasicos(): List<Pair<String, String>> = listOf(
        "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "Fabricante" to Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
        "Modelo" to Build.MODEL,
        "Arquitectura" to Build.SUPPORTED_ABIS.joinToString(", "),
        "HarmonyOS" to if (esHarmony()) "sí (detectado)" else "no detectado",
    )

    /**
     * Las pruebas. Se ejecutan en orden y cada una dice qué pasó, con el texto que
     * devolvió el sistema cuando falla. Nada de «compatible» genérico.
     */
    fun ejecutar(): List<Prueba> {
        val pruebas = mutableListOf<Prueba>()

        pruebas += if (Build.VERSION.SDK_INT >= 26) {
            Prueba("Versión de Android soportada", Prueba.Estado.PASA,
                "API ${Build.VERSION.SDK_INT} ≥ 26 (Android 8.0)")
        } else {
            Prueba("Versión de Android soportada", Prueba.Estado.FALLA,
                "API ${Build.VERSION.SDK_INT} < 26: esta app no se instala aquí")
        }

        if (esHuawei() && esHarmony()) {
            pruebas += Prueba("HarmonyOS", Prueba.Estado.NO_APLICA,
                "HarmonyOS NEXT no ejecuta APKs de Android. Si la app se abrió, es una " +
                    "versión con base Android (EMUI); prueba las funciones de abajo una a una.")
        }

        // Shell de Shizuku: la base de todo lo que escribe.
        val eco = shell.correr("echo fox")
        pruebas += when {
            eco == null -> Prueba("Shell de Shizuku", Prueba.Estado.FALLA,
                "No hay gestor activo (Shizuku u OmniShizuku). Actívalo por depuración inalámbrica.")
            eco.texto.trim() == "fox" -> Prueba("Shell de Shizuku", Prueba.Estado.PASA,
                "responde: «${eco.texto.trim()}»")
            else -> Prueba("Shell de Shizuku", Prueba.Estado.FALLA,
                "respondió algo raro: «${eco.texto.take(60)}»")
        }

        if (shell.disponible()) {
            pruebas += probar("Lectura de propiedades", "getprop ro.product.model")
            // Antes se probaba escribiendo `ro.product.marketname`... y no se deshacía: el
            // teléfono se quedaba con un valor inventado y **la app no lo apuntaba en el registro**,
            // así que «Restaurar» no sabía que tenía que devolverlo. Una prueba que deja basura
            // es peor que no probar.
            pruebas += probarEscritura()
            pruebas += probar("Resolución (wm)", "wm size")
            pruebas += probar("Densidad (wm)", "wm density")
            pruebas += probar("Lista de apps (pm)", "pm list packages -3")
            pruebas += probar("Ajustes del sistema (settings)", "settings get global private_dns_mode")
            pruebas += probar("Congelar apps (pm disable)", "pm list packages -d")
            // `get-fixed-performance-mode-supported` NO EXISTE: `cmd power` en Android 13 sólo
            // tiene `set-mode`, `set-fixed-performance-mode-enabled` y `help`. De ahí el 255.
            // Lo que sí se puede saber sin inventar nada: si el servicio de energía responde y
            // si el sistema admite el modo fijo.
            pruebas += probarModoRendimiento()
        } else {
            val motivo = "Se salta: sin shell no se puede probar"
            for (n in listOf("Lectura de propiedades", "Escribir y deshacer una propiedad", "Resolución (wm)",
                "Densidad (wm)", "Lista de apps (pm)", "Ajustes del sistema (settings)",
                "Congelar apps (pm disable)", "Modo rendimiento (cmd)")) {
                pruebas += Prueba(n, Prueba.Estado.NO_APLICA, motivo)
            }
        }

        return pruebas
    }

    /**
     * Prueba de escritura **que se deshace**.
     *
     * Se lee el valor anterior, se escribe, se comprueba y se devuelve el anterior. Así la prueba
     * contesta la pregunta de verdad («¿puede esta app escribir propiedades con su vía actual?»)
     * sin dejar el teléfono cambiado si sale mal a la mitad.
     */
    private fun probarEscritura(): Prueba {
        val clave = "persist.reborn.prueba"
        val antes = shell.correr("getprop $clave")?.texto?.trim().orEmpty()
        val escribio = shell.correr("setprop $clave 1")
        if (escribio == null) {
            return Prueba("Escribir y deshacer una propiedad", Prueba.Estado.FALLA,
                "sin respuesta del gestor")
        }
        val leido = shell.correr("getprop $clave")?.texto?.trim().orEmpty()
        // Se deshace SIEMPRE, antes de devolver el resultado.
        shell.correr("setprop $clave '${antes.ifBlank { "" }}'")
        return when {
            leido == "1" && antes.isBlank() -> Prueba(
                "Escribir y deshacer una propiedad", Prueba.Estado.PASA,
                "se escribió, se leyó y se borró. No queda nada cambiado.",
            )
            leido.isEmpty() -> Prueba(
                "Escribir y deshacer una propiedad", Prueba.Estado.FALLA,
                "sonó bien pero no cambió: ${escribio.motivo}",
            )
            else -> Prueba(
                "Escribir y deshacer una propiedad", Prueba.Estado.FALLA,
                "se escribió lo que dice el código, no 1 (${leido.take(30)})",
            )
        }
    }

    /**
     * Modo rendimiento, **sin inventar comandos**.
     *
     * Android no tiene una consulta de «¿está soportado?». Lo que se puede hacer, y se hace:
     * preguntar a `cmd power` si responde (código 0 con su ayuda) y probar el encendido leyendo
     * el ajuste que deja (`power_fixed_performance_mode_enabled`). Si el sistema no lo admite,
     * `settings` lo dirá.
     */
    private fun probarModoRendimiento(): Prueba {
        val responde = shell.correr("cmd power help")
        if (responde == null || responde.codigo != 0) {
            return Prueba("Modo rendimiento (cmd)", Prueba.Estado.FALLA,
                "el servicio de energía no responde a `cmd power`: " +
                    "${responde?.motivo ?: "sin respuesta"}")
        }
        val ajuste = shell.correr("settings get global power_fixed_performance_mode_enabled")
        val hayAjuste = ajuste != null && ajuste.codigo == 0 &&
            ajuste.texto.trim() in listOf("0", "1")
        return if (hayAjuste) Prueba("Modo rendimiento (cmd)", Prueba.Estado.PASA,
            "el sistema expone el modo fijo (ahora en ${ajuste!!.texto.trim()})")
        else Prueba("Modo rendimiento (cmd)", Prueba.Estado.FALLA,
            "el servicio responde pero este sistema no expone el modo fijo de rendimiento")
    }

    private fun probar(nombre: String, comando: String): Prueba {
        val r = shell.correr(comando)
        return when {
            r == null -> Prueba(nombre, Prueba.Estado.FALLA, "sin respuesta del gestor")
            r.codigo == 0 -> Prueba(nombre, Prueba.Estado.PASA,
                if (r.texto.isBlank()) "responde sin error" else "«${r.texto.lineSequence().first().take(60)}»")
            // El «sin detalle» de antes no era falta de información: era que se lanzaba el
            // comando sin `2>&1` y los mensajes de error iban a un sitio que no se leía.
            else -> Prueba(nombre, Prueba.Estado.FALLA, "código ${r.codigo}: ${r.motivo}")
        }
    }

    /** Resumen honesto para enseñar arriba. */
    fun resumen(pruebas: List<Prueba>): String {
        val pasan = pruebas.count { it.resultado == Prueba.Estado.PASA }
        val fallan = pruebas.count { it.resultado == Prueba.Estado.FALLA }
        val saltan = pruebas.count { it.resultado == Prueba.Estado.NO_APLICA }
        return cuando(pasan, fallan, saltan)
    }

    private fun cuando(pasan: Int, fallan: Int, saltan: Int): String = buildString {
        append("$pasan pasan")
        if (fallan > 0) append(" · $fallan fallan")
        if (saltan > 0) append(" · $saltan sin probar")
        if (fallan > 0) append(" — las que fallan no funcionarán en este teléfono")
    }
}
