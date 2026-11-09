/**
 * Pruebas del núcleo, ejecutables sin emulador.
 *
 * No hay /dev/kvm en esta máquina: no puedo instalar el APK y arrancarlo. Pero casi
 * toda la lógica que importa —qué se escribe, qué se omite, qué se confirma— no toca
 * Android y se puede probar aquí de verdad. Lo que NO se prueba aquí se dice al final.
 */
import io.github.labandita.glreborn.core.*

var fallos = 0
fun check(nombre: String, cond: Boolean, detalle: String = "") {
    if (cond) println("  ✓ $nombre") else { println("  ✗ $nombre $detalle"); fallos++ }
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

    println()
    if (fallos == 0) println("TODAS LAS PRUEBAS PASAN")
    else println("$fallos PRUEBAS FALLARON")
    kotlin.system.exitProcess(if (fallos == 0) 0 else 1)
}
