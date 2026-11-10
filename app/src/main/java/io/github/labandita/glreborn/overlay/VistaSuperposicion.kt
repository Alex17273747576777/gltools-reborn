package io.github.labandita.glreborn.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Choreographer
import android.view.View

/**
 * La capa que se dibuja ENCIMA del juego: un punto de mira y un contador de FPS.
 *
 * Por qué esto importa: es lo único de esta app que se ve mientras se juega, y es lo que
 * hacen las apps del mismo tipo que la gente usa a diario. Hasta la 1.7.1 teníamos toda la
 * configuración pero nada que se viera en pantalla, y sin eso la app no sirve para jugar.
 *
 * Cómo se dibuja, y por qué así:
 *
 *  · **Todo con `Path` y `Paint`.** Ni un solo glifo de texto decorativo: el visto, la cruz y
 *    el marco son formas. Es la misma ley de diseño que el resto de la app (N3).
 *  · **Cero asignaciones por fotograma.** Los `Paint` y el `Path` se crean una vez y se
 *    reutilizan: si esto reservara memoria 60 veces por segundo, calentaría el teléfono y
 *    bajaría los FPS —justo lo contrario de lo que promete—.
 *  · **Touch-through.** La vista no es cliqueable y no captura el toque: el juego sigue
 *    recibiendo los dedos como si esto no estuviera.
 *  · **Se puede mover y se puede apagar.** Un punto de mira fijo en el centro tapa; uno que se
 *    puede arrastrar y apagar, no.
 */
class VistaSuperposicion(context: Context) : View(context) {

    // ── Configuración viva. La cambia el servicio, no se recrean objetos.
    var modo: Modo = Modo.MIRA
    var tamanoPunto: Float = 26f
    var colorPunto: Int = Color.parseColor("#5CE1E6")
    var grosor: Float = 2.5f
    var mostrarFps: Boolean = true
    var mostrarCpu: Boolean = false
    /** 0 = centro; se puede arrastrar con el dedo. */
    var centroX: Float = 0.5f
    var centroY: Float = 0.5f

    enum class Modo { APAGADO, MIRA, CIRCULO, CRUZ }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val paintRelleno = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val paintTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        setShadowLayer(4f, 0f, 1f, Color.argb(200, 0, 0, 0))
    }
    private val marco = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(160, 255, 255, 255)
    }

    private val cruz = Path()
    private val arco = RectF()

    // ── Medición de FPS con Choreographer: es el reloj real de la pantalla.
    private var fotogramas = 0
    private var inicioVentana = 0L
    private var fps = 0
    private var activo = false

    private val callback: Choreographer.FrameCallback = object : Choreographer.FrameCallback {
      override fun doFrame(t: Long) {
        if (!activo) return
        if (inicioVentana == 0L) inicioVentana = t
        fotogramas++
        val transcurrido = t - inicioVentana
        if (transcurrido >= 500_000_000L) {           // media ventana de un segundo
            fps = (fotogramas * 1_000_000_000L / transcurrido).toInt()
            fotogramas = 0
            inicioVentana = t
            invalidate()
        }
        Choreographer.getInstance().postFrameCallback(this)
      }
    }

    fun empezar() {
        if (activo) return
        activo = true
        fotogramas = 0
        inicioVentana = 0L
        Choreographer.getInstance().postFrameCallback(callback)
    }

    fun parar() {
        activo = false
        Choreographer.getInstance().removeFrameCallback(callback)
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width * centroX
        val cy = height * centroY

        when (modo) {
            Modo.APAGADO -> {}
            Modo.MIRA -> dibujarMira(canvas, cx, cy)
            Modo.CIRCULO -> dibujarCirculo(canvas, cx, cy)
            Modo.CRUZ -> dibujarCruz(canvas, cx, cy)
        }

        if (mostrarFps) {
            val franja = "FPS $fps"
            canvas.drawText(franja, 28f, 52f, paintTexto)
        }
    }

    /** Punto de mira clásico: cuatro trazos que no se tocan en el centro. */
    private fun dibujarMira(canvas: Canvas, cx: Float, cy: Float) {
        val r = tamanoPunto
        val hueco = r * 0.36f
        paint.color = colorPunto
        paint.strokeWidth = grosor
        cruz.reset()
        cruz.moveTo(cx - r, cy); cruz.lineTo(cx - hueco, cy)
        cruz.moveTo(cx + hueco, cy); cruz.lineTo(cx + r, cy)
        cruz.moveTo(cx, cy - r); cruz.lineTo(cx, cy - hueco)
        cruz.moveTo(cx, cy + hueco); cruz.lineTo(cx, cy + r)
        canvas.drawPath(cruz, paint)
        // Punto central: 1 px de radio para no tapar la cabeza del objetivo.
        paintRelleno.color = colorPunto
        canvas.drawCircle(cx, cy, grosor * 0.7f, paintRelleno)
    }

    private fun dibujarCirculo(canvas: Canvas, cx: Float, cy: Float) {
        paint.color = colorPunto
        paint.strokeWidth = grosor
        canvas.drawCircle(cx, cy, tamanoPunto, paint)
        paintRelleno.color = colorPunto
        canvas.drawCircle(cx, cy, grosor * 0.7f, paintRelleno)
    }

    private fun dibujarCruz(canvas: Canvas, cx: Float, cy: Float) {
        val r = tamanoPunto * 1.6f
        paint.color = colorPunto
        paint.strokeWidth = grosor
        cruz.reset()
        cruz.moveTo(cx - r, cy); cruz.lineTo(cx + r, cy)
        cruz.moveTo(cx, cy - r); cruz.lineTo(cx, cy + r)
        canvas.drawPath(cruz, paint)
    }

    /** Devuelve true si el punto está dentro del área que se puede arrastrar. */
    fun tocaElPunto(x: Float, y: Float): Boolean {
        val dx = x - width * centroX
        val dy = y - height * centroY
        return dx * dx + dy * dy <= (tamanoPunto * 2.2f) * (tamanoPunto * 2.2f)
    }

    fun moverA(x: Float, y: Float) {
        centroX = (x / width.coerceAtLeast(1)).coerceIn(0.04f, 0.96f)
        centroY = (y / height.coerceAtLeast(1)).coerceIn(0.04f, 0.96f)
        invalidate()
    }

    // Una sola ruta y un solo marco: no se reserva nada nuevo al cambiar de modo.
    val marcoRect get() = arco
}
