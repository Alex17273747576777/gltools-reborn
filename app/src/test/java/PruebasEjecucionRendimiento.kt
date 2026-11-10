import io.github.labandita.glreborn.core.*
import io.github.labandita.glreborn.shizuku.Shell

/** Pruebas de la ruta de producción con shell simulada, sin ejecutar órdenes en el host. */
fun pruebasEjecucionRendimiento() {
    val aot = Rendimiento.catalogo.first { it.id == "aot" }
    val ordenes = mutableListOf<String>()
    var lista = true
    var sdk = 34
    var responder: (String) -> Shell.Salida? = { Shell.Salida(0, "1") }
    val modos = mutableListOf<Boolean>()
    fun ejecutor() = EjecutorRendimiento(
        disponible = { lista },
        correr = { orden -> ordenes += orden; responder(orden) },
        sdk = sdk, aplicado = null,
        modoFijo = { activar -> modos += activar; Resultado(true, Via.SHIZUKU, "solicitud simulada") },
    )
    fun reset() { ordenes.clear(); modos.clear(); lista = true; sdk = 34; responder = { Shell.Salida(0, "1") } }

    reset()
    check("la ruta real de comprobación sustituye el paquete elegido",
        ejecutor().aplicar(aot, "com.example.app").ok && ordenes.size == 2 &&
            ordenes.last() == aot.comprobar.replace("%PAQUETE%", "com.example.app"))
    check("nunca llega un marcador de paquete a la shell", ordenes.none { "%PAQUETE%" in it })
    for (paquete in listOf<String?>(null, "", "com.x;reboot", "com.x\nreboot", "com.x$(id)", "com.x'", "--user", "com..x", "com.x/Actividad", "com.é")) {
        reset()
        check("aplicar rechaza paquete inválido sin shell: $paquete", !ejecutor().aplicar(aot, paquete).ok && ordenes.isEmpty())
        check("revertir rechaza paquete inválido sin shell: $paquete", !ejecutor().revertir(aot, paquete).ok && ordenes.isEmpty())
    }
    check("identificador android y paquetes con guion bajo se admiten",
        Rendimiento.paqueteValido("android") && Rendimiento.paqueteValido("com.example_app"))
    reset(); responder = { Shell.Salida(1, "1") }
    check("aplicar no confirma con texto coincidente y código de error",
        !ejecutor().aplicar(aot, "com.x").ok && ordenes.size == 1)
    reset(); responder = { if (it == aot.aplicar.first().replace("%PAQUETE%", "com.x")) Shell.Salida(0, "") else Shell.Salida(1, "1") }
    check("fallo de lectura no pasa aunque imprima el valor esperado", !ejecutor().aplicar(aot, "com.x").ok)
    reset(); responder = { null }
    check("sin respuesta no continúa la aplicación", !ejecutor().aplicar(aot, "com.x").ok && ordenes.size == 1)
    reset(); responder = { Shell.Salida(-1, "1", true) }
    check("timeout no cuenta como efecto confirmado", !ejecutor().aplicar(aot, "com.x").ok && ordenes.size == 1)
    reset(); responder = { Shell.Salida(0, "1", true) }
    check("timeout explícito prima incluso ante código cero", !ejecutor().aplicar(aot, "com.x").ok)
    reset(); responder = { Shell.Salida(1, "Permission denied") }
    check("revertir ya no da éxito incondicional", !ejecutor().revertir(aot, "com.x").ok)
    reset(); responder = { null }
    check("revertir sin respuesta falla", !ejecutor().revertir(aot, "com.x").ok)
    reset(); responder = { Shell.Salida(-1, "", true) }
    check("revertir con timeout falla", !ejecutor().revertir(aot, "com.x").ok)
    reset()
    val reversa = ejecutor().revertir(aot, "com.x")
    check("revertir aceptado no afirma restauración del estado original", reversa.ok && reversa.detalle.contains("No se verificó"))
    check("reversión usa el paquete validado", ordenes == Rendimiento.revertir(aot, "com.x"))
    reset(); lista = false
    check("sin vía ni aplicar ni revertir ejecutan órdenes",
        !ejecutor().aplicar(aot, "com.x").ok && !ejecutor().revertir(aot, "com.x").ok && ordenes.isEmpty())
    reset(); sdk = 25
    check("la versión se valida también fuera de la UI",
        !ejecutor().aplicar(aot, "com.x").ok && !ejecutor().revertir(aot, "com.x").ok && ordenes.isEmpty())
    reset(); val root = aot.copy(necesitaRoot = true)
    check("ambas rutas bloquean una acción que exige root",
        !ejecutor().aplicar(root, "com.x").ok && !ejecutor().revertir(root, "com.x").ok && ordenes.isEmpty())
    reset(); val varias = aot.copy(aplicar = listOf("primera %PAQUETE%", "segunda %PAQUETE%"))
    responder = { Shell.Salida(1, "falló") }
    check("se detiene la secuencia de aplicación al primer error",
        !ejecutor().aplicar(varias, "com.x").ok && ordenes.size == 1)
    reset(); val variasReversiones = aot.copy(revertir = listOf("primera %PAQUETE%", "segunda %PAQUETE%"))
    responder = { Shell.Salida(1, "falló") }
    check("se detiene la reversión al primer error",
        !ejecutor().revertir(variasReversiones, "com.x").ok && ordenes.size == 1)
    reset(); val soloLectura = aot.copy(aplicar = listOf("orden"), comprobar = "leer %PAQUETE%", revertir = listOf("volver"))
    check("un paquete usado sólo en comprobación también se valida",
        !ejecutor().aplicar(soloLectura, "x;id").ok && ordenes.isEmpty())
    reset(); val fijo = Rendimiento.catalogo.first { it.id == "rendimiento-fijo" }
    check("modo fijo conserva la ruta única de solicitud explícita",
        ejecutor().aplicar(fijo).ok && ejecutor().revertir(fijo).ok && modos == listOf(true, false) && ordenes.isEmpty())
    reset(); sdk = 29
    check("API 29 nunca envía una solicitud de modo fijo",
        !ejecutor().aplicar(fijo).ok && modos.isEmpty() && ordenes.isEmpty())
}
