package io.github.labandita.glreborn.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Iconos dibujados a mano con `Path`, no glifos de fuente.
 *
 * Antes la navegación usaba `◇ ◆ ✦ ▤ ?`: caracteres de una tipografía cualquiera, con
 * trazos gruesos y tamaños distintos entre sí. Eso es exactamente lo que hace que una
 * app parezca HTML: iconos que no están dibujados, sino escritos.
 *
 * Aquí cada icono es una ruta vectorial: mismo grosor de trazo, mismas esquinas
 * redondeadas, mismo tamaño óptico. Se adaptan solos al color y al tamaño.
 */
object Iconos {

    @Composable
    fun Casa(modifier: Modifier = Modifier, tamano: Dp = 22.dp, color: Color = Cristal.texto) =
        Trazo(modifier, tamano, color, relleno = false) { w, h ->
            val p = Path()
            // Tejado
            p.moveTo(w * 0.08f, h * 0.46f)
            p.lineTo(w * 0.5f, h * 0.10f)
            p.lineTo(w * 0.92f, h * 0.46f)
            // Cuerpo
            p.moveTo(w * 0.18f, h * 0.42f)
            p.lineTo(w * 0.18f, h * 0.90f)
            p.lineTo(w * 0.82f, h * 0.90f)
            p.lineTo(w * 0.82f, h * 0.42f)
            // Puerta
            p.moveTo(w * 0.42f, h * 0.90f)
            p.lineTo(w * 0.42f, h * 0.66f)
            p.lineTo(w * 0.58f, h * 0.66f)
            p.lineTo(w * 0.58f, h * 0.90f)
            p
        }

    @Composable
    fun Llave(modifier: Modifier = Modifier, tamano: Dp = 22.dp, color: Color = Cristal.texto) =
        Trazo(modifier, tamano, color, relleno = false) { w, h ->
            val p = Path()
            val r = w * 0.26f
            // Anillo
            p.addOval(androidx.compose.ui.geometry.Rect(w * 0.08f, h * 0.10f, w * 0.08f + 2 * r, h * 0.10f + 2 * r))
            val cy = h * 0.10f + r
            // Caña en diagonal
            p.moveTo(w * 0.08f + r * 0.72f, cy + r * 0.72f)
            p.lineTo(w * 0.88f, h * 0.92f)
            // Diente
            p.moveTo(w * 0.68f, h * 0.72f)
            p.lineTo(w * 0.80f, h * 0.60f)
            p
        }

    @Composable
    fun Rayo(modifier: Modifier = Modifier, tamano: Dp = 22.dp, color: Color = Cristal.texto) =
        Trazo(modifier, tamano, color, relleno = true) { w, h ->
            val p = Path()
            p.moveTo(w * 0.56f, h * 0.06f)
            p.lineTo(w * 0.22f, h * 0.56f)
            p.lineTo(w * 0.46f, h * 0.56f)
            p.lineTo(w * 0.40f, h * 0.94f)
            p.lineTo(w * 0.78f, h * 0.42f)
            p.lineTo(w * 0.52f, h * 0.42f)
            p.close()
            p
        }

    @Composable
    fun BaseDeDatos(modifier: Modifier = Modifier, tamano: Dp = 22.dp, color: Color = Cristal.texto) =
        Trazo(modifier, tamano, color, relleno = false) { w, h ->
            val p = Path()
            val rx = w * 0.30f
            val ry = h * 0.11f
            p.addOval(androidx.compose.ui.geometry.Rect(w * 0.20f, h * 0.10f, w * 0.80f, h * 0.10f + 2 * ry))
            p.moveTo(w * 0.20f, h * 0.21f)
            p.lineTo(w * 0.20f, h * 0.79f)
            p.moveTo(w * 0.80f, h * 0.21f)
            p.lineTo(w * 0.80f, h * 0.79f)
            // Dos arcos
            val arco = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(w * 0.20f, h * 0.42f, w * 0.80f, h * 0.42f + 2 * ry))
            }
            val arco2 = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(w * 0.20f, h * 0.62f, w * 0.80f, h * 0.62f + 2 * ry))
            }
            p.addPath(arco)
            p.addPath(arco2)
            p
        }

    @Composable
    fun Interrogacion(modifier: Modifier = Modifier, tamano: Dp = 22.dp, color: Color = Cristal.texto) =
        Trazo(modifier, tamano, color, relleno = false) { w, h ->
            val p = Path()
            // Aro
            p.addOval(androidx.compose.ui.geometry.Rect(w * 0.06f, h * 0.06f, w * 0.94f, h * 0.94f))
            // Signo
            val q = Path()
            q.moveTo(w * 0.38f, h * 0.38f)
            q.cubicTo(w * 0.40f, h * 0.22f, w * 0.64f, h * 0.22f, w * 0.62f, h * 0.42f)
            q.cubicTo(w * 0.60f, h * 0.56f, w * 0.50f, h * 0.56f, w * 0.50f, h * 0.66f)
            p.addPath(q)
            val punto = Path()
            punto.addOval(androidx.compose.ui.geometry.Rect(w * 0.46f, h * 0.74f, w * 0.56f, h * 0.84f))
            p.addPath(punto)
            p
        }

    /** Aspa, para lo que falla. */
    @Composable
    fun Aspa(modifier: Modifier = Modifier, tamano: Dp = 22.dp, color: Color = Cristal.texto) =
        Trazo(modifier, tamano, color, relleno = false) { w, h ->
            val p = Path()
            p.moveTo(w * 0.22f, h * 0.22f)
            p.lineTo(w * 0.78f, h * 0.78f)
            p.moveTo(w * 0.78f, h * 0.22f)
            p.lineTo(w * 0.22f, h * 0.78f)
            p
        }

    /** Guion, para lo que no aplica: ni bien ni mal. */
    @Composable
    fun Guion(modifier: Modifier = Modifier, tamano: Dp = 22.dp, color: Color = Cristal.texto) =
        Trazo(modifier, tamano, color, relleno = false) { w, h ->
            val p = Path()
            p.moveTo(w * 0.22f, h * 0.5f)
            p.lineTo(w * 0.78f, h * 0.5f)
            p
        }

    /** Visto, para el banner verde. */
    @Composable
    fun Visto(modifier: Modifier = Modifier, tamano: Dp = 22.dp, color: Color = Cristal.texto) =
        Trazo(modifier, tamano, color, relleno = false) { w, h ->
            val p = Path()
            p.moveTo(w * 0.16f, h * 0.54f)
            p.lineTo(w * 0.40f, h * 0.78f)
            p.lineTo(w * 0.86f, h * 0.24f)
            p
        }

    /** As de picas / marca: el icono de la app, simplificado. */
    @Composable
    fun Espadas(modifier: Modifier = Modifier, tamano: Dp = 22.dp, color: Color = Cristal.texto) =
        Trazo(modifier, tamano, color, relleno = true) { w, h ->
            val p = Path()
            // Hoja izquierda
            p.moveTo(w * 0.50f, h * 0.06f)
            p.cubicTo(w * 0.18f, h * 0.36f, w * 0.16f, h * 0.62f, w * 0.34f, h * 0.70f)
            p.cubicTo(w * 0.30f, h * 0.84f, w * 0.42f, h * 0.90f, w * 0.50f, h * 0.86f)
            p.cubicTo(w * 0.58f, h * 0.90f, w * 0.70f, h * 0.84f, w * 0.66f, h * 0.70f)
            p.cubicTo(w * 0.84f, h * 0.62f, w * 0.82f, h * 0.36f, w * 0.50f, h * 0.06f)
            p.close()
            p
        }

    /**
     * El motor: dibuja el `Path` con un trazo uniforme. Si [relleno], se rellena y no se
     * traza; si no, se traza con el mismo grosor en todos los iconos.
     */
    @Composable
    private fun Trazo(
        modifier: Modifier,
        tamano: Dp,
        color: Color,
        relleno: Boolean,
        forma: (Float, Float) -> Path,
    ) {
        Canvas(modifier.size(tamano)) {
            val p = forma(size.width, size.height)
            if (relleno) {
                drawPath(p, color)
            } else {
                drawPath(
                    p,
                    color,
                    style = Stroke(
                        width = size.minDimension * 0.085f,
                        cap = StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                    ),
                )
            }
        }
    }


    /** Flecha circular de «volver a leer». */
    @Composable
    fun Refrescar(modifier: Modifier = Modifier, tamano: Dp = 22.dp, color: Color = Cristal.texto) =
        androidx.compose.foundation.Canvas(modifier.size(tamano)) {
            val w = size.width
            val h = size.height
            val grosor = size.minDimension * 0.10f
            // Arco de 280 grados, abierto arriba a la derecha
            drawArc(
                color = color,
                startAngle = -50f,
                sweepAngle = 280f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.10f, h * 0.10f),
                size = Size(w * 0.80f, h * 0.80f),
                style = Stroke(width = grosor, cap = StrokeCap.Round),
            )
            // Punta de flecha al final del arco
            val p = Path()
            p.moveTo(w * 0.62f, h * 0.08f)
            p.lineTo(w * 0.88f, h * 0.20f)
            p.lineTo(w * 0.66f, h * 0.34f)
            drawPath(p, color, style = Stroke(width = grosor, cap = StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
}
