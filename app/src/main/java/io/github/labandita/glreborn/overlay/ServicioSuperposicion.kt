package io.github.labandita.glreborn.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import io.github.labandita.glreborn.R
import io.github.labandita.glreborn.core.AjustesSuperposicion

/**
 * Servicio en primer plano que sostiene la capa de punto de mira y FPS.
 *
 * Reglas que cumple, y por qué:
 *
 *  · **En primer plano y con notificación.** Android mata cualquier servicio que dibuje sin
 *    notificación, y hace bien: nadie debe poder pintar encima de la pantalla en silencio.
 *    La notificación dice qué está pasando y se puede quitar desde ahí.
 *  · **Para dibujar encima hace falta `SYSTEM_ALERT_WINDOW`**, que el usuario concede a mano
 *    en Ajustes. No se pide por la cara: si no está, el servicio no arranca y se dice.
 *  · **No es root, ni Shizuku, ni magia.** Es la API pública de Android para superposiciones.
 *  · **Touch-through**: la ventana se crea sin `FLAG_NOT_TOUCHABLE` sólo cuando se va a mover;
 *    en uso normal no captura toques.
 *  · **Se apaga sola si el usuario la quita** (onTaskRemoved), porque un punto de mira
 *    huérfano en la pantalla es un estorbo.
 */
class ServicioSuperposicion : Service() {

    private var vista: VistaSuperposicion? = null
    private var ventana: WindowManager? = null
    private var params: WindowManager.LayoutParams? = null
    private var arrastrando = false
    private var desfaseX = 0f
    private var desfaseY = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        crearCanal()
        enMarcha = this
    }

    override fun onDestroy() {
        vista?.parar()
        vista?.let { v -> runCatching { ventana?.removeView(v) } }
        vista = null
        enMarcha = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACCION_PARAR) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!puedeDibujar(this)) {
            // Se dice y se cierra. Nada de quedarse en segundo plano sin hacer nada.
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(ID_NOTIFICACION, notificacion())
        if (vista == null) montarVentana()
        aplicarAjustes(AjustesSuperposicion.leer(this))
        return START_STICKY
    }

    private fun montarVentana() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        ventana = wm
        val v = VistaSuperposicion(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            // Touch-through cuando no se está moviendo.
            isClickable = false
            isFocusable = false
        }
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            // Sin foco y sin toque: el juego sigue recibiendo los dedos.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }

        wm.addView(v, p)
        vista = v
        params = p
        v.empezar()
    }

    fun aplicarAjustes(a: AjustesSuperposicion) {
        val v = vista ?: return
        v.modo = when (a.modo.uppercase()) {
            "CIRCULO" -> VistaSuperposicion.Modo.CIRCULO
            "CRUZ" -> VistaSuperposicion.Modo.CRUZ
            "APAGADO" -> VistaSuperposicion.Modo.APAGADO
            else -> VistaSuperposicion.Modo.MIRA
        }
        v.tamanoPunto = a.tamano
        v.grosor = a.grosor
        v.colorPunto = a.color
        v.mostrarFps = a.fps
        v.centroX = a.x
        v.centroY = a.y
        v.invalidate()

        // Si el modo es APAGADO y no hay FPS, no tiene sentido gastar batería: se para.
        if (v.modo == VistaSuperposicion.Modo.APAGADO && !a.fps) {
            stopSelf()
        }
        actualizarNotificacion(a)
    }

    /**
     * Modo «mover»: se hace clickable durante 3 segundos para poder arrastrar el punto.
     * Fuera de ese momento la capa no captura nada, que es lo correcto.
     */
    fun permitirArrastreTemporal(ms: Long = 3000) {
        val p = params ?: return
        val v = vista ?: return
        p.flags = p.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        ventana?.updateViewLayout(v, p)
        v.setOnTouchListener { _, evento ->
            when (evento.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    if (v.tocaElPunto(evento.x, evento.y)) {
                        arrastrando = true
                        desfaseX = 0f; desfaseY = 0f
                        true
                    } else false
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    if (arrastrando) {
                        v.moverA(evento.x - desfaseX, evento.y - desfaseY)
                        AjustesSuperposicion.guardarPosicion(this, v.centroX, v.centroY)
                        true
                    } else false
                }
                android.view.MotionEvent.ACTION_UP -> {
                    arrastrando = false
                    // Se vuelve a dejar transparente al toque.
                    p.flags = p.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    ventana?.updateViewLayout(v, p)
                    true
                }
                else -> false
            }
        }
        v.postDelayed({
            p.flags = p.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            ventana?.updateViewLayout(v, p)
        }, ms)
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        val canal = NotificationChannel(
            CANAL, "Punto de mira y FPS", NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "Mientras está activo, se dibuja encima de la pantalla." }
        nm.createNotificationChannel(canal)
    }

    private fun notificacion(): Notification = construirNotificacion(null)

    private fun actualizarNotificacion(a: AjustesSuperposicion) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(ID_NOTIFICACION, construirNotificacion(a))
    }

    private fun construirNotificacion(a: AjustesSuperposicion?): Notification {
        val parar = PendingIntent.getService(
            this, 1,
            Intent(this, ServicioSuperposicion::class.java).setAction(ACCION_PARAR),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val abrir = PendingIntent.getActivity(
            this, 2, packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CANAL)
        else
            @Suppress("DEPRECATION") Notification.Builder(this)

        return b
            .setContentTitle("Punto de mira activo")
            .setContentText(
                a?.let { "Modo ${it.modo} · ${if (it.fps) "con FPS" else "sin FPS"}" }
                    ?: "Dibujando encima de la pantalla"
            )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(abrir)
            .addAction(
                Notification.Action.Builder(null, "Quitar", parar).build()
            )
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Si el usuario cierra la app, el punto de mira se va con ella.
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        const val CANAL = "superposicion"
        const val ID_NOTIFICACION = 41
        const val ACCION_PARAR = "io.github.labandita.glreborn.PARAR_SUPERPOSICION"

        /** ¿Tiene el usuario concedido «dibujar encima de otras apps»? */
        fun puedeDibujar(c: Context): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                Settings.canDrawOverlays(c)
            else true

        @Volatile
        private var enMarcha: ServicioSuperposicion? = null

        fun iniciar(c: Context, ajustes: AjustesSuperposicion) {
            // Se guarda antes de arrancar: que el servicio lea la última configuración al nacer.
            AjustesSuperposicion.guardar(c, ajustes)
            val i = Intent(c, ServicioSuperposicion::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) c.startForegroundService(i)
            else c.startService(i)
        }

        /** Aplica un cambio sin apagar y encender, para que se vea al instante. */
        fun aplicarEnCaliente(c: Context, a: AjustesSuperposicion) {
            enMarcha?.let { servicio ->
                (c as? android.content.ContextWrapper)?.let { }
                servicio.aplicarAjustes(a)
            } ?: run {
                // Si no está en marcha no se hace nada: no hay nada que aplicar.
            }
        }

        /** Pone la capa en modo arrastre durante 3 segundos. */
        fun activarArrastre(c: Context) {
            enMarcha?.permitirArrastreTemporal()
        }

        fun estaEnMarcha(): Boolean = enMarcha != null

        fun detener(c: Context) {
            c.stopService(Intent(c, ServicioSuperposicion::class.java))
        }
    }
}
