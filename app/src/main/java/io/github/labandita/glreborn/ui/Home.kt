package io.github.labandita.glreborn.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.labandita.glreborn.core.Datos
import io.github.labandita.glreborn.core.Modo
import io.github.labandita.glreborn.core.PerfilDispositivo
import io.github.labandita.glreborn.core.Rendimiento

/**
 * Home: el resumen, y lo primero que se abre.
 *
 * El GLTools original abre en un Home con un cartel verde de «Rooted mode», dos tiles
 * grandes (Add game · Device Info) y la tarjeta del proyecto. Esta pantalla tiene la
 * misma arquitectura —estado arriba, dos tiles de acción, tarjeta de identidad— y el
 * mismo peso visual, pero lo que dice el cartel son datos medidos, y los tiles llevan a
 * algo que se puede hacer en ESTE teléfono.
 */
@Composable
fun PantallaHome(
    banner: Banner,
    puede: String,
    avisos: List<Estado.Par>,
    perfilActivo: PerfilDispositivo?,
    modo: Modo,
    actual: Map<String, String>,
    datosSonda: io.github.labandita.glreborn.core.Sonda.Datos?,
    resumenCompat: String,
    apps: Int,
    congeladas: Int,
    fichas: Int,
    ediciones: Int,
    sistemas: Int,
    acciones: Int,
    version: String,
    onIrA: (Destino, Pestana?) -> Unit,
    onAplicar: () -> Unit,
    onRestaurar: () -> Unit,
    onModo: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 10.dp, 18.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── El cartel grande de estado: lo primero, como en el original.
        item { BannerGrande(banner, puede) }

        // ── Avisos: sólo si hay algo que decir. Sin relleno.
        if (avisos.isNotEmpty()) {
            item { Etiqueta("ANTES DE NADA") }
            items(avisos) { a -> TarjetaAviso(a) }
        }

        // ── El perfil activo o la invitación a elegir uno.
        item { Etiqueta("PERFIL EN USO") }
        item {
            if (perfilActivo == null) {
                Column(Modifier.fillMaxWidth().vidrio(20).padding(16.dp)) {
                    Text("Ninguno todavía", fontSize = 16.sp, color = Cristal.texto,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Elige uno del catálogo o genera uno con lo que tu teléfono declara " +
                            "de verdad.",
                        fontSize = 12.sp, lineHeight = 18.sp, color = Cristal.textoSuave,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonHome("Ver catálogo", Cristal.violeta, Modifier.weight(1f)) {
                            onIrA(Destino.PERFILES, Pestana.CATALOGO)
                        }
                        BotonHome("Generar", Cristal.vidrioFuerte, Modifier.weight(1f)) {
                            onIrA(Destino.PERFILES, Pestana.GENERAR)
                        }
                    }
                }
            } else {
                TarjetaPerfilActivo(perfilActivo, onAplicar, onRestaurar) {
                    onIrA(Destino.PERFILES, Pestana.CATALOGO)
                }
            }
        }

        // ── Los dos tiles grandes, como «Add game» y «Device Info» del original.
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TileGrande(
                    titulo = "Herramientas",
                    subtitulo = "$acciones acciones sin root",
                    color = Cristal.violeta,
                    modifier = Modifier.weight(1f),
                    icono = { c -> Iconos.Rayo(tamano = 26.dp, color = c) },
                ) { onIrA(Destino.HERRAMIENTAS, Pestana.RENDIMIENTO) }
                TileGrande(
                    titulo = "Tu teléfono",
                    subtitulo = datosSonda?.resumen?.take(40) ?: "leer propiedades",
                    color = Cristal.cian,
                    modifier = Modifier.weight(1f),
                    icono = { c -> Iconos.BaseDeDatos(tamano = 26.dp, color = c) },
                ) { onIrA(Destino.AYUDA, Pestana.DIAGNOSTICO) }
            }
        }

        // ── Lo que dice el teléfono ahora, en cuatro datos.
        item { Etiqueta("EL TELÉFONO DICE AHORA") }
        item {
            Column(Modifier.fillMaxWidth().vidrio(20).padding(16.dp)) {
                val claves = listOf(
                    "ro.product.model" to "Modelo",
                    "ro.product.brand" to "Marca",
                    "ro.product.device" to "Device",
                    "ro.opengles.version" to "OpenGL ES",
                )
                claves.forEachIndexed { i, (clave, etiqueta) ->
                    if (i > 0) {
                        Box(Modifier.fillMaxWidth().height(1.dp)
                            .background(Cristal.bordeLuz))
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(etiqueta, fontSize = 12.sp, color = Cristal.textoSuave)
                        Text(
                            actual[clave]?.ifBlank { "—" } ?: "—",
                            fontSize = 12.sp, color = Cristal.texto,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }

        // ── Los números de la app: datos reales que lleva dentro.
        item { Etiqueta("LO QUE HAY DENTRO") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Cifra("$apps", "apps", Cristal.violeta, Modifier.weight(1f))
                Cifra("$congeladas", "congeladas", Cristal.cian, Modifier.weight(1f))
                Cifra("$fichas", "fichas", Cristal.rosa, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Cifra("$ediciones", "ediciones", Cristal.violeta, Modifier.weight(1f))
                Cifra("$sistemas", "sistemas", Cristal.cian, Modifier.weight(1f))
                Cifra("$acciones", "acciones", Cristal.verde, Modifier.weight(1f))
            }
        }

        // ── La tarjeta de identidad, en el sitio donde el original pone la suya.
        item { Spacer(Modifier.height(4.dp)) }
        item { TarjetaIdentidad(version, modo, onModo, resumenCompat) }
    }
}

@Composable
private fun BannerGrande(b: Banner, puede: String) {
    val color = when (b.tono) {
        Banner.Tono.BIEN -> Cristal.verde
        Banner.Tono.AVISO -> Cristal.ambar
        Banner.Tono.MAL -> Cristal.rojo
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.10f))
                )
            )
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(color.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center,
            ) {
                when (b.tono) {
                    Banner.Tono.BIEN -> Iconos.Visto(tamano = 18.dp, color = Cristal.fondoArriba)
                    else -> Box(
                        Modifier.size(4.dp).clip(CircleShape).background(Cristal.fondoArriba)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                b.titulo,
                fontSize = 19.sp, color = Cristal.texto, fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(b.detalle, fontSize = 12.sp, lineHeight = 18.sp, color = Cristal.texto.copy(alpha = 0.85f))
        Spacer(Modifier.height(6.dp))
        Text(puede, fontSize = 11.sp, lineHeight = 16.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TarjetaAviso(a: Estado.Par) {
    val color = when (a.tono) {
        Banner.Tono.BIEN -> Cristal.verde
        Banner.Tono.AVISO -> Cristal.ambar
        Banner.Tono.MAL -> Cristal.rojo
    }
    Row(
        Modifier.fillMaxWidth().vidrio(18).padding(14.dp),
    ) {
        Box(
            Modifier.width(3.dp).height(38.dp).clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(a.titulo, fontSize = 13.sp, color = Cristal.texto, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(a.detalle, fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.textoSuave)
        }
    }
}

@Composable
private fun TarjetaPerfilActivo(
    p: PerfilDispositivo,
    onAplicar: () -> Unit,
    onRestaurar: () -> Unit,
    onCambiar: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().vidrio(20).padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.nombre, fontSize = 17.sp, color = Cristal.texto,
                    fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(
                    "${p.fabricante} · ${p.hardware.ifBlank { p.board.ifBlank { "—" } }}",
                    fontSize = 11.sp, color = Cristal.textoSuave,
                )
            }
            Text(
                when (p.confianza) {
                    PerfilDispositivo.Confianza.VERIFICADO -> "VERIFICADO"
                    PerfilDispositivo.Confianza.PARCIAL -> "PARCIAL"
                    PerfilDispositivo.Confianza.SIN_VERIFICAR -> "SIN VERIFICAR"
                },
                fontSize = 8.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold,
                color = when (p.confianza) {
                    PerfilDispositivo.Confianza.VERIFICADO -> Cristal.verde
                    PerfilDispositivo.Confianza.PARCIAL -> Cristal.ambar
                    PerfilDispositivo.Confianza.SIN_VERIFICAR -> Cristal.rojo
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonHome("Aplicar", Cristal.verde, Modifier.weight(1f)) { onAplicar() }
            BotonHome("Restaurar", Cristal.vidrioFuerte, Modifier.weight(1f)) { onRestaurar() }
            BotonHome("Cambiar", Cristal.vidrioFuerte, Modifier.weight(1f)) { onCambiar() }
        }
    }
}

@Composable
private fun TileGrande(
    titulo: String,
    subtitulo: String,
    color: Color,
    modifier: Modifier = Modifier,
    icono: @Composable (Color) -> Unit,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .height(126.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.08f))
                )
            )
            .border(1.dp, color.copy(alpha = 0.36f), RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        icono(color)
        Column {
            Text(titulo, fontSize = 15.sp, color = Cristal.texto, fontWeight = FontWeight.SemiBold)
            Text(subtitulo, fontSize = 10.sp, lineHeight = 14.sp, color = Cristal.textoSuave,
                maxLines = 2)
        }
    }
}

@Composable
private fun Cifra(valor: String, etiqueta: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Cristal.vidrio)
            .padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(valor, fontSize = 22.sp, color = color, fontWeight = FontWeight.Bold)
        Text(etiqueta, fontSize = 9.sp, letterSpacing = 0.6.sp, color = Cristal.textoSuave)
    }
}

@Composable
private fun TarjetaIdentidad(
    version: String,
    modo: Modo,
    onModo: () -> Unit,
    resumenCompat: String,
) {
    Column(Modifier.fillMaxWidth().vidrio(22).padding(17.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(listOf(Cristal.violeta, Cristal.cian))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Iconos.Espadas(tamano = 24.dp, color = Cristal.fondoArriba)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("GLTools Reborn", fontSize = 16.sp, color = Cristal.texto,
                    fontWeight = FontWeight.SemiBold)
                Text("Perfiles de dispositivo · sin servidores · $version",
                    fontSize = 10.sp, color = Cristal.textoSuave)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Código abierto, Apache 2.0. Sin cuenta, sin telemetría y sin nada de pago: " +
                "no hay servidor al que enviar nada.",
            fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.textoSuave,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonHome(
                if (modo == Modo.SEGURO) "Modo: seguro" else "Modo: experto",
                if (modo == Modo.SEGURO) Cristal.vidrioFuerte else Cristal.ambar,
                Modifier.weight(1f),
            ) { onModo() }
        }
        if (resumenCompat.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(resumenCompat, fontSize = 10.sp, lineHeight = 15.sp, color = Cristal.textoSuave)
        }
    }
}

@Composable
internal fun BotonHome(
    texto: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            texto,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (color == Cristal.vidrioFuerte) Cristal.texto else Cristal.fondoArriba,
            maxLines = 1,
        )
    }
}
