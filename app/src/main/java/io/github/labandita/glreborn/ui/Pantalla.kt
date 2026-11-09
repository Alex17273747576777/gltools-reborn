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

@Composable
fun PantallaPrincipal(
    director: Director,
    estadoVia: Director.Estado,
    perfilActivo: PerfilDispositivo?,
    resultado: Resultado?,
    actual: Map<String, String>,
    onPerfil: (PerfilDispositivo) -> Unit,
    onAplicar: () -> Unit,
    onRestaurar: () -> Unit,
    onRefrescar: () -> Unit,
) {
    Scaffold(
        containerColor = Colores.fondo,
        topBar = {
            Column(Modifier.fillMaxWidth().background(Colores.fondo).padding(20.dp, 18.dp, 20.dp, 8.dp)) {
                Text(
                    "GLTOOLS REBORN",
                    fontSize = 11.sp, letterSpacing = 2.sp,
                    color = Colores.verde, fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Perfiles de dispositivo sin servidores",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Colores.texto,
                )
                Spacer(Modifier.height(10.dp))
                BarraEstado(estadoVia, director.descripcion())
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Etiqueta("EL TELÉFONO DICE AHORA") }
            item { TablaActual(actual) }

            item { Spacer(Modifier.height(8.dp)) }
            item { Etiqueta("PERFILES · ${Perfiles.censo()}") }

            items(Perfiles.todos) { p ->
                TarjetaPerfil(
                    p = p,
                    seleccionado = perfilActivo?.id == p.id,
                    onPerfil = { onPerfil(p) },
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Boton("Aplicar perfil", Colores.verde, Modifier.weight(1f)) { onAplicar() }
                    Boton("Restaurar", Colores.superficieAlta, Modifier.weight(1f)) { onRestaurar() }
                }
            }
            item {
                TextButton(onClick = onRefrescar) {
                    Text("Volver a leer el estado del teléfono", color = Colores.textoSuave, fontSize = 13.sp)
                }
            }

            resultado?.let { r ->
                item { Etiqueta("RESULTADO DE LA ÚLTIMA ACCIÓN") }
                item { TarjetaResultado(r) }
            }

            item { Spacer(Modifier.height(12.dp)) }
            item { Pie() }
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
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Colores.superficie)
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(14.dp),
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
private fun TablaActual(actual: Map<String, String>) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Colores.superficie).border(1.dp, Colores.borde, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (actual.isEmpty()) {
            Text(
                "Sin vía de lectura: Shizuku no está activo y no hay root. " +
                    "Los valores reales no se pueden leer sin uno de los dos.",
                fontSize = 13.sp, color = Colores.ambar, lineHeight = 18.sp,
            )
        } else {
            actual.forEach { (k, v) ->
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
            .clickable { onPerfil() }
            .padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
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
private fun TarjetaResultado(r: Resultado) {
    val color = if (r.ok) Colores.verde else Colores.ambar
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Colores.superficie).border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(14.dp),
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
        mod.clip(RoundedCornerShape(10.dp)).background(color).clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            texto,
            color = if (color == Colores.verde) Colores.fondo else Colores.texto,
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun Pie() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Colores.superficie).padding(14.dp),
    ) {
        Text("CÓDIGO ABIERTO · SIN SERVIDORES", fontSize = 10.sp, letterSpacing = 1.4.sp,
            color = Colores.verde, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Ninguna función de esta app pide nada a internet. No hay cuentas, ni monedas, " +
                "ni comprobación de compra, ni telemetría. Los perfiles viajan dentro del APK: " +
                "si el teléfono está en modo avión, todo funciona igual.",
            fontSize = 12.sp, lineHeight = 17.sp, color = Colores.textoSuave,
        )
    }
}
