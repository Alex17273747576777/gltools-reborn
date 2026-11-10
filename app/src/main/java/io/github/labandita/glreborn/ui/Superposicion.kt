package io.github.labandita.glreborn.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.labandita.glreborn.core.AjustesSuperposicion

/**
 * Superposiciones: el punto de mira y el contador de FPS que se dibujan encima del juego.
 *
 * Es la única pantalla de esta app que se usa **mientras se juega**, y por eso está pensada al
 * revés que las demás: aquí no se lee, se elige con el pulgar y se vuelve al juego. Todo cabe
 * en una pantalla, sin scroll, y la vista previa es **el objeto real** dibujado con el mismo
 * `Path` que la capa de encima, no una imagen.
 *
 * No necesita root ni Shizuku. Necesita un permiso normal de Android («dibujar encima de otras
 * apps»), que el usuario concede a mano y puede quitar cuando quiera.
 */
@Composable
fun PantallaSuperposicion(
    a: AjustesSuperposicion,
    tienePermiso: Boolean,
    activo: Boolean,
    onCambiar: (AjustesSuperposicion) -> Unit,
    onPedirPermiso: () -> Unit,
    onActivar: () -> Unit,
    onDesactivar: () -> Unit,
    onMover: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {

        // ── Cómo se usa, antes de los ajustes. La duda número uno era «vale, lo activo… ¿y
        //    ahora qué?». Se contesta aquí, en tres líneas, antes de pedir nada.
        Grupo(rotulo = "CÓMO SE USA") {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                listOf(
                    "1" to "Dale permiso de dibujar encima de otras apps. Es el único permiso que " +
                        "necesita y se lo puedes quitar cuando quieras.",
                    "2" to "Actívala. Aparece un punto de mira y un contador de FPS, sin tapar los toques: " +
                        "el juego recibe los dedos como si no estuviera.",
                    "3" to "Abre el juego. Si algo no cuadra, arrástrala: se queda donde la dejes.",
                ).forEachIndexed { i, (n, texto) ->
                    if (i > 0) Hilo(sangrado = 15)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)) {
                        Text(
                            n,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Cristal.marca,
                            modifier = Modifier.width(20.dp),
                        )
                        Text(
                            texto,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = Cristal.textoSuave,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ── Estado. Verde si está dibujando, ámbar si falta permiso.
        val color = when {
            activo -> Cristal.verde
            tienePermiso -> Cristal.marca
            else -> Cristal.ambar
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Cristal.superficie)
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(color.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center,
            ) {
                if (activo) Iconos.Visto(tamano = 16.dp, color = Cristal.fondoArriba)
                else Iconos.Rayo(tamano = 15.dp, color = Cristal.fondoArriba)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        activo -> "Dibujando encima"
                        tienePermiso -> "Listo para encender"
                        else -> "Falta el permiso"
                    },
                    fontSize = 14.sp, color = Cristal.texto, fontWeight = FontWeight.SemiBold,
                )
                Text(
                    when {
                        activo -> a.resumen()
                        tienePermiso -> "Android permite dibujar encima: enciéndelo cuando estés en el juego."
                        else -> "Android exige autorización manual para dibujar encima de otras apps."
                    },
                    fontSize = 10.5.sp, lineHeight = 15.sp, color = Cristal.textoSuave,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        if (!tienePermiso) {
            BotonPlano("Dar permiso de superposición", Cristal.ambar, Modifier.fillMaxWidth()) {
                onPedirPermiso()
            }
        } else if (activo) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonPlano("Mover el punto", Cristal.superficieMasAlta, Modifier.weight(1f)) { onMover() }
                BotonPlano("Quitar", Cristal.rojo, Modifier.weight(1f)) { onDesactivar() }
            }
        } else {
            BotonPlano("Encender encima del juego", Cristal.verde, Modifier.fillMaxWidth()) {
                onActivar()
            }
        }

        // ── Vista previa: se dibuja el mismo punto, no una captura.
        Spacer(Modifier.height(14.dp))
        Etiqueta("VISTA PREVIA")
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Cristal.fondoAbajo),
            contentAlignment = Alignment.Center,
        ) {
            VistaPreviaMira(a)
            // Rótulo de FPS, en la esquina, como en la capa real.
            if (a.fps) {
                Text(
                    "FPS 60", fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                    color = Cristal.texto.copy(alpha = 0.75f),
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                )
            }
            Text(
                "así se verá encima del juego",
                fontSize = 9.sp, color = Cristal.textoSuave.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
            )
        }

        // ── Forma del punto.
        Spacer(Modifier.height(14.dp))
        Etiqueta("FORMA")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("MIRA" to "Mira", "CIRCULO" to "Círculo", "CRUZ" to "Cruz",
                "APAGADO" to "Nada").forEach { (valor, texto) ->
                Segmento(texto, a.modo.uppercase() == valor, Modifier.weight(1f)) {
                    onCambiar(a.copy(modo = valor))
                }
            }
        }

        // ── Tamaño y grosor: los cuatro valores, no un deslizador que no se puede afinar.
        Spacer(Modifier.height(12.dp))
        Etiqueta("TAMAÑO · ${a.tamano.toInt()} px")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AjustesSuperposicion.TAMANOS.forEach { t ->
                Segmento("${t.toInt()}", a.tamano == t, Modifier.weight(1f)) {
                    onCambiar(a.copy(tamano = t))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Etiqueta("GROSOR · ${a.grosor} px")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AjustesSuperposicion.GROSORES.forEach { g ->
                Segmento("$g", a.grosor == g, Modifier.weight(1f)) {
                    onCambiar(a.copy(grosor = g))
                }
            }
        }

        // ── Color.
        Spacer(Modifier.height(12.dp))
        Etiqueta("COLOR · ${a.colorLegible().uppercase()}")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AjustesSuperposicion.COLORES.forEach { c ->
                val elegido = a.color == c
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(
                            if (elegido) 2.5.dp else 1.dp,
                            if (elegido) Cristal.texto else Cristal.bordeLuz,
                            CircleShape,
                        )
                        .clickable { onCambiar(a.copy(color = c)) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (elegido) Iconos.Visto(tamano = 14.dp, color = Cristal.fondoArriba)
                }
            }
        }

        // ── Contador de FPS.
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Cristal.superficie)
                .clickable { onCambiar(a.copy(fps = !a.fps)) }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Contador de FPS", fontSize = 13.sp, color = Cristal.texto,
                    fontWeight = FontWeight.Medium)
                Text(
                    "Número real medido con el reloj de la pantalla, no una estimación.",
                    fontSize = 10.sp, lineHeight = 14.sp, color = Cristal.textoSuave,
                )
            }
            Box(
                Modifier
                    .width(42.dp).height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (a.fps) Cristal.marca else Cristal.superficieMasAlta)
                    .padding(3.dp),
                contentAlignment = if (a.fps) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(Modifier.size(18.dp).clip(CircleShape).background(Cristal.fondoArriba))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Ni root ni Shizuku: esto usa la API pública de superposiciones de Android. " +
                "Mientras está encendido verás una notificación fija con el botón «Quitar»: " +
                "Android no permite dibujar encima de la pantalla en silencio, y hace bien.",
            fontSize = 9.5.sp, lineHeight = 14.sp, color = Cristal.textoSuave,
        )
    }
}

/** El punto, dibujado con el mismo criterio que la capa real. */
@Composable
private fun VistaPreviaMira(a: AjustesSuperposicion) {
    val color = a.colorCompose()
    val r = (a.tamano * 1.35f).dp
    Canvas(Modifier.size(120.dp)) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radio = r.toPx() / 2
        val g = a.grosor.dp.toPx()
        when (a.modo.uppercase()) {
            "APAGADO" -> {}
            "CIRCULO" -> {
                drawCircle(color, radius = radio, center = Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(g))
                drawCircle(color, radius = g * 0.7f, center = Offset(cx, cy))
            }
            "CRUZ" -> {
                drawLine(color, Offset(cx - radio * 1.6f, cy), Offset(cx + radio * 1.6f, cy),
                    strokeWidth = g, cap = StrokeCap.Round)
                drawLine(color, Offset(cx, cy - radio * 1.6f), Offset(cx, cy + radio * 1.6f),
                    strokeWidth = g, cap = StrokeCap.Round)
            }
            else -> {
                val hueco = radio * 0.36f
                drawLine(color, Offset(cx - radio, cy), Offset(cx - hueco, cy), g, StrokeCap.Round)
                drawLine(color, Offset(cx + hueco, cy), Offset(cx + radio, cy), g, StrokeCap.Round)
                drawLine(color, Offset(cx, cy - radio), Offset(cx, cy - hueco), g, StrokeCap.Round)
                drawLine(color, Offset(cx, cy + hueco), Offset(cx, cy + radio), g, StrokeCap.Round)
                drawCircle(color, radius = g * 0.7f, center = Offset(cx, cy))
            }
        }
    }
}

/** Segmento de una fila de opciones: el elegido se marca con relleno, no con borde fino. */
@Composable
private fun Segmento(
    texto: String,
    elegido: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (elegido) Cristal.marca else Cristal.superficieAlta)
            .clickable { onClick() }
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            texto,
            fontSize = 11.sp,
            fontWeight = if (elegido) FontWeight.Bold else FontWeight.Normal,
            color = if (elegido) Cristal.texto else Cristal.textoSuave,
            maxLines = 1,
        )
    }
}
