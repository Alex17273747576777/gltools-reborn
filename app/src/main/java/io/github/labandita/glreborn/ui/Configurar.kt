package io.github.labandita.glreborn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.labandita.glreborn.core.PerfilDispositivo

/**
 * Configurar: la pantalla de ajustes del original, con su estructura y sus grupos.
 *
 * El «Configuration» del GLTools tiene tres niveles y aquí se respetan los tres, porque
 * es la razón por la que se entiende tan rápido:
 *
 *  1. Pestañas en mayúsculas con subrayado que se desliza: GENERAL · OPENGL · VULKAN ·
 *     IMAGEN.
 *  2. Dentro, grupos rotulados en pequeño y naranja: rendimiento, compatibilidad,
 *     emulación.
 *  3. Debajo, filas con casilla a la derecha y, algunas, etiquetas de consecuencia
 *     («Performance», «Compatibility», «Dangerous»), que el original pone en píldoras.
 *
 * Lo que NO se copia es lo que hace de menos: el original enciende cosas como «Ignore
 * OpenGL errors» o «Fake eglGetConfigAttrib» sin decir qué pasa. Aquí cada casilla dice
 * qué toca y qué cuesta, y las que no se pueden hacer sin root lo dicen en su etiqueta.
 */
@Composable
fun PantallaConfigurar(
    perfilActivo: PerfilDispositivo?,
    onIrAGenerar: () -> Unit,
    onIrAPerfiles: () -> Unit,
    filas: List<FilaConfig>,
    onAlternar: (String) -> Unit,
) {
    var grupo by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf("GENERAL")
    }
    // OJO: esto se pinta DENTRO del LazyColumn de PantallaPrincipal. Un LazyColumn
    // anidado en otro LazyColumn revienta al medir (altura infinita). Aquí va una
    // Column normal: el que desplaza es el de fuera.
    Column(Modifier.fillMaxWidth()) {
        // Pestañas en mayúsculas con subrayado: la firma visual del Configuration.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Cristal.vidrio)
                .padding(3.dp),
        ) {
            listOf("GENERAL", "OPENGL", "VULKAN", "IMAGEN").forEach { tab ->
                val activa = grupo == tab
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (activa) Cristal.vidrioFuerte else Cristal.vidrio.copy(alpha = 0f))
                        .clickable { grupo = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            tab, fontSize = 9.5.sp, letterSpacing = 0.8.sp,
                            fontWeight = if (activa) FontWeight.Bold else FontWeight.Normal,
                            color = if (activa) Cristal.texto else Cristal.textoSuave,
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .width(18.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (activa) Cristal.cian else Cristal.vidrio.copy(alpha = 0f)),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Plantilla de dispositivo: el «Use a template» del original, con datos reales.
            Etiqueta("PLANTILLA DE DISPOSITIVO")
            TarjetaPlantilla(perfilActivo, onIrAGenerar, onIrAPerfiles)

            val visibles = filas.filter { it.grupo == grupo }
            if (visibles.isEmpty()) {
                run {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Cristal.superficie)
            .border(1.dp, Cristal.bordeLuz, RoundedCornerShape(18.dp)).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Nada que configurar aquí", fontSize = 14.sp, color = Cristal.texto,
                            fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "En este grupo no hay ninguna opción que esta app pueda tocar " +
                                "sin root en tu teléfono. No se ponen casillas que no hacen nada.",
                            fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.textoSuave,
                        )
                    }
                }
            } else {
                visibles.groupBy { it.subgrupo }.forEach { (sub, lista) ->
                    Text(
                        sub.uppercase(),
                        fontSize = 9.sp, letterSpacing = 1.sp, color = Cristal.ambar,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                    )
                    lista.forEach { f -> FilaOpcion(f, onAlternar) }
                }
            }
            Spacer(Modifier.height(8.dp))
            run {
                Text(
                    "Lo que sale en gris no se puede hacer sin root, y se dice. Ninguna casilla " +
                        "de esta pantalla escribe nada hasta que se aplique desde Perfiles.",
                    fontSize = 9.5.sp, lineHeight = 14.sp, color = Cristal.textoSuave,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

/** Una fila de opción: rótulo, etiquetas de consecuencia y casilla a la derecha. */
@Composable
private fun FilaOpcion(f: FilaConfig, onAlternar: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Cristal.vidrio)
            .clickable(enabled = f.disponible) { onAlternar(f.id) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                f.titulo,
                fontSize = 13.sp,
                color = if (f.disponible) Cristal.texto else Cristal.textoSuave,
                fontWeight = FontWeight.Medium,
            )
            if (f.detalle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(f.detalle, fontSize = 10.5.sp, lineHeight = 15.sp, color = Cristal.textoSuave)
            }
            if (f.etiquetas.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    f.etiquetas.forEach { EtiquetaConsecuencia(it) }
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Casilla(f.marcada, f.disponible)
    }
}

@Composable
private fun EtiquetaConsecuencia(e: FilaConfig.Etiqueta) {
    val (texto, color) = when (e) {
        FilaConfig.Etiqueta.RENDIMIENTO -> "Rendimiento" to Cristal.verde
        FilaConfig.Etiqueta.COMPATIBILIDAD -> "Compatibilidad" to Cristal.cian
        FilaConfig.Etiqueta.CALIDAD -> "Calidad" to Cristal.marca
        FilaConfig.Etiqueta.MEMORIA -> "Memoria" to Cristal.ambar
        FilaConfig.Etiqueta.PELIGRO -> "Con riesgo" to Cristal.rojo
        FilaConfig.Etiqueta.NECESITA_ROOT -> "Necesita root" to Cristal.rojo
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(texto, fontSize = 8.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

/** Casilla dibujada: el redondeo y el visto son Path, no un carácter. */
@Composable
private fun Casilla(marcada: Boolean, habilitada: Boolean) {
    val color = when {
        !habilitada -> Cristal.bordeLuz
        marcada -> Cristal.cian
        else -> Cristal.textoSuave
    }
    Box(
        Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (marcada && habilitada) color.copy(alpha = 0.22f) else Cristal.vidrio.copy(alpha = 0f))
            .border(1.4.dp, color, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (marcada && habilitada) Iconos.Visto(tamano = 13.dp, color = color)
    }
}

@Composable
private fun TarjetaPlantilla(
    perfil: PerfilDispositivo?,
    onIrAGenerar: () -> Unit,
    onIrAPerfiles: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Cristal.superficie)
            .padding(16.dp),
    ) {
        if (perfil == null) {
            Text("Sin plantilla", fontSize = 14.sp, color = Cristal.texto,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Una plantilla es un perfil de dispositivo: lo que la app va a decir que eres. " +
                    "Sin ella no se puede aplicar nada.",
                fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.textoSuave,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonPlano("Buscar una", Cristal.marca, Modifier.weight(1f)) { onIrAPerfiles() }
                BotonPlano("Generar", Cristal.vidrioFuerte, Modifier.weight(1f)) { onIrAGenerar() }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                        .background(Cristal.vidrioFuerte),
                    contentAlignment = Alignment.Center,
                ) { Iconos.Espadas(tamano = 19.dp, color = Cristal.cian) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(perfil.nombre, fontSize = 14.sp, color = Cristal.texto,
                        fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        "${perfil.fabricante} · ${perfil.gpu.ifBlank { "GPU sin declarar" }}",
                        fontSize = 10.sp, color = Cristal.textoSuave, maxLines = 1,
                    )
                }
                Text(
                    when (perfil.confianza) {
                        PerfilDispositivo.Confianza.VERIFICADO -> "VERIFICADO"
                        PerfilDispositivo.Confianza.PARCIAL -> "PARCIAL"
                        PerfilDispositivo.Confianza.SIN_VERIFICAR -> "SIN VERIF."
                    },
                    fontSize = 8.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.Bold,
                    color = when (perfil.confianza) {
                        PerfilDispositivo.Confianza.VERIFICADO -> Cristal.verde
                        PerfilDispositivo.Confianza.PARCIAL -> Cristal.ambar
                        else -> Cristal.rojo
                    },
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonPlano("Cambiar", Cristal.vidrioFuerte, Modifier.weight(1f)) { onIrAPerfiles() }
                BotonPlano("Generar otro", Cristal.vidrioFuerte, Modifier.weight(1f)) { onIrAGenerar() }
            }
        }
    }
}

/**
 * Una fila de configuración. Modelo puro para poder probarla: qué se puede tocar sin
 * root, qué no, y qué consecuencia tiene cada cosa.
 */
data class FilaConfig(
    val id: String,
    val titulo: String,
    val detalle: String,
    val grupo: String,
    val subgrupo: String,
    val marcada: Boolean,
    val disponible: Boolean,
    val etiquetas: List<Etiqueta>,
) {
    enum class Etiqueta { RENDIMIENTO, COMPATIBILIDAD, CALIDAD, MEMORIA, PELIGRO, NECESITA_ROOT }
}

object Configuracion {

    /**
     * Las filas que de verdad existen en esta app, agrupadas como el original.
     *
     * Cada una corresponde a algo que `Rendimiento.kt` sabe hacer por shell, o a una
     * lectura. Las que necesitan root se marcan como tal y salen en gris: es mejor una
     * casilla gris explicada que una casilla que miente.
     */
    fun filas(conEsCritura: Boolean, sdk: Int): List<FilaConfig> {
        val disp = conEsCritura
        return listOf(
            FilaConfig(
                "animaciones", "Escala de animaciones", "0.5x en las tres escalas del sistema",
                "GENERAL", "Rendimiento", marcada = false, disponible = disp,
                etiquetas = listOf(FilaConfig.Etiqueta.RENDIMIENTO),
            ),
            FilaConfig(
                "densidad", "Densidad de pantalla", "Baja el dpi para que la GPU mueva menos píxeles",
                "GENERAL", "Pantalla", marcada = false, disponible = disp,
                etiquetas = listOf(FilaConfig.Etiqueta.RENDIMIENTO, FilaConfig.Etiqueta.CALIDAD),
            ),
            FilaConfig(
                "refresco", "Tasa de refresco", "Fija la pantalla a 60 Hz o 120 Hz",
                "GENERAL", "Pantalla", marcada = false, disponible = disp,
                etiquetas = listOf(FilaConfig.Etiqueta.RENDIMIENTO),
            ),
            FilaConfig(
                "no_anim", "Sin animaciones de sistema", "Quita las transiciones por completo",
                "GENERAL", "Rendimiento", marcada = false, disponible = disp,
                etiquetas = listOf(FilaConfig.Etiqueta.RENDIMIENTO, FilaConfig.Etiqueta.CALIDAD),
            ),
            FilaConfig(
                "dns", "DNS privado", "DoT: se cambia el resolutor, no la conexión",
                "GENERAL", "Red", marcada = false, disponible = disp,
                etiquetas = listOf(FilaConfig.Etiqueta.COMPATIBILIDAD),
            ),
            FilaConfig(
                "doze", "Doze sin excepciones", "Deja que Android duerma las apps de fondo",
                "GENERAL", "Batería", marcada = false, disponible = disp,
                etiquetas = listOf(FilaConfig.Etiqueta.MEMORIA, FilaConfig.Etiqueta.RENDIMIENTO),
            ),
            FilaConfig(
                "gl_driver", "Sustituir el driver OpenGL ES",
                "Cambiar el driver exige escribir en /system y montar la partición. Sin root no se puede.",
                "OPENGL", "Driver", marcada = false, disponible = false,
                etiquetas = listOf(FilaConfig.Etiqueta.NECESITA_ROOT, FilaConfig.Etiqueta.PELIGRO),
            ),
            FilaConfig(
                "gl_version", "Versión de OpenGL ES que se declara",
                "Se puede declarar 3.2 en ro.* desde API ${maxOf(sdk, 26)}; el driver real no cambia.",
                "OPENGL", "Device info emulation", marcada = false, disponible = disp,
                etiquetas = listOf(FilaConfig.Etiqueta.COMPATIBILIDAD),
            ),
            FilaConfig(
                "gl_ext", "Extensiones de OpenGL ES",
                "Se declaran extensiones que el driver quizá no tenga: si el juego las usa, falla.",
                "OPENGL", "Device info emulation", marcada = false, disponible = false,
                etiquetas = listOf(FilaConfig.Etiqueta.NECESITA_ROOT, FilaConfig.Etiqueta.PELIGRO),
            ),
            FilaConfig(
                "gl_errores", "Ignorar errores de OpenGL", "El original lo trae encendido. Aquí no se ofrece.",
                "OPENGL", "Expertas", marcada = false, disponible = false,
                etiquetas = listOf(FilaConfig.Etiqueta.NECESITA_ROOT),
            ),
            FilaConfig(
                "vulkan_forzar", "Forzar Vulkan", "Fuerza Vulkan en apps que no lo piden",
                "VULKAN", "General", marcada = false, disponible = false,
                etiquetas = listOf(FilaConfig.Etiqueta.NECESITA_ROOT, FilaConfig.Etiqueta.PELIGRO),
            ),
            FilaConfig(
                "vulkan_quitar", "Quitar Vulkan", "Fuerza GLES en apps que sí lo usan",
                "VULKAN", "General", marcada = false, disponible = false,
                etiquetas = listOf(FilaConfig.Etiqueta.NECESITA_ROOT),
            ),
            FilaConfig(
                "gpu_vendor", "Fabricante de GPU", "Se declara otro fabricante en las propiedades",
                "IMAGEN", "Emulación de dispositivo", marcada = false, disponible = disp,
                etiquetas = listOf(FilaConfig.Etiqueta.COMPATIBILIDAD),
            ),
            FilaConfig(
                "gpu_model", "Modelo de GPU", "Se declara otro modelo en las propiedades",
                "IMAGEN", "Emulación de dispositivo", marcada = false, disponible = disp,
                etiquetas = listOf(FilaConfig.Etiqueta.COMPATIBILIDAD),
            ),
            FilaConfig(
                "antialias", "Antialiasing (MSAA)", "Se elige qué MSAA declarar; el que dibuja es el juego",
                "IMAGEN", "Calidad", marcada = false, disponible = disp,
                etiquetas = listOf(FilaConfig.Etiqueta.CALIDAD),
            ),
            FilaConfig(
                "texturas", "Compresión de texturas", "Se declara ETC2/ATC; el original avisa de que es «unsafe»",
                "IMAGEN", "Calidad", marcada = false, disponible = disp,
                etiquetas = listOf(FilaConfig.Etiqueta.COMPATIBILIDAD, FilaConfig.Etiqueta.PELIGRO),
            ),
        )
    }

    /** Cuántas se pueden marcar de verdad sin root, y cuántas no. Se dice en la cabecera. */
    fun resumen(filas: List<FilaConfig>): String {
        val si = filas.count { it.disponible }
        val no = filas.size - si
        return "$si se pueden cambiar sin root · $no necesitan root y salen en gris"
    }
}
