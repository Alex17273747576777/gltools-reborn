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
        director = Director(this)
        inventario = Inventario(this)
        diagnostico = Diagnostico(director, inventario)
        setContent { TemaReborn { App(director, inventario, diagnostico, ::refrescar, ::pedirPermisoShizuku) } }
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

    companion object { const val REQUEST_SHIZUKU = 4201 }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun App(
    director: Director,
    inventario: Inventario,
    diagnostico: Diagnostico,
    onRefrescar: () -> Unit,
    onPedirShizuku: () -> Unit,
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
    var panelDe by remember { mutableStateOf<String?>(null) }
    var appPanel by remember { mutableStateOf<Inventario.App?>(null) }

    fun releer() {
        onRefrescar()
        estado = director.estado()
        actual = director.telefoniaActual()
        apps = inventario.todas()
        pantallaActual = if (director.hayShell()) director.pantalla().leer() else null
        congeladas = if (director.hayShell()) {
            apps.filter { director.apps().estaCongelada(it.paquete) }.map { it.paquete }.toSet()
        } else emptySet()
        lineasDiag = diagnostico.lineas()
        datosEquipo = director.compatibilidad().datosBasicos()
        pruebasCompat = director.compatibilidad().ejecutar()
        resumenCompat = director.compatibilidad().resumen(pruebasCompat)
        fichas = director.aplicado().fichas(apps.associate { it.paquete to it.nombre })
        limites = diagnostico.limites()
        if (estado == Director.Estado.SHIZUKU_SIN_PERMISO) onPedirShizuku()
    }

    LaunchedEffect(Unit) { releer() }

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
            resultado = director.pantalla().aplicar(w, h, d)
            pantallaActual = director.pantalla().leer()
        },
        onRevertirPantalla = {
            resultado = director.pantalla().revertir()
            pantallaActual = director.pantalla().leer()
        },
        onFijarRefresco = { hz -> resultado = director.pantalla().fijarRefresco(hz) },
        onCongelar = { p ->
            resultado = director.apps().congelar(p)
            congeladas = if (congeladas.contains(p)) congeladas else congeladas + p
        },
        onDescongelar = { p ->
            resultado = director.apps().descongelar(p)
            congeladas = congeladas - p
        },
        onLimpiarCache = { p -> resultado = director.sistema().limpiarCache(p) },
        onDns = { host -> resultado = director.sistema().dnsPrivado(host) },
        onAnimaciones = { e -> resultado = director.sistema().escalasAnimacion(e) },
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
        onRefrescar = { releer() },
    )
}
