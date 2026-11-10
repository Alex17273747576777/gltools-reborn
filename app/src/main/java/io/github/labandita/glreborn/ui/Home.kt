package io.github.labandita.glreborn.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.labandita.glreborn.core.Modo
import io.github.labandita.glreborn.core.PerfilDispositivo
import io.github.labandita.glreborn.core.Sonda

/**
 * Home, reescrita desde cero en la 1.8.1.
 *
 * Lo que había: nueve cajas del mismo tamaño, con el mismo radio, el mismo borde y el mismo
 * vidrio, cada una con un degradado. Al mirarla no había un orden: el ojo no sabía dónde
 * empezar, y eso es exactamente lo que hace que una app parezca una página web.
 *
 * Lo que hay ahora, y por qué:
 *
 *  1. **El estado manda.** Un bloque arriba con el dato más importante del producto —¿puedo
 *     escribir o no?— resuelto con un color que significa algo, sin caja flotante.
 *  2. **Un número grande.** La identidad que está puesta ahora (o «ninguna») se lee de un
 *     vistazo, en 26 sp, no en una fila de texto más.
 *  3. **Filas densas, no cajas.** Los datos del teléfono son filas separadas por una línea de
 *     1 dp, como en un panel de sistema. Diez cajas con borde eran diez veces el mismo peso.
 *  4. **Dos acciones, jerarquía clara.** Una primaria y una secundaria. No tres iguales.
 *  5. **Cero degradados.** El color aparece sólo donde comunica.
 */
@Composable
fun PantallaHome(
    banner: Banner,
    puede: String,
    avisos: List<Estado.Par>,
    perfilActivo: PerfilDispositivo?,
    modo: Modo,
    actual: Map<String, String>,
    datosSonda: Sonda.Datos?,
    resumenCompat: String,
    apps: Int,
    congeladas: Int,
    fichas: Int,
    ediciones: Int,
    sistemas: Int,
    acciones: Int,
    version: String,
    onIrA: (Destino, Pestana?) -> Unit,
    onAplicar: () -> Unit,
    onRestaurar: () -> Unit,
    onModo: () -> Unit,
    falloLectura: String? = null,
    onDescartarFalloLectura: () -> Unit = {},
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
    ) {
        // ── 1. El estado. Sin caja: una barra de color a la izquierda y el texto sobre el fondo.
        item {
            EstadoCabecera(banner, puede)
        }

        // ── 1b. Si alguna lectura del teléfono falló, se dice. Antes se la tragaba.
        if (falloLectura != null) {
            item {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Cristal.superficie)
                        .clickable { onDescartarFalloLectura() }
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(3.dp).height(26.dp).background(Cristal.ambar, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "No pude leer todo el teléfono",
                            fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Cristal.texto,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "$falloLectura · toca para ocultar",
                            fontSize = 11.sp, lineHeight = 15.sp, color = Cristal.textoTenue,
                        )
                    }
                }
            }
        }

        // ── 2. Avisos: filas, no tarjetas. Sólo si hay algo que avisar.
        if (avisos.isNotEmpty()) {
            item { Separador(20.dp) }
            items(avisos) { a -> FilaAviso(a) }
        }

        // ── 3. La identidad en uso: el número grande.
        item { Separador(24.dp) }
        item { Rotulo("IDENTIDAD EN USO") }
        item { Separador(8.dp) }
        item {
            if (perfilActivo == null) {
                SinPerfil(onIrA)
            } else {
                PerfilEnUso(perfilActivo, onAplicar, onRestaurar) {
                    onIrA(Destino.PERFILES, Pestana.CATALOGO)
                }
            }
        }

        // ── 4. El teléfono, en filas densas.
        item { Separador(28.dp) }
        item { Rotulo("EL TELÉFONO DICE") }
        item { Separador(4.dp) }
        item {
            Column(Modifier.fillMaxWidth()) {
                val claves = listOf(
                    "ro.product.model" to "Modelo",
                    "ro.product.brand" to "Marca",
                    "ro.product.device" to "Device",
                    "ro.opengles.version" to "OpenGL ES",
                )
                claves.forEachIndexed { i, (clave, etiqueta) ->
                    if (i > 0) Linea()
                    FilaDato(etiqueta, actual[clave]?.ifBlank { "sin declarar" } ?: "sin declarar")
                }
            }
        }

        // ── 5. Accesos: dos, no seis. Y con peso distinto.
        item { Separador(28.dp) }
        item { Rotulo("ATAJOS") }
        item { Separador(8.dp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Acceso(
                    titulo = "Herramientas",
                    detalle = "$acciones acciones sin root",
                    color = Cristal.marca,
                    modifier = Modifier.weight(1f),
                ) { onIrA(Destino.HERRAMIENTAS, Pestana.RENDIMIENTO) }
                Acceso(
                    titulo = "Punto de mira",
                    detalle = "encima del juego",
                    color = Cristal.verde,
                    modifier = Modifier.weight(1f),
                ) { onIrA(Destino.HERRAMIENTAS, Pestana.SUPERPOSICION) }
            }
        }

        // ── 6. Cifras: UNA fila de cuatro, en texto, no ocho cajas con degradado.
        item { Separador(28.dp) }
        item { Rotulo("DENTRO DEL APK") }
        item { Separador(8.dp) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Cifra("${apps}", "apps", Modifier.weight(1f))
                Cifra("${fichas}", "fichas", Modifier.weight(1f))
                Cifra("${ediciones}", "ediciones", Modifier.weight(1f))
                Cifra("${acciones}", "acciones", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "$congeladas apps congeladas · $sistemas sistemas en el TOP500 · " +
                    "versión $version",
                fontSize = 10.sp, lineHeight = 15.sp, color = Cristal.textoTenue,
            )
        }

        // ── 7. Pie: una línea, sin caja de tarjeta.
        item { Separador(28.dp) }
        item {
            Row(
                Modifier.fillMaxWidth().clickable { onModo() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (modo == Modo.SEGURO) "Modo seguro" else "Modo experto",
                    fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    color = if (modo == Modo.SEGURO) Cristal.verde else Cristal.ambar,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (modo == Modo.SEGURO) "· sólo identidad" else "· toca API, huella y kernel",
                    fontSize = 11.sp, color = Cristal.textoTenue,
                )
                Spacer(Modifier.weight(1f))
                Text("cambiar", fontSize = 11.sp, color = Cristal.marca)
            }
            if (resumenCompat.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(resumenCompat, fontSize = 10.sp, lineHeight = 15.sp,
                    color = Cristal.textoTenue)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "GLTools Reborn · código abierto · sin cuentas ni telemetría",
                fontSize = 10.sp, color = Cristal.textoTenue,
            )
        }
    }
}

/** El estado, como titular. La barra de color a la izquierda es el único acento. */
@Composable
private fun EstadoCabecera(b: Banner, puede: String) {
    val color = when (b.tono) {
        Banner.Tono.BIEN -> Cristal.verde
        Banner.Tono.AVISO -> Cristal.ambar
        Banner.Tono.MAL -> Cristal.rojo
    }
    Row(Modifier.fillMaxWidth()) {
        Box(
            Modifier.width(3.dp).height(72.dp).clip(RoundedCornerShape(2.dp)).background(color),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                b.titulo.uppercase(),
                fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold,
                color = color,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                b.detalle,
                fontSize = 12.5.sp, lineHeight = 18.sp, color = Cristal.texto,
            )
            Spacer(Modifier.height(6.dp))
            Text(puede, fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.textoSuave)
        }
    }
}

@Composable
private fun FilaAviso(a: Estado.Par) {
    val color = when (a.tono) {
        Banner.Tono.BIEN -> Cristal.verde
        Banner.Tono.AVISO -> Cristal.ambar
        Banner.Tono.MAL -> Cristal.rojo
    }
    Row(Modifier.fillMaxWidth().padding(start = 17.dp, top = 10.dp)) {
        Box(Modifier.padding(top = 5.dp).size(5.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(a.titulo, fontSize = 12.5.sp, color = Cristal.texto,
                fontWeight = FontWeight.Medium)
            Text(a.detalle, fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.textoSuave)
        }
    }
}

/** El perfil activo, con el nombre grande. Es el número de esta pantalla. */
@Composable
private fun PerfilEnUso(
    p: PerfilDispositivo,
    onAplicar: () -> Unit,
    onRestaurar: () -> Unit,
    onCambiar: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            p.nombre,
            fontSize = 26.sp, lineHeight = 31.sp, color = Cristal.texto,
            fontWeight = FontWeight.Light, maxLines = 2,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${p.fabricante} · ${p.gpu.ifBlank { "GPU sin declarar" }}",
                fontSize = 11.sp, color = Cristal.textoSuave, maxLines = 1,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                when (p.confianza) {
                    PerfilDispositivo.Confianza.VERIFICADO -> "VERIFICADO"
                    PerfilDispositivo.Confianza.PARCIAL -> "PARCIAL"
                    PerfilDispositivo.Confianza.SIN_VERIFICAR -> "SIN VERIFICAR"
                },
                fontSize = 8.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold,
                color = when (p.confianza) {
                    PerfilDispositivo.Confianza.VERIFICADO -> Cristal.verde
                    PerfilDispositivo.Confianza.PARCIAL -> Cristal.ambar
                    PerfilDispositivo.Confianza.SIN_VERIFICAR -> Cristal.rojo
                },
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonPlano("Aplicar", Cristal.verde, Modifier.weight(1.4f)) { onAplicar() }
            BotonPlano("Restaurar", Cristal.superficieMasAlta, Modifier.weight(1f)) { onRestaurar() }
            BotonPlano("Cambiar", Cristal.superficieMasAlta, Modifier.weight(1f)) { onCambiar() }
        }
    }
}

@Composable
private fun SinPerfil(onIrA: (Destino, Pestana?) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Ninguno",
            fontSize = 26.sp, color = Cristal.textoTenue, fontWeight = FontWeight.Light,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Elige un perfil del catálogo o genera uno con lo que tu teléfono declara de verdad.",
            fontSize = 11.5.sp, lineHeight = 17.sp, color = Cristal.textoSuave,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonPlano("Ver catálogo", Cristal.marca, Modifier.weight(1f)) {
                onIrA(Destino.PERFILES, Pestana.CATALOGO)
            }
            BotonPlano("Generar uno", Cristal.superficieMasAlta, Modifier.weight(1f)) {
                onIrA(Destino.PERFILES, Pestana.GENERAR)
            }
        }
    }
}

/** Una fila de dato: rótulo a la izquierda, valor monoespaciado a la derecha. */
@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(etiqueta, fontSize = 11.5.sp, color = Cristal.textoSuave)
        Text(
            valor,
            fontSize = 11.5.sp, color = Cristal.texto,
            fontFamily = FontFamily.Monospace, maxLines = 1,
        )
    }
}

/** Atajo: título fuerte y detalle tenue, sin caja con degradado. */
@Composable
private fun Acceso(
    titulo: String,
    detalle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Cristal.superficie)
            .clickable { onClick() }
            .padding(14.dp),
    ) {
        Box(Modifier.width(18.dp).height(2.dp).background(color))
        Spacer(Modifier.height(10.dp))
        Text(titulo, fontSize = 13.sp, color = Cristal.texto, fontWeight = FontWeight.SemiBold)
        Text(detalle, fontSize = 10.sp, lineHeight = 14.sp, color = Cristal.textoTenue, maxLines = 2)
    }
}

/** Cifra: número grande, etiqueta pequeña. Sin caja. */
@Composable
private fun Cifra(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(valor, fontSize = 20.sp, color = Cristal.texto, fontWeight = FontWeight.Light)
        Text(etiqueta, fontSize = 9.sp, letterSpacing = 0.6.sp, color = Cristal.textoTenue)
    }
}

@Composable
internal fun Rotulo(t: String) {
    Text(
        t, fontSize = 9.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold,
        color = Cristal.textoTenue,
    )
}

@Composable
private fun Separador(alto: androidx.compose.ui.unit.Dp) = Spacer(Modifier.height(alto))

@Composable
private fun Linea() = Box(Modifier.fillMaxWidth().height(1.dp).background(Cristal.bordeLuz))

/** Botón con dos tratamientos: primario (color) y secundario (superficie). */
@Composable
internal fun BotonPlano(
    texto: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .clickable { onClick() }
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            texto,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (color == Cristal.superficieMasAlta || color == Cristal.superficie)
                Cristal.texto else Cristal.fondoAbajo,
            maxLines = 1,
        )
    }
}

/**
 * Fila que se pulsa entera (el patrón de los ajustes de Android): título, explicación corta
 * y, a la derecha, lo que va a pasar. Sin recuadro alrededor de cada cosa.
 */
@Composable
internal fun BotonFila(
    titulo: String,
    detalle: String,
    modifier: Modifier = Modifier,
    acento: Color = Cristal.marca,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(30.dp).background(acento, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Cristal.texto,
                maxLines = 1,
            )
            if (detalle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(detalle, fontSize = 11.sp, color = Cristal.textoTenue, maxLines = 2)
            }
        }
        Spacer(Modifier.width(10.dp))
        Iconos.Flecha(Modifier, tamano = 15.dp, color = Cristal.textoTenue)
    }
}
