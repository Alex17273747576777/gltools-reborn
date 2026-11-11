package io.github.labandita.glreborn.core

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.os.Build
import android.os.StatFs
import android.os.SystemClock
import java.io.File

/**
 * El auto-diagnóstico: medir el teléfono de verdad, no describirlo.
 *
 * La regla que manda aquí es la de siempre, y es la que hace que esto sirva: **si un dato no se
 * puede medir, se dice que no se puede medir**. No se estima, no se interpola y no se pone un
 * «≈» para que la tabla quede bonita. Cada prueba de este archivo hace trabajo real y devuelve un
 * número con su unidad; lo que no se puede hacer sin root queda marcado como tal.
 *
 * Las pruebas son cortas a propósito (un teléfono de 4 GB no es un banco de pruebas): lo que
 * miden es el ORDEN DE MAGNITUD de este aparato, no una puntuación comparable con AnTuTu. Y eso
 * está escrito en el informe, no en la letra pequeña.
 */

/** El resultado de una prueba. `valor == null` significa «no se pudo medir», con el motivo. */
data class Medida(
    val nombre: String,
    val valor: Double?,
    val unidad: String,
    val detalle: String,
    val motivoSinDato: String = "",
    /**
     * Cuando lo leído es una CATEGORÍA y no una cantidad, se dice con palabras aquí.
     *
     * Existía [valor] y nada más, y en 1.14.0 metí el estado térmico ahí como nivel 0-6. La pantalla
     * hizo lo que tenía que hacer con un número: dibujó una barra vacía y «0.00 nivel 0-6», que se
     * lee como si no hubiera nada cuando el sistema acababa de decir «sin estrangulamiento». Un
     * estado no se mide, se declara, y se enseña con palabras. Las cantidades siguen con su número y
     * su barra.
     */
    val texto: String = "",
) {
    val medida: Boolean get() = valor != null

    /** Si hay que enseñarla con palabras en vez de con número y barra. */
    val esCategoria: Boolean get() = texto.isNotBlank()
}

/** Lo que se midió, con la hora y el teléfono en que se midió. */
data class DiagnosticoCompleto(
    val cuando: Long,
    val duracionMs: Long,
    val equipo: List<Pair<String, String>>,
    val medidas: List<Medida>,
    val errores: List<String>,
    /**
     * Las pruebas que el perfil dejó fuera. Desde 1.14.0 el perfil se respeta de verdad, y eso tiene
     * un efecto que hasta 1.15.1 no se decía: una prueba desmarcada no aparece en el informe, y no
     * aparecer no es lo mismo que decir «no se ejecutó porque tú no la marcaste». Sin esta lista, un
     * informe corto se parece mucho a un informe que no midió.
     */
    val sinEjecutar: List<String> = emptyList(),
)

object Autodiagnostico {

    // ── Lo que se puede leer sin root ───────────────────────────────────────

    /**
     * Memoria libre y total. `ActivityManager.MemoryInfo` es API pública desde siempre.
     */
    fun memoria(context: Context): Medida = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        Medida(
            nombre = "Memoria disponible",
            valor = info.availMem / 1048576.0,
            unidad = "MB",
            detalle = "de ${(info.totalMem / 1048576)} MB · umbral de memoria baja: " +
                "${(info.threshold / 1048576)} MB" + if (info.lowMemory) " · el sistema dice BAJA" else "",
        )
    } catch (t: Throwable) {
        Medida("Memoria disponible", null, "MB", "", "el sistema no dio el dato: ${t.javaClass.simpleName}")
    }

    /** Almacenamiento interno libre. */
    fun almacenamiento(): Medida = try {
        val sf = StatFs(android.os.Environment.getDataDirectory().path)
        val libre = sf.availableBlocksLong * sf.blockSizeLong
        val total = sf.blockCountLong * sf.blockSizeLong
        Medida(
            nombre = "Almacenamiento libre",
            valor = libre / 1048576.0,
            unidad = "MB",
            detalle = "de ${total / 1048576} MB · ${(100.0 * libre / total).toInt()} % libre",
        )
    } catch (t: Throwable) {
        Medida("Almacenamiento libre", null, "MB", "", "StatFs falló: ${t.javaClass.simpleName}")
    }

    /** Batería: nivel, temperatura y estado. Se lee del sticky broadcast, sin permisos. */
    fun bateria(context: Context): List<Medida> {
        val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return listOf(Medida("Batería", null, "%", "", "el sistema no publicó el estado de la batería"))
        val nivel = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val escala = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val decimas = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val voltios = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val cargando = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1) in setOf(
            BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_STATUS_FULL,
        )
        val salud = when (i.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "buena"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "se está calentando"
            BatteryManager.BATTERY_HEALTH_DEAD -> "agotada"
            BatteryManager.BATTERY_HEALTH_COLD -> "fría"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "tensión alta"
            else -> "sin clasificar por el sistema"
        }
        return listOf(
            Medida(
                "Batería", if (nivel >= 0 && escala > 0) 100.0 * nivel / escala else null, "%",
                "cargando: ${if (cargando) "sí" else "no"} · salud: $salud" +
                    if (voltios > 0) " · ${voltios} mV" else "",
                if (nivel < 0) "el sistema no dio el nivel" else "",
            ),
            Medida(
                "Temperatura de batería",
                if (decimas == Int.MIN_VALUE) null else decimas / 10.0, "°C",
                "sensor de batería (no es la temperatura de la CPU)",
                // Esto es importante y se ha mentido mucho en apps de este tipo: no hay API
                // pública para la temperatura de la CPU en Android moderno. Se dice, no se inventa.
                if (decimas == Int.MIN_VALUE) "el sistema no publicó la temperatura" else "",
            ),
        )
    }

    /**
     * Temperatura de la CPU: NO hay API pública. Se intenta por el sensor de batería nada más;
     * si algún teléfono publica `TYPE_AMBIENT_TEMPERATURE` se lee, y si no, se dice que no.
     */
    fun temperaturaCpu(context: Context): Medida {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager
            ?: return Medida("Temperatura de CPU", null, "°C", "", "sin servicio de sensores")
        val s = sm.getDefaultSensor(android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE)
            ?: return Medida(
                "Temperatura de CPU", null, "°C",
                "este teléfono no publica un sensor de temperatura legible por apps sin root",
                "no hay sensor de ambiente ni API pública de temperatura de CPU",
            )
        // Leer un sensor una vez es asíncrono; aquí se intenta y si no llega, no se inventa.
        return Medida(
            "Temperatura de CPU", null, "°C",
            "hay sensor de ambiente (${s.name}), pero leerlo requiere escuchar sus eventos: " +
                "se declara no medido en vez de suponerlo",
            "la lectura de sensores es asíncrona y esta prueba es síncrona",
        )
    }

    /**
     * Trabajo real de punto flotante (una suma que el compilador no puede optimizar) y cuántas
     * operaciones por segundo salen. Relativo a este teléfono, no a AnTuTu.
     *
     * Desde la 1.14.0 el nombre y el texto los pone `ReglasDiagnostico`, que es pura y se puede
     * probar: antes se llamaba «Carga de CPU» y no medía carga ninguna.
     */
    fun cargaCpu(ms: Long = 400): Medida {
        var x = 1.000001
        var vueltas = 0L
        val t0 = SystemClock.elapsedRealtime()
        while (SystemClock.elapsedRealtime() - t0 < ms) {
            for (i in 0 until 20_000) x = x * 1.0000001 + 0.0000001
            vueltas += 20_000
        }
        val transcurrido = (SystemClock.elapsedRealtime() - t0).coerceAtLeast(1)
        val hechas = if (x == 0.0) 0L else vueltas
        return ReglasDiagnostico.cpu(
            hechas, transcurrido, ms, Runtime.getRuntime().availableProcessors(),
        )
    }

    /**
     * Lo único que Android publica sobre calor sin root: el estado térmico (API 29) y el margen
     * (API 30). NO son grados. La temperatura de la batería sí son grados y va en [bateria]; la de
     * la CPU no tiene API pública y [temperaturaCpu] lo declara en vez de suponerla.
     */
    fun termico(context: Context, errores: MutableList<String> = mutableListOf()): List<Medida> {
        val api = Build.VERSION.SDK_INT
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        // Se captura la excepción por separado a propósito. Envolverlo todo en un getOrNull()
        // convertía «el fabricante lanzó un fallo» en «el sistema no devolvió nada», que es afirmar
        // lo que no se sabe y además esconde que la API falló en este aparato.
        var falloEstado = ""
        val etiqueta = if (api >= 29 && pm != null) {
            try {
                when (pm.currentThermalStatus) {
                    PowerManager.THERMAL_STATUS_NONE -> "NONE"
                    PowerManager.THERMAL_STATUS_LIGHT -> "LIGHT"
                    PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
                    PowerManager.THERMAL_STATUS_SEVERE -> "SEVERE"
                    PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL"
                    PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY"
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> "SHUTDOWN"
                    else -> "OTRO(${pm.currentThermalStatus})"
                }
            } catch (t: Throwable) {
                falloEstado = "${t.javaClass.simpleName}: ${t.message}"
                errores += "estado térmico: $falloEstado"
                null
            }
        } else null
        var falloMargen = ""
        val margen = if (api >= 30 && pm != null) {
            try {
                pm.getThermalHeadroom(10)
            } catch (t: Throwable) {
                falloMargen = "${t.javaClass.simpleName}: ${t.message}"
                errores += "margen térmico: $falloMargen"
                null
            }
        } else null
        return listOf(
            ReglasDiagnostico.termico(etiqueta, api, falloEstado),
            ReglasDiagnostico.margenTermico(margen, api, falloMargen),
        )
    }

    /**
     * Memoria RAM: velocidad de copia. Se mide con un array real y `System.arraycopy`, y se dice
     * claro que es la velocidad del bus de memoria del proceso, no un test de latencia.
     */
    fun velocidadMemoria(): Medida {
        val tamano = 4 * 1024 * 1024
        return try {
            val origen = ByteArray(tamano) { it.toByte() }
            val destino = ByteArray(tamano)
            // Calentar (JIT + caché) antes de medir: sin esto se mide el compilador.
            repeat(3) { System.arraycopy(origen, 0, destino, 0, tamano) }
            val t0 = System.nanoTime()
            val repeticiones = 40
            repeat(repeticiones) { System.arraycopy(origen, 0, destino, 0, tamano) }
            val ns = (System.nanoTime() - t0).coerceAtLeast(1)
            val bytes = tamano.toDouble() * repeticiones
            Medida(
                "Velocidad de copia en RAM",
                bytes / (ns / 1e9) / (1024 * 1024),
                "MB/s",
                "copiando bloques de 4 MB, $repeticiones veces · mide el bus de memoria del " +
                    "proceso, no la latencia de la RAM",
            )
        } catch (t: Throwable) {
            Medida("Velocidad de copia en RAM", null, "MB/s", "", "sin memoria para la prueba: ${t.javaClass.simpleName}")
        }
    }

    /** I/O de almacenamiento: escribir y leer un fichero temporal de 4 MB y borrarlo. */
    fun ioAlmacenamiento(context: Context): List<Medida> {
        val bloque = ByteArray(4 * 1024 * 1024) { (it % 251).toByte() }
        val f = File(context.cacheDir, "diag-io.bin")
        val escritura = try {
            val t0 = System.nanoTime()
            // Con fsync: sin él se mide sobre todo la caché de páginas, y el número que sale no es
            // lo que tardó el almacenamiento sino lo que tardó la RAM en fingir que escribía.
            java.io.FileOutputStream(f).use { it.write(bloque); it.fd.sync() }
            val ns = (System.nanoTime() - t0).coerceAtLeast(1)
            ReglasDiagnostico.escrituraIo(bloque.size, ns, conFsync = true)
        } catch (t: Throwable) {
            Medida(ReglasDiagnostico.NOMBRE_ESCRITURA, null, "MB/s", "", "no se pudo escribir: ${t.javaClass.simpleName}")
        }
        val lectura = try {
            val t0 = System.nanoTime()
            val leidos = f.inputStream().use { ins ->
                var total = 0
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = ins.read(buf)
                    if (n <= 0) break
                    total += n
                }
                total
            }
            val ns = (System.nanoTime() - t0).coerceAtLeast(1)
            Medida("Lectura de almacenamiento", leidos / (ns / 1e9) / (1024 * 1024), "MB/s",
                "$leidos B leídos del mismo fichero")
        } catch (t: Throwable) {
            Medida("Lectura de almacenamiento", null, "MB/s", "", "no se pudo leer: ${t.javaClass.simpleName}")
        }
        runCatching { f.delete() }
        return listOf(escritura, lectura)
    }

    /** Red: no se hace ninguna petición. Se dice qué se puede saber y qué no, sin tocar internet. */
    fun red(context: Context): List<Medida> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            ?: return listOf(Medida("Red", null, "", "", "sin servicio de conectividad"))
        val activa = runCatching { cm.activeNetwork }.getOrNull()
        val caps = activa?.let { runCatching { cm.getNetworkCapabilities(it) }.getOrNull() }
        val bajada = caps?.linkDownstreamBandwidthKbps
        return listOf(
            Medida(
                "Enlace de red",
                bajada?.toDouble(),
                "kbps",
                "estimación que declara el sistema para el enlace activo; no se mide velocidad real " +
                    "porque esta app no hace peticiones a internet",
                if (bajada == null || bajada <= 0) "el sistema no declaró ancho de banda para el enlace" else "",
            ),
            Medida(
                "Conectividad",
                if (caps != null) 1.0 else 0.0,
                "sí/no",
                if (caps == null) "sin red activa" else
                    "red activa · " + if (caps.hasCapability(
                            android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED,
                        )
                    ) "validada por el sistema" else "no validada por el sistema",
            ),
        )
    }

    /**
     * Vulkan: se comprueba si el teléfono declara la característica y qué versión. No se
     * inicializa un contexto de render (haría falta una superficie y cerraría apps de juego).
     */
    fun vulkan(context: Context): Medida {
        val tiene = context.packageManager.hasSystemFeature("android.hardware.vulkan.version")
        val nivel = runCatching {
            context.packageManager.systemAvailableFeatures
                .firstOrNull { it.name == "android.hardware.vulkan.version" }?.version
        }.getOrNull() ?: 0
        return Medida(
            "Vulkan",
            if (tiene) nivel.toDouble() else 0.0,
            "nivel",
            if (nivel > 0) "el sistema declara Vulkan nivel $nivel (0x${Integer.toHexString(nivel)})"
            else if (tiene) "el sistema declara Vulkan pero sin número de nivel" else "no declarado",
            if (tiene) "" else "este teléfono no declara Vulkan en el manifiesto del sistema",
        )
    }

    /** Estado general del equipo, lo que va a la cabecera del informe. */
    fun equipo(context: Context): List<Pair<String, String>> {
        val rt = Runtime.getRuntime()
        return listOf(
            "Modelo declarado" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "Código de dispositivo" to Build.DEVICE,
            "Placa" to Build.BOARD,
            "Android" to "API ${Build.VERSION.SDK_INT} · ${Build.VERSION.RELEASE}",
            "Compilación" to Build.DISPLAY,
            "ABI" to Build.SUPPORTED_ABIS.joinToString(", "),
            "Núcleos" to Runtime.getRuntime().availableProcessors().toString(),
            "Memoria de la máquina virtual" to "${rt.totalMemory() / 1048576} MB reservados, " +
                "${rt.maxMemory() / 1048576} MB máximos",
            "Uptime" to "${SystemClock.elapsedRealtime() / 3_600_000} h " +
                "${(SystemClock.elapsedRealtime() / 60_000) % 60} min",
        )
    }

    /**
     * Corre todo. `profundo` añade la prueba de I/O y la de memoria; el modo rápido existe porque
     * medir cuesta batería y un teléfono con 4 GB lo nota.
     */
    /**
     * @param pruebas las claves del perfil. Hasta la 1.13.2 este parámetro no existía y el perfil
     *   prometía «las que no estén marcadas no se ejecutan» sin que fuera verdad: se ejecutaban
     *   todas. Ahora se respeta, y decide [ReglasDiagnostico.seleccion] para que el teléfono y las
     *   pruebas usen la misma regla.
     * @param msCpu lo que el usuario eligió en el perfil (150/400/1000). Antes se ignoraba y se
     *   usaba `if (profundo) 400 else 150`, así que el selector no servía de nada.
     */
    fun ejecutar(
        context: Context,
        profundo: Boolean = true,
        erroresPrevios: List<String> = emptyList(),
        pruebas: Set<String> = PerfilDiagnostico.TODAS,
        msCpu: Int = 400,
    ): DiagnosticoCompleto {
        val t0 = SystemClock.elapsedRealtime()
        val medidas = mutableListOf<Medida>()
        val errores = erroresPrevios.toMutableList()
        val activa = ReglasDiagnostico.seleccion(pruebas, profundo)

        fun intentar(nombre: String, bloque: () -> Medida) {
            try {
                medidas += bloque()
            } catch (t: Throwable) {
                errores += "$nombre: ${t.javaClass.simpleName}: ${t.message}"
                medidas += Medida(nombre, null, "", "", "la prueba lanzó ${t.javaClass.simpleName}: ${t.message}")
            }
        }

        fun elegido(clave: String) = clave in activa

        if (elegido(PerfilDiagnostico.MEMORIA)) intentar("memoria") { memoria(context) }
        if (elegido(PerfilDiagnostico.ALMACENAMIENTO)) intentar("almacenamiento") { almacenamiento() }
        if (elegido(PerfilDiagnostico.BATERIA)) bateria(context).forEach { medidas += it }
        if (elegido(PerfilDiagnostico.CPU)) intentar("cpu") { cargaCpu(ReglasDiagnostico.msCpu(msCpu)) }
        if (elegido(PerfilDiagnostico.TERMICO)) {
            // La temperatura de la CPU no se puede medir y se declara; el estado térmico sí.
            intentar("cpu-temperatura") { temperaturaCpu(context) }
            try {
                termico(context, errores).forEach { medidas += it }
            } catch (t: Throwable) {
                errores += "térmico: ${t.javaClass.simpleName}: ${t.message}"
                medidas += Medida(ReglasDiagnostico.NOMBRE_TERMICO, null, "", "",
                    "no se pudo leer el estado térmico: ${t.javaClass.simpleName}")
            }
        }
        if (elegido(PerfilDiagnostico.VULKAN)) intentar("vulkan") { vulkan(context) }
        if (elegido(PerfilDiagnostico.RED)) red(context).forEach { medidas += it }
        if (profundo) {
            if (elegido(PerfilDiagnostico.RAM)) intentar("ram") { velocidadMemoria() }
            if (elegido(PerfilDiagnostico.IO)) ioAlmacenamiento(context).forEach { medidas += it }
        }

        return DiagnosticoCompleto(
            cuando = System.currentTimeMillis(),
            duracionMs = SystemClock.elapsedRealtime() - t0,
            equipo = equipo(context),
            medidas = medidas,
            errores = errores,
            sinEjecutar = ReglasDiagnostico.noElegidas(pruebas, profundo),
        )
    }
}

/**
 * Las reglas del informe, sin Android de por medio: así se pueden probar de verdad.
 *
 * El informe es Markdown porque se lee en cualquier sitio y se pega en un issue tal cual. Lleva
 * tablas, y cuando un dato no se midió lo dice con la palabra «no medido» y su motivo; nunca
 * pone un cero, que se confundiría con un resultado.
 */
object Informe {

    /** Barras ASCII para que un número se vea de un vistazo en un archivo de texto. */
    fun barra(parte: Double, total: Double, ancho: Int = 24): String {
        if (total <= 0 || parte <= 0) return "░".repeat(ancho)
        val llenos = ((parte / total) * ancho).toInt().coerceIn(0, ancho)
        return "█".repeat(llenos) + "░".repeat(ancho - llenos)
    }

    /** El informe en Markdown. Determinista: mismos datos, mismo texto. */
    fun markdown(d: DiagnosticoCompleto, version: String, fuenteRegistro: String): String {
        val fecha = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date(d.cuando))
        return buildString {
            appendLine("# Auto-diagnóstico · GLTools Reborn $version")
            appendLine()
            appendLine("Generado en el teléfono el **$fecha** en ${d.duracionMs} ms. ")
            appendLine("Este archivo se hace solo al cerrar la app y se queda en tu teléfono: no se sube a ningún sitio.")
            appendLine()
            appendLine("## El equipo")
            appendLine()
            appendLine("| Dato | Valor |")
            appendLine("|---|---|")
            d.equipo.forEach { (k, v) -> appendLine("| $k | ${escapar(v)} |") }
            appendLine()
            appendLine("## Lo medido")
            appendLine()
            appendLine("| Prueba | Valor | Unidad | Cómo se obtuvo |")
            appendLine("|---|---:|---|---|")
            d.medidas.forEach { m ->
                // Una categoría va con palabras: en 1.14.0 el estado térmico salía aquí como «0.00»
                // con unidad «nivel 0-6», que se lee como si no hubiera dato.
                val valor = when {
                    m.esCategoria -> escapar(m.texto)
                    m.valor != null -> formato(m.valor!!)
                    else -> "no medido"
                }
                appendLine(
                    "| ${escapar(m.nombre)} | $valor | ${m.unidad} | " +
                        "${escapar(m.detalle.ifBlank { m.motivoSinDato })} |",
                )
            }
            appendLine()
            appendLine("### De un vistazo")
            appendLine()
            appendLine("```text")
            d.medidas.filter { it.medida }.forEach { m ->
                if (m.esCategoria) {
                    // Sin barra: un estado no tiene proporción, y una barra vacía junto a un «0.00»
                    // dice «aquí no hay nada» cuando el sistema acaba de declarar algo.
                    appendLine("${m.nombre.take(26).padEnd(26)} ${m.texto}")
                } else {
                    appendLine("${m.nombre.take(26).padEnd(26)} ${barra(m.valor ?: 0.0, topeVisible(m), 20)} " +
                        "${formato(m.valor ?: 0.0)} ${m.unidad}")
                }
            }
            val sinMedir = d.medidas.filterNot { it.medida }
            if (sinMedir.isNotEmpty()) {
                appendLine()
                appendLine("no medido en este teléfono:")
                sinMedir.forEach { appendLine("  · ${it.nombre} — ${it.motivoSinDato}") }
            }
            if (d.sinEjecutar.isNotEmpty()) {
                appendLine()
                appendLine("no ejecutado porque el perfil no lo marca:")
                d.sinEjecutar.forEach { appendLine("  · $it") }
            }
            appendLine("```")
            appendLine()
            appendLine("## Qué NO mide este informe")
            appendLine()
            appendLine("- **Nada de rendimiento comparable**: los números son de este aparato y de este")
            appendLine("  momento. No son puntos de AnTuTu ni se pueden comparar con los de otro teléfono.")
            appendLine("- **Temperatura de la CPU**: Android no da una API pública. Sólo se informa el")
            appendLine("  sensor de batería, que no es lo mismo.")
            appendLine("- **Velocidad real de internet**: la app no hace peticiones, así que sólo se")
            appendLine("  informa lo que el sistema declara del enlace.")
            appendLine("- **Procesos, GPU y temperatura por root**: requieren `su`, y aquí no se usa.")
            appendLine()
            if (d.errores.isNotEmpty()) {
                appendLine("## Errores durante el diagnóstico")
                appendLine()
                d.errores.forEach { appendLine("- $it") }
                appendLine()
            }
            appendLine("## Registro de fallos")
            appendLine()
            appendLine("Fuente: `$fuenteRegistro`. El registro completo va en el archivo `.txt` que se")
            appendLine("genera junto a este, con la hora exacta de cada línea.")
            appendLine()
            appendLine("---")
            appendLine("Hecho por la propia app en el teléfono. Sin servidor, sin cuenta y sin subir nada.")
        }
    }

    /** El tope de la barra: se escala contra el valor más alto de su misma unidad. */
    private fun topeVisible(m: Medida): Double = when (m.unidad) {
        "%" -> 100.0
        else -> maxOf(m.valor ?: 1.0, 1.0)
    }

    private fun formato(v: Double): String =
        if (v >= 1000) "%,.0f".format(v) else if (v >= 10) "%.1f".format(v) else "%.2f".format(v)

    /** En una tabla de Markdown una barra vertical parte la fila: hay que escaparla. */
    private fun escapar(s: String): String = s.replace("|", "\\|").replace("\n", " ")
}

/**
 * El registro de errores en texto plano: una línea por suceso, con hora hasta el milisegundo.
 *
 * Se escribe con `appendText`, que abre y cierra el fichero cada vez: si la app muere a mitad, lo
 * escrito antes sigue ahí. Un registro en un `StringBuilder` que se guarda al final es un registro
 * que se pierde justo cuando hace falta.
 */
object RegistroErrores {

    const val FICHERO = "errores-reborn.txt"

    /** Tope del archivo. Un registro que crece sin fin acaba llenando el teléfono. */
    const val MAX_BYTES = 512 * 1024

    private val formato = object : ThreadLocal<java.text.SimpleDateFormat>() {
        override fun initialValue() =
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US)
    }

    fun archivo(c: Context): File = File(c.filesDir, FICHERO)

    /** Anota una línea. No puede fallar: se llama desde sitios donde ya hubo un fallo. */
    fun anotar(c: Context, nivel: String, donde: String, mensaje: String) {
        runCatching {
            val f = archivo(c)
            if (f.exists() && f.length() > MAX_BYTES) {
                // Se queda con la mitad más reciente: tirar todo el registro es perder justo el
                // contexto del fallo que se está investigando.
                val lineas = f.readLines()
                f.writeText(lineas.drop(lineas.size / 2).joinToString("\n") + "\n")
            }
            f.appendText(
                "${formato.get()!!.format(java.util.Date())}\t$nivel\t$donde\t" +
                    "${mensaje.replace("\n", " ⏎ ")}\n",
            )
        }
    }

    fun leer(c: Context): String = runCatching {
        archivo(c).takeIf { it.exists() }?.readText() ?: ""
    }.getOrDefault("")

    fun borrar(c: Context) { runCatching { archivo(c).delete() } }

    /** Recuento por nivel, para el resumen del informe sin tener que abrir el archivo. */
    fun recuento(c: Context): Map<String, Int> =
        leer(c).lines().filter { it.isNotBlank() }.groupingBy { linea ->
            linea.split("\t").getOrNull(1) ?: "?"
        }.eachCount()
}
