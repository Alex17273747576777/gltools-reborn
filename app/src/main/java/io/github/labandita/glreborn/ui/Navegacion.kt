package io.github.labandita.glreborn.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * La barra de abajo: cinco destinos en una píldora de cristal, y las subpestañas encima.
 *
 * Dos piezas, como en el iOS de ReDantotsu y como en el GLTools original (que tiene
 * Home · Configure games · Help en una barra):
 *
 *  · [PildoraDestinos] — cinco destinos, el activo con su pastilla y un muelle al cambiar.
 *  · [FilaPestanas] — las subpestañas del destino, en una tira con scroll si no caben.
 *
 * Con cinco destinos caben de verdad: antes había diez secciones en la misma píldora y
 * los títulos se pisaban.
 */
@Composable
fun PildoraDestinos(
    actual: Destino,
    onDestino: (Destino) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Cristal.superficie)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Destino.entries.forEach { d ->
            val activo = d == actual
            val escala by animateFloatAsState(
                targetValue = if (activo) 1f else 0.92f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "muelle-${d.name}",
            )
            Column(
                Modifier
                    .weight(1f)
                    .scale(escala)
                    .clickable { onDestino(d) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconoDestino(
                    destino = d,
                    color = if (activo) Cristal.marca else Cristal.textoTenue,
                    tamano = 22.dp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    d.titulo,
                    fontSize = 9.sp,
                    letterSpacing = 0.2.sp,
                    fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal,
                    color = if (activo) Cristal.texto else Cristal.textoTenue,
                    maxLines = 1,
                )
                // Indicador de 2dp: dice dónde estás sin pintar una pastilla entera.
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (activo) Cristal.marca else Color.Transparent),
                )
            }
        }
    }
}

/** Un icono dibujado por destino. Mismo trazo, mismo tamaño óptico en los cinco. */
@Composable
private fun IconoDestino(destino: Destino, color: androidx.compose.ui.graphics.Color, tamano: androidx.compose.ui.unit.Dp) {
    when (destino) {
        Destino.HOME -> Iconos.Casa(tamano = tamano, color = color)
        Destino.PERFILES -> Iconos.Llave(tamano = tamano, color = color)
        Destino.HERRAMIENTAS -> Iconos.Rayo(tamano = tamano, color = color)
        Destino.DATOS -> Iconos.BaseDeDatos(tamano = tamano, color = color)
        Destino.AYUDA -> Iconos.Interrogacion(tamano = tamano, color = color)
    }
}

/**
 * Las subpestañas del destino, en una tira. Si el destino no tiene, no se pinta nada:
 * Home es un resumen y no necesita subpestañas.
 */
@Composable
fun FilaPestanas(
    destino: Destino,
    actual: Pestana?,
    onPestana: (Pestana) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lista = Navegacion.pestanas(destino)
    if (lista.isEmpty()) return

    Row(
        modifier
            .fillMaxWidth()
            .background(Cristal.fondoArriba)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        lista.forEach { p ->
            val activa = p == actual
            Column(
                Modifier.clickable { onPestana(p) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    p.titulo.uppercase(),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    fontWeight = if (activa) FontWeight.Bold else FontWeight.Medium,
                    color = if (activa) Cristal.texto else Cristal.textoTenue,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (activa) Cristal.marca else Color.Transparent),
                )
            }
        }
    }
    // La línea que cierra la tira, para que el subrayado tenga de dónde colgar.
    Box(Modifier.fillMaxWidth().height(1.dp).background(Cristal.bordeLuz))
}

/** Chip de modo para la cabecera. Usa el enum real del core, no una copia. */
@Composable
fun ChipModo(modo: io.github.labandita.glreborn.core.Modo, onModo: () -> Unit, modifier: Modifier = Modifier) {
    val (texto, color) = when (modo) {
        io.github.labandita.glreborn.core.Modo.SEGURO -> "SEGURO" to Cristal.verde
        io.github.labandita.glreborn.core.Modo.EXPERTO -> "EXPERTO" to Cristal.ambar
    }
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable { onModo() }
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(texto, fontSize = 9.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold,
            color = color)
    }
}
