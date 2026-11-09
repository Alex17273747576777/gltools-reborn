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

enum class Seccion(val titulo: String) {
    PERFILES("Perfiles"),
    APLICACIONES("Apps"),
    GPU("GPU"),
    DIAGNOSTICO("Diagnóstico"),
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
    onPerfil: (PerfilDispositivo) -> Unit,
    onAplicar: () -> Unit,
    onRestaurar: () -> Unit,
    onRefrescar: () -> Unit,
) {
    var seccion by remember { mutableStateOf(Seccion.PERFILES) }

    Scaffold(
        containerColor = Colores.fondo,
        topBar = {
            Column(Modifier.fillMaxWidth().background(Colores.fondo).padding(20.dp, 18.dp, 20.dp, 0.dp)) {
                Text("GLTOOLS REBORN", fontSize = 11.sp, letterSpacing = 2.sp,
                    color = Colores.verde, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Perfiles de dispositivo", style = MaterialTheme.typography.headlineMedium, color = Colores.texto)
                Spacer(Modifier.height(10.dp))
                BarraEstado(estadoVia, director.descripcion())
                Spacer(Modifier.height(14.dp))
                Pestanas(seccion) { seccion = it }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp),
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
                            Boton("Aplicar perfil", Colores.verde, Modifier.weight(1f)) { onAplicar() }
                            Boton("Restaurar", Colores.superficieAlta, Modifier.weight(1f)) { onRestaurar() }
                        }
                    }
                    resultado?.let { r ->
                        item { Spacer(Modifier.height(6.dp)) }
                        item { Etiqueta("RESULTADO DE LA ÚLTIMA ACCIÓN") }
                        item { TarjetaResultado(r) }
                    }
                }

                Seccion.APLICACIONES -> {
                    item { Etiqueta("TODO LO INSTALADO, SIN FILTRO") }
                    item {
                        Aviso(
                            "${apps.size} apps detectadas. No hay lista blanca ni cuota: el perfil se aplica " +
                                "al sistema, así que alcanza a todas a la vez —incluidas estas— sin que tengas que " +
                                "elegir una por una.",
                            Colores.verde,
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
                            Colores.ambar,
                        )
                    }
                    item { Etiqueta("QUÉ PERFILES DE GPU HAY") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Perfiles.todos.map { it.gpu }.distinct().forEach { g ->
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                        .background(Colores.superficie).padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(g, fontSize = 13.sp, color = Colores.texto)
                                    Text(
                                        "${Perfiles.todos.count { it.gpu == g }} perfil(es)",
                                        fontSize = 11.sp, color = Colores.textoSuave,
                                    )
                                }
                            }
                        }
                    }
                }

                Seccion.DIAGNOSTICO -> {
                    item { Etiqueta("ESTADO MEDIDO") }
                    items(lineasDiag) { l -> FilaDiag(l) }
                    item { Spacer(Modifier.height(6.dp)) }
                    item { Etiqueta("LO QUE ESTA APP NO PUEDE HACER") }
                    item {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Colores.superficie)
                                .border(1.dp, Colores.borde, RoundedCornerShape(12.dp)).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            limites.forEach {
                                Row {
                                    Text("· ", color = Colores.rojo, fontSize = 12.sp)
                                    Text(it, fontSize = 12.sp, lineHeight = 17.sp, color = Colores.textoSuave)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(10.dp)) }
            item {
                TextButton(onClick = onRefrescar) {
                    Text("Volver a leer el estado del teléfono", color = Colores.textoSuave, fontSize = 13.sp)
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
            item { Pie() }
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
                    .background(if (sel) Colores.verde else Colores.superficie)
                    .clickable { onSeccion(s) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    s.titulo,
                    fontSize = 11.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                    color = if (sel) Colores.fondo else Colores.textoSuave,
                )
            }
        }
    }
}

@Composable
private fun BarraEstado(estado: Director.Estado, descripcion: String) {
    val (color, texto) = when (estado) {
        Director.Estado.SHIZUKU_LISTO -> Colores.verde to "SHIZUKU"
        Director.Estado.ROOT_LISTO -> Colores.verde to "ROOT"
        Director.Estado.SHIZUKU_SIN_PERMISO -> Colores.ambar to "SIN PERMISO"
        Director.Estado.SIN_VIA -> Colores.rojo to "SIN VÍA"
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Colores.superficie)
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp)).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
            Spacer(Modifier.width(8.dp))
            Text(texto, fontSize = 11.sp, letterSpacing = 1.5.sp, color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text(descripcion, fontSize = 13.sp, lineHeight = 18.sp, color = Colores.textoSuave)
    }
}

@Composable
private fun Etiqueta(t: String) {
    Text(t, fontSize = 10.sp, letterSpacing = 1.6.sp, color = Colores.textoSuave, fontWeight = FontWeight.Bold)
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Colores.superficie)
            .border(1.dp, Colores.borde, RoundedCornerShape(12.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (actual.isEmpty()) {
            Text(
                "Sin vía de lectura: Shizuku no está activo y no hay root. Los valores reales " +
                    "no se pueden leer sin uno de los dos.",
                fontSize = 13.sp, color = Colores.ambar, lineHeight = 18.sp,
            )
        } else {
            claves.forEach { k ->
                val v = actual[k] ?: return@forEach
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(k.replace("ro.", ""), fontSize = 12.sp, color = Colores.textoSuave,
                        fontFamily = FontFamily.Monospace)
                    Text(v.ifBlank { "—" }, fontSize = 12.sp, color = Colores.texto,
                        fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun TarjetaPerfil(p: PerfilDispositivo, seleccionado: Boolean, onPerfil: () -> Unit) {
    val borde = if (seleccionado) Colores.verde else Colores.borde
    val (colorConf, textoConf) = when (p.confianza) {
        PerfilDispositivo.Confianza.VERIFICADO -> Colores.verde to "VERIFICADO"
        PerfilDispositivo.Confianza.PARCIAL -> Colores.ambar to "PARCIAL"
        PerfilDispositivo.Confianza.SIN_VERIFICAR -> Colores.rojo to "SIN VERIFICAR"
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (seleccionado) Colores.superficieAlta else Colores.superficie)
            .border(if (seleccionado) 1.5.dp else 1.dp, borde, RoundedCornerShape(12.dp))
            .clickable { onPerfil() }.padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(p.nombre, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Colores.texto)
                Spacer(Modifier.height(2.dp))
                Text("${p.soc} · ${p.gpu}", fontSize = 12.sp, color = Colores.textoSuave)
            }
            Text(textoConf, fontSize = 9.sp, letterSpacing = 1.sp, color = colorConf, fontWeight = FontWeight.Bold)
        }
        if (p.nota.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(p.nota, fontSize = 11.sp, lineHeight = 15.sp, color = Colores.textoSuave.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun FilaApp(a: Inventario.App) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Colores.superficie)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(a.nombre, fontSize = 13.sp, color = Colores.texto)
            Text(a.paquete, fontSize = 10.sp, color = Colores.textoSuave, fontFamily = FontFamily.Monospace)
        }
        Text(
            if (a.esSistema) "sistema" else "usuario",
            fontSize = 9.sp, letterSpacing = 0.6.sp, color = Colores.textoSuave,
        )
    }
}

@Composable
private fun FilaDiag(l: Diagnostico.Linea) {
    val color = when (l.estado) {
        Diagnostico.Linea.Estado.BIEN -> Colores.verde
        Diagnostico.Linea.Estado.ATENCION -> Colores.ambar
        Diagnostico.Linea.Estado.MAL -> Colores.rojo
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Colores.superficie)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(l.etiqueta, fontSize = 11.sp, color = Colores.textoSuave)
            Spacer(Modifier.height(2.dp))
            Text(l.valor, fontSize = 13.sp, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TarjetaResultado(r: Resultado) {
    val color = if (r.ok) Colores.verde else Colores.ambar
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Colores.superficie)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(14.dp),
    ) {
        Text(r.detalle, fontSize = 13.sp, color = color, lineHeight = 19.sp, fontWeight = FontWeight.Medium)
        if (r.lineas.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            r.lineas.forEach {
                Text(it, fontSize = 11.sp, color = Colores.textoSuave,
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
        Text(texto, color = if (color == Colores.verde) Colores.fondo else Colores.texto,
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Pie() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Colores.superficie).padding(14.dp),
    ) {
        Text("CÓDIGO ABIERTO · SIN SERVIDORES", fontSize = 10.sp, letterSpacing = 1.4.sp,
            color = Colores.verde, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Ninguna función de esta app pide nada a internet. No hay cuentas, ni monedas, ni " +
                "comprobación de compra, ni telemetría. Los perfiles viajan dentro del APK: si el " +
                "teléfono está en modo avión, todo funciona igual.",
            fontSize = 12.sp, lineHeight = 17.sp, color = Colores.textoSuave,
        )
    }
}
