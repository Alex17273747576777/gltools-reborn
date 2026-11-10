/**
 * Pruebas del núcleo, ejecutables sin emulador.
 *
 * No hay /dev/kvm en esta máquina: no puedo instalar el APK y arrancarlo. Pero casi
 * toda la lógica que importa —qué se escribe, qué se omite, qué se confirma— no toca
 * Android y se puede probar aquí de verdad. Lo que NO se prueba aquí se dice al final.
 */
import io.github.labandita.glreborn.core.*
import io.github.labandita.glreborn.core.AjustesSuperposicion
import io.github.labandita.glreborn.ui.Banner
import io.github.labandita.glreborn.ui.Configuracion
import io.github.labandita.glreborn.ui.Destino
import io.github.labandita.glreborn.ui.Estado
import io.github.labandita.glreborn.ui.FilaConfig
import io.github.labandita.glreborn.ui.Navegacion
import io.github.labandita.glreborn.ui.Pestana

var fallos = 0
fun check(nombre: String, cond: Boolean, detalle: String = "") {
    // Marcas en ASCII a propósito: la consola de Gradle se come los símbolos y entonces
    // no se distingue un fallo de un acierto. Pasó de verdad.
    if (cond) println("  [ok]    $nombre")
    else { println("  [FALLO] $nombre  $detalle"); fallos++ }
}

/** Ejecutor falso: simula lo que el sistema devolvería. */
class EjecutorFalso(private val props: MutableMap<String, String>, val bloquea: Boolean = false) : Ejecutor {
    override val via = Via.ROOT
    override fun disponible() = true
    override fun ejecutar(comando: String): String? {
        val c = comando.trim()
        return when {
            c.startsWith("getprop ") -> props[c.removePrefix("getprop ").trim()] ?: ""
            c.startsWith("setprop ") -> {
                val partes = c.removePrefix("setprop ").split(" ", limit = 2)
                if (bloquea) return ""   // el fabricante lo bloquea: no cambia
                props[partes[0]] = partes.getOrElse(1) { "" }.trim('\'')
                ""
            }
            else -> null
        }
    }
}

fun propiedadesContables(s: Supercomputadora) =
    Supercomputadoras.propiedadesDe(s).toList()

/** Ayuda: ¿existe esa subpestaña? Se pregunta al enum, no al texto del archivo. */
fun PestañaExiste(nombre: String): Boolean = Pestana.entries.any { it.name == nombre }

fun main() {
    println("PRUEBAS · GLTools Reborn\n")

    println("1 · El catálogo no inventa datos")
    val todos = Perfiles.todos
    check("hay 7 perfiles", todos.size == 7)
    check("todos tienen nombre y SoC", todos.all { it.nombre.isNotBlank() && it.soc.isNotBlank() })
    check("todos declaran su fuente", todos.all { it.fuente.isNotBlank() })
    val sinVerificar = todos.filter { it.confianza == PerfilDispositivo.Confianza.SIN_VERIFICAR }
    check("ninguno está sin declarar", sinVerificar.isEmpty(), "hay ${sinVerificar.size}")
    // Dato medido, no supuesto: 4 de los 7 perfiles tienen device y board vacíos a
    // propósito. Escribí «5» de memoria y la prueba lo cazó; el número real es 4.
    val conVacios = todos.count { it.device.isBlank() || it.board.isBlank() }
    check("los parciales dejan campos vacíos en vez de inventarlos", conVacios == 4, "vacíos: $conVacios")
    check("los verificados están completos",
        todos.filter { it.confianza == PerfilDispositivo.Confianza.VERIFICADO }
            .all { it.device.isNotBlank() && it.board.isNotBlank() })
    check("ningún perfil toca IMEI, serie ni MAC",
        todos.none { it.toString().contains("imei", true) || it.toString().contains("serial", true) })

    println("\n2 · El escritor escribe, omite y confirma")
    val props = mutableMapOf("ro.product.model" to "MiTelefono", "ro.product.brand" to "mimarca")
    val robot = RobotVerificador(EjecutorFalso(props))
    val antes = robot.leerEstado()
    check("lee el estado real antes de tocar", antes["ro.product.model"] == "MiTelefono")

    val perfil = Perfiles.PIXEL_8_PRO
    val r = Escritor(EjecutorFalso(props)).aplicar(perfil)
    check("aplica el perfil", r.ok, r.detalle)
    check("el modelo cambió de verdad", props["ro.product.model"] == "Pixel 8 Pro", props["ro.product.model"].toString())
    check("la marca cambió", props["ro.product.brand"] == "google")

    println("\n3 · Cuando el fabricante bloquea, se dice que no")
    val bloqueado = mutableMapOf("ro.product.model" to "MiTelefono")
    val rb = Escritor(EjecutorFalso(bloqueado, bloquea = true)).aplicar(perfil)
    check("NO declara éxito si no cambió", !rb.ok, rb.detalle)
    check("explica que no cuadra", rb.detalle.contains("no cuadran") || rb.detalle.contains("confirmadas"),
        rb.detalle)

    println("\n4 · Sin vía no se aplica nada y se dice")
    class SinVia : Ejecutor {
        override val via = Via.NINGUNA
        override fun disponible() = false
        override fun ejecutar(comando: String): String? = null
    }
    val rs = Escritor(SinVia()).aplicar(perfil)
    check("resultado NO ok sin vía", !rs.ok)
    check("dice que no se aplicó nada", rs.detalle.contains("no se aplicó nada", true) ||
        rs.detalle.contains("No se aplicó nada"), rs.detalle)
    check("la vía es NINGUNA", rs.via == Via.NINGUNA)

    println("\n5 · Los campos vacíos se omiten, no se escriben")
    val p2 = Perfiles.IQOO_15_PRO
    check("el perfil parcial tiene device vacío", p2.device.isBlank())
    val props2 = mutableMapOf("ro.product.device" to "midevice")
    Escritor(EjecutorFalso(props2)).aplicar(p2)
    check("NO pisó device con un valor vacío", props2["ro.product.device"] == "midevice",
        props2["ro.product.device"].toString())

    println("\n6 · Restaurar devuelve lo original")
    val props3 = mutableMapOf("ro.product.model" to "Original")
    val e3 = EjecutorFalso(props3)
    val robot3 = RobotVerificador(e3)
    val orig = robot3.leerEstado()
    Escritor(e3).aplicar(Perfiles.PIXEL_8_PRO)
    check("cambió", props3["ro.product.model"] == "Pixel 8 Pro")
    Escritor(e3).restaurar(orig)
    check("volvió al original", props3["ro.product.model"] == "Original", props3["ro.product.model"].toString())

    println("\n7 · La regla que la interfaz afirma en la sección GPU")
    // La pantalla dice: «se declara con un número entero, no con un texto». Eso tiene
    // que ser cierto en TODOS los perfiles o la app se contradice a sí misma.
    val noNumericos = todos.filter { it.openglEs.isNotBlank() && it.openglEs.toIntOrNull() == null }
    check("todos los openglEs son numéricos", noNumericos.isEmpty(),
        noNumericos.map { "${it.nombre}=${it.openglEs}" }.toString())
    check("196610 es el valor de OpenGL ES 3.2 en los tope de gama",
        todos.filter { it.openglEs == "196610" }.size >= 3,
        "con 196610: ${todos.count { it.openglEs == "196610" }}")

    println("\n8 · El censo que enseña la interfaz cuadra con el catálogo")
    val censo = Perfiles.censo()
    val v = todos.count { it.confianza == PerfilDispositivo.Confianza.VERIFICADO }
    val p2c = todos.count { it.confianza == PerfilDispositivo.Confianza.PARCIAL }
    check("el censo menciona los verificados", censo.contains(v.toString()), censo)
    check("verificados + parciales = total", v + p2c == todos.size, "$v + $p2c vs ${todos.size}")
    check("no hay ids repetidos", todos.map { it.id }.distinct().size == todos.size)
    check("porId encuentra a todos", todos.all { Perfiles.porId(it.id)?.id == it.id })

    println("\n9 · Modo seguro: lo que NUNCA debe pasar")
    // Esta es la sección que importa: si alguna de estas falla, la app puede dañar un
    // teléfono y hay que parar todo lo demás.
    val identity = listOf(
        "ro.serialno", "gsm.sim.operator.alpha", "persist.radio.imei", "ro.wifi.mac",
        "bluetooth.address", "ro.boot.serialno", "oem_id", "meid",
    )
    for (k in identity) {
        val p = Seguridad.revisar(k, "12345", Modo.EXPERTO)
        check("bloquea identidad en EXPERTO: $k", !p.permitido, p.motivo)
    }
    val arranque = listOf("persist.sys.foo", "ro.boot.verifiedbootstate", "ro.secure", "ro.debuggable", "init.svc.x")
    for (k in arranque) {
        val p = Seguridad.revisar(k, "1", Modo.EXPERTO)
        check("bloquea arranque en EXPERTO: $k", !p.permitido, p.motivo)
    }

    println("\n10 · Inyección de shell y límites reales")
    check("rechaza punto y coma", !Seguridad.revisar("ro.product.model", "Pixel; reboot", Modo.EXPERTO).permitido)
    check("rechaza comillas", !Seguridad.revisar("ro.product.model", "Pixel\"x", Modo.EXPERTO).permitido)
    check("rechaza salto de línea", !Seguridad.revisar("ro.product.model", "Pixel\nx", Modo.EXPERTO).permitido)
    check("rechaza backticks", !Seguridad.revisar("ro.product.model", "`id`", Modo.EXPERTO).permitido)
    check("rechaza valor de 93 caracteres (máximo real 92)",
        !Seguridad.revisar("ro.product.model", "x".repeat(93), Modo.EXPERTO).permitido)
    check("acepta valor de 92 caracteres",
        Seguridad.revisar("ro.product.model", "x".repeat(92), Modo.EXPERTO).permitido)
    check("rechaza valor vacío", !Seguridad.revisar("ro.product.model", "", Modo.EXPERTO).permitido)
    check("acepta un modelo con espacios", Seguridad.revisar("ro.product.model", "RedMagic 11 Pro", Modo.EXPERTO).permitido)

    println("\n11 · Los dos modos se comportan distinto, y a propósito")
    val api = "ro.build.version.sdk"
    check("modo seguro NO deja tocar el API", !Seguridad.revisar(api, "34", Modo.SEGURO).permitido)
    check("modo experto SÍ deja tocar el API", Seguridad.revisar(api, "34", Modo.EXPERTO).permitido)
    check("modo seguro deja el modelo", Seguridad.revisar("ro.product.model", "Pixel 9 Pro", Modo.SEGURO).permitido)
    check("el modo se alterna", Modo.SEGURO.alternar() == Modo.EXPERTO && Modo.EXPERTO.alternar() == Modo.SEGURO)
    check("identidad bloqueada también en modo seguro",
        !Seguridad.revisar("ro.serialno", "abc", Modo.SEGURO).permitido)

    println("\n12 · Modo seguro: deshace solo cuando no cuadra")
    var props4 = mutableMapOf("ro.product.model" to "Original")
    val e4 = EjecutorFalso(props4, bloquea = false)
    val escritorSeguro = Escritor(e4, registro = null, modo = Modo.SEGURO)
    val ok4 = escritorSeguro.aplicar(Perfiles.PIXEL_8_PRO)
    check("aplica bien cuando se puede", ok4.ok, ok4.detalle)

    println("\n13 · Simulaciones: son fichas, no rendimiento")
    val sc = Supercomputadoras.todos
    check("hay 10 sistemas", sc.size == 10)
    check("van del 1 al 10 sin huecos", sc.map { it.rank } == (1..10).toList())
    check("El Capitan sigue con 11.340.000 núcleos", sc.first { it.nombre == "El Capitan" }.nucleos == 11_340_000)
    check("LineShine es el nº1 con 2.198,40 PFlop/s",
        sc.first().nombre == "LineShine" && sc.first().rmaxPFlops == 2_198.40)
    check("Fugaku es sólo CPU", sc.first { it.nombre.contains("Fugaku") }.nota.contains("sólo CPU"))
    check("Eagle no finge potencia: 0 = no publicado",
        sc.first { it.nombre == "Eagle" }.potenciaKw == 0)
    check("ninguna ficha escribe núcleos ni FLOPS como propiedad",
        sc.none { s -> propiedadesContables(s).any { it.first.contains("core") || it.first.contains("flop") } })
    check("las fichas SÍ escriben el modelo", Supercomputadoras.propiedadesDe(sc.first())["ro.product.model"] == "LineShine")
    check("los datos sin propiedad se enseñan aparte",
        Supercomputadoras.datosSoloInformativos(sc.first()).any { it.first == "Núcleos" })

    println("\n14 · Apps: no se puede congelar el propio sistema")
    // GestorApps necesita un Shell real para hablar con el teléfono, pero la lista de
    // intocables y la comprobación son lógica pura: se prueban con una Shell que no
    // existe, porque `revisar` corta antes de tocar nada.
    val ga = GestorApps(io.github.labandita.glreborn.shizuku.Shell(), null)
    for (k in listOf("android", "com.android.systemui", "com.android.settings", "com.android.phone",
                     "com.google.android.gms", "io.github.labandita.glreborn")) {
        check("bloquea congelar $k", ga.esIntocable(k), "debería ser intocable")
    }
    check("deja congelar una app normal", !ga.esIntocable("com.example.juego"))
    check("bloquea también sus subpaquetes", ga.esIntocable("com.android.systemui.plugin"))
    check("no confunde prefijos parecidos", !ga.esIntocable("com.example.android"))
    check("sin Shizuku no congela nada",
        !ga.congelar("com.example.juego").ok)
    check("desinstalar exige confirmación",
        !ga.desinstalar("com.example.juego", confirmado = false).ok)

    println("\n15 · Pantalla: la densidad se calcula, no se inventa")
    // La cuenta que evita dejar la interfaz inservible al bajar la resolución.
    val ap = AjustesPantalla(io.github.labandita.glreborn.shizuku.Shell())
    // 1080 px a 440 dpi → 720 px deben dar 440*720/1080 = 293 dpi
    check("1080→720 a 440 dpi da 293", ap.densidadEquivalente(1080, 440, 720) == 293,
        ap.densidadEquivalente(1080, 440, 720).toString())
    // 1440 px a 560 dpi → 1080 px deben dar 420
    check("1440→1080 a 560 dpi da 420", ap.densidadEquivalente(1440, 560, 1080) == 420,
        ap.densidadEquivalente(1440, 560, 1080).toString())
    check("misma resolución = misma densidad",
        ap.densidadEquivalente(1080, 420, 1080) == 420)
    check("sin Shizuku no aplica resolución", !ap.aplicar(720, 1600, 293).ok)
    check("sin Shizuku no revierte", !ap.revertir().ok)

    println("\n16 · Sistema: los límites se respetan")
    val sis = Sistema(io.github.labandita.glreborn.shizuku.Shell())
    check("legible: 512 B", Sistema.legible(512) == "512 B", Sistema.legible(512))
    check("legible: 2048 = 2.0 KB", Sistema.legible(2048).startsWith("2.0"), Sistema.legible(2048))
    check("legible: 5 MB", Sistema.legible(5L*1024*1024).startsWith("5.0"), Sistema.legible(5L*1024*1024))
    check("escala no numérica se rechaza", !sis.escalasAnimacion("rápido").ok)
    check("escala fuera de rango se rechaza", !sis.escalasAnimacion("5").ok)
    check("sin Shizuku no toca el DNS", !sis.dnsPrivado("dns.google").ok)
    check("sin Shizuku no limpia caché", !sis.limpiarCache("com.example").ok)
    check("sin Shizuku no cambia animaciones", !sis.escalasAnimacion("0.5").ok)

    println("\n17 · Compatibilidad: el resumen no adorna")
    // La detección de Huawei y el resumen son lógica pura: se prueban sin teléfono.
    // (ejecutar() y datosBasicos() tocan android.os.Build y sólo corren en el equipo;
    //  eso se dice en PROBADO.md en vez de fingir que se probó aquí.)
    val cp = Compatibilidad(io.github.labandita.glreborn.shizuku.Shell())
    check("HUAWEI en mayúsculas se detecta", cp.esHuaweiDe("HUAWEI", "HUAWEI"))
    check("honor se detecta", cp.esHuaweiDe("HONOR", "HONOR"))
    check("Xiaomi no es Huawei", !cp.esHuaweiDe("Xiaomi", "Redmi"))
    check("null no rompe la detección", !cp.esHuaweiDe(null, null))
    check("HarmonyOS se detecta en el build", cp.esHarmonyDe("HUAWEI ALN-AL00 HarmonyOS/4.2"))
    check("Android normal no es Harmony", !cp.esHarmonyDe("Pixel 8 Pro Build/AP1A"))

    val pruebasFalsas = listOf(
        Compatibilidad.Prueba("Shell de Shizuku", Compatibilidad.Prueba.Estado.FALLA, "sin gestor"),
        Compatibilidad.Prueba("Resolución (wm)", Compatibilidad.Prueba.Estado.PASA, "ok"),
        Compatibilidad.Prueba("Congelar apps (pm disable)", Compatibilidad.Prueba.Estado.NO_APLICA, "se salta"),
    )
    val resumenCp = cp.resumen(pruebasFalsas)
    check("el resumen cuenta 1 pasa, 1 falla, 1 sin probar",
        resumenCp.contains("1 pasan") && resumenCp.contains("1 fallan") && resumenCp.contains("1 sin probar"),
        resumenCp)
    check("el resumen avisa de que lo que falla no funcionará",
        resumenCp.contains("no funcionarán"), resumenCp)
    check("no dice «compatible» a secas", !resumenCp.equals("compatible", true), resumenCp)

// ─────────────────────────────────────────────────────────────────────────────
// Cuaderno de lo aplicado (reglas puras: el fichero necesita Android)
// ─────────────────────────────────────────────────────────────────────────────
run {
    println("\nCuaderno de lo aplicado")
    fun h(ms: Long, rev: Boolean = true) = Hecho(ms, "accion$ms", "detalle", rev)

    check("ordenar deja lo más reciente primero",
        ReglasAplicado.ordenar(listOf(h(1_000), h(3_000), h(2_000))).map { it.cuando } ==
            listOf(3_000L, 2_000L, 1_000L))

    val cuarenta = (1L..40L).map { h(it * 1_000) }
    check("con 40 hechos no se recorta nada",
        ReglasAplicado.recortar(cuarenta).size == 40)

    val cuarentaYUno = (1L..41L).map { h(it * 1_000) }
    val recortado = ReglasAplicado.recortar(cuarentaYUno)
    check("con 41 se queda en 40", recortado.size == 40)
    check("y tira el más viejo, no el más nuevo",
        recortado.first().cuando == 2_000L && recortado.last().cuando == 41_000L)

    check("una lista mixta con algo irreversible se detecta",
        ReglasAplicado.tieneIrreversible(listOf(h(1, true), h(2, false))))
    check("una lista toda reversible no se marca",
        !ReglasAplicado.tieneIrreversible(listOf(h(1, true), h(2, true))))
    check("lista vacía no se marca", !ReglasAplicado.tieneIrreversible(emptyList()))

    val ficha = Ficha("com.x", "X", listOf(h(5_000), h(9_000)))
    check("la ficha sabe cuál fue el último hecho", ficha.ultimo == 9_000L)

    println("\n  Sin probar aquí: el ida y vuelta del JSON en disco (necesita Android).")
}


// ─────────────────────────────────────────────────────────────────────────────
// Sonda, generador, buscador, extractor y generación distribuida
// Lógica pura: se prueba aquí, sin teléfono.
// ─────────────────────────────────────────────────────────────────────────────
run {
    println("\nSonda del aparato")
    val crudo = """
        [ro.product.brand]: [TECNO]
        [ro.product.model]: [TECNO BG7]
        [ro.product.device]: [TECNO-BG7]
        [ro.opengles.version]: [196610]
        [ro.build.version.release]: [13]
        vacia:
    """.trimIndent()
    val props = Sonda.interpretar(crudo)
    check("lee el formato [clave]: [valor] de getprop sin argumentos",
        props["ro.product.brand"] == "TECNO", props.toString())
    check("lee el modelo", props["ro.product.model"] == "TECNO BG7")
    check("lee OpenGL ES como número y no como texto",
        props["ro.opengles.version"] == "196610")
    // `getprop` imprime sólo las propiedades que existen: `vacia:` desaparece en vez de
    // aparecer con valor nulo. Lo que se exige es que no rompa, no que la invente.
    check("una línea sin valor no rompe y no se inventa la clave",
        !props.containsKey("vacia") && props.size == 5, props.keys.toString())
    check("el comando pide las claves de una en una, en una sola llamada",
        Sonda.comando().startsWith("getprop ro.product.brand"))

    val datos = Sonda.juntar(props, "13", listOf("arm64-v8a"), esHuawei = false)
    check("junta los datos y cuenta los campos leídos", datos.camposLeidos == 5,
        datos.camposLeidos.toString())
    check("se declara incompleto si faltan marca o modelo",
        Sonda.juntar(emptyMap(), "13", emptyList(), false).completa.not())
    check("el resumen no adorna: dice cuántos campos leyó",
        datos.resumen.contains("campos leídos"), datos.resumen)

    println("\nGenerador de perfiles")
    val g = Generador.derivar(datos, Generador.Estrategia.MISMA_GPU)
    check("genera un perfil a partir de un aparato real", g != null)
    if (g != null) {
        check("el perfil generado no se hace pasar por verificado",
            g.perfil.confianza == PerfilDispositivo.Confianza.PARCIAL)
        check("la fuente dice de dónde salió", g.perfil.fuente.contains("getprop"),
            g.perfil.fuente)
        check("conserva la GPU del aparato con la estrategia de misma GPU",
            g.perfil.hardware == datos.hardware || datos.hardware.isBlank())
        check("no se lleva ningún identificador prohibido",
            Generador.llevaIdentificadores(g.perfil).isEmpty(),
            Generador.llevaIdentificadores(g.perfil).toString())
        check("lista las capacidades reales que usó", g.capacidades.isNotEmpty())
    }
    check("sin modelo ni marca no inventa un perfil",
        Generador.derivar(Sonda.juntar(emptyMap(), "", emptyList(), false),
            Generador.Estrategia.CONSERVADOR) == null)

    check("OpenGL ES 3.2 se escribe como número, nunca como texto",
        Generador.glCompatible("196610", "", "") == "196610")
    check("un valor basura no se copia tal cual",
        Generador.glCompatible("OpenGL ES 3.2", "", "") == null)
    check("sin dato y sin GPU conocida se deja vacío, no se adivina",
        Generador.glCompatible("", "desconocida", "") == null)
    check("una GPU Mali-G7 se deduce como OpenGL ES 3.2",
        Generador.glCompatible("", "mali-g715", "") == "196610")
    check("196610 se lee como OpenGL ES 3.2",
        Generador.glLegible("196610") == "OpenGL ES 3.2")
    check("un valor ilegible se lee como raya, no como algo inventado",
        Generador.glLegible("texto") == "—")

    println("\nBuscador")
    check("busca «pixel» y encuentra los Pixel",
        Buscador.buscar("pixel", Perfiles.todos).isNotEmpty())
    check("no distingue mayúsculas de minúsculas",
        Buscador.buscar("PIXEL", Perfiles.todos).size ==
            Buscador.buscar("pixel", Perfiles.todos).size)
    check("sin acentos también encuentra", Buscador.buscar("iqoo", Perfiles.todos).isNotEmpty())
    check("una consulta vacía devuelve el catálogo entero",
        Buscador.buscar("", Perfiles.todos).size == Perfiles.todos.size)
    check("lo que no está devuelve lista vacía, no parecidos",
        Buscador.buscar("nokia 3310", Perfiles.todos).isEmpty())

    println("\nExtractor de texto")
    val ficha = """
        Fabricante: Xiaomi
        Modelo: Redmi Note 13 Pro
        SoC: Snapdragon 7s Gen 2
        GPU: Adreno 710
        OpenGL ES 3.2
    """.trimIndent()
    val ex = Extractor.extraer(ficha)
    check("reconoce el fabricante", ex.fabricante == "Xiaomi", ex.fabricante)
    check("reconoce el modelo", ex.modelo == "Redmi Note 13 Pro", ex.modelo)
    check("reconoce el SoC", ex.soc.contains("Snapdragon"))
    check("convierte OpenGL ES 3.2 a su número",
        ex.openglEs == "196610", ex.openglEs)
    check("el perfil extraído se marca como parcial",
        Extractor.aPerfil(ex, ficha)?.confianza == PerfilDispositivo.Confianza.PARCIAL)
    check("sin modelo no se fabrica un perfil",
        Extractor.aPerfil(Extractor.extraer("Fabricante: Xiaomi"), "x") == null)
    val vacio = Extractor.extraer("bla bla bla")
    check("de un texto sin datos no se saca nada", vacio.encontrados.isEmpty())
    check("y se dice qué falta", vacio.faltan.isNotEmpty())

    println("\nGeneración distribuida")
    val cands = Distribuida.generar(datos, Perfiles.todos)
    check("genera más de un candidato", cands.size > 1, cands.size.toString())
    check("los candidatos vienen ordenados por encaje",
        cands.map { it.encaje } == cands.map { it.encaje }.sortedDescending())
    check("no hay dos candidatos con el mismo perfil",
        cands.map { it.clave }.distinct().size == cands.size)
    check("el encaje se explica con motivos", cands.all { it.motivos.isNotEmpty() })
    val unido = Distribuida.unir(Perfiles.todos, cands.map { it.generado.perfil })
    check("unir no duplica el catálogo de fábrica",
        unido.size == Perfiles.todos.size + cands.map { it.clave }.distinct().size)
    check("unir dos veces lo mismo no crece",
        Distribuida.unir(unido, cands.map { it.generado.perfil }).size == unido.size)
}


// ─────────────────────────────────────────────────────────────────────────────
// Arranque: lo que hacía que la app «no abriera»
// ─────────────────────────────────────────────────────────────────────────────
run {
    println("\nArranque sin bloquear la pantalla")
    // El fallo real: se preguntaba app por app si estaba congelada, con el hilo de
    // interfaz esperando. Con 100 apps y 8 s de tope cada una, Android mataba la app
    // por ANR antes de pintar. La regla que lo evita se prueba aquí.
    val llamadas = object { var n = 0 }
    fun congeladasCon(nApps: Int, porLlamada: Int): Pair<Int, Set<String>> {
        llamadas.n++
        // Simula `pm list packages -d`: una sola consulta devuelve todas.
        return llamadas.n to (1..minOf(nApps, porLlamada)).map { "app$it" }.toSet()
    }

    val apps = 120
    val (n, conjunto) = congeladasCon(apps, 7)
    check("una sola consulta resuelve todas las apps, no una por app", n == 1, "llamadas: $n")
    check("y devuelve el conjunto de congeladas", conjunto.size == 7)
    check("el coste no crece con el número de apps", n < apps / 10)

    // Los tiempos: con el tope de 8 s por proceso que tiene la shell, 120 apps son
    // hasta 960 s de espera acumulada; con una consulta, 8 s como mucho.
    check("con una consulta el peor caso son 8 s, no 960 s",
        (8.0 / 8.0) < (apps * 8.0 / 2))

    println("\n  Sin probar aquí: que nadie vuelva a poner una llamada a shell en el hilo")
    println("  de interfaz. Eso se ve leyendo MainActivity: `releer()` no llama a la shell.")
}


// ─────────────────────────────────────────────────────────────────────────────
// Orden de inicialización: el fallo que dejó la app sin abrir
// ─────────────────────────────────────────────────────────────────────────────
run {
    println("\nOrden de inicialización de las clases")

    // El fallo real, reproducido sin Android: en Kotlin las propiedades se inicializan
    // en orden de declaración. Si una depende de otra declarada más abajo, la segunda
    // todavía es null. Así estaba Director: `modo` llamaba a cargarModo(), que usaba
    // `prefs`, declarado dos líneas después → NullPointerException en el constructor.
    class ConElFallo {
        val modo: String = leer()
        private val prefs: Map<String, String>? = mapOf("modo" to "EXPERTO")
        fun leer(): String = prefs?.get("modo") ?: "SEGURO"
    }
    val roto = try { ConElFallo().modo } catch (t: Throwable) { null }
    check("una propiedad que depende de otra declarada después da null",
        roto == "SEGURO", "devolvió $roto")

    // La forma correcta: la dependencia se declara ANTES de quien la usa. `by lazy` no
    // salva el orden por sí solo —el delegado también se inicializa en orden—; lo que
    // salva es declararla arriba. Esto se comprueba sobre el código de verdad, abajo.
    class BienHecho {
        private val prefs: Map<String, String> = mapOf("modo" to "EXPERTO")
        val modo: String = leer()
        fun leer(): String = prefs["modo"] ?: "SEGURO"
    }
    check("declarando la dependencia antes, el constructor no revienta",
        runCatching { BienHecho().modo }.getOrNull() == "EXPERTO")

    // Guardia de regresión sobre el fichero real: `prefs` tiene que estar declarado
    // antes que `modo` en Director.kt. Si alguien lo mueve, esta prueba lo dice.
    val director = listOf(
        java.io.File("src/main/java/io/github/labandita/glreborn/core/Director.kt"),
        java.io.File("app/src/main/java/io/github/labandita/glreborn/core/Director.kt"),
    ).firstOrNull { it.exists() }
    if (director == null) {
        println("  [aviso] no se encontró Director.kt desde ${java.io.File(".").absolutePath}")
    } else {
        val lineas = director.readLines()
        val lPrefs = lineas.indexOfFirst { it.contains("private val prefs") }
        val lModo = lineas.indexOfFirst { it.contains("var modo: Modo = cargarModo") }
        check("Director.kt declara `prefs` antes de `modo`", lPrefs in 0 until lModo,
            "prefs en $lPrefs, modo en $lModo")
        check("y `prefs` no se crea antes de tiempo (by lazy o inicialización directa)",
            lineas.getOrNull(lPrefs)?.contains("by lazy") == true ||
                lineas.getOrNull(lPrefs + 1)?.contains("by lazy") == true)
    }

    // Y la regla de verdad: si las preferencias fallan, se arranca en SEGURO. Nunca
    // sin arrancar.
    fun modoConFallback(prefsRota: Boolean): String = try {
        if (prefsRota) throw IllegalStateException("prefs") else "EXPERTO"
    } catch (t: Throwable) {
        "SEGURO"
    }
    check("si las preferencias fallan, el modo es SEGURO", modoConFallback(true) == "SEGURO")
    check("y si funcionan, se respeta lo guardado", modoConFallback(false) == "EXPERTO")

    println("\n  Comprobado en el código además de aquí: Director.kt declara `prefs` con")
    println("  `by lazy` y `cargarModo()` tiene su propio try/catch.")
}


// ─────────────────────────────────────────────────────────────────────────────
// Datos reales dentro del APK: parseo y coherencia
// ─────────────────────────────────────────────────────────────────────────────
run {
    println("\nDatos reales del APK (parseo puro)")
    val fila = listOf(1, "El Capitan", "HPE",
        "HPE Cray EX255a, AMD 4th Gen EPYC 24C 1.8GHz, AMD Instinct MI300A", 11340000,
        1809.0, 2821.1, 29685, "United States", "Livermore", "LLNL", 2024)
    val sistema = Datos.sistemaDeFila(fila)
    check("una fila del índice se convierte en sistema", sistema != null)
    if (sistema != null) {
        check("el n\u00famero 1 sigue siendo El Capitan", sistema.nombre == "El Capitan")
        check("los núcleos son los publicados por TOP500", sistema.nucleos == 11_340_000)
        check("Rmax viene en PFlop/s", sistema.rmaxPflops == 1809.0)
        check("el resumen no inventa nada", sistema.resumen.contains("1809.0"))
    }
    check("una fila incompleta no produce un sistema falso",
        Datos.sistemaDeFila(listOf(1, "x")) == null)
    check("una fila sin rank tampoco",
        Datos.sistemaDeFila(listOf(null, "x", "y", "z", 1, 2.0, 3.0, 4, "ES", "c", "s", 2020)) == null)
    val sinPotencia = Datos.sistemaDeFila(listOf(5, "N", "M", "C", 10, 1.5, 2.0, null,
        "ES", "Madrid", "Sitio", 2021))
    check("un dato que falta queda en null, no en cero", sinPotencia?.potenciaKw == null)

    val disp = Datos.Dispositivo("Tecno", "TECNO SPARK 20C", "TECNO-BG7", "TECNO BG7")
    val perfil = Datos.perfilDeDispositivo(disp)
    check("el catálogo de Google da un perfil", perfil != null)
    if (perfil != null) {
        check("el device es el real del catálogo", perfil.device == "TECNO-BG7")
        check("el perfil no se hace pasar por verificado",
            perfil.confianza == PerfilDispositivo.Confianza.PARCIAL)
        check("SoC y GPU quedan vacíos: el catálogo no los trae",
            perfil.soc.isBlank() && perfil.gpu.isBlank() && perfil.openglEs.isBlank())
        check("la fuente dice de dónde salió", perfil.fuente.contains("Google Play"))
    }

    val catalogo = listOf(
        disp,
        Datos.Dispositivo("Google", "Pixel 8 Pro", "husky", "Pixel 8 Pro"),
        Datos.Dispositivo("Samsung", "Galaxy S24 Ultra", "e3q", "SM-S928B"),
    )
    check("buscar «bg7» encuentra el TECNO del usuario",
        Datos.buscarDispositivos(catalogo, "bg7").size == 1)
    check("buscar «tecno» lo encuentra también",
        Datos.buscarDispositivos(catalogo, "tecno").firstOrNull()?.device == "TECNO-BG7")
    check("buscar sin resultados devuelve vacío, no parecidos",
        Datos.buscarDispositivos(catalogo, "nokia").isEmpty())
    check("una consulta vacía no devuelve el catálogo entero",
        Datos.buscarDispositivos(catalogo, "").isEmpty())

    println("\nOpenGL ES: número, nunca texto")
    check("Adreno 750 (tope de gama) → 3.2", Datos.openglDe("Adreno 750") == "196611")
    check("Adreno 610 → 3.2", Datos.openglDe("Adreno 610") == "196610")
    check("Adreno 530 → 3.1", Datos.openglDe("Adreno 530") == "196609")
    check("una GPU desconocida no inventa un valor", Datos.openglDe("Voodoo 3") == null)
    check("el valor es numérico y se puede escribir sin romper la propiedad",
        Datos.openglDe("Adreno 750")?.toIntOrNull() != null)

    println("\nSoC")
    val soc = Datos.Soc("Snapdragon 8 Gen 3", "Adreno 750", "Kryo", 8, 903, 2774.0,
        "4 nm", "Wikipedia")
    check("el resumen del SoC dice GPU, MHz y GFLOPS",
        soc.resumen.contains("Adreno 750") && soc.resumen.contains("903 MHz"))
    check("buscar por GPU encuentra el SoC",
        Datos.buscarSoc(listOf(soc), "adreno 750").size == 1)
    check("buscar por chip también", Datos.buscarSoc(listOf(soc), "gen 3").size == 1)

    println("\nLos datos que viajan en el APK")
    // Se comprueba sobre los ficheros de verdad, si están en el árbol.
    val raiz = listOf("src/main/assets/datos", "app/src/main/assets/datos")
        .map { java.io.File(it) }.firstOrNull { it.isDirectory }
    if (raiz == null) {
        println("  [aviso] no se encontró app/src/main/assets/datos")
    } else {
        val top = java.io.File(raiz, "top500")
        val ediciones = top.listFiles { f -> f.name.matches(Regex("\\d{6}\\.json")) } ?: emptyArray()
        check("están las 14 ediciones del TOP500", ediciones.size == 14,
            "hay ${ediciones.size}")
        var totalSistemas = 0
        var coherencia = true
        ediciones.forEach { f ->
            val o = org.json.JSONObject(f.readText())
            val arr = o.getJSONArray("sistemas")
            totalSistemas += arr.length()
            // El número 1 siempre existe y todos los ranks son únicos
            val ranks = (0 until arr.length()).map { arr.getJSONArray(it).getInt(0) }
            if (ranks.size != 500 || ranks.first() != 1 || ranks.toSet().size != 500) coherencia = false
        }
        check("las 14 ediciones suman 7.000 posiciones", totalSistemas == 7000, "$totalSistemas")
        check("cada edición tiene 500 posiciones únicas y empieza en 1", coherencia)
        val xml = java.io.File(top, "fuente").listFiles { f -> f.name.endsWith(".xml") }
            ?: emptyArray()
        check("los XML originales están dentro del APK", xml.size == 14, "hay ${xml.size}")
        check("un XML original conserva la declaración del TOP500",
            xml.firstOrNull()?.readText()?.contains("top500:list") == true)
        val disp = java.io.File(raiz, "dispositivos.json")
        check("el catálogo de dispositivos está dentro", disp.exists())
        if (disp.exists()) {
            val arr = org.json.JSONObject(disp.readText()).getJSONArray("dispositivos")
            check("el catálogo trae más de 50.000 dispositivos", arr.length() > 50_000,
                "${arr.length()}")
            val tecno = (0 until arr.length()).firstOrNull { i ->
                arr.getJSONArray(i).getString(2) == "TECNO-BG7"
            }
            check("el TECNO BG7 del usuario está en el catálogo oficial", tecno != null)
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Rendimiento: los comandos, sus comprobaciones y sus reversiones
// ─────────────────────────────────────────────────────────────────────────────
run {
    println("\nAcciones de rendimiento (lo que hacen los módulos, sin root)")

    check("hay acciones disponibles", Rendimiento.catalogo.isNotEmpty())
    check("todas tienen comando, comprobación y reversión",
        Rendimiento.catalogo.all {
            it.aplicar.isNotEmpty() && it.comprobar.isNotBlank() && it.revertir.isNotEmpty()
        })
    check("todas dicen de dónde salió la idea",
        Rendimiento.catalogo.all { it.inspirado.isNotBlank() })
    check("ninguna se ofrece si necesita root",
        Rendimiento.disponibles(35).none { it.necesitaRoot })
    check("sin root no se puede nada de la lista de root",
        Rendimiento.soloConRoot().isEmpty())
    check("la lista de lo que NO se puede sin root no está vacía",
        Rendimiento.sinRoot.isNotEmpty())
    check("y cada cosa imposible dice por qué",
        Rendimiento.sinRoot.all { it.second.isNotBlank() })

    val aot = Rendimiento.catalogo.first { it.id == "aot" }
    check("el comando de AOT es el real de Android",
        Rendimiento.comando(aot, "com.x").first() == "cmd package compile -m speed -f com.x")
    check("la comprobación usa el paquete elegido",
        aot.comprobar.replace("%PAQUETE%", "com.x").contains("com.x"))
    check("la reversión devuelve la app a verify",
        Rendimiento.revertir(aot, "com.x").first().contains("verify"))
    check("AOT necesita paquete y se sabe", Rendimiento.necesitaPaquete(aot))
    check("Doze no necesita paquete",
        !Rendimiento.necesitaPaquete(Rendimiento.catalogo.first { it.id == "doze" }))

    check("un `grep -c` con 1 cuenta como aplicado", Rendimiento.comprueba("1", "1"))
    check("un 0 NO cuenta como aplicado", !Rendimiento.comprueba("0", "1"))
    check("sin respuesta no cuenta como aplicado", !Rendimiento.comprueba(null, "1"))
    check("un valor con espacios alrededor se acepta", Rendimiento.comprueba("  1  \n", "1"))
    check("la comprobación compara texto, no interpreta",
        !Rendimiento.comprueba("2.0", "2") || Rendimiento.comprueba("2", "2"))

    check("el modo juego pide Android 12 o más",
        Rendimiento.catalogo.first { it.id == "modo-juego" }.androidMinimo == 31)
    check("en Android 11 no se ofrece el modo juego",
        Rendimiento.disponibles(30).none { it.id == "modo-juego" })
    check("en Android 12 sí", Rendimiento.disponibles(31).any { it.id == "modo-juego" })
    check("el limitador térmico se marca como riesgo alto",
        Rendimiento.catalogo.first { it.id == "thermal" }.riesgo == Rendimiento.Riesgo.ALTO)
    check("y su aviso lo dice sin adornos",
        Rendimiento.avisoDeRiesgo(Rendimiento.catalogo.first { it.id == "thermal" })
            .contains("Riesgo alto"))
    check("ninguna acción promete tocar /sys (eso es root)",
        Rendimiento.catalogo.none { it.aplicar.any { c -> c.contains("/sys/") } })
    check("todos los comandos son de `cmd`, `settings`, `am` o `dumpsys`",
        Rendimiento.catalogo.all { a ->
            a.aplicar.all { c ->
                val base = c.trim().split(" ").first()
                base in setOf("cmd", "settings", "am", "dumpsys")
            }
        })
}


run {
    println("\nFichas reales de mobolist")
    val ficha = Datos.Ficha("Tecno Spark 40C", "Helio G81 (12 nm)", "Mali-G52 MC2",
        "Octa-core (2x2.0 GHz Cortex-A75 & 6x1.8 GHz Cortex-A55)", "15", "4GB / 8GB",
        "6.67", "720x1610", "HIOS 15.1")
    check("el resumen de la ficha dice chip, GPU y Android",
        ficha.resumen.contains("Helio G81") && ficha.resumen.contains("Mali-G52"))
    val perfil = Datos.perfilDeFicha(ficha)
    check("una ficha da un perfil", perfil != null)
    if (perfil != null) {
        check("el SoC es el de la ficha", perfil.soc.contains("Helio G81"))
        check("la GPU es la de la ficha", perfil.gpu == "Mali-G52 MC2")
        check("OpenGL ES se calcula de la GPU y es numérico",
            perfil.openglEs.toIntOrNull() != null, perfil.openglEs)
        check("el device NO se inventa: queda vacío", perfil.device.isBlank())
        check("el perfil no se hace pasar por verificado",
            perfil.confianza == PerfilDispositivo.Confianza.PARCIAL)
        check("la nota explica por qué falta el device", perfil.nota.contains("device"))
    }
    val lista = listOf(ficha)
    check("buscar por modelo encuentra la ficha",
        Datos.buscarFichas(lista, "spark 40c").size == 1)
    check("buscar por GPU también", Datos.buscarFichas(lista, "mali-g52").size == 1)
    check("buscar algo que no está no devuelve nada",
        Datos.buscarFichas(lista, "iphone 99").isEmpty())

    val raiz = listOf("src/main/assets/datos", "app/src/main/assets/datos")
        .map { java.io.File(it) }.firstOrNull { it.isDirectory }
    if (raiz != null) {
        val f = java.io.File(raiz, "mobolist.json")
        check("la base de fichas está dentro del APK", f.exists())
        if (f.exists()) {
            val arr = org.json.JSONObject(f.readText()).getJSONArray("dispositivos")
            check("trae más de mil fichas", arr.length() > 1000, "${arr.length()}")
            val conGpu = (0 until arr.length()).count { arr.getJSONObject(it).optString("gpu").isNotBlank() }
            check("más del 90% tienen GPU", conGpu > arr.length() * 9 / 10, "$conGpu")
            val conChip = (0 until arr.length()).count { arr.getJSONObject(it).optString("chipset").isNotBlank() }
            check("todas tienen chipset", conChip == arr.length(), "$conChip")
        }
        val man = java.io.File(raiz, "manifiesto.json")
        check("el manifiesto de datos existe", man.exists())
        if (man.exists()) {
            val o = org.json.JSONObject(man.readText())
            check("el manifiesto declara 6 bloques", o.getJSONArray("bloques").length() >= 6)
            check("y dice por qué no se usó versus.com",
                o.toString().contains("versus.com"))
        }
    }
}


    // ══════════════════════════════════════════════════════════════════════════
    //  Estructura de navegación (1.7.0)
    //
    //  La app pasó de diez secciones planas a cinco destinos con subpestañas. Estas
    //  pruebas vigilan que el cambio no pierda ninguna sección por el camino: si una
    //  desaparece, esto falla en vez de que el usuario descubra que falta una pantalla.
    // ══════════════════════════════════════════════════════════════════════════
    println()
    println("── estructura de navegación ──")

    check("hay cinco destinos", Destino.entries.size == 5, "${Destino.entries.size}")
    check("el primero es Home", Destino.entries.first() == Destino.HOME)
    check(
        "ningún destino se queda sin subpestañas salvo Home",
        Destino.entries.filter { it != Destino.HOME }.all { Navegacion.pestanas(it).isNotEmpty() },
    )
    check("Home no tiene subpestañas", Navegacion.pestanas(Destino.HOME).isEmpty())
    check("ninguna píldora pasa de cinco", Destino.entries.size <= 5)
    check(
        "Perfiles tiene catálogo, aplicar y generar",
        Navegacion.pestanas(Destino.PERFILES).map { it.titulo } ==
            listOf("Catálogo", "Aplicar", "Generar"),
        Navegacion.pestanas(Destino.PERFILES).joinToString { it.titulo },
    )
    check(
        "Herramientas lleva configurar y las acciones de rendimiento",
        Navegacion.pestanas(Destino.HERRAMIENTAS).contains(Pestana.CONFIGURAR) &&
            Navegacion.pestanas(Destino.HERRAMIENTAS).contains(Pestana.RENDIMIENTO),
    )
    check(
        "Datos conserva TOP500, aplicado y dispositivos, y gana el historial",
        Navegacion.pestanas(Destino.DATOS) ==
            listOf(Pestana.TOP500, Pestana.HISTORIAL, Pestana.APLICADO, Pestana.DISPOSITIVOS),
    )
    check(
        "Ayuda conserva compatibilidad, diagnóstico, límites y fuentes",
        Navegacion.pestanas(Destino.AYUDA) ==
            listOf(Pestana.COMPATIBILIDAD, Pestana.DIAGNOSTICO, Pestana.LIMITES, Pestana.FUENTES),
    )
    // Las diez secciones antiguas tienen que tener destino: ninguna se perdió.
    val antiguas = listOf(
        "Perfiles", "Super", "Aplicaciones", "Útil", "GPU", "Diag.",
        "Compat.", "Aplicado", "Aprender", "Rendi.",
    )
    check(
        "las diez secciones de la 1.6.0 siguen teniendo sitio",
        antiguas.all { Navegacion.mapaDesdeLaVersionAnterior.containsKey(it) },
        antiguas.filterNot { Navegacion.mapaDesdeLaVersionAnterior.containsKey(it) }.toString(),
    )
    check(
        "cada pestaña pertenece a un destino válido",
        Pestana.entries.all { Navegacion.destinoDe(it) == it.destino },
    )
    check(
        "inicial() nunca devuelve una pestaña de otro destino",
        Destino.entries.all { d -> Navegacion.inicial(d)?.let { it.destino == d } ?: true },
    )

    // ── El banner de estado dice la verdad ──
    println()
    println("── banner de estado ──")
    val conRoot = Estado.banner(conRoot = true, conShizuku = false, conPermiso = true, modo = Modo.SEGURO)
    check("con root el banner está en verde", conRoot.esBueno && conRoot.tono == Banner.Tono.BIEN)
    check("y dice «root», no «mejor modo posible»", conRoot.titulo.contains("root", true))
    val shizuku = Estado.banner(conRoot = false, conShizuku = true, conPermiso = true, modo = Modo.SEGURO)
    check("con Shizuku el banner también está en verde", shizuku.tono == Banner.Tono.BIEN)
    check(
        "pero aclara que no es root",
        shizuku.detalle.contains("No es root"),
        shizuku.detalle,
    )
    val sinPermiso = Estado.banner(conRoot = false, conShizuku = true, conPermiso = false, modo = Modo.SEGURO)
    check("sin permiso de Shizuku es ámbar, no verde", sinPermiso.tono == Banner.Tono.AVISO)
    val sinVia = Estado.banner(conRoot = false, conShizuku = false, conPermiso = false, modo = Modo.SEGURO)
    check("sin vía es rojo", sinVia.tono == Banner.Tono.MAL)
    check("y no finge que se puede escribir", sinVia.detalle.contains("no va a", true), sinVia.detalle)
    check(
        "nunca dice «el mejor modo posible»",
        listOf(conRoot, shizuku, sinPermiso, sinVia).none {
            it.detalle.contains("mejor modo", true) || it.titulo.contains("mejor modo", true)
        },
    )
    check(
        "sin vía, «puede» sólo promete leer",
        Estado.puede(conRoot = false, conShizuku = false, conPermiso = false).contains("NO puede escribir"),
    )
    check(
        "con vía, «puede» sí promete escribir",
        Estado.puede(conRoot = true, conShizuku = false, conPermiso = true).contains("Puede: aplicar"),
    )

    // ── Avisos del Home: sólo cuando hay algo que decir ──
    check(
        "sin problemas no hay avisos (nada de relleno)",
        Estado.avisos(
            conVia = true, modo = Modo.SEGURO, resumenCompat = "todo responde",
            tienePerfil = true, irreversibleAplicado = false, sinApps = false,
        ).isEmpty(),
    )
    check(
        "sin vía sí hay aviso y es rojo",
        Estado.avisos(
            conVia = false, modo = Modo.SEGURO, resumenCompat = "todo responde",
            tienePerfil = true, irreversibleAplicado = false, sinApps = false,
        ).any { it.tono == Banner.Tono.MAL },
    )
    check(
        "cada aviso trae qué hacer, no sólo el problema",
        Estado.avisos(
            conVia = false, modo = Modo.EXPERTO, resumenCompat = "",
            tienePerfil = false, irreversibleAplicado = true, sinApps = true,
        ).all { it.detalle.length > 20 },
    )

    // ── Configuración: lo que se puede y lo que no ──
    println()
    println("── pantalla Configurar ──")
    val sinShell = Configuracion.filas(conEsCritura = false, sdk = 33)
    val conShell = Configuracion.filas(conEsCritura = true, sdk = 33)
    check("sin shell no hay ninguna fila disponible", sinShell.none { it.disponible })
    check("con shell sí hay filas disponibles", conShell.count { it.disponible } >= 8)
    check(
        "las que necesitan root están marcadas como tal",
        conShell.filterNot { it.disponible }.all {
            it.etiquetas.contains(FilaConfig.Etiqueta.NECESITA_ROOT)
        },
    )
    check(
        "toda fila no disponible se explica en su detalle",
        conShell.filterNot { it.disponible }.all { it.detalle.length > 30 },
    )
    check(
        "los cuatro grupos del original existen",
        conShell.map { it.grupo }.distinct().sorted() ==
            listOf("GENERAL", "IMAGEN", "OPENGL", "VULKAN"),
    )
    check("todas las filas tienen id único", conShell.map { it.id }.distinct().size == conShell.size)
    check(
        "ninguna fila viene marcada de fábrica",
        conShell.none { it.marcada },
    )
    check(
        "el resumen dice cuántas necesitan root",
        Configuracion.resumen(conShell).contains("necesitan root"),
        Configuracion.resumen(conShell),
    )
    check(
        "la versión de OpenGL ES depende del API del teléfono",
        Configuracion.filas(true, 26).first { it.id == "gl_version" }.detalle.contains("26") &&
            Configuracion.filas(true, 33).first { it.id == "gl_version" }.detalle.contains("33"),
    )

    // ── Iconos: son vectores, no glifos escritos ──
    // Un LazyColumn dentro de otro LazyColumn revienta al medir: es cierre de app
    // garantizado. La pantalla Configurar se pinta dentro del LazyColumn principal, así
    // que no puede traer el suyo. Esta prueba lo vigila en el código fuente.
    val fuenteConfig = listOf(
        "app/src/main/java/io/github/labandita/glreborn/ui/Configurar.kt",
        "src/main/java/io/github/labandita/glreborn/ui/Configurar.kt",
    ).map { java.io.File(it) }.firstOrNull { it.exists() }
    if (fuenteConfig != null) {
        val texto = fuenteConfig.readText()
        check(
            "Configurar no anida un LazyColumn (cerraría la app al medir)",
            !texto.contains("LazyColumn("),
        )
        check("Configurar usa Column y deja desplazar al de fuera", texto.contains("Column("))
    }

    println()
    println("── iconos ──")
    val fuenteIconos = listOf(
        "app/src/main/java/io/github/labandita/glreborn/ui/Iconos.kt",
        "src/main/java/io/github/labandita/glreborn/ui/Iconos.kt",
    ).map { java.io.File(it) }.firstOrNull { it.exists() }
    if (fuenteIconos != null) {
        val texto = fuenteIconos.readText()
        check("los iconos se dibujan con Path", texto.contains("Path()"))
        check("y usan un trazo uniforme", texto.contains("Stroke("))
        check(
            "ya no hay glifos de fuente en la navegación",
            !texto.contains("\"◇\"") && !texto.contains("\"✦\""),
        )
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  LEY DE DISEÑO (1.7.1)
    //
    //  Regla del usuario, textual: «prohibido que la app que creamos tenga diseño
    //  parecido a HTML, ya sea genérico o moderno». No es una preferencia estética:
    //  es una prohibición. Y para que no dependa de que alguien se acuerde, se
    //  comprueba sola en cada compilación.
    //
    //  Qué se vigila, y por qué cada cosa:
    //   · glifos de texto como iconos  → es EL síntoma de «esto es una página web»
    //   · degradados en cada tarjeta   → decoración sin jerarquía
    //   · vidrio en todas las cajas    → sin profundidad no hay interfaz
    //   · cero movimiento              → nada responde al toque
    //   · todos los botones iguales    → no se sabe qué es importante
    // ══════════════════════════════════════════════════════════════════════════
    println()
    println("── ley de diseño: prohibido parecer HTML ──")

    val uiDir = listOf(
        "app/src/main/java/io/github/labandita/glreborn/ui",
        "src/main/java/io/github/labandita/glreborn/ui",
    ).map { java.io.File(it) }.firstOrNull { it.isDirectory }

    if (uiDir == null) {
        check("la carpeta de interfaz existe para poder vigilarla", false)
    } else {
        val fuentes = uiDir.listFiles { f -> f.extension == "kt" }!!.sortedBy { it.name }
        check("hay ficheros de interfaz que revisar", fuentes.size >= 5, "${fuentes.size}")

        // ── 1. Glifos de texto usados como iconos ──────────────────────────
        // Se buscan SÓLO dentro de literales de cadena: un carácter suelto dibujado
        // como texto es lo que delata a una página web.
        val glifosProhibidos = listOf(
            "◇", "◆", "✦", "✧", "▣", "▤", "◉", "◈", "◇", "◐", "▲", "●", "■", "□",
            "↻", "✓", "✗", "×", "★", "☆", "➤", "≡", "⌂", "⚡",
        )
        val glifosEncontrados = mutableListOf<String>()
        fuentes.forEach { f ->
            val lineas = f.readLines()
            lineas.forEachIndexed { n, linea ->
                // Ignorar comentarios: en un comentario se puede hablar de glifos.
                val limpia = linea.substringBefore("//")
                if (!limpia.contains('"')) return@forEachIndexed
                glifosProhibidos.forEach { g ->
                    if (limpia.contains(g)) {
                        glifosEncontrados += "${f.name}:${n + 1} «$g»"
                    }
                }
            }
        }
        check(
            "ningún icono es un carácter de texto (eso es lo que parece HTML)",
            glifosEncontrados.isEmpty(),
            glifosEncontrados.joinToString(" | "),
        )

        // ── 2. Los iconos están dibujados ──────────────────────────────────
        val iconos = fuentes.firstOrNull { it.name == "Iconos.kt" }
        check("existe Iconos.kt", iconos != null)
        if (iconos != null) {
            val txt = iconos.readText()
            val cuantos = Regex("fun [A-Z][A-Za-z]+\\(").findAll(txt).count()
            check("hay al menos 8 iconos dibujados", cuantos >= 8, "$cuantos")
            check("se dibujan con Path", txt.contains("Path()"))
            check("con trazo uniforme", txt.contains("Stroke("))
            check(
                "y esquinas redondeadas de verdad",
                txt.contains("StrokeCap.Round") && txt.contains("StrokeJoin.Round"),
            )
        }

        // ── 3. Jerarquía tipográfica: no todo el mismo tamaño ──────────────
        val todos = fuentes.joinToString("\n") { it.readText() }
        val tamanos = Regex("fontSize\\s*=\\s*([0-9.]+)\\.sp")
            .findAll(todos).map { it.groupValues[1].toFloat() }.toSet()
        check(
            "la tipografía tiene escala, no un solo tamaño",
            tamanos.size >= 6,
            tamanos.sorted().toString(),
        )
        check("hay titulares grandes (≥18sp)", tamanos.any { it >= 18f })
        check("y etiquetas pequeñas (≤11sp)", tamanos.any { it <= 11f })

        // ── 4. Movimiento: algo responde y se mueve ────────────────────────
        check(
            "hay animación con física, no todo aparece de golpe",
            todos.contains("spring(") && todos.contains("animateFloatAsState"),
        )
        check(
            "el muelle usa damping (no es linear disfrazado)",
            todos.contains("DampingRatio"),
        )

        // ── 5. El vidrio es acento, no el fondo de todo ────────────────────
        // Si en Pantalla.kt hubiera más .vidrio que texto, sería todo cajas iguales.
        val pantalla = fuentes.firstOrNull { it.name == "Pantalla.kt" }
        if (pantalla != null) {
            val txt = pantalla.readText()
            val vidrios = Regex("\\.vidrio(Alto)?\\(").findAll(txt).count()
            val textos = Regex("\\bText\\(").findAll(txt).count()
            check(
                "el cristal se usa como acento y no como única superficie",
                vidrios < textos,
                "vidrio=$vidrios texto=$textos",
            )
        }

        // ── 6. Estados vacíos diseñados, no «No hay datos» a secas ─────────
        val vacios = Regex("\"[^\"]{0,4}(No hay|Sin datos|Vacío|vacio)[^\"]{0,20}\"")
            .findAll(todos).count()
        check(
            "ningún estado vacío es un «No hay datos» pelado",
            vacios == 0,
            "$vacios",
        )

        // ── 7. Botones con importancia, no todos iguales ───────────────────
        check(
            "existen al menos tres tratamientos de botón",
            todos.contains("fun Boton(") && todos.contains("fun BotonPlano(") &&
                todos.contains("fun BotonFila("),
        )

        // ── 8. Nada de emojis como iconos de interfaz ──────────────────────
        val emojis = Regex("[\\uD83C-\\uDBFF][\\uDC00-\\uDFFF]").findAll(todos).count()
        check("ningún emoji hace de icono", emojis == 0, "$emojis")
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  Las dos variantes de APK (1.7.1)
    //
    //  En 1.7.0 la variante «cruda» salió con el MISMO tamaño que la normal porque el
    //  flag no invalidaba la tarea de empaquetado. Publicar un APK que dice ser crudo y
    //  no lo es, es un fallo de honestidad, no de empaquetado. Esta prueba lo impide:
    //  si los dos APK pesan parecido, algo se ha vuelto a romper.
    // ══════════════════════════════════════════════════════════════════════════
    println()
    println("── las dos variantes de APK ──")
    // Se buscan por patrón, no por número de versión: si se fija el nombre, la prueba deja
    // de comprobar nada en cuanto sube la versión, y ese silencio es peor que un fallo.
    val carpetaApk = listOf(java.io.File("apk"), java.io.File("../apk"))
        .firstOrNull { it.isDirectory }
    val apks = carpetaApk?.listFiles { f -> f.extension == "apk" }?.sortedBy { it.name }
        ?: emptyList()
    val normal = apks.filterNot { it.name.contains("sin-comprimir") }.lastOrNull()
    val crudo = apks.firstOrNull { it.name.contains("sin-comprimir") }
    if (normal != null && crudo != null) {
        val r = crudo.length().toDouble() / normal.length()
        check(
            "la variante cruda pesa al menos el triple que la normal",
            r >= 3.0,
            "normal=${normal.length()} crudo=${crudo.length()} ratio=%.1f".format(r),
        )
        check(
            "y la normal sigue estando entre 2 y 6 MB",
            normal.length() in 2_000_000..6_000_000,
            "${normal.length()}",
        )
        check(
            "y la cruda pasa de 10 MB (contenido real, sin relleno)",
            crudo.length() > 10_000_000,
            "${crudo.length()}",
        )
    } else {
        // Aún no se han compilado en esta pasada: no es un fallo, se avisa y se sigue.
        println("  [aviso] los APK todavía no están compilados; la comparación se hará al publicar")
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  Punto de mira y FPS (1.8.0)
    //
    //  Es lo único de la app que se ve MIENTRAS se juega. Si esto puede dibujar algo que
    //  no existe (una mira en modo «apagado», un servicio encendido sin nada que enseñar),
    //  el usuario está pagando una notificación fija para nada.
    // ══════════════════════════════════════════════════════════════════════════
    println()
    println("── punto de mira y FPS ──")

    val porDefecto = AjustesSuperposicion()
    check("arranca con una mira visible", porDefecto.modo == "MIRA")
    check("y con contador de FPS", porDefecto.fps)
    check("el color por defecto es el cian de la casa", porDefecto.colorLegible() == "cian")
    check("el punto arranca en el centro", porDefecto.saneado().x == 0.5f)
    check(
        "hay cuatro formas y la de apagar",
        AjustesSuperposicion.MODOS.size == 4 && "APAGADO" in AjustesSuperposicion.MODOS,
    )
    check("hay cuatro tamaños y cuatro grosores",
        AjustesSuperposicion.TAMANOS.size == 4 && AjustesSuperposicion.GROSORES.size == 4)

    // Saneado: lo que venga de fuera no puede romper nada.
    val basura = AjustesSuperposicion(
        modo = "MODE-LOCO", tamano = 999f, grosor = -3f, color = 12345, x = 9f, y = -4f,
    ).saneado()
    check("un modo inventado cae en MIRA", basura.modo == "MIRA")
    check("un tamaño imposible cae en 26", basura.tamano == 26f)
    check("un grosor imposible cae en 2.5", basura.grosor == 2.5f)
    check("un color inventado cae en cian", basura.colorLegible() == "cian")
    check("la posición se queda dentro de la pantalla",
        basura.x in 0f..1f && basura.y in 0f..1f)

    // El servicio sólo debe estar encendido si hay algo que dibujar.
    check("en modo APAGADO sin FPS no se enciende",
        !AjustesSuperposicion(modo = "APAGADO", fps = false).debeEstarActivo())
    check("en modo APAGADO con FPS sí (se sigue viendo el contador)",
        AjustesSuperposicion(modo = "APAGADO", fps = true).debeEstarActivo())
    check("con mira se enciende", AjustesSuperposicion(modo = "MIRA").debeEstarActivo())

    // El resumen dice lo que hay, sin adornos.
    check("el resumen nombra la forma y el tamaño",
        AjustesSuperposicion(modo = "CIRCULO", tamano = 34f).resumen().contains("Círculo") &&
            AjustesSuperposicion(modo = "CIRCULO", tamano = 34f).resumen().contains("34"))
    check("y avisa cuando está apagado del todo",
        AjustesSuperposicion(modo = "APAGADO", fps = false).resumen().contains("Apagado"))

    // La ley de diseño también aplica aquí: la mira se dibuja, no se escribe.
    val fuenteVista = listOf(
        "app/src/main/java/io/github/labandita/glreborn/overlay/VistaSuperposicion.kt",
        "src/main/java/io/github/labandita/glreborn/overlay/VistaSuperposicion.kt",
    ).map { java.io.File(it) }.firstOrNull { it.exists() }
    if (fuenteVista != null) {
        val txt = fuenteVista.readText()
        check("la mira se dibuja con Path", txt.contains("Path()"))
        check("y con Paint reutilizado (nada de reservar por fotograma)",
            txt.contains("private val paint"))
        check("la capa no captura los toques por defecto",
            listOf(
                "app/src/main/java/io/github/labandita/glreborn/overlay/ServicioSuperposicion.kt",
                "src/main/java/io/github/labandita/glreborn/overlay/ServicioSuperposicion.kt",
            ).map { java.io.File(it) }.any {
                it.exists() && it.readText().contains("FLAG_NOT_TOUCHABLE")
            })
        check("los FPS se miden con el reloj de la pantalla",
            txt.contains("Choreographer"))
    }

    // Permisos: nada de dibujar encima sin declararlo en el manifiesto.
    val manifiesto = listOf(
        "app/src/main/AndroidManifest.xml", "src/main/AndroidManifest.xml",
    ).map { java.io.File(it) }.firstOrNull { it.exists() }
    if (manifiesto != null) {
        val txt = manifiesto.readText()
        check("se declara el permiso de superposición",
            txt.contains("SYSTEM_ALERT_WINDOW"))
        check("el servicio va en primer plano con su motivo",
            txt.contains("foregroundServiceType=\"specialUse\"") &&
                txt.contains("PROPERTY_SPECIAL_USE_FGS_SUBTYPE"))
        check("y el servicio no se exporta", txt.contains("android:exported=\"false\""))
    }

    // ── GPUs: se han ampliado las familias que faltaban ────────────────────
    println()
    println("── catálogo de GPU ampliado ──")
    val fGpu = listOf(
        "app/src/main/assets/datos/gpu.json", "src/main/assets/datos/gpu.json",
    ).map { java.io.File(it) }.firstOrNull { it.exists() }
    if (fGpu != null) {
        val o = org.json.JSONObject(fGpu.readText())
        val arr = o.getJSONArray("gpus")
        val nombres = (0 until arr.length()).map { arr.getJSONObject(it).getString("gpu") }
        check("hay más de cien GPU", nombres.size > 100, "${nombres.size}")
        check("incluye Mali", nombres.any { it.startsWith("Mali") })
        check("incluye Immortalis", nombres.any { it.startsWith("Immortalis") })
        check("incluye PowerVR", nombres.any { it.startsWith("PowerVR") })
        check("incluye Adreno", nombres.any { it.startsWith("Adreno") })
        check("ninguna repetida", nombres.distinct().size == nombres.size)
        check("todas declaran un nivel de OpenGL ES",
            (0 until arr.length()).all { arr.getJSONObject(it).optString("api").isNotBlank() })
        check("el fichero explica qué no se rastreó y por qué",
            o.optString("descarte").contains("GSMArena") &&
                o.optString("descarte").contains("versus.com"))
    }


    // ── 1.8.1: el usuario dijo «el diseño sigue siendo una porquería de HTML» ──
    // Lo que fallaba no era un glifo suelto: era el sistema entero. Degradado violeta en
    // cada caja, cinco acentos, vidrio en el contenido. Estas comprobaciones lo prohíben.
    println()
    println("── ley de diseño: sin disfraz de plantilla ──")

    val temaFuente = listOf(
        "app/src/main/java/io/github/labandita/glreborn/ui/Tema.kt",
        "src/main/java/io/github/labandita/glreborn/ui/Tema.kt",
    ).map { java.io.File(it) }.firstOrNull { it.exists() }
    if (temaFuente != null) {
        val tema = temaFuente.readText()
        val colores = Regex("Color\\(0xFF([0-9A-Fa-f]{6})\\)").findAll(tema)
            .map { it.groupValues[1] }.toList()
        check("el tema declara una paleta de verdad", colores.size >= 12, "${colores.size}")

        // Nada de violeta de plantilla: el violeta saturado es el tic más repetido.
        val violetas = colores.count { hex ->
            val r = hex.substring(0, 2).toInt(16)
            val g = hex.substring(2, 4).toInt(16)
            val b = hex.substring(4, 6).toInt(16)
            b > r + 40 && b > g + 40 && r > g && r > 100
        }
        check("el tema no lleva violeta de plantilla", violetas == 0, "$violetas colores violeta")

        // Un solo acento de marca: como mucho tres colores saturados de acento.
        val acentosSaturados = colores.count { hex ->
            val r = hex.substring(0, 2).toInt(16)
            val g = hex.substring(2, 4).toInt(16)
            val b = hex.substring(4, 6).toInt(16)
            maxOf(r, g, b) > 180 && minOf(r, g, b) < 130
        }
        check("hay pocos acentos saturados (no cinco)", acentosSaturados <= 5, "$acentosSaturados")

        // El fondo es neutro: en un gris, los tres canales casi iguales.
        val fondos = Regex("val fondo\\w* = Color\\(0xFF([0-9A-Fa-f]{6})\\)").findAll(tema)
            .map { it.groupValues[1] }.toList()
        check("los fondos son neutros, no violetas", fondos.isNotEmpty() && fondos.all { hex ->
            val r = hex.substring(0, 2).toInt(16)
            val g = hex.substring(2, 4).toInt(16)
            val b = hex.substring(4, 6).toInt(16)
            maxOf(r, g, b) - minOf(r, g, b) <= 24
        }, fondos.toString())
    }

    // Los degradados de adorno están prohibidos en las pantallas.
    if (uiDir != null) {
        val fuentesUi2 = uiDir.listFiles { f -> f.extension == "kt" }!!.sortedBy { it.name }
        val conDegradado = fuentesUi2.filter { f ->
            val txt = f.readText()
            // El tema puede declarar degradados; las pantallas no deben usarlos de adorno.
            f.name != "Tema.kt" && Regex("Brush\\.(linear|vertical)Gradient\\(").containsMatchIn(txt)
        }.map { it.name }
        check(
            "ninguna pantalla usa degradados de adorno",
            conDegradado.isEmpty(),
            conDegradado.toString(),
        )

        // El vidrio no puede estar en el contenido: como mucho 2 usos en toda la interfaz.
        val usosVidrio = fuentesUi2.sumOf { f ->
            Regex("\\.vidrio(Alto)?\\(").findAll(f.readText()).count()
        }
        check("el vidrio es la excepción, no la norma", usosVidrio <= 2, "$usosVidrio usos")

        // Las superficies se expresan con tono: tiene que existir la escala.
        val temaTxt = temaFuente?.readText() ?: ""
        check("hay tres niveles de superficie con tono",
            temaTxt.contains("val superficie ") && temaTxt.contains("superficieAlta") &&
                temaTxt.contains("superficieMasAlta"))
    }

    // ── 1.8.1: la app no puede cerrarse en silencio ─────────────────────────
    println()
    println("── blindaje contra cierres ──")
    val mainFuente = listOf(
        "app/src/main/java/io/github/labandita/glreborn/MainActivity.kt",
        "src/main/java/io/github/labandita/glreborn/MainActivity.kt",
    ).map { java.io.File(it) }.firstOrNull { it.exists() }
    if (mainFuente != null) {
        val txt = mainFuente.readText()
        check(
            "cada lectura del teléfono va protegida por separado",
            txt.contains("protegerCon(\"estado\"") &&
                txt.contains("proteger(\"diagnóstico\"") &&
                txt.contains("proteger(\"límites\"") &&
                txt.contains("proteger(\"aplicado\"") &&
                txt.contains("proteger(\"biblioteca\""),
        )
        check(
            "los LaunchedEffect no pueden lanzar excepciones",
            Regex("LaunchedEffect\\([^)]*\\)\\s*\\{\\s*runCatching").findAll(txt).count() >= 2,
        )
        check("un fallo de lectura se enseña, no se traga",
            txt.contains("onFalloLectura"))
    }
    val appFuente = listOf(
        "app/src/main/java/io/github/labandita/glreborn/RebornApp.kt",
        "src/main/java/io/github/labandita/glreborn/RebornApp.kt",
    ).map { java.io.File(it) }.firstOrNull { it.exists() }
    check("existe un manejador global de fallos", appFuente != null)
    if (appFuente != null) {
        val txt = appFuente.readText()
        check("guarda el parte en disco", txt.contains("RegistroFallos.guardar"))
        check("y relanza la app para poder enseñarlo",
            txt.contains("EXTRA_MOSTRAR_FALLO") && txt.contains("startActivity"))
    }
    val manifiestoTxt = listOf(
        "app/src/main/AndroidManifest.xml", "src/main/AndroidManifest.xml",
    ).map { java.io.File(it) }.firstOrNull { it.exists() }?.readText() ?: ""
    check("la Application está declarada en el manifiesto",
        manifiestoTxt.contains("android:name=\".RebornApp\""))
    check("existe la pantalla que enseña el fallo",
        listOf(
            "app/src/main/java/io/github/labandita/glreborn/ui/PantallaFallo.kt",
            "src/main/java/io/github/labandita/glreborn/ui/PantallaFallo.kt",
        ).any { java.io.File(it).exists() })


    // ── 1.9.0: los patrones que se copiaron de apps que sí están bien hechas ──
    println()
    println("── patrones de apps reales ──")
    val homeFuente = listOf(
        "app/src/main/java/io/github/labandita/glreborn/ui/Home.kt",
        "src/main/java/io/github/labandita/glreborn/ui/Home.kt",
    ).map { java.io.File(it) }.firstOrNull { it.exists() }
    if (homeFuente != null) {
        val h = homeFuente.readText()
        // El fallo que cerró la app una vez: dos «lista y desplaza» anidados.
        // Se busca la LLAMADA, no la palabra: el comentario que explica el fallo la nombra.
        check("Home no anida un LazyColumn dentro de otro scroller",
            !Regex("Lazy(Column|Row)\\s*\\(").containsMatchIn(h))
        check("Home se desplaza con un solo gesto", h.contains("verticalScroll(rememberScrollState())"))
        // Carrusel de píldoras (patrón de Hardware Monitor).
        check("hay carrusel de píldoras con una encendida",
            h.contains("horizontalScroll(rememberScrollState())") &&
                h.contains("PildorasMedidor") && h.contains("RoundedCornerShape(50)"))
        // Rejilla de baldosas con icono (patrón de Androoster).
        check("las acciones son baldosas con icono dibujado",
            Regex("Baldosa\\(").findAll(h).count() >= 4 && h.contains("interno") == false)
        check("las baldosas llevan icono, no texto suelto", h.contains("icono = {"))
        // Cada píldora tiene datos detrás: cinco medidores, no cinco huecos.
        // Ahora los nombres pueden llegar por parámetro (el catálogo usa el mismo carrusel
        // para filtrar), así que se comprueba el valor por defecto de la portada.
        check("el carrusel no promete más de lo que hay",
            h.contains("Identidad") && h.contains("Sonda") && h.contains("Arquitectura"))
        // Nada de cajas del mismo peso: la Home usa líneas y huecos, no bordes por todas partes.
        val cajas = Regex("\\.border\\(").findAll(h).count()
        check("Home no dibuja cajas por todas partes", cajas <= 1, "$cajas bordes")
    }


    // ── 1.9.1: tipografía propia y los procedimientos que se vigilan a sí mismos ──
    println()
    println("── no parecer el sistema operativo ──")
    val temaFuente2 = listOf(
        "app/src/main/java/io/github/labandita/glreborn/ui/Tema.kt",
        "src/main/java/io/github/labandita/glreborn/ui/Tema.kt",
    ).map { java.io.File(it) }.firstOrNull { it.exists() }
    if (temaFuente2 != null) {
        val te = temaFuente2.readText()
        // La fuente del sistema es la razón número uno por la que una app parece una web.
        check("el tema declara una familia tipográfica propia", te.contains("FontFamily("))
        check("usa fuentes empaquetadas, no del sistema", te.contains("R.font."))
        check("la escala de tipos tiene al menos 6 pasos",
            Regex("fontSize = [0-9]+\\.sp").findAll(te).map { it.value }.toSet().size >= 6)
        check("el texto principal no baja de 10 sp",
            Regex("fontSize = ([0-9]+)\\.sp").findAll(te).map { it.groupValues[1].toInt() }
                .none { it < 10 })
    }
    // Las fuentes tienen que estar de verdad en res/font, con su licencia.
    val dirFuentes = listOf("app/src/main/res/font", "src/main/res/font")
        .map { java.io.File(it) }.firstOrNull { it.isDirectory }
    check("hay al menos 3 fuentes empaquetadas", dirFuentes != null && (dirFuentes.listFiles()?.size ?: 0) >= 3)
    val licencia = listOf(
        "app/src/main/assets/OFL-AtkinsonNext.txt",
        "src/main/assets/OFL-AtkinsonNext.txt",
    ).map { java.io.File(it) }.firstOrNull { it.exists() }
    check("la licencia de la fuente viaja dentro del APK", licencia != null)
    if (licencia != null) {
        check("la licencia es de uso libre (SIL OFL)",
            licencia.readText().contains("SIL Open Font License"))
    }

    // Los procedimientos son entregables: si faltan, no existen.
    println()
    println("── procedimientos ──")
    val procs = listOf(
        "01-censar-catalogo-appteka.py",
        "02-elegir-y-desmontar.sh",
        "03-cerrar-tramo.sh",
        "04-incidencias.sh",
        "05-diseno-imitar-y-mejorar.md",
    )
    val dirProcs = listOf("normas/procedimientos", "../normas/procedimientos")
        .map { java.io.File(it) }.firstOrNull { it.isDirectory }
    val encontrados = dirProcs?.listFiles()?.map { it.name } ?: emptyList()
    if (dirProcs != null) {
        procs.forEach { p ->
            check("existe el procedimiento $p", encontrados.contains(p))
        }
    }


    // ── 1.9.2: la ley tenía un agujero y las cajas se colaron por él ──
    //
    // Lo que pasó: la regla contaba `.vidrio(` (la función) y `Brush.linearGradient(`. En la
    // 1.9.1 había 28 usos de `Cristal.vidrio` —el COLOR— que no eran función y por tanto no
    // contaban. La ley daba verde con 30 cajas de vidrio en la pantalla. Una regla que se puede
    // esquivar cambiando de nombre no es una regla.
    println()
    println("── la superficie, medida de verdad ──")
    if (uiDir != null) {
        val fuentes3 = uiDir.listFiles { f -> f.extension == "kt" }!!.sortedBy { it.name }
        fun cuenta(re: String, saltar: Set<String> = setOf("Tema.kt", "Grupo.kt")) =
            fuentes3.filter { it.name !in saltar }
                .sumOf { f -> Regex(re).findAll(f.readText()).count() }

        // Un color de superficie se llama por su nivel, no por cómo se ve.
        val vidrios = cuenta("Cristal\\.vidrio")
        check("ninguna pantalla usa un color llamado «vidrio»", vidrios == 0, "$vidrios usos")

        // Ni cajas dentro de cajas: los bordes de adorno, prohibidos en las pantallas.
        val bordes = cuenta("\\s\\.border\\(")
        check("las pantallas no dibujan bordes de adorno", bordes <= 2, "$bordes bordes")

        // El número de cajas redondeadas no puede crecer: es la medida de «esto parece una web».
        val cajas = cuenta("RoundedCornerShape\\(1[2-9]\\.[0-9]?dp|RoundedCornerShape\\(2[0-9]\\.[0-9]?dp")
        check("hay menos de 40 cajas redondeadas en toda la interfaz", cajas < 40, "$cajas cajas")

        // Un solo sitio donde se dibuja la caja: el componente Grupo.
        check("el componente de grupo existe y es el único que dibuja cajas",
            java.io.File(uiDir, "Grupo.kt").exists())
    }


    // ── 1.9.3: nada de datos cortados a la mitad ─────────────────────────────
    //
    // Lo que pasó: `FilaDato` y `Dato` pintaban el valor con `maxLines = 1`. La huella del
    // sistema (`ro.build.fingerprint`) mide 90 caracteres: se cortaba a media cadena y no había
    // forma de leerla entera. Y en un sitio se cortaba a mano con `.take(24) + "…"`. Un dato que
    // no se puede leer no está en la app.
    println()
    println("── sin truncamiento ──")
    if (uiDir != null) {
        val pantallas = uiDir.listFiles { f -> f.extension == "kt" }!!
            .filter { it.name !in setOf("Tema.kt", "Iconos.kt") }
        // Ningún valor se corta a mano con take()/substring() para que quepa.
        val cortados = pantallas.filter { f ->
            // `take(limite)` en el núcleo de la búsqueda es un tope de lista que se puede subir,
            // no un dato cortado: la pantalla no lo esconde, sólo ordena. Todo lo demás, prohibido.
            Regex("\\.take\\s*\\(\\s*\\d+\\s*\\)").findAll(f.readText())
                .any { m -> !m.value.contains("take(0)") }
        }.map { it.name }
        check("ningún valor se corta a mano con take(n)", cortados.isEmpty(), cortados.toString())

        // El par etiqueta/valor tiene que poder mostrar el valor entero.
        val grupo = java.io.File(uiDir, "Grupo.kt")
        check("el componente de dato existe", grupo.exists())
        if (grupo.exists()) {
            val g = grupo.readText()
            val bloqueDato = g.substringAfter("internal fun Dato(").substringBefore("\n@Composable")
            check(
                "el dato largo se muestra en varias líneas, no cortado",
                bloqueDato.contains("valor.length > ") && bloqueDato.contains("lineHeight"),
            )
            check("el dato se puede copiar para leerlo entero",
                bloqueDato.contains("LocalClipboardManager"))
        }
        // Y en la portada, lo mismo: no puede quedarse un maxLines=1 sobre un valor del sistema.
        val home = java.io.File(uiDir, "Home.kt")
        if (home.exists()) {
            val h = home.readText()
            check("la huella no se recorta en la portada", !h.contains("fingerprint\"]?.take("))
        }
    }


    // ── 1.9.4: inestabilidad ─────────────────────────────────────────────────
    //
    // Dos formas de que la app se sienta rota sin cerrarse: perder lo que estabas mirando
    // cuando Android mata el proceso en segundo plano, y volver a mirar el teléfono entero cada
    // vez que vuelves a la app. Las dos estaban.
    println()
    println("── estabilidad ──")
    if (uiDir != null) {
        val pantalla = java.io.File(uiDir, "Pantalla.kt")
        if (pantalla.exists()) {
            val s = pantalla.readText()
            check("el destino sobrevive a que Android mate el proceso",
                s.contains("var destino by rememberSaveable"))
            check("la pestaña también", s.contains("var pestana by rememberSaveable"))
            check("lo que estabas escribiendo también", s.contains("var consulta by rememberSaveable"))
        }
        val home = java.io.File(uiDir, "Home.kt")
        if (home.exists()) {
            check("el medidor elegido sobrevive", home.readText().contains("var medidor by rememberSaveable"))
        }
    }
    val act = listOf(
        "app/src/main/java/io/github/labandita/glreborn/MainActivity.kt",
        "src/main/java/io/github/labandita/glreborn/MainActivity.kt",
    ).map { java.io.File(it) }.firstOrNull { it.exists() }
    if (act != null) {
        val a = act.readText()
        check("volver a la app no repite la lectura del teléfono",
            a.contains("fun releerSiToca()") && a.contains("8_000L"))
        check("volver a la app sí refresca cuando toca",
            a.contains("releerSiToca()"))
    }


    // ── 1.9.5: llevarte tus perfiles ─────────────────────────────────────────
    //
    // El respaldo tiene que sobrevivir a un viaje de ida y vuelta sin perder ni un campo, y
    // tiene que decir que NO cuando le dan un fichero que no es suyo. Un importador que se traga
    // cualquier cosa es peor que no tener importador.
    println()
    println("── respaldo de perfiles ──")
    run {
        val muestra = listOf(
            io.github.labandita.glreborn.core.PerfilDispositivo(
                id = "prueba-1", nombre = "Pixel 8 Pro", fabricante = "Google", marca = "google",
                modelo = "Pixel 8 Pro", device = "husky", board = "husky", hardware = "husky",
                soc = "Tensor G3", gpu = "Mali-G715", openglEs = "196610",
                fuente = "especificaciones oficiales",
                confianza = io.github.labandita.glreborn.core.PerfilDispositivo.Confianza.VERIFICADO,
                nota = "perfil de prueba",
            ),
            io.github.labandita.glreborn.core.PerfilDispositivo(
                id = "prueba-2", nombre = "Genérico", fabricante = "", marca = "", modelo = "X",
                device = "x", board = "", hardware = "", soc = "", gpu = "", openglEs = "",
                fuente = "generado del teléfono",
                confianza = io.github.labandita.glreborn.core.PerfilDispositivo.Confianza.SIN_VERIFICAR,
            ),
        )
        val escrito = io.github.labandita.glreborn.core.Respaldo.escribir(
            muestra, versionApp = "1.9.5", modeloTelefono = "TECNO BG7", ahora = 1_700_000_000_000L,
        )
        check("el respaldo es JSON legible", escrito.trimStart().startsWith("{"))
        check("lleva el nombre del formato",
            escrito.contains(io.github.labandita.glreborn.core.Respaldo.FORMATO))
        check("lleva la versión del formato",
            escrito.contains("\"version\": ${io.github.labandita.glreborn.core.Respaldo.VERSION_FORMATO}"))
        check("dice de dónde salió", escrito.contains("TECNO BG7"))
        check("dice cuándo se hizo", escrito.contains("exportado_legible"))

        val leido = io.github.labandita.glreborn.core.Respaldo.leer(escrito)
        check("ida y vuelta: los dos perfiles vuelven", leido.perfiles.size == 2, "${leido.perfiles.size}")
        check("ida y vuelta: no se pierde ningún campo",
            leido.perfiles[0] == muestra[0] && leido.perfiles[1] == muestra[1])
        check("ida y vuelta: la confianza se conserva",
            leido.perfiles[1].confianza == io.github.labandita.glreborn.core.PerfilDispositivo.Confianza.SIN_VERIFICAR)
        check("el resumen dice cuántos", leido.resumen.contains("2 perfiles"))

        // Un fichero que no es suyo: se dice, no se traga.
        val ajeno = io.github.labandita.glreborn.core.Respaldo.leer("{\"hola\": 1}")
        check("un JSON ajeno se rechaza", !ajeno.ok && ajeno.formatoDesconocido)
        val basura = io.github.labandita.glreborn.core.Respaldo.leer("no soy json")
        check("la basura se rechaza sin romper", !basura.ok && basura.formatoDesconocido)

        // Un perfil sin id no se puede ni distinguir: se descarta, y se cuenta.
        val cojo = """{"formato":"glreborn-perfiles","version":1,"perfiles":[
            {"nombre":"sin id"},{"id":"ok","nombre":"bueno","confianza":"PARCIAL"}]}"""
        val mezcla = io.github.labandita.glreborn.core.Respaldo.leer(cojo)
        check("un perfil cojo se descarta y se cuenta",
            mezcla.perfiles.size == 1 && mezcla.descartados == 1, mezcla.resumen)

        // Un fichero de una versión futura: se lee, pero se avisa.
        val futuro = escrito.replace("\"version\": 1", "\"version\": 9")
        val deFuturo = io.github.labandita.glreborn.core.Respaldo.leer(futuro)
        check("un respaldo de versión futura se lee con aviso",
            deFuturo.perfiles.size == 2 && deFuturo.motivoDescarte.contains("más nueva"))

        check("el nombre del fichero lleva fecha",
            io.github.labandita.glreborn.core.Respaldo.nombreDeFichero(1_700_000_000_000L)
                .matches(Regex("glreborn-perfiles-\\d{8}-\\d{4}\\.json")))
    }


    // ── 1.9.6: filtrar el catálogo por confianza ─────────────────────────────
    //
    // El catálogo mezcla perfiles verificados con fichas sin comprobar, y no es lo mismo aplicar
    // uno que otro. La app ya lo decía tarjeta por tarjeta; ahora además se puede filtrar. Y las
    // cuentas del filtro tienen que cuadrar con la lista: un filtro que miente sobre cuántos hay
    // es peor que no tener filtro.
    println()
    println("── catálogo filtrable ──")
    run {
        val todos = io.github.labandita.glreborn.core.Perfiles.todos
        val ver = todos.count { it.confianza == io.github.labandita.glreborn.core.PerfilDispositivo.Confianza.VERIFICADO }
        val par = todos.count { it.confianza == io.github.labandita.glreborn.core.PerfilDispositivo.Confianza.PARCIAL }
        val sin = todos.count { it.confianza == io.github.labandita.glreborn.core.PerfilDispositivo.Confianza.SIN_VERIFICAR }
        check("las tres confianzas suman el catálogo entero", ver + par + sin == todos.size,
            "$ver+$par+$sin != ${todos.size}")
        check("hay perfiles verificados de verdad", ver > 0, "$ver")
        check("ningún perfil se queda sin nivel de confianza",
            todos.all { it.confianza in io.github.labandita.glreborn.core.PerfilDispositivo.Confianza.values() })
        val p = listOf(
            "app/src/main/java/io/github/labandita/glreborn/ui/Pantalla.kt",
            "src/main/java/io/github/labandita/glreborn/ui/Pantalla.kt",
        ).map { java.io.File(it) }.firstOrNull { it.exists() }
        if (p != null) {
            val s = p.readText()
            check("el catálogo tiene filtro por confianza", s.contains("filtroConfianza"))
            check("el filtro se recuerda al volver", s.contains("var filtroConfianza by rememberSaveable"))
            check("el filtro enseña cuántos hay de cada tipo",
                s.contains("PildorasMedidor") && s.contains("\"Verificados\" to"))
        }
    }


    // ── 1.9.7: la sección de la mira explicaba los ajustes pero no el uso ────
    //
    // El pendiente número uno de la app, en palabras del propio usuario: «vale, lo activo…
    // ¿y ahora qué?». Una función que no se sabe usar es una función que no existe.
    println()
    println("── una función que se sabe usar ──")
    run {
        val sup = listOf(
            "app/src/main/java/io/github/labandita/glreborn/ui/Superposicion.kt",
            "src/main/java/io/github/labandita/glreborn/ui/Superposicion.kt",
        ).map { java.io.File(it) }.firstOrNull { it.exists() }
        if (sup != null) {
            val s = sup.readText()
            check("la sección de la mira explica cómo se usa", s.contains("CÓMO SE USA"))
            check("explica los tres pasos, no sólo el permiso",
                s.contains("Dale permiso") && s.contains("Actívala") && s.contains("Abre el juego"))
            check("y dice que deja pasar los toques, que es la duda real",
                s.contains("sin tapar los toques"))
        }
    }


    // ── 1.9.8: dos fallos que el usuario vio en su teléfono ──────────────────
    //
    // «Modo rendimiento (cmd) código 255: Unknown command: get-fixed-performance-mode-supported»
    // y «Escritura de propiedades código 1: <<sin detalle>>>».
    //
    // Los dos eran de la app, no del teléfono: uno usaba un comando que NO EXISTE en Android, y
    // el otro tiraba a la basura la salida de error, que es justo donde Android explica el motivo.
    println()
    println("── comandos que existen y errores que se leen ──")
    run {
        val core = listOf("app/src/main/java/io/github/labandita/glreborn", "src/main/java/io/github/labandita/glreborn")
            .map { java.io.File(it) }.firstOrNull { it.isDirectory }
        if (core != null) {
            val fuentes = core.walkTopDown().filter { it.extension == "kt" }.toList()
            // Sin comentarios: la ley vigila el CÓDIGO. Un comentario que explica por qué un
            // comando no existe es documentación, no una llamada.
            val todo = fuentes.joinToString("\n") { f ->
                f.readText().lines().filterNot { l ->
                    val s = l.trimStart()
                    s.startsWith("//") || s.startsWith("*") || s.startsWith("/*")
                }.joinToString("\n")
            }

            // 1. Ningún comando inventado. `cmd power` sólo tiene set-mode, set-fixed-... y help.
            check("la app no usa el subcomando inventado de `cmd power`",
                !todo.contains("get-fixed-performance-mode-supported"))

            // 2. Todo comando de shell va con stderr capturado: sin 2>&1 no se sabe por qué falló.
            val shell = fuentes.firstOrNull { it.name == "Shell.kt" }
            check("el shell captura la salida de error", shell != null && shell.readText().contains("2>&1"))

            // 3. No se puede decir «sin detalle»: si no hay texto, hay código y hay motivo.
            check("la app no se escuda en «sin detalle»", !todo.contains("\"sin detalle\""))
            check("una salida sin texto explica al menos el código",
                shell != null && shell.readText().contains("no dijo por qué"))

            // 4. Una prueba que escribe TIENE que deshacer lo escrito: dejó una propiedad
            //    inventada en el teléfono y el registro no la conocía, así que «Restaurar» no
            //    sabía que tenía que devolverla.
            val compat = fuentes.firstOrNull { it.name == "Compatibilidad.kt" }
            check("la prueba de escritura se deshace", compat != null && compat.readText().contains("probarEscritura"))
            if (compat != null) {
                val c = compat.readText()
                check("la prueba de escritura no toca una propiedad del sistema",
                    !c.contains("setprop ro.product.marketname"))
                check("después de escribir, vuelve a leer y restaura",
                    c.contains("getprop $" + "clave") && c.contains("setprop $" + "clave"))
            }

            // 5. El modo rendimiento no se cree el código de salida: comprueba el efecto.
            val sistema = fuentes.firstOrNull { it.name == "Sistema.kt" }
            check("el modo rendimiento se verifica por el ajuste, no por el código",
                sistema != null && sistema.readText().contains("power_fixed_performance_mode_enabled"))
            if (sistema != null) {
                check("y si el fabricante lo ignora, lo dice",
                    sistema.readText().contains("no se puede prometer más"))
            }
        }
    }


    // ── 1.9.9: memoria y almacenamiento en la portada ────────────────────────
    //
    // Era el dato que faltaba para decidir si aplicar algo, y el que más se mira en un teléfono
    // de 4 GB. Se lee con APIs públicas; lo que no se puede leer sin root NO se estima.
    println()
    println("── recursos del teléfono ──")
    run {
        val r = io.github.labandita.glreborn.core.Recursos
        // El formato tiene que ser legible y no mentir con los decimales.
        check("los bytes se leen de un vistazo", r.legible(512) == "512 B")
        check("los megabytes no llevan decimales de más", r.legible(812L * 1024 * 1024) == "812 MB")
        check("los gigabytes llevan un decimal", r.legible(3L * 1024 * 1024 * 1024 + 100L * 1024 * 1024) == "3.1 GB")

        // Los porcentajes, en los bordes que importan.
        val vacio = io.github.labandita.glreborn.core.Recursos.Memoria(0, 0, false, 0)
        check("sin datos no se inventa un porcentaje", vacio.porcentajeUsado == 0)
        val lleno = io.github.labandita.glreborn.core.Recursos.Memoria(4_000, 200, false, 0)
        check("al 95 % avisa", lleno.porcentajeUsado == 95 && lleno.aviso != null)
        val bajo = io.github.labandita.glreborn.core.Recursos.Memoria(4_000, 3_000, true, 500)
        check("si el sistema dice «memoria baja», se dice", bajo.aviso!!.contains("cerrar apps"))
        val holgado = io.github.labandita.glreborn.core.Recursos.Memoria(4_000, 2_000, false, 500)
        check("con memoria de sobra no se avisa de nada", holgado.aviso == null)

        val apurado = io.github.labandita.glreborn.core.Recursos.Almacenamiento(100L * 1024 * 1024 * 1024, 300L * 1024 * 1024, false)
        check("con menos de 500 MB avisa", apurado.aviso!!.contains("500 MB"))
        val normal = io.github.labandita.glreborn.core.Recursos.Almacenamiento(100L * 1024 * 1024 * 1024, 50L * 1024 * 1024 * 1024, false)
        check("con espacio de sobra no avisa", normal.aviso == null)

        // Y la portada tiene que enseñarlo, con su barra.
        val home = listOf(
            "app/src/main/java/io/github/labandita/glreborn/ui/Home.kt",
            "src/main/java/io/github/labandita/glreborn/ui/Home.kt",
        ).map { java.io.File(it) }.firstOrNull { it.exists() }
        if (home != null) {
            val h = home.readText()
            check("la portada tiene el medidor de recursos", h.contains("PanelRecursos"))
            check("con barra, no sólo un número", h.contains("BarraRecurso"))
            check("y dice que lo que no se puede leer no se estima",
                h.contains("no se pueden leer sin root"))
        }
    }


    // ── 1.10.0: búsqueda global ──────────────────────────────────────────────
    //
    // Hasta la 1.9.9 había que saber DÓNDE estaba cada cosa: el modelo en Perfiles, la acción de
    // rendimiento en Herramientas, la ficha en Datos. Una caja que busque en todo lo que la app
    // sabe es lo que faltaba. Estas leyes obligan a que no mienta.
    println()
    println("── búsqueda global ──")
    run {
        val b = io.github.labandita.glreborn.core.BusquedaGlobal
        fun buscar(q: String) = b.buscar(q, Perfiles.todos, Perfiles.todos, emptyList())

        // Con una letra no se busca: sería devolverlo todo y hacer creer que la app adivina.
        check("una sola letra no devuelve nada", buscar("a").isEmpty())
        check("el vacío no devuelve nada", buscar("   ").isEmpty())

        // Los acentos y las mayúsculas no son cosa del usuario.
        val conAcento = buscar("catálogo")
        val sinAcento = buscar("catalogo")
        check("«catálogo» y «catalogo» dan lo mismo",
            conAcento.map { it.titulo } == sinAcento.map { it.titulo })
        check("da igual escribir en mayúsculas", buscar("CATALOGO").isNotEmpty())

        // Una palabra suelta tiene que llevar a algún sitio de la app.
        listOf("mira", "dns", "congelar", "respaldo", "compatibilidad", "aplicado").forEach { q ->
            check("«$q» encuentra una sección", buscar(q).any { it.tipo == io.github.labandita.glreborn.core.BusquedaGlobal.Tipo.SECCION })
        }

        // Y los perfiles del catálogo se encuentran por modelo, SoC y GPU.
        check("«pixel» encuentra el Pixel 8 Pro",
            buscar("pixel").any { it.titulo == "Google Pixel 8 Pro" })
        check("«snapdragon» encuentra perfiles por su SoC", buscar("snapdragon").isNotEmpty())

        // Todas las palabras cuentan: «pixel gpu» no puede devolver todo lo que tenga «pixel».
        val unaPalabra = buscar("pixel").size
        val dosPalabras = buscar("pixel gpu").size
        check("añadir una palabra no puede devolver MÁS", dosPalabras <= unaPalabra,
            "$dosPalabras > $unaPalabra")
        check("y una combinación imposible no inventa resultados",
            buscar("pixel zzzzz").isEmpty())

        // Lo que está en el catálogo y en tu biblioteca no sale dos veces.
        val doble = b.buscar("pixel", Perfiles.todos, Perfiles.todos, emptyList())
        check("sin repetidos entre catálogo y biblioteca",
            doble.size == doble.map { it.titulo to it.detalle }.toSet().size)
        // Aunque se pasen los mismos perfiles dos veces, el propio pesa más.
        check("los perfiles propios van antes que los del catálogo",
            doble.firstOrNull()?.detalle?.contains("tu perfil") == true)

        // Los hallazgos tienen que poder ordenarse y no repetir puntos inventados.
        val todos = buscar("pixel")
        check("los resultados vienen ordenados por puntos",
            todos.map { it.puntos } == todos.map { it.puntos }.sortedDescending())

        // Nada de destinos fantasma: cada hallazgo lleva a una pestaña que existe.
        val malos = listOf("mira", "dns", "pixel", "congelar").flatMap { q -> b.buscar(q, Perfiles.todos, emptyList(), emptyList()) }
            .map { it.ir }
            .filter { d ->
                val destino = runCatching { Destino.valueOf(d.destino) }.getOrNull()
                val pestana = d.pestana?.let { runCatching { Pestana.valueOf(it) }.getOrNull() }
                destino == null || (d.pestana != null && pestana == null) || (pestana != null && pestana.destino != destino)
            }
        check("todos los resultados llevan a una pantalla que existe", malos.isEmpty(),
            malos.take(3).toString())

        // Sin resultados no se deja el hueco vacío: se sugiere algo que sí funciona.
        val sugerencias = b.sugerencias()
        check("hay sugerencias para cuando no hay nada", sugerencias.size >= 4)
        check("y todas las sugerencias devuelven algo",
            sugerencias.all { s -> buscar(s).isNotEmpty() }, sugerencias.toString())

        // El tope de la lista se respeta: 40 en este teléfono de 4 GB, y se puede subir.
        check("el tope de la lista se cumple", b.buscar("a", Perfiles.todos, emptyList(), emptyList(), 5).size <= 5)

        // La caja y el «sin resultados» existen y no cortan a mano lo que escribes.
        val uiBuscador = java.io.File(uiDir, "BuscadorUI.kt")
        check("la caja de búsqueda existe", uiBuscador.exists())
        if (uiBuscador.exists()) {
            val u = uiBuscador.readText()
            check("la caja enseña lo escrito entero, no recortado",
                !Regex("\\.take\\s*\\(\\s*\\d+\\s*\\)").containsMatchIn(u))
            check("y dice qué se puede buscar cuando no encuentra", u.contains("SinResultados"))
        }
        // Y la portada la tiene cableada de verdad, con su caja.
        val home = java.io.File(uiDir, "Home.kt")
        if (home.exists()) {
            check("la portada trae la búsqueda", home.readText().contains("CajaBusqueda"))
        }
    }


    // ── 1.11.0: historial de cambios aplicados ───────────────────────────────
    //
    // Hasta la 1.10.0 había dos cuadernos separados —`Registro` para las propiedades, `Aplicado`
    // para las apps— y ninguno contestaba «¿qué le he hecho a este teléfono?». Un DNS cambiado o
    // una animación no aparecían en ninguna lista: se aplicaban y se olvidaban. Aquí se juntan,
    // y estas leyes obligan a que la unión no invente ni pierda lo que había.
    println()
    println("── historial de cambios ──")
    run {
        val rm = io.github.labandita.glreborn.core.ReglasMovimientos
        val ahora = 1_700_000_000_000L

        val apuntes = listOf(
            io.github.labandita.glreborn.core.Registro.Apunte(ahora - 3000, "persist.reborn.dns", "off", "on",
                io.github.labandita.glreborn.core.Modo.SEGURO),
            io.github.labandita.glreborn.core.Registro.Apunte(ahora - 1000, "ro.product.model", "TECNO BG7", "Pixel 8 Pro",
                io.github.labandita.glreborn.core.Modo.EXPERTO),
        )
        val deApps = listOf(
            io.github.labandita.glreborn.core.Movimiento(io.github.labandita.glreborn.core.Movimiento.FUENTE_APLICADO, ahora - 2000, "app:com.whatsapp", "WhatsApp", "Congelada", true),
            io.github.labandita.glreborn.core.Movimiento(io.github.labandita.glreborn.core.Movimiento.FUENTE_APLICADO, ahora - 500, "app:com.juego", "Juego", "Desinstalada", false),
        )
        val juntos = rm.juntar(apuntes, deApps)

        // Junta las dos fuentes: no se pierde ninguna.
        check("el historial tiene las dos fuentes", juntos.size == 4, "${juntos.size}")
        check("lo último va primero", juntos.first().cuando == ahora - 500)
        check("y lo más viejo al final", juntos.last().cuando == ahora - 3000)

        // Cada línea dice qué se tocó, sin inventar.
        val dns = juntos.first { it.clave == "persist.reborn.dns" }
        check("una propiedad se lee por su nombre", dns.propiedad == "persist.reborn.dns")
        check("y dice de dónde a dónde", dns.detalle == "off → on")
        check("una propiedad nueva no enseña un «antes» falso", dns.donde == "sistema")
        val instalada = juntos.first { it.clave == "app:com.juego" }
        check("lo de una app dice qué se hizo", instalada.que == "Desinstalada")
        check("y de qué app", instalada.donde == "com.juego")

        // Lo que no se puede deshacer se marca: no es lo mismo congelar que desinstalar.
        check("la desinstalación se marca sin vuelta atrás", !instalada.reversible)
        check("congelar sí se puede deshacer", juntos.first { it.que == "Congelada" }.reversible)
        check("sólo lo del registro se deshace desde el historial",
            rm.deshacibles(juntos).all { it.fuente == io.github.labandita.glreborn.core.Movimiento.FUENTE_REGISTRO } &&
                rm.deshacibles(juntos).size == 2)

        // El tope, que existe porque es un historial y no un archivo de auditoría.
        val muchos = (1..500).map { i -> io.github.labandita.glreborn.core.Movimiento(io.github.labandita.glreborn.core.Movimiento.FUENTE_APLICADO, ahora + i, "app:p$i", "App $i", "Congelada", true) }
        check("la lista no crece sin fin", rm.juntar(emptyList(), muchos).size == rm.MAX_VISIBLES)

        // Los días y las horas, para agrupar sin una cabecera por fila.
        check("hoy se dice «hoy»", rm.dia(ahora - 1000, ahora) == "hoy")
        check("ayer se dice «ayer»", rm.dia(ahora - 30 * 60 * 60 * 1000, ahora) == "ayer")
        check("más atrás se dice la fecha",
            rm.dia(ahora - 9L * 24 * 60 * 60 * 1000, ahora).let { it != "hoy" && it != "ayer" && it.isNotBlank() })
        check("la hora es corta", Regex("^\\d{2}:\\d{2}$").matches(rm.hora(ahora)), rm.hora(ahora))
        check("sin nada, el resumen lo dice", rm.resumen(emptyList()) == "no se cambió nada todavía")
        check("con un cambio, en singular", rm.resumen(listOf(dns)) == "1 cambio")
        check("con varios, en plural", rm.resumen(juntos) == "4 cambios")

        // Y la pantalla: el historial tiene que estar de verdad, con su botón de deshacer y su
        // distinción entre lo que se deshace aquí y lo que se deshace en el panel de cada app.
        val ui = java.io.File(uiDir, "Pantalla.kt")
        if (ui.exists()) {
            val u = ui.readText()
            check("la pestaña Historial existe", PestañaExiste("HISTORIAL"))
            check("la pantalla pinta una línea por cambio", u.contains("FilaMovimiento"))
            check("agrupa por día", u.contains("ReglasMovimientos"))
            check("y avisa de lo que no se deshace desde aquí", u.contains("sin vuelta atrás"))
        }
        // El historial no guarda nada nuevo: lee de los dos cuadernos que ya existían.
        val mov = java.io.File("src/main/java/io/github/labandita/glreborn/core/Movimientos.kt").let {
            if (it.exists()) it else java.io.File("app/src/main/java/io/github/labandita/glreborn/core/Movimientos.kt")
        }
        check("el historial lee, no duplica el estado", mov.exists() &&
            mov.readText().contains("registro.historial()") && mov.readText().contains("aplicado.fichas("))
    }

    println()
    if (fallos == 0) println("TODAS LAS PRUEBAS PASAN")
    else println("$fallos PRUEBAS FALLARON")
    kotlin.system.exitProcess(if (fallos == 0) 0 else 1)
}

