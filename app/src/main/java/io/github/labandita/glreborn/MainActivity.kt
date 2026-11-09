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
import io.github.labandita.glreborn.ui.PantallaPrincipal
import io.github.labandita.glreborn.ui.TemaReborn
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private lateinit var director: Director
    private lateinit var inventario: Inventario
    private lateinit var diagnostico: Diagnostico

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

        runCatching { pedirNotificaciones() }

        setContent {
            TemaReborn {
                if (error == null) {
                    App(director, inventario, diagnostico, ::refrescar, ::pedirPermisoShizuku) {
                        error = it
                    }
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
) {
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
    var fichas by remember { mutableStateOf<List<Ficha>>(emptyList()) }
    var datosSonda by remember { mutableStateOf<Sonda.Datos?>(null) }
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
    fun releer() {
        onRefrescar()
        estado = director.estado()
        // Un fallo al leer la lista de apps no puede impedir que la app arranque.
        apps = try { inventario.todas() } catch (t: Throwable) { emptyList() }
        lineasDiag = diagnostico.lineas()
        limites = diagnostico.limites()
        datosEquipo = director.compatibilidad().datosBasicos()
        fichas = director.aplicado().fichas(apps.associate { it.paquete to it.nombre })
        biblioteca = try { director.biblioteca().leer() } catch (t: Throwable) { emptyList() }
        if (estado == Director.Estado.SHIZUKU_SIN_PERMISO) onPedirShizuku()
    }

    /** Lo que necesita la shell. Corre en otro hilo y jamás tumba la pantalla. */
    suspend fun releerShell() = withContext(Dispatchers.IO) {
        val congeladasLeidas = director.conShell { director.apps().congeladas() } ?: emptySet()
        val pantalla = director.conShell { director.pantalla().leer() }
        val compat = director.conShell { director.compatibilidad().ejecutar() } ?: emptyList()
        val sonda = director.conShell { director.compatibilidad().sondar() }
        withContext(Dispatchers.Main) {
            congeladas = congeladasLeidas
            pantallaActual = pantalla
            pruebasCompat = compat
            resumenCompat = director.compatibilidad().resumen(compat)
            datosSonda = sonda
        }
    }

    LaunchedEffect(Unit) { releer() }
    LaunchedEffect(estado) { if (director.hayShell()) releerShell() }

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