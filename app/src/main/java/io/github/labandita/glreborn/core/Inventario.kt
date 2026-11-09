package io.github.labandita.glreborn.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/**
 * El inventario de todo lo instalado.
 *
 * Existe para una sola cosa: demostrar con datos que aquí no hay lista blanca. Una app
 * de esta clase que «funciona en 40 juegos» está eligiendo por ti; aquí el perfil se
 * aplica al sistema y esta pantalla enseña, contadas, TODAS las apps que hay —las del
 * usuario y las del sistema— sin filtrar ninguna, sin pedir cuota y sin que falte la
 * que a ti te interesa.
 */
class Inventario(private val context: Context) {

    data class App(
        val paquete: String,
        val nombre: String,
        val version: String,
        val esSistema: Boolean,
    )

    fun todas(): List<App> {
        val pm = context.packageManager
        return try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .map { info ->
                    App(
                        paquete = info.packageName,
                        nombre = pm.getApplicationLabel(info).toString(),
                        version = try {
                            pm.getPackageInfo(info.packageName, 0).versionName ?: "—"
                        } catch (t: Throwable) {
                            "—"
                        },
                        esSistema = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    )
                }
                .sortedBy { it.nombre.lowercase() }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    data class Resumen(val total: Int, val usuario: Int, val sistema: Int) {
        val texto: String
            get() = "$total apps · $usuario de usuario · $sistema de sistema · ninguna excluida"
    }

    fun resumen(): Resumen {
        val t = todas()
        val s = t.count { it.esSistema }
        return Resumen(t.size, t.size - s, s)
    }
}
