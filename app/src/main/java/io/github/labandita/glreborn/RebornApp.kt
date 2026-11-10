package io.github.labandita.glreborn

import android.app.Application
import android.content.Intent

/**
 * La aplicación, y el cinturón de seguridad de toda ella.
 *
 * El problema que resuelve: la app se cerraba y no decía nada. Un cierre sin mensaje puede ser
 * cualquier cosa —una excepción, memoria, el fabricante matando el proceso— y sin datos sólo se
 * puede adivinar. Adivinar es lo que nos ha costado tres vueltas.
 *
 * Ahora, cuando algo revienta:
 *  1. Se guarda la excepción con su archivo y su línea, en `filesDir/ultimo-fallo.txt`.
 *  2. Se relanza la app **enseñando ese informe**, con un botón para copiarlo.
 *  3. Si el fallo es de memoria, se detecta y se dice, porque el consejo es otro.
 *
 * No se recoge ni un dato personal: sólo la excepción, la versión y el modelo.
 */
class RebornApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { hilo, fallo ->
            runCatching { RegistroFallos.guardar(this, fallo, "hilo ${hilo.name}") }

            // Relanzar para poder ENSEÑAR el fallo. Es lo que convierte un cierre opaco en un
            // informe legible. Si esto falla, se deja que el sistema haga lo de siempre.
            val relanzado = runCatching {
                val i = Intent(this, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                    putExtra(EXTRA_MOSTRAR_FALLO, true)
                }
                startActivity(i)
                true
            }.getOrDefault(false)

            if (!relanzado) anterior?.uncaughtException(hilo, fallo)
            else {
                // Se mata el proceso actual sin volver a pasar por este manejador.
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }

    companion object {
        const val EXTRA_MOSTRAR_FALLO = "mostrar_fallo"
    }
}
