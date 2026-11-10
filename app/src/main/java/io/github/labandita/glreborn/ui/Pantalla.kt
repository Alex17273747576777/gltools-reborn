package io.github.labandita.glreborn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.labandita.glreborn.core.*

@Composable
fun PantallaPrincipal(
    director: Director,
    estadoVia: Director.Estado,
    perfilActivo: PerfilDispositivo?,
    resultado: Resultado?,
    actual: Map<String, String>,
    apps: List<Inventario.App>,
    lineasDiag: List<Diagnostico.Linea>,
    limites: List<String>,
    modo: Modo,
    superActiva: Supercomputadora?,
    onModo: () -> Unit,
    onSimular: (Supercomputadora) -> Unit,
    onDeshacer: () -> Unit,
    pantallaActual: AjustesPantalla.Actual?,
    congeladas: Set<String>,
    hayShell: Boolean,
    onFijarResolucion: (Int, Int, Int) -> Unit,
    onRevertirPantalla: () -> Unit,
    onFijarRefresco: (Int) -> Unit,
    onCongelar: (String) -> Unit,
    onDescongelar: (String) -> Unit,
    onLimpiarCache: (String) -> Unit,
    onDns: (String) -> Unit,
    onAnimaciones: (String) -> Unit,
    datosEquipo: List<Pair<String, String>>,
    pruebasCompat: List<Compatibilidad.Prueba>,
    resumenCompat: String,
    onProbarCompatibilidad: () -> Unit,
    fichas: List<Ficha>,
    movimientos: List<Movimiento>,
    onDeshacerRegistro: () -> Unit,
    onPanel: (String) -> Unit,
    datosSonda: Sonda.Datos?,
    edicionesTop: List<Datos.Edicion>,
    edicionElegida: String,
    onEdicion: (String) -> Unit,
    sistemasTop: List<Datos.Sistema>,
    catalogoDispositivos: List<Datos.Dispositivo>,
    socs: List<Datos.Soc>,
    manifiesto: String?,
    fichasMobolist: List<Datos.Ficha>,
    accionesRendimiento: List<Rendimiento.Accion>,
    sinRootRendimiento: List<Pair<String, String>>,
    sdkTelefono: Int,
    onRendimiento: (Rendimiento.Accion?) -> Unit,
    onRevertirRendimiento: (Rendimiento.Accion) -> Unit,
    biblioteca: List<PerfilDispositivo>,
    onSondar: () -> Unit,
    onGuardarPerfil: (PerfilDispositivo) -> Unit,
    onGuardarVarios: (List<PerfilDispositivo>) -> Unit,
    onBorrarPerfil: (String) -> Unit,
    onSeleccionarPerfil: (PerfilDispositivo) -> Unit,
    onPerfil: (PerfilDispositivo) -> Unit,
    onAplicar: () -> Unit,
    onRestaurar: () -> Unit,
    onRefrescar: () -> Unit,
    banner: Banner,
    puedeHacer: String,
    avisosHome: List<Estado.Par>,
    version: String,
    ajustesSuperposicion: AjustesSuperposicion,
    superposicionActiva: Boolean,
    tienePermisoSuperposicion: Boolean,
    onCambiarSuperposicion: (AjustesSuperposicion) -> Unit,
    onPermisoSuperposicion: () -> Unit,
    onActivarSuperposicion: () -> Unit,
    onDesactivarSuperposicion: () -> Unit,
    onMoverSuperposicion: () -> Unit,
    falloLectura: String? = null,
    onDescartarFalloLectura: () -> Unit = {},
    // La búsqueda no entra por parámetro: su estado vive aquí (ver arriba).
) {
    var destino by rememberSaveable { mutableStateOf(Destino.HOME) }
    // La búsqueda vive aquí porque aquí vive la navegación: un resultado tiene que poder mover
    // el destino y la pestaña, y eso sólo se hace desde dentro.
    var busqueda by rememberSaveable { mutableStateOf("") }
    val hallazgos = remember(busqueda, biblioteca, catalogoDispositivos) {
        if (busqueda.length < 2) emptyList()
        else io.github.labandita.glreborn.core.BusquedaGlobal.buscar(
            consulta = busqueda,
            perfiles = io.github.labandita.glreborn.core.Perfiles.todos,
            biblioteca = biblioteca,
            dispositivos = catalogoDispositivos,
        )
    }
    // Qué confianza se está mostrando en el catálogo (0 = todas).
    var filtroConfianza by rememberSaveable { mutableStateOf(0) }
    var pestana by rememberSaveable { mutableStateOf<Pestana?>(null) }

    // Estado de la sección Aprender. Vive aquí, no dentro del LazyColumn: la lambda de
    // contenido de un LazyColumn no es @Composable y `remember` no compila ahí dentro.
    var consulta by rememberSaveable { mutableStateOf("") }
    var textoPegado by rememberSaveable { mutableStateOf("") }
    var extraido by remember { mutableStateOf<Extractor.Extraido?>(null) }
    var candidatos by remember { mutableStateOf<List<Distribuida.Candidato>>(emptyList()) }
    var consultaCatalogo by remember { mutableStateOf("") }
    var consultaSoc by remember { mutableStateOf("") }
    var consultaFicha by remember { mutableStateOf("") }
    var marcadas by remember { mutableStateOf(setOf<String>()) }
    val catalogoPerfiles = remember(biblioteca) {
        Distribuida.unir(Perfiles.todos, biblioteca)
    }

    Column(Modifier.fillMaxSize()) {
        // Cabecera compacta: marca, modo y estado. El banner grande vive en Home.
        Row(
            Modifier
                .fillMaxWidth()
                .background(Cristal.fondoArriba)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "GLTOOLS",
                    fontSize = 10.sp, letterSpacing = 3.sp, color = Cristal.cian,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    when (destino) {
                        Destino.HOME -> "REBORN"
                        else -> destino.titulo.uppercase()
                    },
                    fontSize = 19.sp, letterSpacing = 0.5.sp, color = Cristal.texto,
                    fontWeight = FontWeight.Light,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChipModo(modo, onModo)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Cristal.superficie)
                        .clickable { onRefrescar() }
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                ) {
                    Iconos.Refrescar(tamano = 15.dp, color = Cristal.textoSuave)
                }
            }
        }

        if (Navegacion.tienePestanas(destino)) {
            FilaPestanas(destino, pestana, onPestana = { pestana = it })
            Spacer(Modifier.height(10.dp))
        }

        Box(Modifier.weight(1f)) {
        when (destino) {
            Destino.HOME -> PantallaHome(
                banner = banner,
                puede = puedeHacer,
                avisos = avisosHome,
                perfilActivo = perfilActivo,
                modo = modo,
                actual = actual,
                datosSonda = datosSonda,
                resumenCompat = resumenCompat,
                apps = apps.size,
                congeladas = congeladas.size,
                fichas = fichasMobolist.size,
                ediciones = edicionesTop.size,
                sistemas = edicionesTop.sumOf { it.sistemas },
                acciones = accionesRendimiento.size,
                version = version,
                onIrA = { d, p ->
                    destino = d
                    pestana = p ?: Navegacion.inicial(d)
                },
                onAplicar = onAplicar,
                onRestaurar = onRestaurar,
                onModo = onModo,
                falloLectura = falloLectura,
                onDescartarFalloLectura = onDescartarFalloLectura,
                busqueda = busqueda,
                onBuscar = { busqueda = it },
                hallazgos = hallazgos,
                onIrABusqueda = { d ->
                    runCatching {
                        destino = Destino.valueOf(d.destino)
                        pestana = d.pestana?.let { Pestana.valueOf(it) } ?: Navegacion.inicial(destino)
                        busqueda = ""
                    }
                },
            )
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(18.dp, 4.dp, 18.dp, 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            when (pestana) {
                Pestana.CATALOGO, Pestana.APLICAR -> {
                    item { Etiqueta("EL TELÉFONO DICE AHORA") }
                    item { TablaActual(actual) }
                    item { Spacer(Modifier.height(6.dp)) }
                    // Filtro por confianza. El catálogo mezcla perfiles verificados con fichas
                    // sin comprobar, y no es lo mismo aplicar uno que otro. Antes había que
                    // mirar la etiqueta de cada tarjeta; ahora se elige arriba y la lista cambia.
                    item { Etiqueta("CATÁLOGO · ${Perfiles.censo()}") }
                    item {
                        PildorasMedidor(
                            elegido = filtroConfianza,
                            onMedidor = { filtroConfianza = it },
                            nombres = listOf(
                                "Todos" to Perfiles.todos.size,
                                "Verificados" to Perfiles.todos.count {
                                    it.confianza == PerfilDispositivo.Confianza.VERIFICADO
                                },
                                "Parciales" to Perfiles.todos.count {
                                    it.confianza == PerfilDispositivo.Confianza.PARCIAL
                                },
                                "Sin verificar" to Perfiles.todos.count {
                                    it.confianza == PerfilDispositivo.Confianza.SIN_VERIFICAR
                                },
                            ),
                        )
                    }
                    items(
                        Perfiles.todos.filter { p ->
                            when (filtroConfianza) {
                                1 -> p.confianza == PerfilDispositivo.Confianza.VERIFICADO
                                2 -> p.confianza == PerfilDispositivo.Confianza.PARCIAL
                                3 -> p.confianza == PerfilDispositivo.Confianza.SIN_VERIFICAR
                                else -> true
                            }
                        },
                    ) { p ->
                        TarjetaPerfil(p, perfilActivo?.id == p.id) { onPerfil(p) }
                    }
                    item { Spacer(Modifier.height(6.dp)) }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Boton("Aplicar perfil", Cristal.verde, Modifier.weight(1f)) { onAplicar() }
                            Boton("Restaurar", Cristal.superficieMasAlta, Modifier.weight(1f)) { onRestaurar() }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Boton("Deshacer todo", Cristal.ambar, Modifier.weight(1f)) { onDeshacer() }
                        }
                    }
                    resultado?.let { r ->
                        item { Spacer(Modifier.height(6.dp)) }
                        item { Etiqueta("RESULTADO DE LA ÚLTIMA ACCIÓN") }
                        item { TarjetaResultado(r) }
                    }
                }

                Pestana.TOP500 -> {
                    item {
                        Aviso(
                            "SIMULACIÓN, no rendimiento. Estas fichas hacen que el teléfono DECLARE " +
                                "los datos de un sistema real del TOP500. No añaden núcleos, ni memoria, " +
                                "ni gigaflops: tu teléfono sigue teniendo los que tiene. Sirven para " +
                                "probar cómo reacciona una app a esos valores, y nada más.",
                            Cristal.rojo,
                        )
                    }
                    item { Etiqueta("TOP500 · ${Supercomputadoras.censo()}") }
                    items(Supercomputadoras.todos) { s ->
                        TarjetaSuper(s, superActiva?.rank == s.rank) { onSimular(s) }
                    }

                    if (edicionesTop.isNotEmpty()) {
                        item { Spacer(Modifier.height(10.dp)) }
                        item {
                            Etiqueta(
                                "LAS 14 LISTAS OFICIALES · ${edicionesTop.sumOf { it.sistemas }} posiciones"
                            )
                        }
                        item {
                            Aviso(
                                "Esto no es simulación: son las catorce ediciones publicadas por " +
                                    "top500.org entre 2019 y 2025, con los XML originales dentro " +
                                    "del APK. Elige una y salen sus 500 sistemas tal como se " +
                                    "publicaron.",
                                Cristal.cian,
                            )
                        }
                        item {
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(edicionesTop) { e ->
                                    val sel = e.edicion == edicionElegida
                                    Column(
                                        Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (sel) Cristal.marca.copy(alpha = 0.25f)
                                                else Cristal.superficieMasAlta
                                            )
                                            .clickable { onEdicion(e.edicion) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            e.edicion,
                                            fontSize = 12.sp,
                                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sel) Cristal.texto else Cristal.textoSuave,
                                        )
                                        Text("${e.sistemas}", fontSize = 9.sp,
                                            color = Cristal.textoSuave)
                                    }
                                }
                            }
                        }
                        items(sistemasTop) { s ->
                            FilaSistemaTop(s)
                        }
                        if (sistemasTop.size > 100) {
                            item {
                                Text(
                                    "Se muestran los 100 primeros de ${sistemasTop.size}. " +
                                        "Los 500 están en el APK: ${edicionElegida}.",
                                    fontSize = 10.sp, color = Cristal.textoSuave,
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                    item {
                        Aviso(
                            "Los núcleos, FLOPS y potencia NO tienen propiedad en Android: no existe " +
                                "la clave donde escribirlos. Se enseñan como información del sistema real, " +
                                "no como algo que esta app pueda simular. Escribirlos sería inventar.",
                            Cristal.ambar,
                        )
                    }
                }

                Pestana.PANTALLA, Pestana.RED -> {
                    item {
                        if (!hayShell) {
                            Aviso(
                                "Estas funciones necesitan la shell de Shizuku. Sin ella no hacen nada: " +
                                    "no hay botón que finja funcionar. Activa Shizuku y vuelve a abrir.",
                                Cristal.rojo,
                            )
                        } else {
                            Aviso(
                                "Todo lo de esta pantalla pasa por la shell de Shizuku, que da los mismos " +
                                    "permisos que un adb shell. No es root: no toca el sistema de archivos raíz " +
                                    "ni el kernel.",
                                Cristal.verde,
                            )
                        }
                    }

                    item { Etiqueta("PANTALLA") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pantallaActual?.let { a ->
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(Cristal.superficie).padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Actual", fontSize = 11.sp, color = Cristal.textoSuave)
                                    Text("${a.resolucion} @ ${a.densidad}dpi", fontSize = 12.sp,
                                        color = Cristal.texto, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Text("Bajar la resolución baja el trabajo de la GPU. La densidad se recalcula " +
                                "sola para que todo siga del mismo tamaño.",
                                fontSize = 11.sp, lineHeight = 15.sp, color = Cristal.textoSuave)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Boton("720p", Cristal.superficieMasAlta, Modifier.weight(1f)) {
                                    onFijarResolucion(720, 1600, 293)
                                }
                                Boton("1080p", Cristal.superficieMasAlta, Modifier.weight(1f)) {
                                    onFijarResolucion(1080, 2400, 440)
                                }
                                Boton("Volver", Cristal.ambar, Modifier.weight(1f)) { onRevertirPantalla() }
                            }
                            Aviso(
                                "Si no confirmas en 15 segundos, el teléfono vuelve solo a su resolución. " +
                                    "El comando de vuelta se lanza aparte: funciona aunque la app se cierre.",
                                Cristal.ambar,
                            )
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Boton("60 Hz", Cristal.superficieMasAlta, Modifier.weight(1f)) { onFijarRefresco(60) }
                            Boton("90 Hz", Cristal.superficieMasAlta, Modifier.weight(1f)) { onFijarRefresco(90) }
                            Boton("120 Hz", Cristal.superficieMasAlta, Modifier.weight(1f)) { onFijarRefresco(120) }
                        }
                    }

                    item { Spacer(Modifier.height(6.dp)) }
                    item { Etiqueta("SISTEMA") }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Boton("Anim 0.5x", Cristal.superficieMasAlta, Modifier.weight(1f)) { onAnimaciones("0.5") }
                            Boton("Anim 1x", Cristal.superficieMasAlta, Modifier.weight(1f)) { onAnimaciones("1.0") }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Boton("DNS Google", Cristal.superficieMasAlta, Modifier.weight(1f)) { onDns("dns.google") }
                            Boton("DNS Cloudflare", Cristal.superficieMasAlta, Modifier.weight(1f)) { onDns("1dot1dot1dot1.cloudflare-dns.com") }
                        }
                    }
                    item {
                        Boton("Quitar DNS privado", Cristal.ambar, Modifier.fillMaxWidth()) { onDns("off") }
                    }

                    item { Spacer(Modifier.height(6.dp)) }
                    item { Etiqueta("CONGELAR APPS · libera batería sin desinstalar") }
                    item {
                        Text("Se puede congelar cualquier app instalada. Las del sistema están bloqueadas " +
                            "en el código: congelar SystemUI deja el teléfono sin interfaz.",
                            fontSize = 11.sp, lineHeight = 15.sp, color = Cristal.textoSuave)
                    }
                    items(apps) { a ->
                        FilaAppAccion(
                            app = a,
                            congelada = congeladas.contains(a.paquete),
                            hechos = fichas.firstOrNull { it.paquete == a.paquete }?.hechos?.size ?: 0,
                            onCongelar = { onCongelar(a.paquete) },
                            onDescongelar = { onDescongelar(a.paquete) },
                            onLimpiar = { onLimpiarCache(a.paquete) },
                            onPanel = { onPanel(a.paquete) },
                        )
                    }
                }

                Pestana.APPS -> {
                    item { Etiqueta("TODO LO INSTALADO, SIN FILTRO") }
                    item {
                        Aviso(
                            "${apps.size} apps detectadas. No hay lista blanca ni cuota: el perfil se aplica " +
                                "al sistema, así que alcanza a todas a la vez —incluidas estas— sin que tengas que " +
                                "elegir una por una.",
                            Cristal.verde,
                        )
                    }
                    items(apps) { a -> FilaApp(a) }
                }

                Pestana.LIMITES, Pestana.FUENTES, Pestana.DISPOSITIVOS, null -> {
                    item { Etiqueta("LO QUE DECLARA LA GPU") }
                    item { TablaFiltrada(actual, listOf("ro.opengles.version", "ro.hardware", "ro.board.platform", "ro.product.board")) }
                    item { Spacer(Modifier.height(6.dp)) }
                    item {
                        Aviso(
                            "La versión de OpenGL ES se declara con un número entero, no con un texto: " +
                                "196610 = 3.2. El catálogo usa ese formato porque es el que lee el sistema; " +
                                "poner «OpenGL ES 3.2» ahí no lo cambia, lo rompe.",
                            Cristal.ambar,
                        )
                    }
                    item { Etiqueta("QUÉ PERFILES DE GPU HAY") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Perfiles.todos.map { it.gpu }.distinct().forEach { g ->
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                        .background(Cristal.superficie).padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(g, fontSize = 13.sp, color = Cristal.texto)
                                    Text(
                                        "${Perfiles.todos.count { it.gpu == g }} perfil(es)",
                                        fontSize = 11.sp, color = Cristal.textoSuave,
                                    )
                                }
                            }
                        }
                    }
                }

                Pestana.GENERAR -> {
                    item {
                        Text(
                            "Buscar, extraer y generar",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Cristal.texto,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "El catálogo de fábrica son ${Perfiles.todos.size} perfiles. Aquí puedes " +
                                "buscar entre ellos y los tuyos, generar perfiles a partir de lo que " +
                                "tu teléfono declara de verdad, y extraerlos de un texto pegado.",
                            fontSize = 12.sp, lineHeight = 18.sp, color = Cristal.textoSuave,
                        )
                    }

                    // ── Búsqueda
                    item { Etiqueta("BUSCAR EN EL CATÁLOGO (${catalogoPerfiles.size})") }
                    item {
                        CampoTexto(
                            valor = consulta,
                            onCambio = { consulta = it },
                            pista = "pixel · iqoo · adreno · tecno · helio…",
                        )
                    }
                    val hallados = if (consulta.isBlank()) emptyList()
                    else Buscador.buscar(consulta, catalogoPerfiles)
                    if (consulta.isNotBlank()) {
                        if (hallados.isEmpty()) {
                            item {
                                Text(
                                    "Sin resultados para «$consulta». No se rellena con parecidos: " +
                                        "si no está, no está.",
                                    fontSize = 12.sp, color = Cristal.ambar,
                                )
                            }
                        } else {
                            items(hallados) { r ->
                                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cristal.superficie).padding(13.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(r.perfil.nombre, fontSize = 14.sp,
                                            color = Cristal.texto, fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f))
                                        Text(
                                            "encaje ${r.puntuacion}",
                                            fontSize = 10.sp, color = Cristal.cian,
                                        )
                                    }
                                    Text(
                                        r.motivos.joinToString(" · "),
                                        fontSize = 10.sp, color = Cristal.textoSuave,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Boton("Usar este perfil", Cristal.marca, Modifier.weight(1f)) {
                                            onSeleccionarPerfil(r.perfil)
                                        }
                                        if (biblioteca.any { it.id == r.perfil.id }) {
                                            Boton("Borrar", Cristal.superficieMasAlta, Modifier.weight(1f)) {
                                                onBorrarPerfil(r.perfil.id)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Catálogo oficial de Google Play
                    item { Etiqueta("CATÁLOGO REAL DE DISPOSITIVOS (${catalogoDispositivos.size})") }
                    item {
                        Text(
                            "El catálogo de Google Play, tal cual: marca, nombre comercial, " +
                                "device y modelo de cada aparato que existe. Busca el tuyo por " +
                                "marca o modelo y sale un perfil con sus datos reales.",
                            fontSize = 11.sp, lineHeight = 17.sp, color = Cristal.textoSuave,
                        )
                    }
                    item {
                        CampoTexto(
                            valor = consultaCatalogo,
                            onCambio = { consultaCatalogo = it },
                            pista = "tecno bg7 · samsung s24 · pixel 8…",
                        )
                    }
                    val halladosCatalogo = if (consultaCatalogo.isBlank()) emptyList()
                    else Datos.buscarDispositivos(catalogoDispositivos, consultaCatalogo, 30)
                    if (consultaCatalogo.isNotBlank()) {
                        if (halladosCatalogo.isEmpty()) {
                            item {
                                Text(
                                    "Ningún aparato del catálogo coincide con «$consultaCatalogo».",
                                    fontSize = 12.sp, color = Cristal.ambar,
                                )
                            }
                        } else {
                            items(halladosCatalogo) { d ->
                                val p = Datos.perfilDeDispositivo(d)
                                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cristal.superficie).padding(13.dp)) {
                                    Text(d.legible, fontSize = 13.sp, color = Cristal.texto,
                                        fontWeight = FontWeight.Medium)
                                    Text(
                                        "device ${d.device} · modelo ${d.modelo}",
                                        fontSize = 10.sp, color = Cristal.textoSuave,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                    if (p != null) {
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Boton("Usar", Cristal.marca, Modifier.weight(1f)) {
                                                onSeleccionarPerfil(p)
                                            }
                                            Boton("Guardar", Cristal.superficieMasAlta, Modifier.weight(1f)) {
                                                onGuardarPerfil(p)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Fichas reales con SoC y GPU
                    if (fichasMobolist.isNotEmpty()) {
                        item { Etiqueta("FICHAS CON SoC Y GPU · ${fichasMobolist.size} MODELOS") }
                        item {
                            Text(
                                "Modelos con su chip, su GPU y la versión de Android que " +
                                    "declaran, de las fichas técnicas de mobolist.net. Aquí sí " +
                                    "se puede calcular un OpenGL ES coherente, porque se sabe " +
                                    "la GPU.",
                                fontSize = 11.sp, lineHeight = 17.sp, color = Cristal.textoSuave,
                            )
                        }
                        item {
                            CampoTexto(
                                valor = consultaFicha,
                                onCambio = { consultaFicha = it },
                                pista = "spark 20c · helio g36 · adreno 610…",
                            )
                        }
                        if (consultaFicha.isNotBlank()) {
                            val halladas = Datos.buscarFichas(fichasMobolist, consultaFicha, 20)
                            if (halladas.isEmpty()) {
                                item {
                                    Text("Ninguna ficha coincide con «$consultaFicha».",
                                        fontSize = 12.sp, color = Cristal.ambar)
                                }
                            } else {
                                items(halladas) { f ->
                                    val perfil = Datos.perfilDeFicha(f)
                                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cristal.superficie).padding(13.dp)) {
                                        Text(f.nombre, fontSize = 13.sp, color = Cristal.texto,
                                            fontWeight = FontWeight.Medium)
                                        Text(f.resumen, fontSize = 10.sp, lineHeight = 15.sp,
                                            color = Cristal.textoSuave)
                                        if (perfil != null && perfil.openglEs.isNotBlank()) {
                                            Text(
                                                "OpenGL ES ${Generador.glLegible(perfil.openglEs)} " +
                                                    "(${perfil.openglEs})",
                                                fontSize = 9.sp, color = Cristal.verde,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                        }
                                        if (perfil != null) {
                                            Spacer(Modifier.height(8.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Boton("Usar", Cristal.marca, Modifier.weight(1f)) {
                                                    onSeleccionarPerfil(perfil)
                                                }
                                                Boton("Guardar", Cristal.superficieMasAlta, Modifier.weight(1f)) {
                                                    onGuardarPerfil(perfil)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── SoC y GPU reales
                    if (socs.isNotEmpty()) {
                        item { Etiqueta("SoC Y GPU · ${socs.size} FICHAS REALES") }
                        item {
                            CampoTexto(
                                valor = consultaSoc,
                                onCambio = { consultaSoc = it },
                                pista = "adreno 610 · helio g36 · snapdragon 8 gen 3…",
                            )
                        }
                        items(Datos.buscarSoc(socs, consultaSoc, 15)) { s ->
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cristal.superficie).padding(12.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(s.soc, fontSize = 12.sp, color = Cristal.texto,
                                        fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    Text(s.gpu, fontSize = 11.sp, color = Cristal.cian)
                                }
                                if (s.resumen.isNotBlank()) {
                                    Text(s.resumen, fontSize = 10.sp, color = Cristal.textoSuave)
                                }
                                Datos.openglDe(s.gpu)?.let { n ->
                                    Text(
                                        "OpenGL ES para esta GPU: $n (${Generador.glLegible(n)})",
                                        fontSize = 9.sp, color = Cristal.verde,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }

                    // ── Generar desde el aparato real
                    item { Etiqueta("GENERAR DESDE TU TELÉFONO") }
                    item {
                        if (datosSonda == null) {
                            Aviso(
                                "Todavía no se han leído las propiedades del teléfono. Hace falta " +
                                    "Shizuku con permiso: sin shell no se puede leer `getprop`.",
                                Cristal.ambar,
                            )
                        } else {
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Cristal.superficie).padding(14.dp)) {
                                Text(datosSonda.resumen, fontSize = 12.sp, color = Cristal.texto,
                                    fontWeight = FontWeight.Medium)
                                if (!datosSonda.completa) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Faltan datos básicos. Se puede generar un perfil, pero " +
                                            "quedará incompleto y así se dirá.",
                                        fontSize = 11.sp, color = Cristal.ambar,
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Boton("Volver a leer el teléfono", Cristal.superficieMasAlta, Modifier.weight(1f)) {
                                onSondar()
                            }
                            Boton("Generar todos", Cristal.verde, Modifier.weight(1f)) {
                                datosSonda?.let { d ->
                                    candidatos = Distribuida.generar(d, catalogoPerfiles)
                                }
                            }
                        }
                    }
                    items(candidatos) { c ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Cristal.superficie).padding(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(c.generado.estrategia.titulo, fontSize = 13.sp,
                                        color = Cristal.texto, fontWeight = FontWeight.SemiBold)
                                    Text(c.generado.perfil.nombre, fontSize = 11.sp,
                                        color = Cristal.cian)
                                }
                                Text("${c.encaje}", fontSize = 18.sp, color = Cristal.marca,
                                    fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                c.generado.estrategia.explicacion,
                                fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.textoSuave,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(c.motivos.joinToString(" · "), fontSize = 10.sp,
                                color = Cristal.textoSuave)
                            c.generado.avisos.forEach { a ->
                                Spacer(Modifier.height(4.dp))
                                Text("• $a", fontSize = 10.sp, lineHeight = 15.sp,
                                    color = Cristal.ambar)
                            }
                            Spacer(Modifier.height(9.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Boton("Guardar y usar", Cristal.marca, Modifier.weight(1f)) {
                                    onGuardarPerfil(c.generado.perfil)
                                    onSeleccionarPerfil(c.generado.perfil)
                                }
                                Boton("Usar sin guardar", Cristal.superficieMasAlta, Modifier.weight(1f)) {
                                    onSeleccionarPerfil(c.generado.perfil)
                                }
                            }
                        }
                    }

                    // ── Extraer de un texto
                    item { Etiqueta("EXTRAER DE UN TEXTO PEGADO") }
                    item {
                        Text(
                            "Pega una ficha de especificaciones (de una tienda, de una caja, de " +
                                "donde sea). Se reconocen marca, modelo, device, SoC, GPU y OpenGL ES. " +
                                "Lo que no aparezca se queda vacío: no se rellena a ojo.",
                            fontSize = 11.sp, lineHeight = 17.sp, color = Cristal.textoSuave,
                        )
                    }
                    item {
                        CampoTexto(
                            valor = textoPegado,
                            onCambio = { textoPegado = it; extraido = null },
                            pista = "Fabricante: …\nModelo: …\nSoC: …",
                            lineas = 5,
                        )
                    }
                    item {
                        Boton("Extraer", Cristal.cian, Modifier.fillMaxWidth()) {
                            extraido = if (textoPegado.isBlank()) null
                            else Extractor.extraer(textoPegado)
                        }
                    }
                    extraido?.let { e ->
                        item {
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Cristal.superficie).padding(14.dp)) {
                                Text(e.resumen, fontSize = 13.sp, color = Cristal.texto,
                                    fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(6.dp))
                                Fila("Fabricante", e.fabricante)
                                Fila("Modelo", e.modelo)
                                Fila("Device", e.device)
                                Fila("SoC", e.soc)
                                Fila("GPU", e.gpu)
                                Fila("OpenGL ES", if (e.openglEs.isBlank()) ""
                                else "${Generador.glLegible(e.openglEs)} (${e.openglEs})")
                                if (e.faltan.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "No apareció: ${e.faltan.joinToString(", ")}.",
                                        fontSize = 10.sp, color = Cristal.ambar,
                                    )
                                }
                                val perfil = Extractor.aPerfil(e, textoPegado)
                                if (perfil == null) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Con esto no se puede hacer un perfil: falta marca o " +
                                            "modelo, y sin eso no hay nada que escribir.",
                                        fontSize = 11.sp, color = Cristal.rojo,
                                    )
                                } else {
                                    Spacer(Modifier.height(9.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Boton("Guardar y usar", Cristal.marca, Modifier.weight(1f)) {
                                            onGuardarPerfil(perfil)
                                            onSeleccionarPerfil(perfil)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Los míos
                    item { Etiqueta("TUS PERFILES (${biblioteca.size})") }
                    if (biblioteca.isEmpty()) {
                        item {
                            Text(
                                "Todavía no has guardado ninguno. Los que generes o extraigas " +
                                    "aparecerán aquí, y en la búsqueda de arriba.",
                                fontSize = 12.sp, color = Cristal.textoSuave,
                            )
                        }
                    } else {
                        items(biblioteca) { p ->
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cristal.superficie).padding(13.dp)) {
                                Text(p.nombre, fontSize = 13.sp, color = Cristal.texto,
                                    fontWeight = FontWeight.Medium)
                                Text(p.fuente, fontSize = 10.sp, lineHeight = 15.sp,
                                    color = Cristal.textoSuave)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Boton("Usar", Cristal.marca, Modifier.weight(1f)) {
                                        onSeleccionarPerfil(p)
                                    }
                                    Boton("Borrar", Cristal.superficieMasAlta, Modifier.weight(1f)) {
                                        onBorrarPerfil(p.id)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Aviso(
                            "«Generación distribuida» aquí significa varios generadores " +
                                "trabajando a la vez dentro de este teléfono, cada uno con su " +
                                "criterio, y un encaje que mide parecido con tu aparato. No es " +
                                "una red de máquinas: llamarlo así sería vender otra cosa.",
                            Cristal.cian,
                        )
                    }
                    item { Spacer(Modifier.height(10.dp)) }
                    item {
                        SeccionRespaldo(
                            perfiles = biblioteca,
                            versionApp = version,
                            modeloTelefono = datosSonda?.modelo ?: "sin modelo declarado",
                            onImportar = { lista -> onGuardarVarios(lista) },
                        )
                    }
                }
                Pestana.SUPERPOSICION -> {
                    item {
                        PantallaSuperposicion(
                            a = ajustesSuperposicion,
                            tienePermiso = tienePermisoSuperposicion,
                            activo = superposicionActiva,
                            onCambiar = onCambiarSuperposicion,
                            onPedirPermiso = onPermisoSuperposicion,
                            onActivar = onActivarSuperposicion,
                            onDesactivar = onDesactivarSuperposicion,
                            onMover = onMoverSuperposicion,
                        )
                    }
                }
                Pestana.CONFIGURAR -> {
                    item {
                        Text(
                            Configuracion.resumen(
                                Configuracion.filas(hayShell, sdkTelefono)
                            ),
                            fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.textoSuave,
                        )
                        Spacer(Modifier.height(10.dp))
                        PantallaConfigurar(
                            perfilActivo = perfilActivo,
                            onIrAGenerar = { pestana = Pestana.GENERAR; destino = Destino.PERFILES },
                            onIrAPerfiles = { pestana = Pestana.CATALOGO; destino = Destino.PERFILES },
                            filas = Configuracion.filas(hayShell, sdkTelefono).map {
                                it.copy(marcada = it.id in marcadas)
                            },
                            onAlternar = { id ->
                                marcadas = if (id in marcadas) marcadas - id else marcadas + id
                            },
                        )
                    }
                }
                Pestana.RENDIMIENTO -> {
                    item {
                        Text(
                            "Lo que hacen los módulos, sin root",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Cristal.texto,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Se leyeron las fichas de los 280 módulos de la categoría " +
                                "AxManager Plugins para ver qué hacen, y se hizo lo que se " +
                                "puede hacer con los permisos de `adb shell` —los mismos que " +
                                "tiene Shizuku—. Cada tarjeta enseña el comando exacto, cómo " +
                                "se comprueba y cómo se deshace.",
                            fontSize = 12.sp, lineHeight = 18.sp, color = Cristal.textoSuave,
                        )
                    }
                    item {
                        Aviso(
                            "Esto NO es lo mismo que un módulo con root. Lo que necesita root " +
                                "está al final, dicho con su nombre: gobernadores de CPU, " +
                                "Vulkan forzado, SurfaceFlinger, sondeo táctil. Aquí no hay un " +
                                "botón que finja hacer eso.",
                            Cristal.ambar,
                        )
                    }
                    items(accionesRendimiento) { a ->
                        TarjetaRendimiento(a, seleccion = null) { onRendimiento(a) }
                    }
                    item { Etiqueta("LO QUE NECESITA ROOT (Y AQUÍ NO ESTÁ)") }
                    items(sinRootRendimiento) { (que, porQue) ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Cristal.superficie).padding(12.dp)) {
                            Text(que, fontSize = 12.sp, color = Cristal.texto,
                                fontWeight = FontWeight.Medium)
                            Text(porQue, fontSize = 10.sp, lineHeight = 15.sp,
                                color = Cristal.textoSuave)
                        }
                    }
                    item {
                        Aviso(
                            "Sobre los módulos de esa lista: muchos son buenos y otros piden " +
                                "root para cosas que esta app no puede hacer. Ninguno se ha " +
                                "copiado: no publican código, publican fichas. Lo que hay aquí " +
                                "está escrito desde cero con comandos documentados de Android.",
                            Cristal.cian,
                        )
                    }
                }

                Pestana.HISTORIAL -> {
                    item {
                        Aviso(
                            "Todo lo que la app cambió en este teléfono, junto y por fecha: las " +
                                "propiedades de «Identidad» y lo que se le hizo a cada app. Se lee " +
                                "de los dos cuadernos que ya existían; aquí no se guarda nada nuevo.",
                            Cristal.marca,
                        )
                    }
                    if (movimientos.isEmpty()) {
                        item {
                            Text(
                                "Todavía no se cambió nada. Aplica un perfil, congela una app o " +
                                    "toca un ajuste, y cada cambio aparecerá aquí con su hora y " +
                                    "su vuelta atrás.",
                                fontSize = 13.sp, lineHeight = 19.sp, color = Cristal.textoSuave,
                            )
                        }
                    } else {
                        // Agrupado por día: una cabecera por jornada y no por fila.
                        var diaActual = ""
                        movimientos.forEach { m ->
                            val d = io.github.labandita.glreborn.core.ReglasMovimientos
                                .dia(m.cuando, System.currentTimeMillis())
                            if (d != diaActual) {
                                diaActual = d
                                item { Etiqueta(d.uppercase()) }
                            }
                            item { FilaMovimiento(m) }
                        }
                        item {
                            Spacer(Modifier.height(8.dp))
                            Boton("Deshacer lo escrito en propiedades", Cristal.ambar, Modifier.fillMaxWidth()) {
                                onDeshacerRegistro()
                            }
                        }
                        item {
                            Text(
                                "Ese botón devuelve las propiedades a su valor original. Lo que " +
                                    "se hizo a las apps (congelar, detener, desinstalar) se deshace " +
                                    "desde el panel de cada app, en Aplicado: no todo se deshace " +
                                    "igual, y decirlo es parte de hacerlo bien.",
                                fontSize = 11.sp, lineHeight = 17.sp, color = Cristal.textoSuave,
                            )
                        }
                    }
                }

                Pestana.APLICADO -> {
                    item {
                        Aviso(
                            "Cada app tiene su propio panel con lo que se le aplicó y cuándo. " +
                                "Toca una para abrirlo: se genera en tu teléfono, no hay servidor.",
                            Cristal.marca,
                        )
                    }
                    if (fichas.isEmpty()) {
                        item {
                            Text(
                                "Todavía no se le aplicó nada a ninguna app. Congela, detén o " +
                                    "limpia la caché de una, y aparecerá aquí con su panel.",
                                fontSize = 13.sp, lineHeight = 19.sp, color = Cristal.textoSuave,
                            )
                        }
                    } else {
                        items(fichas) { f ->
                            FilaFicha(f) { onPanel(f.paquete) }
                        }
                    }
                }

                Pestana.COMPATIBILIDAD -> {
                    item {
                        Aviso(
                            "Estas pruebas se ejecutan EN TU TELÉFONO, no en el mío: aquí no hay " +
                                "emulador y no puedo probar cada Android ni cada Huawei. Cada línea " +
                                "lanza el comando de verdad y enseña lo que responde.",
                            Cristal.verde,
                        )
                    }
                    item { Etiqueta("TU EQUIPO") }
                    item {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Cristal.superficie).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            datosEquipo.forEach { (k, v) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(k, fontSize = 11.sp, color = Cristal.textoSuave)
                                    Text(v, fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.texto,
                                        fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(6.dp)) }
                    item { Etiqueta("RESULTADO · $resumenCompat") }
                    items(pruebasCompat) { pr ->
                        val color = when (pr.resultado) {
                            Compatibilidad.Prueba.Estado.PASA -> Cristal.verde
                            Compatibilidad.Prueba.Estado.FALLA -> Cristal.rojo
                            Compatibilidad.Prueba.Estado.NO_APLICA -> Cristal.ambar
                        }
                        // Iconos dibujados, no caracteres: un «✓» de texto es el
                        // síntoma exacto del diseño tipo HTML que está prohibido.
                        val icono: @Composable () -> Unit = {
                            when (pr.resultado) {
                                Compatibilidad.Prueba.Estado.PASA ->
                                    Iconos.Visto(tamano = 14.dp, color = color)
                                Compatibilidad.Prueba.Estado.FALLA ->
                                    Iconos.Aspa(tamano = 14.dp, color = color)
                                Compatibilidad.Prueba.Estado.NO_APLICA ->
                                    Iconos.Guion(tamano = 14.dp, color = color)
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(Cristal.superficie).padding(12.dp),
                        ) {
                            icono()
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(pr.nombre, fontSize = 12.sp, color = Cristal.texto,
                                    fontWeight = FontWeight.Medium)
                                Text(pr.detalle, fontSize = 11.sp, lineHeight = 15.sp, color = color)
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(6.dp))
                        Boton("Volver a probar", Cristal.verde, Modifier.fillMaxWidth()) {
                            onProbarCompatibilidad()
                        }
                    }
                }

                Pestana.DIAGNOSTICO -> {
                    manifiesto?.let { m ->
                        item { Etiqueta("DATOS DENTRO DEL APK, CON SU FUENTE") }
                        item {
                            Text(
                                m,
                                fontSize = 11.sp, lineHeight = 17.sp, color = Cristal.textoSuave,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                        item { Spacer(Modifier.height(6.dp)) }
                    }
                    item { Etiqueta("ESTADO MEDIDO") }
                    items(lineasDiag) { l -> FilaDiag(l) }
                    item { Spacer(Modifier.height(6.dp)) }
                    item { Etiqueta("EL FILTRO DE SEGURIDAD, A LA VISTA") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Seguridad.resumenListas().forEach { (titulo, lista) ->
                                Column(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                        .background(Cristal.superficie).padding(12.dp),
                                ) {
                                    Text(titulo, fontSize = 10.sp, letterSpacing = 0.8.sp,
                                        color = Cristal.textoSuave, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(3.dp))
                                    Text(lista, fontSize = 11.sp, lineHeight = 15.sp,
                                        color = Cristal.texto, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                    item { Etiqueta("LO QUE ESTA APP NO PUEDE HACER") }
                    item {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Cristal.superficie).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            limites.forEach {
                                Row {
                                    Text("· ", color = Cristal.rojo, fontSize = 12.sp)
                                    Text(it, fontSize = 12.sp, lineHeight = 17.sp, color = Cristal.textoSuave)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(10.dp)) }
            item {
                TextButton(onClick = onRefrescar) {
                    Text("Volver a leer el estado del teléfono", color = Cristal.textoSuave, fontSize = 13.sp)
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
            item { Pie() }
            }
            } // cierra when (destino)

            // Píldora de destinos: cinco, flotando sobre el contenido.
            PildoraDestinos(
                actual = destino,
                onDestino = { d ->
                    destino = d
                    pestana = Navegacion.inicial(d)
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        } // Box del cuerpo
        } // Column
    }


@Composable
private fun BarraEstado(estado: Director.Estado, descripcion: String) {
    val (color, texto) = when (estado) {
        Director.Estado.SHIZUKU_LISTO -> Cristal.verde to "SHIZUKU"
        Director.Estado.ROOT_LISTO -> Cristal.verde to "ROOT"
        Director.Estado.SHIZUKU_SIN_PERMISO -> Cristal.ambar to "SIN PERMISO"
        Director.Estado.SIN_VIA -> Cristal.rojo to "SIN VÍA"
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Cristal.superficie).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
            Spacer(Modifier.width(8.dp))
            Text(texto, fontSize = 11.sp, letterSpacing = 1.5.sp, color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text(descripcion, fontSize = 13.sp, lineHeight = 18.sp, color = Cristal.textoSuave)
    }
}

@Composable
internal fun Etiqueta(t: String) {
    Text(t, fontSize = 10.sp, letterSpacing = 1.6.sp, color = Cristal.textoSuave, fontWeight = FontWeight.Bold)
}

@Composable
private fun Aviso(texto: String, color: Color) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f)).padding(12.dp),
    ) {
        Text(texto, fontSize = 12.sp, lineHeight = 17.sp, color = color.copy(alpha = 0.95f))
    }
}

@Composable
private fun TablaActual(actual: Map<String, String>) = TablaFiltrada(actual, actual.keys.toList())

@Composable
private fun TablaFiltrada(actual: Map<String, String>, claves: List<String>) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Cristal.superficie).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (actual.isEmpty()) {
            Text(
                "Sin vía de lectura: Shizuku no está activo y no hay root. Los valores reales " +
                    "no se pueden leer sin uno de los dos.",
                fontSize = 13.sp, color = Cristal.ambar, lineHeight = 18.sp,
            )
        } else {
            claves.forEach { k ->
                val v = actual[k] ?: return@forEach
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(k.replace("ro.", ""), fontSize = 12.sp, color = Cristal.textoSuave,
                        fontFamily = FontFamily.Monospace)
                    Text(v.ifBlank { "—" }, fontSize = 12.sp, color = Cristal.texto,
                        fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun TarjetaPerfil(p: PerfilDispositivo, seleccionado: Boolean, onPerfil: () -> Unit) {
    val borde = if (seleccionado) Cristal.verde else Cristal.bordeLuz
    val (colorConf, textoConf) = when (p.confianza) {
        PerfilDispositivo.Confianza.VERIFICADO -> Cristal.verde to "VERIFICADO"
        PerfilDispositivo.Confianza.PARCIAL -> Cristal.ambar to "PARCIAL"
        PerfilDispositivo.Confianza.SIN_VERIFICAR -> Cristal.rojo to "SIN VERIFICAR"
    }
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (seleccionado) Cristal.superficieAlta else Color.Transparent)
            .clickable { onPerfil() }.padding(vertical = 11.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.width(3.dp).height(40.dp).background(borde, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(p.nombre, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Cristal.texto)
                    Spacer(Modifier.height(2.dp))
                    Text("${p.soc} · ${p.gpu}", fontSize = 12.sp, color = Cristal.textoSuave)
                }
                Text(textoConf, fontSize = 9.sp, letterSpacing = 1.sp, color = colorConf, fontWeight = FontWeight.Bold)
            }
            if (p.nota.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(p.nota, fontSize = 11.sp, lineHeight = 15.sp, color = Cristal.textoTenue)
            }
        }
    }
}

@Composable
private fun FilaApp(a: Inventario.App) {
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(a.nombre, fontSize = 13.sp, color = Cristal.texto)
            Text(a.paquete, fontSize = 10.sp, color = Cristal.textoSuave, fontFamily = FontFamily.Monospace)
        }
        Text(
            if (a.esSistema) "sistema" else "usuario",
            fontSize = 9.sp, letterSpacing = 0.6.sp, color = Cristal.textoSuave,
        )
    }
}

@Composable
private fun FilaDiag(l: Diagnostico.Linea) {
    val color = when (l.estado) {
        Diagnostico.Linea.Estado.BIEN -> Cristal.verde
        Diagnostico.Linea.Estado.ATENCION -> Cristal.ambar
        Diagnostico.Linea.Estado.MAL -> Cristal.rojo
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Cristal.superficieAlta)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(l.etiqueta, fontSize = 11.sp, color = Cristal.textoSuave)
            Spacer(Modifier.height(2.dp))
            Text(l.valor, fontSize = 13.sp, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TarjetaResultado(r: Resultado) {
    val color = if (r.ok) Cristal.verde else Cristal.ambar
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
    ) {
        Box(Modifier.width(2.dp).height(16.dp).background(color, RoundedCornerShape(1.dp)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
        Text(r.detalle, fontSize = 13.sp, color = color, lineHeight = 19.sp, fontWeight = FontWeight.Medium)
        if (r.lineas.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            r.lineas.forEach {
                Text(it, fontSize = 11.sp, color = Cristal.textoSuave,
                    fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
            }
        }
        }
    }
}

@Composable
internal fun Boton(texto: String, color: Color, mod: Modifier, onClick: () -> Unit) {
    Box(
        mod.clip(RoundedCornerShape(10.dp)).background(color).clickable { onClick() }.padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, color = if (color == Cristal.verde) Cristal.fondoArriba else Cristal.texto,
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun Pie() {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text("CÓDIGO ABIERTO · SIN SERVIDORES", fontSize = 10.sp, letterSpacing = 1.4.sp,
            color = Cristal.verde, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Ninguna función de esta app pide nada a internet. No hay cuentas, ni monedas, ni " +
                "comprobación de compra, ni telemetría. Los perfiles viajan dentro del APK: si el " +
                "teléfono está en modo avión, todo funciona igual.",
            fontSize = 12.sp, lineHeight = 17.sp, color = Cristal.textoSuave,
        )
    }
}

/** El interruptor del modo. Es la decisión más importante de la pantalla, así que se ve siempre. */
@Composable
private fun BarraModo(modo: Modo, onModo: () -> Unit) {
    val color = if (modo == Modo.SEGURO) Cristal.verde else Cristal.ambar
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                modo.titulo.uppercase(),
                fontSize = 10.sp, letterSpacing = 1.5.sp, color = color, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (modo == Modo.SEGURO)
                    "Sólo modelo y gráficos · deshace solo si algo no cuadra"
                else
                    "Añade kernel, API y build · cada escritura se anota antes de hacerla",
                fontSize = 11.sp, color = Cristal.textoSuave, lineHeight = 15.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.clip(RoundedCornerShape(8.dp)).background(color).clickable { onModo() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                if (modo == Modo.SEGURO) "Pasar a experto" else "Volver a seguro",
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = if (modo == Modo.SEGURO) Cristal.fondoArriba else Cristal.fondoArriba,
            )
        }
    }
}

@Composable
private fun TarjetaSuper(s: Supercomputadora, activa: Boolean, onSimular: () -> Unit) {
    // Seleccionada = barra a la izquierda y tono más alto. Sin borde: el borde era el adorno
    // que hacía que doce tarjetas seguidas pareciesen doce cromos.
    Row(
        Modifier.fillMaxWidth()
            .background(if (activa) Cristal.superficieAlta else Color.Transparent)
            .clickable { onSimular() }.padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.width(3.dp).height(38.dp).clip(RoundedCornerShape(2.dp))
                .background(if (activa) Cristal.rojo else Cristal.bordeLuz),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("#${s.rank}  ${s.nombre}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = Cristal.texto)
                Spacer(Modifier.height(2.dp))
                Text("${s.sitio} · ${s.pais}", fontSize = 12.sp, color = Cristal.textoSuave)
            }
            Text("SIMULACIÓN", fontSize = 8.sp, letterSpacing = 1.sp, color = Cristal.rojo,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${"%,d".format(s.nucleos)} núcleos", fontSize = 11.sp, color = Cristal.ambar,
                fontFamily = FontFamily.Monospace)
            Text("%.2f PFlop/s".format(s.rmaxPFlops), fontSize = 11.sp, color = Cristal.ambar,
                fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(6.dp))
        Text(s.nota, fontSize = 11.sp, lineHeight = 15.sp, color = Cristal.textoSuave.copy(alpha = 0.85f))
        Spacer(Modifier.height(8.dp))
        Text(
            "→ el teléfono declarará llamarse «${s.nombre}»",
            fontSize = 11.sp, color = Cristal.textoSuave, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
        )
        }
    }
}

@Composable
private fun FilaAppAccion(
    app: Inventario.App,
    congelada: Boolean,
    hechos: Int,
    onCongelar: () -> Unit,
    onDescongelar: () -> Unit,
    onLimpiar: () -> Unit,
    onPanel: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Cristal.superficie).padding(13.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(app.nombre, fontSize = 13.sp, color = Cristal.texto, fontWeight = FontWeight.Medium)
                Text(app.paquete, fontSize = 10.sp, color = Cristal.textoSuave,
                    fontFamily = FontFamily.Monospace)
            }
            if (congelada) {
                Text("CONGELADA", fontSize = 8.sp, letterSpacing = 0.8.sp,
                    color = Cristal.ambar, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (congelada) {
                Boton("Activar", Cristal.verde, Modifier.weight(1f)) { onDescongelar() }
            } else {
                Boton("Congelar", Cristal.superficieMasAlta, Modifier.weight(1f)) { onCongelar() }
            }
            Boton("Caché", Cristal.superficieMasAlta, Modifier.weight(1f)) { onLimpiar() }
            Boton(
                if (hechos > 0) "Panel · $hechos" else "Panel",
                Cristal.marca, Modifier.weight(1f),
            ) { onPanel() }
        }
    }
}

/**
 * Una línea del historial: qué, dónde, cuándo y si se puede deshacer.
 *
 * Sin caja: dentro de un grupo las filas se separan con un hilo, no con bordes. Es el idioma de
 * `Grupo.kt`, y una lista de historial de 40 cajas apiladas sería justo lo que no queremos.
 */
@Composable
private fun FilaMovimiento(m: Movimiento) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Fila {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (m.fuente == Movimiento.FUENTE_REGISTRO) m.propiedad else m.despues,
                    fontSize = 13.sp, lineHeight = 17.sp, color = Cristal.texto,
                    fontFamily = FontFamily.Monospace,
                )
                Text(m.detalle, fontSize = 12.sp, lineHeight = 17.sp, color = Cristal.textoSuave)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (m.fuente == Movimiento.FUENTE_REGISTRO) "propiedad" else m.donde,
                        fontSize = 10.sp, color = Cristal.cian, fontWeight = FontWeight.Medium,
                    )
                    if (!m.reversible) Text(
                        "sin vuelta atrás", fontSize = 10.sp, color = Cristal.rojo,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Text(
                io.github.labandita.glreborn.core.ReglasMovimientos.hora(m.cuando),
                fontSize = 11.sp, color = Cristal.textoSuave,
                fontFamily = FontFamily.Monospace,
            )
        }
        Hilo()
    }
}

@Composable
private fun FilaFicha(f: Ficha, onPanel: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Cristal.superficie).clickable { onPanel() }.padding(15.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(f.nombre, fontSize = 15.sp, color = Cristal.texto, fontWeight = FontWeight.SemiBold)
                Text(f.paquete, fontSize = 10.sp, color = Cristal.textoSuave,
                    fontFamily = FontFamily.Monospace)
            }
            Text(
                "${f.hechos.size}",
                fontSize = 20.sp, color = Cristal.marca, fontWeight = FontWeight.Bold,
            )
        }
        if (f.tieneIrreversible) {
            Spacer(Modifier.height(7.dp))
            Text(
                "Incluye una acción sin vuelta atrás",
                fontSize = 10.sp, color = Cristal.rojo, fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            f.hechos.firstOrNull()?.accion ?: "—",
            fontSize = 12.sp, color = Cristal.textoSuave,
        )
    }
}

/** Campo de texto de cristal. Sin Material de por medio: el fondo lo pone el tema. */
@Composable
private fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    pista: String,
    lineas: Int = 1,
) {
    androidx.compose.foundation.text.BasicTextField(
        value = valor,
        onValueChange = onCambio,
        singleLine = lineas == 1,
        maxLines = lineas,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = Cristal.texto, fontSize = 13.sp, lineHeight = 19.sp,
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(Cristal.marca),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)).background(Cristal.superficie)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { interior ->
            if (valor.isEmpty()) {
                Text(pista, fontSize = 12.sp, color = Cristal.textoSuave.copy(alpha = 0.6f))
            }
            interior()
        },
    )
}

@Composable
private fun Fila(clave: String, valor: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(clave, fontSize = 11.sp, color = Cristal.textoSuave)
        Text(valor.ifBlank { "—" }, fontSize = 11.sp, color = Cristal.texto,
            fontFamily = FontFamily.Monospace)
    }
}

/** Una posición del TOP500, de una edición concreta. Dato real, no simulado. */
@Composable
private fun FilaSistemaTop(s: Datos.Sistema) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Cristal.superficie).padding(11.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${s.rank}",
                fontSize = 12.sp, color = Cristal.cian, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(34.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(s.nombre, fontSize = 12.sp, color = Cristal.texto,
                    fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    "${s.fabricante} · ${s.pais}",
                    fontSize = 9.sp, color = Cristal.textoSuave, maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${s.rmaxPflops ?: "—"} PFlop/s",
                    fontSize = 11.sp, color = Cristal.marca, fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${s.nucleos} núcleos",
                    fontSize = 9.sp, color = Cristal.textoSuave,
                )
            }
        }
    }
}

/**
 * Una acción de rendimiento, con el comando a la vista ANTES de ejecutarla.
 *
 * El botón de aplicar está desactivado si la acción necesita un paquete y no hay ninguno
 * elegido: es mejor que el botón diga «hace falta elegir una app» a que falle al pulsarlo.
 */
@Composable
private fun TarjetaRendimiento(
    a: Rendimiento.Accion,
    seleccion: String?,
    onAplicar: () -> Unit,
) {
    val color = when (a.riesgo) {
        Rendimiento.Riesgo.BAJO -> Cristal.verde
        Rendimiento.Riesgo.MEDIO -> Cristal.ambar
        Rendimiento.Riesgo.ALTO -> Cristal.rojo
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Cristal.superficie).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                a.nombre, fontSize = 14.sp, color = Cristal.texto,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
            )
            Text(
                when (a.riesgo) {
                    Rendimiento.Riesgo.BAJO -> "BAJO"
                    Rendimiento.Riesgo.MEDIO -> "MEDIO"
                    Rendimiento.Riesgo.ALTO -> "ALTO"
                },
                fontSize = 8.sp, letterSpacing = 0.8.sp, color = color,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(a.queHace, fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.textoSuave)
        Spacer(Modifier.height(8.dp))
        Text("COMANDO", fontSize = 8.sp, letterSpacing = 1.sp, color = Cristal.textoSuave)
        Text(
            a.comandosLegibles,
            fontSize = 10.sp, color = Cristal.cian, fontFamily = FontFamily.Monospace,
            lineHeight = 15.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Comprueba: ${a.comprobar} → ${a.esperado}",
            fontSize = 9.sp, color = Cristal.textoSuave, fontFamily = FontFamily.Monospace,
        )
        if (a.aviso.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(a.aviso, fontSize = 10.sp, lineHeight = 15.sp, color = color)
        }
        Text(
            "De dónde salió la idea: ${a.inspirado}",
            fontSize = 9.sp, color = Cristal.textoSuave,
        )
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Boton("Aplicar", color, Modifier.weight(1f)) { onAplicar() }
        }
        Text(
            Rendimiento.avisoDeRiesgo(a),
            fontSize = 9.sp, color = Cristal.textoSuave,
        )
    }
}

@Composable
fun PantallaArrancando(error: String? = null) {
    Box(
        Modifier.fillMaxSize().background(Cristal.fondoArriba),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(36.dp).clip(RoundedCornerShape(26.dp)).background(Cristal.superficie).padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "GLTOOLS",
                fontSize = 13.sp, letterSpacing = 5.sp, color = Cristal.cian,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "REBORN",
                fontSize = 30.sp, letterSpacing = 1.sp, color = Cristal.texto,
                fontWeight = FontWeight.Light,
            )
            Spacer(Modifier.height(18.dp))
            if (error == null) {
                Text("Leyendo el teléfono…", fontSize = 12.sp, color = Cristal.textoSuave)
            } else {
                Text("Algo falló al arrancar", fontSize = 13.sp, color = Cristal.rojo,
                    fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text(error, fontSize = 11.sp, color = Cristal.textoSuave, lineHeight = 17.sp)
            }
        }
    }
}
