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
            pruebas += probar("Escritura de propiedades", "setprop ro.product.marketname prueba_reborn")
            pruebas += probar("Resolución (wm)", "wm size")
            pruebas += probar("Densidad (wm)", "wm density")
            pruebas += probar("Lista de apps (pm)", "pm list packages -3")
            pruebas += probar("Ajustes del sistema (settings)", "settings get global private_dns_mode")
            pruebas += probar("Congelar apps (pm disable)", "pm list packages -d")
            pruebas += probar("Modo rendimiento (cmd)", "cmd power get-fixed-performance-mode-supported")
        } else {
            val motivo = "Se salta: sin shell no se puede probar"
            for (n in listOf("Lectura de propiedades", "Escritura de propiedades", "Resolución (wm)",
                "Densidad (wm)", "Lista de apps (pm)", "Ajustes del sistema (settings)",
                "Congelar apps (pm disable)", "Modo rendimiento (cmd)")) {
                pruebas += Prueba(n, Prueba.Estado.NO_APLICA, motivo)
            }
        }

        return pruebas
    }

    private fun probar(nombre: String, comando: String): Prueba {
        val r = shell.correr(comando)
        return when {
            r == null -> Prueba(nombre, Prueba.Estado.FALLA, "sin respuesta del gestor")
            r.codigo == 0 -> Prueba(nombre, Prueba.Estado.PASA,
                if (r.texto.isBlank()) "responde sin error" else "«${r.texto.lineSequence().first().take(60)}»")
            else -> Prueba(nombre, Prueba.Estado.FALLA,
                "código ${r.codigo}: «${r.texto.take(80).ifBlank { "sin detalle" }}»")
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
