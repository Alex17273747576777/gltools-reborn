package io.github.labandita.glreborn.core

import android.content.Context
import org.json.JSONObject

/**
 * El ranking AnTuTu Mundial, congelado dentro del APK.
 *
 * Por qué está aquí y no se pide a internet: la app no habla con ningún servidor. Se descargó una
 * vez, se guardó con su fecha y su fuente, y desde entonces es un dato local que no caduca solo.
 * Cuando cambie, se vuelve a descargar y se sube una versión nueva: así un ranking viejo nunca se
 * presenta como si fuera de hoy.
 *
 * Lo que este fichero NO es: una promesa de rendimiento. Un perfil hecho con estos datos hace que
 * el teléfono DECLARE ese modelo. No le da sus núcleos, ni su GPU, ni sus 2.500.000 puntos.
 */
data class FichaAntutu(
    val puesto: Int,
    val nombre: String,
    val marca: String,
    /** Puntos AnTuTu publicados. Null cuando la lista consultada no los da para ese modelo. */
    val antutu: Int?,
    val chipset: String,
    val gpu: String,
    val ram: String,
    val android: String,
    val nucleos: String,
    val device: String,
    val modelo: String,
    /** El nombre exacto de la ficha técnica de origen, o vacío si no había. */
    val ficha: String,
    val url: String,
    val specsFuente: String,
)

object Antutu {

    /** El día en que se copió la lista. Va a la vista: un ranking sin fecha es un rumor. */
    fun consultado(context: Context): String = runCatching {
        val raiz = JSONObject(Datos.leerTexto(context, "datos/antutu.json") ?: return "")
        raiz.optString("consultado")
    }.getOrDefault("")

    fun fuente(context: Context): String = runCatching {
        val raiz = JSONObject(Datos.leerTexto(context, "datos/antutu.json") ?: return "")
        raiz.optString("url")
    }.getOrDefault("")

    /** Los 50 del ranking, en orden. Si el fichero no está, lista vacía y la pantalla lo dice. */
    fun top(context: Context): List<FichaAntutu> = leer(context, "dispositivos")

    /** Los modelos citados que no están en el ranking, con sus datos reales y sin puntuación. */
    fun ademas(context: Context): List<FichaAntutu> = leer(context, "ademas")

    private fun leer(context: Context, clave: String): List<FichaAntutu> {
        val json = Datos.leerTexto(context, "datos/antutu.json") ?: return emptyList()
        return runCatching {
            val arr = JSONObject(json).optJSONArray(clave) ?: return emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                FichaAntutu(
                    puesto = o.optInt("puesto", 0),
                    nombre = o.optString("nombre"),
                    marca = o.optString("marca"),
                    antutu = if (o.has("antutu") && !o.isNull("antutu")) o.optInt("antutu") else null,
                    chipset = o.optString("chipset"),
                    gpu = o.optString("gpu"),
                    ram = o.optString("ram"),
                    android = o.optString("android"),
                    nucleos = o.optString("nucleos"),
                    device = o.optString("device"),
                    modelo = o.optString("modelo"),
                    ficha = o.optString("ficha"),
                    url = o.optString("url"),
                    specsFuente = o.optString("specs_fuente"),
                )
            }
        }.getOrDefault(emptyList())
    }

    /**
     * El perfil que se puede aplicar de una ficha del ranking, o el motivo por el que no.
     *
     * Regla dura: sin código de dispositivo no hay perfil. Escribir sólo la marca y el nombre
     * comercial deja `ro.product.device` con el valor del teléfono real, y entonces la app
     * prometería una cosa y el teléfono declararía otra. Un perfil a medias es peor que ninguno.
     */
    fun perfil(f: FichaAntutu): Pair<PerfilDispositivo?, String> {
        if (f.device.isBlank()) return null to
            "Sin código de dispositivo verificado: no se puede aplicar sin dejar datos a medias."
        val modelo = f.modelo.ifBlank { f.nombre }
        val marca = f.marca.ifBlank { f.nombre.split(" ").first() }
        val perfil = PerfilDispositivo(
            id = "antutu-${f.device.lowercase()}",
            nombre = f.nombre,
            fabricante = marca,
            marca = marca.lowercase(),
            modelo = modelo,
            device = f.device,
            board = f.device,
            hardware = "",
            soc = f.chipset,
            gpu = f.gpu,
            // Versión de OpenGL ES: no viene en la ficha de mobolist y NO se inventa.
            openglEs = "",
            fuente = "mobolist.net (ranking AnTuTu Mundial, copia local) + catálogo de Google Play",
            confianza = if (f.chipset.isNotBlank()) PerfilDispositivo.Confianza.PARCIAL
            else PerfilDispositivo.Confianza.SIN_VERIFICAR,
            nota = buildString {
                append("Puesto ${f.puesto.takeIf { it > 0 } ?: "—"} del ranking AnTuTu Mundial")
                if (f.antutu != null) append(", ${"%,d".format(f.antutu)} puntos publicados")
                append(". ")
                append(
                    "Aplicarlo hace que el teléfono DECLARE este modelo; no le da su rendimiento. " +
                        "Los datos que la ficha no trae (versión de OpenGL) se dejan sin escribir.",
                )
            },
        )
        return perfil to ""
    }

}
