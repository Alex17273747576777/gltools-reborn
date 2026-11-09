package io.github.labandita.glreborn.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * La única notificación de la app, y sólo cuando de verdad hace falta.
 *
 * Sirve para una cosa concreta: cuando se aplica un cambio de resolución con reversión
 * automática a los 15 segundos, el aviso permite confirmar o volver sin abrir la app —
 * y, sobre todo, sobrevive a que el sistema cierre la pantalla. Sin esto, el usuario
 * tenía que fiarse de que la app siguiera viva.
 *
 * No hay notificaciones de marketing, ni de estado, ni de «novedades»: la app no tiene
 * servidor del que recibirlas ni quiere tenerlo.
 */
object Avisos {

    private const val CANAL = "reversion"
    private const val ID = 4202

    /** Crea el canal. En Android 8+ es obligatorio declararlo antes de notificar. */
    fun preparar(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val gestor = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        if (gestor.getNotificationChannel(CANAL) != null) return
        val canal = NotificationChannel(
            CANAL,
            "Reversión automática",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Avisa cuando un cambio de pantalla se va a deshacer solo, y " +
                "deja confirmarlo sin abrir la app."
            enableVibration(true)
        }
        gestor.createNotificationChannel(canal)
    }

    /**
     * Muestra el aviso de reversión pendiente.
     *
     * La notificación NO se pone sola: sólo se llama cuando `AjustesPantalla.aplicar`
     * salió bien y por tanto hay algo que confirmar en 15 segundos.
     */
    fun reversionPendiente(context: Context, segundos: Int, detalle: String) {
        preparar(context)
        val gestor = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val abrir = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendiente = abrir?.let {
            PendingIntent.getActivity(
                context, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val constructor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            android.app.Notification.Builder(context, CANAL)
        else
            @Suppress("DEPRECATION") android.app.Notification.Builder(context)

        val aviso = constructor
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("La pantalla vuelve sola en $segundos s")
            .setContentText(detalle)
            .setStyle(android.app.Notification.BigTextStyle().bigText(detalle))
            .setOngoing(true)
            .setAutoCancel(false)
            .apply { pendiente?.let { setContentIntent(it) } }
            .build()

        try {
            gestor.notify(ID, aviso)
        } catch (t: Throwable) {
            // Sin permiso de notificaciones (Android 13+) no se avisa y no pasa nada:
            // la reversión automática ya corre en la shell, con app o sin ella.
        }
    }

    fun retirar(context: Context) {
        val gestor = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        try {
            gestor.cancel(ID)
        } catch (t: Throwable) {
        }
    }
}
