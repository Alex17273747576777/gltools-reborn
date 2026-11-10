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

    companion object {
        /** Propiedad de la prueba de escritura: de las que la vía ADB sí puede tocar. */
        const val CLAVE_PRUEBA = "debug.reborn.prueba"
        /** Propiedad que sólo root puede escribir. Sólo se prueba cuando la vía es root. */
        const val CLAVE_PROTEGIDA = "persist.reborn.prueba"
        /** Prefijos que el dominio `shell` puede escribir (AOSP, system/sepolicy). */
        val PREFIJOS_SIN_ROOT = listOf("debug.", "persist.debug.", "log.tag.", "persist.log.tag.")
        /** La única pregunta que Android contesta sobre el modo fijo: su código de salida. */
        const val SONDA_MODO_FIJO = "cmd power help"

        /**
         * ¿Puede la vía ADB —Shizuku sin root: dominio SELinux `shell`, uid 2000— escribir esta
         * propiedad?
         *
         * No es una suposición: sale de `system/sepolicy` de AOSP (Android 14). `private/shell.te`
         * da a `shell` permiso sobre `debug_prop`, `persist_debug_prop` y `log_tag_prop`, y
         * `private/property_contexts` asigna esos tipos a los prefijos de abajo. Todo lo que no
         * casa con ningún prefijo —`persist.reborn.prueba`, por ejemplo— cae en `default_prop`,
         * que `shell` NO puede escribir. Por eso la prueba antigua fallaba en todos los teléfonos
         * sin root con «Failed to set property … See dmesg»: pedía algo que Android prohíbe.
         */
        fun escribibleSinRoot(clave: String): Boolean = PREFIJOS_SIN_ROOT.any { clave.startsWith(it) }
    }

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
            pruebas += probarProtegidas()
            pruebas += probar("Resolución (wm)", "wm size")
            pruebas += probar("Densidad (wm)", "wm density")
            pruebas += probar("Lista de apps (pm)", "pm list packages -3")
            pruebas += probar("Ajustes del sistema (settings)", "settings get global private_dns_mode")
            pruebas += probar("Congelar apps (pm disable)", "pm list packages -d")
            // Cómo se prueba y por qué, en `juzgarModoFijo`.
            pruebas += probarModoRendimiento()
        } else {
            val motivo = "Se salta: sin shell no se puede probar"
            for (n in listOf("Lectura de propiedades", "Escribir y deshacer una propiedad",
                "Propiedades protegidas (persist.*, ro.*)", "Resolución (wm)",
                "Densidad (wm)", "Lista de apps (pm)", "Ajustes del sistema (settings)",
                "Congelar apps (pm disable)", "Modo rendimiento (cmd)")) {
                pruebas += Prueba(n, Prueba.Estado.NO_APLICA, motivo)
            }
        }

        return pruebas
    }

    // ── Las dos pruebas que mentían (corregidas el 2026-11-10) ─────────────────────────────
    //
    // La lógica es pura a propósito, como `esHuaweiDe`: recibe lo que contestó el teléfono y
    // decide. Así se prueba sin un teléfono delante, que es donde se equivocaba.

    /** `id -u` devuelve 0 sólo cuando la vía es root (Shizuku arrancado con root, o Sui). */
    fun esRootDe(salidaIdU: String?): Boolean = salidaIdU?.trim() == "0"

    /** Decide la prueba de escritura a partir de lo que contestó el teléfono. */
    fun juzgarEscritura(
        nombre: String,
        clave: String,
        codigo: Int?,
        motivo: String,
        leido: String,
    ): Prueba = when {
        codigo == null -> Prueba(nombre, Prueba.Estado.FALLA, "sin respuesta del gestor")
        leido == "1" -> Prueba(nombre, Prueba.Estado.PASA,
            "se escribió `$clave`, se leyó y se devolvió a como estaba. No queda nada cambiado.")
        motivo.contains("Failed to set property") -> Prueba(nombre, Prueba.Estado.FALLA,
            "Android rechazó escribir `$clave`. Es una propiedad que esta vía debería poder " +
                "tocar, así que el bloqueo lo pone el fabricante: los perfiles que escriben " +
                "propiedades no funcionarán aquí. Contestó: ${motivo.take(120)}")
        leido.isEmpty() -> Prueba(nombre, Prueba.Estado.FALLA,
            "el comando no dio error, pero `$clave` no cambió: ${motivo.take(120)}")
        else -> Prueba(nombre, Prueba.Estado.FALLA,
            "se pidió escribir 1 en `$clave` y al leer salió «${leido.take(30)}»")
    }

    /**
     * Decide la prueba del modo rendimiento a partir de lo que contestó
     * `cmd power set-fixed-performance-mode-enabled`.
     *
     * Leído en `PowerManagerShellCommand.java` de AOSP: esa orden existe desde Android 11
     * (API 30) y devuelve 0 **sólo si el HAL de energía aceptó el modo**; si no, -1 (255 en
     * la shell) con «Failed to set FIXED_PERFORMANCE mode». Ese código es la única respuesta
     * que da Android: no hay consulta de «¿está soportado?» ni ajuste que leer después.
     *
     * Dos errores de la prueba anterior, los dos visibles en el mismo teléfono:
     *  · preguntaba `cmd power help` y exigía código 0, pero `help` devuelve -1 **siempre**
     *    (`handleDefaultCommands` imprime la ayuda y sale con -1). Resultado: «el servicio de
     *    energía no responde» pegado a la ayuda que el servicio acababa de responder;
     *  · después leía el ajuste `power_fixed_performance_mode_enabled`, que no existe en
     *    ninguna versión de Android: aunque lo anterior hubiera pasado, esto no podía pasar.
     */
    fun juzgarModoFijo(sdk: Int, codigo: Int?, texto: String): Prueba {
        val nombre = "Modo rendimiento (cmd)"
        return when {
            sdk < 30 -> Prueba(nombre, Prueba.Estado.NO_APLICA,
                "el modo de rendimiento fijo existe desde Android 11 (API 30); este teléfono es API $sdk")
            codigo == null -> Prueba(nombre, Prueba.Estado.FALLA, "sin respuesta del gestor")
            texto.contains("Power manager (power) commands:") -> Prueba(nombre, Prueba.Estado.NO_APLICA,
                "La orden devolvió ayuda, no una confirmación. El servicio sí respondió.")
            codigo == 0 && !texto.contains("Failed to") && !texto.contains("Unknown command") -> Prueba(nombre, Prueba.Estado.PASA,
                "La shell aceptó la solicitud. Esto no mide CPU/GPU ni confirma una mejora física.")
            texto.contains("FIXED_PERFORMANCE") || texto.contains("Power HAL") ->
                Prueba(nombre, Prueba.Estado.FALLA,
                    "el servicio de energía responde, pero este teléfono no implementa el modo " +
                        "fijo (el fabricante no lo conectó al HAL de energía). El interruptor " +
                        "no hará nada aquí.")
            texto.contains("Unknown command") -> Prueba(nombre, Prueba.Estado.FALLA,
                "este Android no tiene la orden del modo fijo: `cmd power` contestó «Unknown command»")
            else -> Prueba(nombre, Prueba.Estado.FALLA,
                "código $codigo: ${texto.lineSequence().firstOrNull().orEmpty().take(120).ifBlank { "sin detalle" }}")
        }
    }

    /**
     * Prueba de escritura **que se deshace**.
     *
     * Se lee el valor anterior, se escribe, se comprueba y se devuelve el anterior. La clave es
     * `debug.reborn.prueba` porque es de las que la vía ADB sí puede escribir (ver
     * `escribibleSinRoot`) y porque las `debug.*` no se guardan: desaparece al reiniciar.
     */
    private fun probarEscritura(): Prueba =
        escribirYDeshacer("Escribir y deshacer una propiedad", CLAVE_PRUEBA)

    /**
     * Lo que la prueba de arriba NO contesta: las propiedades que no son `debug.*`.
     *
     * Sin root no se intenta: Android lo prohíbe y el resultado se sabe de antemano. Decirlo
     * así es más útil que un «Failed to set property» sin contexto. Con root sí se prueba.
     */
    private fun probarProtegidas(): Prueba {
        val nombre = "Propiedades protegidas (persist.*, ro.*)"
        val uid = shell.correr("id -u")?.texto?.trim()
        if (!esRootDe(uid)) {
            return Prueba(nombre, Prueba.Estado.NO_APLICA,
                "la vía actual es ADB (uid ${uid?.take(8).orEmpty().ifBlank { "?" }}), no root. " +
                    "Android sólo deja a esa vía escribir debug.* y persist.debug.*; el resto lo " +
                    "reserva al sistema y a root. Los perfiles que cambian esas propiedades " +
                    "necesitan root: no es un fallo de tu teléfono.")
        }
        return escribirYDeshacer(nombre, CLAVE_PROTEGIDA)
    }

    private fun escribirYDeshacer(nombre: String, clave: String): Prueba {
        fun citar(valor: String): String = "'" + valor.replace("'", "'\"'\"'") + "'"
        val lectura = shell.correr("getprop $clave")
        if (lectura?.ok != true) return Prueba(nombre, Prueba.Estado.FALLA,
            "No se pudo leer el valor original; no se intentó escribir.")
        val antes = lectura.texto.trim()
        val escribio = shell.correr("setprop $clave 1")
        val lecturaNueva = shell.correr("getprop $clave")
        val restauracion = shell.correr("setprop $clave ${citar(antes)}")
        val lecturaFinal = shell.correr("getprop $clave")
        if (restauracion?.ok != true || lecturaFinal?.ok != true || lecturaFinal.texto.trim() != antes) {
            return Prueba(nombre, Prueba.Estado.FALLA,
                "Restauración de `$clave` NO CONFIRMADA. Valor original: ${antes.take(90)}. " +
                    "Respuesta: ${restauracion?.motivo ?: "sin respuesta"}. Revisa el dispositivo.")
        }
        if (escribio?.ok != true || lecturaNueva?.ok != true) return Prueba(nombre, Prueba.Estado.FALLA,
            "La escritura o su lectura falló. Se confirmó el valor anterior por lectura; " +
                "esto no demuestra eliminación de la clave ni persistencia tras reiniciar.")
        val resultado = juzgarEscritura(nombre, clave, escribio.codigo, escribio.motivo, lecturaNueva.texto.trim())
        return resultado.copy(detalle = if (resultado.resultado == Prueba.Estado.PASA)
            "Escritura y restauración del valor confirmadas por lectura. No demuestra rendimiento, " +
                "eliminación de la clave ni persistencia tras reiniciar."
            else resultado.detalle)
    }

    /** Sólo lectura: una prueba de compatibilidad no debe apagar un modo activo. */
    fun juzgarAyudaModoFijo(sdk: Int, codigo: Int?, texto: String): Prueba {
        val nombre = "Modo rendimiento (cmd)"
        if (sdk < 30) return Prueba(nombre, Prueba.Estado.NO_APLICA, "Disponible desde API 30.")
        if (codigo == null) return Prueba(nombre, Prueba.Estado.FALLA, "Sin respuesta de la shell; causa no determinada.")
        // BasicShellCommandHandler imprime ayuda y devuelve -1 (255 en shell). No es fallo del servicio.
        val ayudaValida = texto.contains("Power manager (power) commands:") && codigo in listOf(0, -1, 255)
        if (!ayudaValida) return Prueba(nombre, Prueba.Estado.FALLA,
            "No se obtuvo ayuda reconocible (código $codigo): ${texto.take(600)}")
        val anunciada = Regex("(?m)^\\s*set-fixed-performance-mode-enabled(?:\\s|\\[|$)").containsMatchIn(texto)
        return if (anunciada) Prueba(nombre, Prueba.Estado.PASA,
            "La ROM anuncia la orden. Sólo se consultó ayuda: soporte del HAL y efecto físico NO VERIFICADOS.")
        else Prueba(nombre, Prueba.Estado.NO_APLICA,
            "La ayuda no anuncia esta orden. El servicio respondió; no se activó ni desactivó nada.")
    }

    private fun probarModoRendimiento(): Prueba {
        if (Build.VERSION.SDK_INT < 30) return juzgarAyudaModoFijo(Build.VERSION.SDK_INT, null, "")
        val r = shell.correr(SONDA_MODO_FIJO)
        return juzgarAyudaModoFijo(Build.VERSION.SDK_INT, r?.codigo, r?.texto.orEmpty())
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
