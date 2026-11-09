package io.github.labandita.glreborn.core

/**
 * Catálogo de perfiles de dispositivo.
 *
 * REGLA DE ESTA CASA, y es la razón de que este archivo exista:
 * aquí no hay un solo `ro.product.model` inventado. Cada perfil declara de qué fuente
 * salió su dato y con qué confianza. Los que están marcados como VERIFICADO los pude
 * leer en una ficha pública; los que están como PARCIAL sólo tienen algunos campos
 * confirmados y el resto va vacío a propósito. Un perfil con campos vacíos es más
 * útil que un perfil con campos inventados: el vacío avisa, el inventado engaña.
 *
 * Lo que se escribe y lo que NO:
 *   SÍ  → ro.product.brand / manufacturer / model / device / board / hardware,
 *         y ro.opengles.version.
 *   NO  → IMEI, número de serie, MAC, Android ID, ni nada que identifique un aparato.
 *         Falsificar un identificador único no sirve para cambiar de dispositivo:
 *         sirve para romper el rastreo y las licencias, y aquí no se toca.
 */
data class PerfilDispositivo(
    val id: String,
    val nombre: String,
    val fabricante: String,
    val marca: String,
    val modelo: String,
    val device: String,
    val board: String,
    val hardware: String,
    val soc: String,
    val gpu: String,
    val openglEs: String,
    val fuente: String,
    val confianza: Confianza,
    val nota: String = "",
) {
    enum class Confianza { VERIFICADO, PARCIAL, SIN_VERIFICAR }
}

object Perfiles {

    // ── Android genérico: sirve cuando no hay catálogo para lo que se quiere simular.

    val PIXEL_8_PRO = PerfilDispositivo(
        id = "pixel8pro",
        nombre = "Google Pixel 8 Pro",
        fabricante = "Google",
        marca = "google",
        modelo = "Pixel 8 Pro",
        device = "husky",
        board = "husky",
        hardware = "husky",
        soc = "Google Tensor G3",
        gpu = "Mali-G715",
        openglEs = "196610",
        fuente = "Google, especificaciones oficiales del Pixel 8 Pro",
        confianza = PerfilDispositivo.Confianza.VERIFICADO,
        nota = "Perfil de referencia: es el que menos sorpresas da al escribir propiedades.",
    )

    val PIXEL_9_PRO = PerfilDispositivo(
        id = "pixel9pro",
        nombre = "Google Pixel 9 Pro",
        fabricante = "Google",
        marca = "google",
        modelo = "Pixel 9 Pro",
        device = "caiman",
        board = "caiman",
        hardware = "caiman",
        soc = "Google Tensor G4",
        gpu = "Mali-G715",
        openglEs = "196610",
        fuente = "Google, especificaciones oficiales del Pixel 9 Pro",
        confianza = PerfilDispositivo.Confianza.VERIFICADO,
        nota = "El nombre de device (caiman) está publicado por Google en el árbol de AOSP.",
    )

    // ── El fabricante vivo del juego: iQOO.

    val IQOO_13 = PerfilDispositivo(
        id = "iqoo13",
        nombre = "iQOO 13",
        fabricante = "vivo",
        marca = "iQOO",
        modelo = "iQOO 13",
        device = "PD2408",
        board = "sun",
        hardware = "qcom",
        soc = "Snapdragon 8 Elite",
        gpu = "Adreno 830",
        openglEs = "196610",
        fuente = "Especificaciones publicadas del iQOO 13 (vivo). El nombre de device es PARCIAL.",
        confianza = PerfilDispositivo.Confianza.PARCIAL,
        nota = "Producto real y vendido. El modelo comercial es exacto; el código interno de dispositivo no lo pude confirmar en fuente primaria, y se marca.",
    )

    val IQOO_15 = PerfilDispositivo(
        id = "iqoo15",
        nombre = "iQOO 15",
        fabricante = "vivo",
        marca = "iQOO",
        modelo = "iQOO 15",
        device = "",
        board = "",
        hardware = "qcom",
        soc = "Snapdragon 8 Elite Gen 5",
        gpu = "Adreno 840",
        openglEs = "196610",
        fuente = "Especificaciones publicadas del iQOO 15 (vivo).",
        confianza = PerfilDispositivo.Confianza.PARCIAL,
        nota = "Los campos de device y board van VACÍOS a propósito: no encontré fuente primaria. Vacío avisa; inventado engaña.",
    )

    val IQOO_15_PRO = PerfilDispositivo(
        id = "iqoo15pro",
        nombre = "iQOO 15 Pro",
        fabricante = "vivo",
        marca = "iQOO",
        modelo = "iQOO 15 Pro",
        device = "",
        board = "",
        hardware = "qcom",
        soc = "Snapdragon 8 Elite Gen 5",
        gpu = "Adreno 840",
        openglEs = "196610",
        fuente = "Fichas de prensa del iQOO 15 Pro; el chipset aparece como Snapdragon 8 Elite 2 / Gen 5 según la fuente.",
        confianza = PerfilDispositivo.Confianza.PARCIAL,
        nota = "El nombre del chipset NO está unificado entre fuentes. Se escribe el más reciente y se dice. device y board vacíos.",
    )

    // ── Nubia / Red Magic.

    val RED_MAGIC_10_PRO = PerfilDispositivo(
        id = "redmagic10pro",
        nombre = "Red Magic 10 Pro",
        fabricante = "nubia",
        marca = "RedMagic",
        modelo = "RedMagic 10 Pro",
        device = "",
        board = "",
        hardware = "qcom",
        soc = "Snapdragon 8 Elite",
        gpu = "Adreno 830",
        openglEs = "196610",
        fuente = "Especificaciones publicadas de nubia para el Red Magic 10 Pro.",
        confianza = PerfilDispositivo.Confianza.PARCIAL,
        nota = "Gaming phone real. device y board vacíos por falta de fuente primaria.",
    )

    val RED_MAGIC_11_PRO = PerfilDispositivo(
        id = "redmagic11pro",
        nombre = "Red Magic 11 Pro",
        fabricante = "nubia",
        marca = "RedMagic",
        modelo = "RedMagic 11 Pro",
        device = "",
        board = "",
        hardware = "qcom",
        soc = "Snapdragon 8 Elite Gen 5",
        gpu = "Adreno 840",
        openglEs = "196610",
        fuente = "Especificaciones publicadas de nubia para el Red Magic 11 Pro.",
        confianza = PerfilDispositivo.Confianza.PARCIAL,
        nota = "device y board vacíos. El SoC es el mismo de los tope de gama de su generación.",
    )

    // ── Referencia de GPU: el techo contra el que se compara.

    val ADRENO_840 = "Adreno 840"
    val ADRENO_830 = "Adreno 830"

    val todos: List<PerfilDispositivo> = listOf(
        PIXEL_8_PRO, PIXEL_9_PRO,
        IQOO_13, IQOO_15, IQOO_15_PRO,
        RED_MAGIC_10_PRO, RED_MAGIC_11_PRO,
    )

    fun porId(id: String): PerfilDispositivo? = todos.firstOrNull { it.id == id }

    /** Cuántos perfiles están completos. Se publica en la interfaz: no se esconde nunca. */
    fun censo(): String {
        val completos = todos.count { it.confianza == PerfilDispositivo.Confianza.VERIFICADO }
        val parciales = todos.count { it.confianza == PerfilDispositivo.Confianza.PARCIAL }
        return "$completos verificados · $parciales parciales · ${todos.size} en total"
    }
}
