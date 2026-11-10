package io.github.labandita.glreborn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.labandita.glreborn.ui.Cristal
import io.github.labandita.glreborn.ui.PanelApp
import androidx.compose.ui.text.font.FontWeight
import io.github.labandita.glreborn.core.*
import io.github.labandita.glreborn.core.AjustesSuperposicion
import io.github.labandita.glreborn.overlay.ServicioSuperposicion
import io.github.labandita.glreborn.ui.Banner
import io.github.labandita.glreborn.ui.Estado
import io.github.labandita.glreborn.ui.PantallaPrincipal
import io.github.labandita.glreborn.ui.TemaReborn
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private lateinit var director: Director
    private lateinit var inventario: Inventario
    private lateinit var diagnostico: Diagnostico

    // El informe de un cierre anterior, si lo hay. Se enseña antes que nada.
    private var informeFallo: String? = null

    // Si una lectura del teléfono falla, se dice en pantalla. No puede quedarse en un log.
    private val falloLectura = androidx.compose.runtime.mutableStateOf<String?>(null)

    /** El aviso de lectura se descarta con la última palabra: cadena vacía quiere decir «nada». */
    private fun anotarFalloLectura(texto: String) {
        falloLectura.value = texto.ifBlank { null }
    }

    private val pedirPermiso = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Shizuku ya avisa por su propio canal; aquí sólo se refresca el estado real.
        runOnUiThread { refrescar() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nada de lo que sigue puede dejar la pantalla en negro: si algo falla, se dice
        // en pantalla con el mensaje real, y la app sigue viva.
        val arranque = runCatching {
            director = Director(this)
            Avisos.preparar(this)
            inventario = Inventario(this)
            diagnostico = Diagnostico(director, inventario)
        }
        var error = arranque.exceptionOrNull()?.let { "${it.javaClass.simpleName}: ${it.message}" }

        // Si la app se cerró la última vez, aquí se enseña el parte en vez de fingir que nada.
        val pedidoMostrarFallo = intent?.getBooleanExtra(RebornApp.EXTRA_MOSTRAR_FALLO, false) == true
        informeFallo = if (pedidoMostrarFallo) RegistroFallos.leer(this) else null
        if (pedidoMostrarFallo) intent?.removeExtra(RebornApp.EXTRA_MOSTRAR_FALLO)

        runCatching { pedirNotificaciones() }

        setContent {
            TemaReborn {
                if (informeFallo != null) {
                    io.github.labandita.glreborn.ui.PantallaFallo(
                        informe = informeFallo.orEmpty(),
                        onCopiar = { texto ->
                            val portapapeles = getSystemService(android.content.ClipboardManager::class.java)
                            portapapeles?.setPrimaryClip(
                                android.content.ClipData.newPlainText("fallo", texto)
                            )
                            android.widget.Toast.makeText(
                                this@MainActivity, "Informe copiado",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                        },
                        onContinuar = {
                            RegistroFallos.borrar(this@MainActivity)
                            informeFallo = null
                        },
                    )
                } else if (error == null) {
                    App(
                        director, inventario, diagnostico, ::refrescar, ::pedirPermisoShizuku,
                        onFallo = { error = it },
                        onFalloLectura = ::anotarFalloLectura,
                        falloLectura = falloLectura.value,
                    )
                } else {
                    io.github.labandita.glreborn.ui.PantallaArrancando(error)
                }
            }
        }
    }

    private fun shizukuListo(): Boolean = try {
        Shizuku.pingBinder()
    } catch (t: Throwable) {
        false
    }

    private fun refrescar() {
        director.refrescar(shizukuListo())
    }

    private fun pedirPermisoShizuku() {
        try {
            Shizuku.requestPermission(REQUEST_SHIZUKU)
        } catch (t: Throwable) {
            // Sin Shizuku instalado, esto no hace nada; el estado lo dirá.
        }
    }

    /**
     * Pide el permiso de notificaciones (Android 13+) la primera vez.
     *
     * Es opcional: si se niega, la app funciona igual y la reversión de pantalla sigue
     * corriendo en la shell. Sólo se pierde el aviso que permite confirmar sin abrirla.
     */
    private fun pedirNotificaciones() {
        if (android.os.Build.VERSION.SDK_INT < 33) return
        val permiso = android.Manifest.permission.POST_NOTIFICATIONS
        if (checkSelfPermission(permiso) == android.content.pm.PackageManager.PERMISSION_GRANTED) return
        try {
            requestPermissions(arrayOf(permiso), REQUEST_NOTIFICACIONES)
        } catch (t: Throwable) {
            // Si el sistema no deja pedirlo, se sigue sin notificaciones.
        }
    }

    companion object {
        const val REQUEST_SHIZUKU = 4201
        const val REQUEST_NOTIFICACIONES = 4203
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun App(
    director: Director,
    inventario: Inventario,
    diagnostico: Diagnostico,
    onRefrescar: () -> Unit,
    onPedirShizuku: () -> Unit,
    onFallo: (String) -> Unit = {},
    onFalloLectura: (String) -> Unit = {},
    falloLectura: String? = null,
) {
    // Dentro de la composición el aviso se anota por la lambda que llega de la Activity.
    val anotarFalloLectura: (String) -> Unit = onFalloLectura
    var estado by remember { mutableStateOf(Director.Estado.SIN_VIA) }
    var perfil by remember { mutableStateOf<PerfilDispositivo?>(null) }
    var resultado by remember { mutableStateOf<Resultado?>(null) }
    var actual by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var apps by remember { mutableStateOf<List<Inventario.App>>(emptyList()) }
    var lineasDiag by remember { mutableStateOf<List<Diagnostico.Linea>>(emptyList()) }
    var limites by remember { mutableStateOf<List<String>>(emptyList()) }
    var modo by remember { mutableStateOf(director.modo) }
    var superActiva by remember { mutableStateOf<Supercomputadora?>(null) }
    var pantallaActual by remember { mutableStateOf<AjustesPantalla.Actual?>(null) }
    var congeladas by remember { mutableStateOf<Set<String>>(emptySet()) }
    var datosEquipo by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var pruebasCompat by remember { mutableStateOf<List<Compatibilidad.Prueba>>(emptyList()) }
    var resumenCompat by remember { mutableStateOf("") }

    // ── Punto de mira y FPS. Vive en el Activity porque sobrevive a los cambios de pantalla.
    val contextoSuperposicion = LocalContext.current.applicationContext
    var ajustesSuperposicion by remember { mutableStateOf(AjustesSuperposicion.leer(contextoSuperposicion)) }
    var superposicionActiva by remember { mutableStateOf(false) }
    var tienePermisoSuperposicion by remember {
        mutableStateOf(ServicioSuperposicion.puedeDibujar(contextoSuperposicion))
    }
    // Al volver de Ajustes hay que releer el permiso: el usuario pudo concederlo o quitarlo.
    val ciclo = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(ciclo) {
        val observador = androidx.lifecycle.LifecycleEventObserver { _, evento ->
            if (evento == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                tienePermisoSuperposicion =
                    ServicioSuperposicion.puedeDibujar(contextoSuperposicion)
            }
        }
        ciclo.lifecycle.addObserver(observador)
        onDispose { ciclo.lifecycle.removeObserver(observador) }
    }
    var fichas by remember { mutableStateOf<List<Ficha>>(emptyList()) }
    var datosSonda by remember { mutableStateOf<Sonda.Datos?>(null) }
    var edicionesTop by remember { mutableStateOf<List<Datos.Edicion>>(emptyList()) }
    var edicionElegida by remember { mutableStateOf("") }
    var sistemasTop by remember { mutableStateOf<List<Datos.Sistema>>(emptyList()) }
    var catalogoDispositivos by remember { mutableStateOf<List<Datos.Dispositivo>>(emptyList()) }
    var socs by remember { mutableStateOf<List<Datos.Soc>>(emptyList()) }
    var manifiesto by remember { mutableStateOf<String?>(null) }
    var fichasMobolist by remember { mutableStateOf<List<Datos.Ficha>>(emptyList()) }
    var biblioteca by remember { mutableStateOf<List<PerfilDispositivo>>(emptyList()) }
    val contexto = LocalContext.current
    var panelDe by remember { mutableStateOf<String?>(null) }
    var appPanel by remember { mutableStateOf<Inventario.App?>(null) }
    val alcance = rememberCoroutineScope()

    /**
     * Relee el estado del teléfono FUERA del hilo de interfaz.
     *
     * Esto antes corría entero en el hilo principal: una llamada a shell por cada app
     * instalada para preguntar si estaba congelada (con 100 apps y 8 s de espera cada
     * una, si el proceso se atasca) más las nueve pruebas de compatibilidad. En un
     * teléfono modesto eso no termina nunca: Android mata la app por ANR y «no abre».
     * Ahora: lo de Android (instantáneo) se lee aquí, y lo de la shell va aparte, con
     * una sola consulta y sin bloquear la pantalla.
     */
    /** Un fallo que no debe cerrar la app, pero que no se puede perder. */
    fun registrarFalloSilencioso(etiqueta: String, t: Throwable) {
        anotarFalloLectura("$etiqueta · ${t.javaClass.simpleName}: ${t.message}")
        runCatching { RegistroFallos.guardar(contexto, t, "lectura: $etiqueta") }
    }

    fun <T> proteger(etiqueta: String, bloque: () -> T): T? =
        try {
            bloque()
        } catch (t: Throwable) {
            registrarFalloSilencioso(etiqueta, t)
            null
        }

    fun <T> protegerCon(etiqueta: String, porDefecto: T, bloque: () -> T): T =
        proteger(etiqueta, bloque) ?: porDefecto

    fun protegerLectura(etiqueta: String, bloque: () -> Unit) {
        try {
            bloque()
        } catch (t: Throwable) {
            registrarFalloSilencioso(etiqueta, t)
        }
    }


    /**
     * Relee el estado del teléfono.
     *
     * CADA lectura va protegida por separado, y esto no es paranoia: antes había tres sentencias
     * sin protección (`diagnostico.lineas()`, `diagnostico.limites()`, `datosBasicos()` y
     * `aplicado().fichas()`). Una sola excepción en cualquiera de ellas —y en un HIOS con
     * Shizuku a medias las hay— se lanzaba dentro de un `LaunchedEffect` y **cerraba la app**.
     * Un fallo leyendo el diagnóstico no puede impedir que la pantalla principal se dibuje.
     */
    fun releer() {
        protegerLectura("refrescar") { onRefrescar() }
        estado = protegerCon("estado", Director.Estado.SIN_VIA) { director.estado() }
        apps = proteger("apps") { inventario.todas() } ?: emptyList()
        lineasDiag = proteger("diagnóstico") { diagnostico.lineas() } ?: emptyList()
        limites = proteger("límites") { diagnostico.limites() } ?: emptyList()
        datosEquipo = proteger("datos básicos") { director.compatibilidad().datosBasicos() }
            ?: emptyList()
        fichas = proteger("aplicado") {
            director.aplicado().fichas(apps.associate { it.paquete to it.nombre })
        } ?: emptyList()
        biblioteca = proteger("biblioteca") { director.biblioteca().leer() } ?: emptyList()
        if (estado == Director.Estado.SHIZUKU_SIN_PERMISO) {
            protegerLectura("pedir permiso") { onPedirShizuku() }
        }
    }

    /** Lo que necesita la shell. Corre en otro hilo y jamás tumba la pantalla. */
    suspend fun releerShell() = withContext(Dispatchers.IO) {
        val congeladasLeidas = runCatching {
            director.conShell { director.apps().congeladas() } ?: emptySet()
        }.getOrElse { anotarFalloLectura("congeladas · ${it.message}"); emptySet() }
        val pantalla = runCatching { director.conShell { director.pantalla().leer() } }
            .getOrElse { anotarFalloLectura("pantalla · ${it.message}"); null }
        val compat: List<Compatibilidad.Prueba> = runCatching {
            director.conShell { director.compatibilidad().ejecutar() }
        }.getOrElse { anotarFalloLectura("compatibilidad · ${it.message}"); emptyList() }
            ?: emptyList()
        val sonda = runCatching { director.conShell { director.compatibilidad().sondar() } }
            .getOrElse { anotarFalloLectura("sonda · ${it.message}"); null }
        withContext(Dispatchers.Main) {
            congeladas = congeladasLeidas
            pantallaActual = pantalla
            pruebasCompat = compat
            resumenCompat = director.compatibilidad().resumen(compat)
            datosSonda = sonda
        }
    }

    // Un LaunchedEffect que lanza una excepción tumba la app entera: aquí NADA sale sin red.
    LaunchedEffect(Unit) { runCatching { releer() } }

    // Los datos del APK se leen una vez, en segundo plano: son varios MB y no pueden
    // bloquear el primer pintado. Mientras tanto la pantalla ya está en pie.
    LaunchedEffect(Unit) {
      runCatching {
        withContext(Dispatchers.IO) {
            val ctx = contexto
            val eds = Datos.ediciones(ctx)
            val disp = Datos.dispositivos(ctx)
            val soc = Datos.socs(ctx)
            val fichas = Datos.fichas(ctx)
            val man = Datos.manifiesto(ctx)?.let { m ->
                runCatching {
                    val bloques = m.optJSONArray("bloques")
                    buildString {
                        appendLine("«${m.optString("que_es")}»")
                        appendLine()
                        if (bloques != null) {
                            for (i in 0 until bloques.length()) {
                                val b = bloques.optJSONObject(i) ?: continue
                                appendLine("· ${b.optString("id").uppercase()} — ${b.optString("que")}")
                                appendLine("  fuente: ${b.optString("fuente")} ${b.optString("url")}")
                            }
                        }
                        val peso = m.optJSONObject("peso")
                        if (peso != null) {
                            appendLine()
                            appendLine("Peso total: ${peso.optInt("total") / 1048576.0} MB")
                        }
                        appendLine()
                        appendLine(m.optString("nota"))
                    }
                }.getOrNull()
            }
            val primera = eds.firstOrNull()?.edicion.orEmpty()
            val sis = if (primera.isNotBlank()) Datos.top500(ctx, primera) else emptyList()
            withContext(Dispatchers.Main) {
                edicionesTop = eds
                catalogoDispositivos = disp
                socs = soc
                fichasMobolist = fichas
                manifiesto = man
                if (edicionElegida.isBlank() && primera.isNotBlank()) {
                    edicionElegida = primera
                    sistemasTop = sis
                }
            }
        }
      }.onFailure {
            // Los datos del APK son 14 MB de JSON: si uno viene corrupto, se dice y se sigue.
            anotarFalloLectura("datos del APK · ${it.javaClass.simpleName}: ${it.message}")
      }
    }
    LaunchedEffect(estado) {
        if (director.hayShell()) runCatching { releerShell() }
    }

    // El panel de cada app: hoja de cristal a pantalla completa.
    panelDe?.let { paquete ->
        val contexto = LocalContext.current
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { panelDe = null },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Cristal.fondoMedio,
            dragHandle = null,
        ) {
            androidx.compose.foundation.layout.Column(
                androidx.compose.ui.Modifier
                    .fillMaxHeight(0.94f)
                    .padding(top = 8.dp)
            ) {
                Row(
                    androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Panel de la app", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = Cristal.texto)
                    Text(
                        "Cerrar",
                        fontSize = 13.sp, color = Cristal.violeta, fontWeight = FontWeight.Medium,
                        modifier = androidx.compose.ui.Modifier.clickable { panelDe = null },
                    )
                }
                PanelApp(
                    context = contexto,
                    ficha = director.aplicado().de(paquete),
                    app = appPanel,
                    telefono = actual,
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                )
            }
        }
    }

    PantallaPrincipal(
        falloLectura = falloLectura,
        onDescartarFalloLectura = { onFalloLectura("") },
        director = director,
        estadoVia = estado,
        perfilActivo = perfil,
        resultado = resultado,
        actual = actual,
        apps = apps,
        lineasDiag = lineasDiag,
        limites = limites,
        modo = modo,
        superActiva = superActiva,
        onModo = {
            modo = director.cambiarModo()
            resultado = Resultado(
                true, Via.NINGUNA,
                if (modo == Modo.SEGURO)
                    "Modo seguro: sólo modelo y gráficos. Si algo no cuadra al releer, se deshace solo."
                else
                    "Modo experto: también kernel, API y build. Cada escritura se anota antes de " +
                        "hacerse y se deshace sola si la app muere a mitad. La identidad del teléfono " +
                        "sigue bloqueada.",
            )
        },
        onSimular = { s ->
            superActiva = s
            resultado = director.simular(s)
            actual = director.telefoniaActual()
        },
        onDeshacer = {
            resultado = director.restaurar()
            actual = director.telefoniaActual()
        },
        pantallaActual = pantallaActual,
        congeladas = congeladas,
        hayShell = director.hayShell(),
        onFijarResolucion = { w, h, d ->
            resultado = proteger(onFallo) { director.pantalla().aplicar(w, h, d) }
            if (resultado?.ok == true) {
                Avisos.reversionPendiente(
                    contexto,
                    15,
                    "Resolución ${w}x$h aplicada. Si no confirmas en 15 s, el teléfono vuelve solo.",
                )
            }
            alcance.launch { releerShell() }
        },
        onRevertirPantalla = {
            resultado = proteger(onFallo) { director.pantalla().revertir() }
            Avisos.retirar(contexto)
            alcance.launch { releerShell() }
        },
        onFijarRefresco = { hz ->
            resultado = proteger(onFallo) { director.pantalla().fijarRefresco(hz) }
        },
        onCongelar = { p ->
            resultado = proteger(onFallo) { director.apps().congelar(p) }
            congeladas = if (congeladas.contains(p)) congeladas else congeladas + p
        },
        onDescongelar = { p ->
            resultado = proteger(onFallo) { director.apps().descongelar(p) }
            congeladas = congeladas - p
        },
        onLimpiarCache = { p ->
            resultado = proteger(onFallo) { director.sistema().limpiarCache(p) }
        },
        onDns = { host ->
            resultado = proteger(onFallo) { director.sistema().dnsPrivado(host) }
        },
        onAnimaciones = { e ->
            resultado = proteger(onFallo) { director.sistema().escalasAnimacion(e) }
        },
        datosEquipo = datosEquipo,
        pruebasCompat = pruebasCompat,
        resumenCompat = resumenCompat,
        onProbarCompatibilidad = {
            pruebasCompat = director.compatibilidad().ejecutar()
            resumenCompat = director.compatibilidad().resumen(pruebasCompat)
        },
        fichas = fichas,
        onPanel = { paquete ->
            appPanel = apps.firstOrNull { it.paquete == paquete }
            panelDe = paquete
        },
        datosSonda = datosSonda,
        edicionesTop = edicionesTop,
        edicionElegida = edicionElegida,
        onEdicion = { ed ->
            edicionElegida = ed
            alcance.launch {
                sistemasTop = withContext(Dispatchers.IO) { Datos.top500(contexto, ed) }
            }
        },
        sistemasTop = sistemasTop,
        catalogoDispositivos = catalogoDispositivos,
        socs = socs,
        manifiesto = manifiesto,
        fichasMobolist = fichasMobolist,
        accionesRendimiento = Rendimiento.disponibles(android.os.Build.VERSION.SDK_INT),
        sinRootRendimiento = Rendimiento.sinRoot,
        sdkTelefono = android.os.Build.VERSION.SDK_INT,
        onRendimiento = { accion ->
            if (accion != null) {
                alcance.launch {
                    resultado = withContext(Dispatchers.IO) {
                        proteger(onFallo) { director.rendimiento().aplicar(accion) }
                    }
                }
            }
        },
        onRevertirRendimiento = { accion ->
            alcance.launch {
                resultado = withContext(Dispatchers.IO) {
                    proteger(onFallo) { director.rendimiento().revertir(accion) }
                }
            }
        },
        biblioteca = biblioteca,
        onSondar = { alcance.launch { releerShell() } },
        onGuardarPerfil = { p ->
            director.biblioteca().guardar(p)
            biblioteca = director.biblioteca().leer()
        },
        onGuardarVarios = { lista ->
            director.biblioteca().guardarVarios(lista)
            biblioteca = director.biblioteca().leer()
        },
        onBorrarPerfil = { id ->
            director.biblioteca().borrar(id)
            biblioteca = director.biblioteca().leer()
        },
        onSeleccionarPerfil = { p -> perfil = p },
        onPerfil = { perfil = it },
        onAplicar = {
            val p = perfil
            resultado = if (p == null) {
                Resultado(false, Via.NINGUNA, "Elige un perfil primero.")
            } else director.aplicar(p)
            actual = director.telefoniaActual()
        },
        onRestaurar = {
            resultado = director.restaurar()
            actual = director.telefoniaActual()
        },
        onRefrescar = {
            releer()
            alcance.launch { releerShell() }
        },
        ajustesSuperposicion = ajustesSuperposicion,
        superposicionActiva = superposicionActiva,
        tienePermisoSuperposicion = tienePermisoSuperposicion,
        onCambiarSuperposicion = { nuevos ->
            val saneados = nuevos.saneado()
            ajustesSuperposicion = saneados
            AjustesSuperposicion.guardar(contextoSuperposicion, saneados)
            if (superposicionActiva) {
                // Se aplica en caliente: no hay que apagar y encender para ver el cambio.
                ServicioSuperposicion.aplicarEnCaliente(contextoSuperposicion, saneados)
            }
            if (!saneados.debeEstarActivo() && superposicionActiva) {
                ServicioSuperposicion.detener(contextoSuperposicion)
                superposicionActiva = false
            }
        },
        onPermisoSuperposicion = {
            // Android exige que el usuario lo conceda a mano: se abre la pantalla exacta.
            val i = android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:" + contextoSuperposicion.packageName),
            )
            try {
                contextoSuperposicion.startActivity(
                    i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                // Si el fabricante esconde esa pantalla, se dice en vez de fingir.
                resultado = Resultado(
                    ok = false,
                    via = Via.NINGUNA,
                    detalle = "Este teléfono no abre la pantalla de superposiciones.",
                    lineas = listOf(
                        "Búscala a mano: Ajustes → Apps → Acceso especial → Dibujar sobre otras apps.",
                    ),
                )
            }
        },
        onActivarSuperposicion = {
            if (ServicioSuperposicion.puedeDibujar(contextoSuperposicion)) {
                ServicioSuperposicion.iniciar(contextoSuperposicion, ajustesSuperposicion)
                superposicionActiva = true
            } else {
                tienePermisoSuperposicion = false
            }
        },
        onDesactivarSuperposicion = {
            ServicioSuperposicion.detener(contextoSuperposicion)
            superposicionActiva = false
        },
        onMoverSuperposicion = {
            ServicioSuperposicion.activarArrastre(contextoSuperposicion)
            android.widget.Toast.makeText(
                contextoSuperposicion,
                "Arrastra el punto 3 segundos",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        },
        banner = Estado.banner(
            conRoot = estado == Director.Estado.ROOT_LISTO,
            conShizuku = estado == Director.Estado.SHIZUKU_LISTO ||
                estado == Director.Estado.SHIZUKU_SIN_PERMISO,
            conPermiso = estado == Director.Estado.SHIZUKU_LISTO ||
                estado == Director.Estado.ROOT_LISTO,
            modo = modo,
        ),
        puedeHacer = Estado.puede(
            conRoot = estado == Director.Estado.ROOT_LISTO,
            conShizuku = estado == Director.Estado.SHIZUKU_LISTO ||
                estado == Director.Estado.SHIZUKU_SIN_PERMISO,
            conPermiso = estado == Director.Estado.SHIZUKU_LISTO ||
                estado == Director.Estado.ROOT_LISTO,
        ),
        avisosHome = Estado.avisos(
            conVia = estado == Director.Estado.SHIZUKU_LISTO ||
                estado == Director.Estado.ROOT_LISTO,
            modo = modo,
            resumenCompat = resumenCompat,
            tienePerfil = perfil != null,
            irreversibleAplicado = false,
            sinApps = apps.isEmpty(),
        ),
        version = "1.9.0",
    )
}

/**
 * Envuelve una acción que habla con la shell.
 *
 * Si revienta, la app NO se cierra: se convierte en un resultado rojo con el motivo.
 * Esto es lo que faltaba para que una excepción de una acción dejara la app muerta.
 */
private inline fun proteger(onFallo: (String) -> Unit, bloque: () -> Resultado): Resultado =
    try {
        bloque()
    } catch (t: Throwable) {
        val texto = "${t.javaClass.simpleName}: ${t.message ?: "sin mensaje"}"
        onFallo(texto)
        Resultado(false, Via.NINGUNA, "La acción falló: $texto")
    }