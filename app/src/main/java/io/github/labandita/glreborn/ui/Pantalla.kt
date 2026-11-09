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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.labandita.glreborn.core.*

enum class Seccion(val titulo: String, val icono: String) {
    PERFILES("Perfiles", "◆"),
    SUPER("Super", "▲"),
    APLICACIONES("Apps", "▣"),
    UTILIDADES("Útil", "✦"),
    GPU("GPU", "◉"),
    DIAGNOSTICO("Diag.", "◈"),
    COMPATIBILIDAD("Compat.", "◇"),
    APLICADO("Aplicado", "◐"),
}

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
    onPanel: (String) -> Unit,
    onPerfil: (PerfilDispositivo) -> Unit,
    onAplicar: () -> Unit,
    onRestaurar: () -> Unit,
    onRefrescar: () -> Unit,
) {
    var seccion by remember { mutableStateOf(Seccion.PERFILES) }

    Scaffold(
        containerColor = Cristal.fondoArriba,
        topBar = {
            Column(Modifier.fillMaxWidth().background(Cristal.fondoArriba).padding(20.dp, 18.dp, 20.dp, 0.dp)) {
                Text("GLTOOLS REBORN", fontSize = 11.sp, letterSpacing = 2.sp,
                    color = Cristal.verde, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Perfiles de dispositivo", style = MaterialTheme.typography.headlineMedium, color = Cristal.texto)
                Spacer(Modifier.height(10.dp))
                BarraEstado(estadoVia, director.descripcion())
                Spacer(Modifier.height(10.dp))
                BarraModo(modo, onModo)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 116.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (seccion) {
                Seccion.PERFILES -> {
                    item { Etiqueta("EL TELÉFONO DICE AHORA") }
                    item { TablaActual(actual) }
                    item { Spacer(Modifier.height(6.dp)) }
                    item { Etiqueta("CATÁLOGO · ${Perfiles.censo()}") }
                    items(Perfiles.todos) { p ->
                        TarjetaPerfil(p, perfilActivo?.id == p.id) { onPerfil(p) }
                    }
                    item { Spacer(Modifier.height(6.dp)) }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Boton("Aplicar perfil", Cristal.verde, Modifier.weight(1f)) { onAplicar() }
                            Boton("Restaurar", Cristal.vidrioFuerte, Modifier.weight(1f)) { onRestaurar() }
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

                Seccion.SUPER -> {
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

                Seccion.UTILIDADES -> {
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
                                    .background(Cristal.vidrio).padding(12.dp),
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
                                Boton("720p", Cristal.vidrioFuerte, Modifier.weight(1f)) {
                                    onFijarResolucion(720, 1600, 293)
                                }
                                Boton("1080p", Cristal.vidrioFuerte, Modifier.weight(1f)) {
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
                            Boton("60 Hz", Cristal.vidrioFuerte, Modifier.weight(1f)) { onFijarRefresco(60) }
                            Boton("90 Hz", Cristal.vidrioFuerte, Modifier.weight(1f)) { onFijarRefresco(90) }
                            Boton("120 Hz", Cristal.vidrioFuerte, Modifier.weight(1f)) { onFijarRefresco(120) }
                        }
                    }

                    item { Spacer(Modifier.height(6.dp)) }
                    item { Etiqueta("SISTEMA") }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Boton("Anim 0.5x", Cristal.vidrioFuerte, Modifier.weight(1f)) { onAnimaciones("0.5") }
                            Boton("Anim 1x", Cristal.vidrioFuerte, Modifier.weight(1f)) { onAnimaciones("1.0") }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Boton("DNS Google", Cristal.vidrioFuerte, Modifier.weight(1f)) { onDns("dns.google") }
                            Boton("DNS Cloudflare", Cristal.vidrioFuerte, Modifier.weight(1f)) { onDns("1dot1dot1dot1.cloudflare-dns.com") }
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
                    items(apps.take(40)) { a ->
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

                Seccion.APLICACIONES -> {
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

                Seccion.GPU -> {
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
                                        .background(Cristal.vidrio).padding(12.dp),
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

                Seccion.APLICADO -> {
                    item {
                        Aviso(
                            "Cada app tiene su propio panel con lo que se le aplicó y cuándo. " +
                                "Toca una para abrirlo: se genera en tu teléfono, no hay servidor.",
                            Cristal.violeta,
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

                Seccion.COMPATIBILIDAD -> {
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
                                .background(Cristal.vidrio).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            datosEquipo.forEach { (k, v) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(k, fontSize = 11.sp, color = Cristal.textoSuave)
                                    Text(v.take(30), fontSize = 11.sp, color = Cristal.texto,
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
                        val icono = when (pr.resultado) {
                            Compatibilidad.Prueba.Estado.PASA -> "✓"
                            Compatibilidad.Prueba.Estado.FALLA -> "✗"
                            Compatibilidad.Prueba.Estado.NO_APLICA -> "—"
                        }
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(Cristal.vidrio).padding(12.dp),
                        ) {
                            Text(icono, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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

                Seccion.DIAGNOSTICO -> {
                    item { Etiqueta("ESTADO MEDIDO") }
                    items(lineasDiag) { l -> FilaDiag(l) }
                    item { Spacer(Modifier.height(6.dp)) }
                    item { Etiqueta("EL FILTRO DE SEGURIDAD, A LA VISTA") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Seguridad.resumenListas().forEach { (titulo, lista) ->
                                Column(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                        .background(Cristal.vidrio).padding(12.dp),
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
                                .background(Cristal.vidrio)
                                .border(1.dp, Cristal.bordeLuz, RoundedCornerShape(12.dp)).padding(14.dp),
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

        // Píldora flotante: la navegación vive encima del contenido, no en una fila.
        PildoraFlotante(
            actual = seccion,
            onSeccion = { seccion = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        }
    }
}

/**
 * Barra de navegación en píldora, flotando sobre el contenido.
 *
 * Es el gesto de iOS 26 / ReDantotsu: cristal translúcido, esquinas de píldora y la
 * sección activa marcada con una pastilla que se desliza. Solo caben las secciones sin
 * scroll horizontal: si algún día hay más de seis, esto tiene que cambiar a scroll.
 */
@Composable
private fun PildoraFlotante(
    actual: Seccion,
    onSeccion: (Seccion) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .padding(horizontal = 14.dp, vertical = 18.dp)
            .fillMaxWidth()
            .vidrioAlto(28)
            .padding(horizontal = 5.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Seccion.entries.forEach { s ->
            val sel = s == actual
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (sel) Cristal.violeta.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable { onSeccion(s) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    s.icono,
                    fontSize = 13.sp,
                    color = if (sel) Cristal.violeta else Cristal.textoSuave,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    s.titulo,
                    fontSize = 8.sp,
                    letterSpacing = 0.2.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                    color = if (sel) Cristal.texto else Cristal.textoSuave,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Pestanas(actual: Seccion, onSeccion: (Seccion) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Seccion.entries.forEach { s ->
            val sel = s == actual
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (sel) Cristal.verde else Cristal.vidrio)
                    .clickable { onSeccion(s) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    s.titulo,
                    fontSize = 11.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                    color = if (sel) Cristal.fondoArriba else Cristal.textoSuave,
                )
            }
        }
    }
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Cristal.vidrio)
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp)).padding(14.dp),
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
private fun Etiqueta(t: String) {
    Text(t, fontSize = 10.sp, letterSpacing = 1.6.sp, color = Cristal.textoSuave, fontWeight = FontWeight.Bold)
}

@Composable
private fun Aviso(texto: String, color: Color) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(10.dp)).padding(12.dp),
    ) {
        Text(texto, fontSize = 12.sp, lineHeight = 17.sp, color = color.copy(alpha = 0.95f))
    }
}

@Composable
private fun TablaActual(actual: Map<String, String>) = TablaFiltrada(actual, actual.keys.toList())

@Composable
private fun TablaFiltrada(actual: Map<String, String>, claves: List<String>) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Cristal.vidrio)
            .border(1.dp, Cristal.bordeLuz, RoundedCornerShape(12.dp)).padding(14.dp),
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
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (seleccionado) Cristal.vidrioFuerte else Cristal.vidrio)
            .border(if (seleccionado) 1.5.dp else 1.dp, borde, RoundedCornerShape(12.dp))
            .clickable { onPerfil() }.padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(p.nombre, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Cristal.texto)
                Spacer(Modifier.height(2.dp))
                Text("${p.soc} · ${p.gpu}", fontSize = 12.sp, color = Cristal.textoSuave)
            }
            Text(textoConf, fontSize = 9.sp, letterSpacing = 1.sp, color = colorConf, fontWeight = FontWeight.Bold)
        }
        if (p.nota.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(p.nota, fontSize = 11.sp, lineHeight = 15.sp, color = Cristal.textoSuave.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun FilaApp(a: Inventario.App) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Cristal.vidrio)
            .padding(horizontal = 12.dp, vertical = 9.dp),
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Cristal.vidrio)
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
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Cristal.vidrio)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(14.dp),
    ) {
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

@Composable
private fun Boton(texto: String, color: Color, mod: Modifier, onClick: () -> Unit) {
    Box(
        mod.clip(RoundedCornerShape(10.dp)).background(color).clickable { onClick() }.padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, color = if (color == Cristal.verde) Cristal.fondoArriba else Cristal.texto,
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Pie() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Cristal.vidrio).padding(14.dp),
    ) {
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
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
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
    val borde = if (activa) Cristal.rojo else Cristal.bordeLuz
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (activa) Cristal.vidrioFuerte else Cristal.vidrio)
            .border(if (activa) 1.5.dp else 1.dp, borde, RoundedCornerShape(12.dp))
            .clickable { onSimular() }.padding(14.dp),
    ) {
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
    Column(Modifier.fillMaxWidth().vidrio(16).padding(13.dp)) {
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
                Boton("Congelar", Cristal.vidrioFuerte, Modifier.weight(1f)) { onCongelar() }
            }
            Boton("Caché", Cristal.vidrioFuerte, Modifier.weight(1f)) { onLimpiar() }
            Boton(
                if (hechos > 0) "Panel · $hechos" else "Panel",
                Cristal.violeta, Modifier.weight(1f),
            ) { onPanel() }
        }
    }
}

@Composable
private fun FilaFicha(f: Ficha, onPanel: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().vidrio(18).clickable { onPanel() }.padding(15.dp),
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
                fontSize = 20.sp, color = Cristal.violeta, fontWeight = FontWeight.Bold,
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
