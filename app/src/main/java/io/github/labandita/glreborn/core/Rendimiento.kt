package io.github.labandita.glreborn.core

/**
 * Las funciones que hacen los módulos de AxManager, hechas aquí con la shell de Shizuku.
 *
 * De dónde sale cada idea: de la categoría *AxManager Plugins* de magiskmodule.com
 * (280 módulos). Se leyó **qué hace** cada uno —su ficha— y se buscó el equivalente que
 * se pueda hacer con los permisos de `adb shell`, que son los que tiene esta app. No se
 * copia ni una línea de código ni el diseño de aquel sitio: no publica código, publica
 * descripciones.
 *
 * la regla de esta casa, y aquí se nota mucho:
 *
 *  · Cada acción declara **el comando exacto** que ejecuta. Se enseña en pantalla antes
 *    de aplicarla. Si no puedes leer lo que va a hacer, no deberías ejecutarlo.
 *  · Cada acción dice **cómo se comprueba** que funcionó y **cómo se deshace**.
 *  · Muchos módulos de aquella lista piden root para lo que hacen (gobernadores de CPU,
 *    Vulkan forzado, tocar SurfaceFlinger, polling táctil). **Aquí eso no está**: sin
 *    root no se puede, y poner un botón que finja hacerlo sería mentir. Cada fila dice
 *    si necesita root.
 */
object Rendimiento {

    enum class Riesgo { BAJO, MEDIO, ALTO }

    /**
     * Una acción concreta sobre el teléfono.
     *
     * @param aplicar   comandos que se ejecutan, en orden.
     * @param comprobar comando de lectura: si devuelve [esperado], funcionó.
     * @param revertir  comandos para dejarlo como estaba.
     * @param necesitaRoot si es true, sin root NO se ofrece: se queda como información.
     */
    data class Accion(
        val id: String,
        val nombre: String,
        val queHace: String,
        val aplicar: List<String>,
        val comprobar: String,
        val esperado: String,
        val revertir: List<String>,
        val riesgo: Riesgo,
        val necesitaRoot: Boolean,
        val inspirado: String,
        val androidMinimo: Int = 26,
        val aviso: String = "",
    ) {
        val comandosLegibles: String get() = aplicar.joinToString(" ; ")
    }

    val catalogo: List<Accion> = listOf(
        Accion(
            id = "aot",
            nombre = "Compilar apps por adelantado (AOT)",
            queHace = "Le pide al sistema que compile una app entera a código máquina en vez " +
                "de ir interpretándola al abrirla. La primera apertura tarda más; todas las " +
                "siguientes, menos. Es lo que hace «Dexaot Optimizer».",
            aplicar = listOf("cmd package compile -m speed -f %PAQUETE%"),
            comprobar = "cmd package dump %PAQUETE% | grep -c 'compiler-filter: speed'",
            esperado = "1",
            revertir = listOf("cmd package compile -m verify -f %PAQUETE%"),
            riesgo = Riesgo.BAJO,
            necesitaRoot = false,
            inspirado = "Dexaot Optimizer · Game GPU Tune (AOT)",
        ),
        Accion(
            id = "suspender",
            nombre = "Suspender una app (sin desinstalar)",
            queHace = "Congela la app con `am suspend`: no puede correr en segundo plano, pero " +
                "conserva sus datos y vuelve con `am unsuspend`. Es menos agresivo que " +
                "deshabilitarla y es lo que usa «App Freezer».",
            aplicar = listOf("am suspend --user 0 %PAQUETE%"),
            comprobar = "dumpsys package %PAQUETE% | grep -c 'suspended=true'",
            esperado = "1",
            revertir = listOf("am unsuspend --user 0 %PAQUETE%"),
            riesgo = Riesgo.BAJO,
            necesitaRoot = false,
            inspirado = "App Freezer · ZKM GameSpace",
        ),
        Accion(
            id = "doze",
            nombre = "Forzar Doze ahora",
            queHace = "Mete el teléfono en reposo profundo en el acto, como si llevara horas " +
                "quieto. Sirve para probar cómo aguanta la batería en reposo. Se sale con " +
                "`unforce`. Idea de «SMART DOZE».",
            aplicar = listOf("dumpsys deviceidle force-idle"),
            comprobar = "dumpsys deviceidle | grep -c 'IDLE'",
            esperado = "1",
            revertir = listOf("dumpsys deviceidle unforce"),
            riesgo = Riesgo.MEDIO,
            necesitaRoot = false,
            inspirado = "SMART DOZE",
            aviso = "Mientras está forzado, las apps no se despiertan solas. Algunas " +
                "notificaciones pueden llegar tarde hasta que se salga.",
        ),
        Accion(
            id = "modo-juego",
            nombre = "Modo juego para una app",
            queHace = "Le dice al sistema que esa app es un juego y que priorice su rendimiento. " +
                "Android 12 en adelante. Idea de «Game Optimizers» y «Gamer Eclipse».",
            aplicar = listOf("cmd game set --mode performance %PAQUETE%"),
            comprobar = "cmd game get mode %PAQUETE%",
            esperado = "2",
            revertir = listOf("cmd game set --mode standard %PAQUETE%"),
            riesgo = Riesgo.BAJO,
            necesitaRoot = false,
            inspirado = "Game Optimizers · Gamer Eclipse · NO PARTY ESPORT",
            androidMinimo = 31,
            aviso = "Algunos fabricantes ignoran esta orden. La comprobación lo dirá: si " +
                "después de aplicarla `cmd game get` no dice 2, es que tu ROM no la acepta.",
        ),
        Accion(
            id = "rendimiento-fijo",
            nombre = "Modo de rendimiento fijo",
            queHace = "Solicita límites acotados de CPU/GPU para comparar pruebas. No es el modo " +
                "sostenido de las apps ni garantiza frecuencias máximas. Puede sobrecalentarse.",
            aplicar = listOf("cmd power set-fixed-performance-mode-enabled true"),
            // La comprobación es el código de salida de la propia orden, que es lo único que
            // Android contesta: 0 sólo si el HAL de energía aceptó el modo (AOSP,
            // PowerManagerShellCommand). Repetir «true» no cambia nada si ya está encendido.
            // Antes se leía el ajuste `power_fixed_performance_mode_enabled`, que no existe:
            // nunca daba 1 y la acción parecía rota en todos los teléfonos.
            comprobar = "cmd power set-fixed-performance-mode-enabled true >/dev/null 2>&1; echo \$?",
            esperado = "0",
            revertir = listOf("cmd power set-fixed-performance-mode-enabled false"),
            riesgo = Riesgo.MEDIO,
            necesitaRoot = false,
            inspirado = "Performance Mode · CPU Booster · AY Lightning Pro",
            androidMinimo = 30,
            aviso = "La orden existe desde Android 11. Se comprueba con el código que devuelve " +
                "el servicio de energía: si no es 0, tu teléfono no implementa este modo y la " +
                "app te lo dirá. No se guarda: tras reiniciar hay que aplicarlo otra vez.",
        ),
        Accion(
            id = "thermal",
            nombre = "Quitar el limitador térmico",
            queHace = "Desactiva los límites que el sistema pone cuando el teléfono se calienta. " +
                "Da más rendimiento sostenido a cambio de temperatura. Idea de «CPU Booster» " +
                "y «FROST GUARD».",
            aplicar = listOf("cmd thermalservice override-status 0"),
            comprobar = "dumpsys thermalservice | grep -c 'Thermal Status: 0'",
            esperado = "1",
            revertir = listOf("cmd thermalservice reset"),
            riesgo = Riesgo.ALTO,
            necesitaRoot = false,
            inspirado = "CPU Booster · FROST GUARD · Heinkel Perf",
            androidMinimo = 29,
            aviso = "ALTO: sin límite térmico el teléfono puede calentarse de verdad. Es " +
                "reversible con un toque, y al reiniciar vuelve solo. No lo dejes puesto.",
        ),
        Accion(
            id = "escaner-wifi",
            nombre = "Que el WiFi no escanee sin parar",
            queHace = "El escaneo constante de redes gasta batería. Esto lo limita cuando no " +
                "hace falta. Idea de «VOID NET» y «Network Optimizer».",
            aplicar = listOf("settings put global wifi_scan_throttle_enabled 1"),
            comprobar = "settings get global wifi_scan_throttle_enabled",
            esperado = "1",
            revertir = listOf("settings put global wifi_scan_throttle_enabled 0"),
            riesgo = Riesgo.BAJO,
            necesitaRoot = false,
            inspirado = "VOID NET · Network Optimizer",
            aviso = "Puede tardar un poco más en engancharse a una red nueva al moverte.",
        ),
        Accion(
            id = "ahorro",
            nombre = "Ahorro de batería forzado",
            queHace = "Enciende el ahorro de batería del sistema. Baja el rendimiento y la " +
                "frecuencia de la pantalla para estirar la carga.",
            aplicar = listOf("settings put global low_power 1"),
            comprobar = "settings get global low_power",
            esperado = "1",
            revertir = listOf("settings put global low_power 0"),
            riesgo = Riesgo.BAJO,
            necesitaRoot = false,
            inspirado = "VOID BATTERY · Battery Optimizer Tool",
        ),
    )

    /** Las que se pueden ofrecer en este teléfono, con su versión de Android. */
    fun disponibles(sdk: Int): List<Accion> =
        catalogo.filter { !it.necesitaRoot && sdk >= it.androidMinimo }

    /** Las que piden root: se enseñan como información, no como botón. */
    fun soloConRoot(): List<Accion> = catalogo.filter { it.necesitaRoot }

    /**
     * Lo que hacen los módulos de la lista y **no** se puede hacer sin root.
     *
     * Se enseña a propósito: una app que dice «todo lo que hace aquello, lo hago yo» está
     * mintiendo. Estas son las que necesitan más permisos que los de `adb shell`.
     */
    val sinRoot: List<Pair<String, String>> = listOf(
        "Gobernadores de CPU y subir frecuencias" to
            "escribir en /sys/devices/system/cpu: hace falta root.",
        "Forzar Vulkan en todos los juegos" to
            "tocar el driver gráfico por app: root, y si sale mal el juego no abre.",
        "Ajustar SurfaceFlinger o HWUI" to
            "propiedades de sistema que sólo el proceso de root puede reescribir.",
        "Subir el sondeo táctil (Faster Touch)" to
            "es un parámetro del kernel táctil: root, y si se pone mal, la pantalla deja " +
                "de responder.",
        "Limpiar cachés del sistema de golpe" to
            "vaciarlas todas requiere root; por app sí se puede, y aquí está.",
        "Desactivar servicios del sistema (bloatware)" to
            "se puede deshabilitar una a una con `pm disable-user`, pero saber cuáles son " +
                "seguras depende del modelo: aquí no se adivina.",
    )

    // ── Lógica pura: se prueba sin teléfono ────────────────────────────────

    /** Sustituye el paquete en los comandos que lo necesitan. */
    fun comando(accion: Accion, paquete: String? = null): List<String> =
        accion.aplicar.map { it.replace("%PAQUETE%", paquete.orEmpty()) }

    fun revertir(accion: Accion, paquete: String? = null): List<String> =
        accion.revertir.map { it.replace("%PAQUETE%", paquete.orEmpty()) }

    /**
     * ¿Funcionó? Se compara con lo que devolvió la comprobación.
     *
     * `grep -c` devuelve cuántas líneas coinciden; un `settings get` devuelve el valor.
     * Se compara como texto, sin interpretar: la app no decide qué significa «2».
     */
    fun comprueba(salida: String?, esperado: String): Boolean =
        salida?.trim()?.lineSequence()?.any { it.trim() == esperado } == true

    /** ¿Esta acción necesita un paquete concreto para tener sentido? */
    fun necesitaPaquete(accion: Accion): Boolean =
        accion.aplicar.any { it.contains("%PAQUETE%") }

    /** Aviso de riesgo, en una línea, para ponerlo delante del botón. */
    fun avisoDeRiesgo(accion: Accion): String = when (accion.riesgo) {
        Riesgo.BAJO -> "Riesgo bajo · se deshace con un toque"
        Riesgo.MEDIO -> "Riesgo medio · puede gastar más batería · se deshace con un toque"
        Riesgo.ALTO -> "Riesgo alto · puede calentar el teléfono · revierte al reiniciar"
    }
}
