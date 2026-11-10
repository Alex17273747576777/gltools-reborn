package io.github.labandita.glreborn.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
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
) {    // Qué medidor está encendido. Vive aquí para que el carrusel y el panel sean uno.
    var medidor by rememberSaveable { mutableStateOf(0) }

    // El carrusel de píldoras vive DENTRO de este contenedor, así que aquí no puede haber un
    // LazyColumn: dos «lista y desplaza» anidados se pelean por el gesto y ya cerraron la app
    // una vez. Un Column que se desplaza entero, con un solo gesto.
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
    ) {
        // ── 1. Dónde estás. El estado, sin caja; una barra que significa algo.
        EstadoCabecera(banner, puede)

        // ── 2. Si alguna lectura del teléfono falló, se dice.
        if (falloLectura != null) {
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

        // ── 3. Avisos: filas, no tarjetas. Sólo si hay algo que avisar.
        if (avisos.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            avisos.forEach { a -> FilaAviso(a) }
        }

        // ── 4. LA IDENTIDAD. Lo primero que se lee: sobre qué dispositivo crees que corres.
        Spacer(Modifier.height(22.dp))
        Rotulo("IDENTIDAD EN USO")
        Spacer(Modifier.height(6.dp))
        if (perfilActivo == null) {
            SinPerfil(onIrA)
        } else {
            PerfilEnUso(perfilActivo, onAplicar, onRestaurar) {
                onIrA(Destino.PERFILES, Pestana.CATALOGO)
            }
        }

        // ── 5. CARRUSEL. El patrón de Hardware Monitor: píldoras que se desplazan, una
        //       encendida, y el panel de debajo cambia. Cinco grupos de datos en el sitio
        //       de una lista interminable.
        Spacer(Modifier.height(26.dp))
        Rotulo("LO QUE EL TELÉFONO DICE")
        Spacer(Modifier.height(9.dp))
        PildorasMedidor(medidor, onMedidor = { medidor = it })
        Spacer(Modifier.height(14.dp))
        when (medidor) {
            0 -> PanelIdentidad(datosSonda, actual)
            1 -> PanelSonda(datosSonda)
            2 -> PanelArquitectura(datosSonda)
            3 -> PanelSistema(datosSonda, actual, resumenCompat)
            else -> PanelApps(apps, congeladas, fichas)
        }

        // ── 6. ACCIONES. Rejilla de baldosas, el patrón de Androoster: se pulsan sin leer
        //       porque llevan el icono dibujado. Antes eran botones de texto con el mismo peso.
        Spacer(Modifier.height(26.dp))
        Rotulo("HACER")
        Spacer(Modifier.height(9.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Baldosa(
                    "Catálogo", "elegir identidad", Cristal.marca, Modifier.weight(1f),
                    icono = { Iconos.Llave(Modifier, tamano = 19.dp, color = Cristal.marca) },
                ) { onIrA(Destino.PERFILES, Pestana.CATALOGO) }
                Baldosa(
                    "Punto de mira", "encima del juego", Cristal.verde, Modifier.weight(1f),
                    icono = { Iconos.Rayo(Modifier, tamano = 19.dp, color = Cristal.verde) },
                ) { onIrA(Destino.HERRAMIENTAS, Pestana.SUPERPOSICION) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Baldosa(
                    "Herramientas", "$acciones sin root", Cristal.ambar, Modifier.weight(1f),
                    icono = { Iconos.Espadas(Modifier, tamano = 19.dp, color = Cristal.ambar) },
                ) { onIrA(Destino.HERRAMIENTAS, Pestana.RENDIMIENTO) }
                Baldosa(
                    "Datos", "$fichas fichas", Cristal.marca, Modifier.weight(1f),
                    icono = { Iconos.BaseDeDatos(Modifier, tamano = 19.dp, color = Cristal.marca) },
                ) { onIrA(Destino.DATOS, null) }
            }
        }

        // ── 7. Cifras: una fila en texto. No ocho cajas del mismo tamaño.
        Spacer(Modifier.height(24.dp))
        Rotulo("DENTRO DEL APK")
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Cifra("$apps", "apps", Modifier.weight(1f))
            Cifra("$fichas", "fichas", Modifier.weight(1f))
            Cifra("$ediciones", "ediciones", Modifier.weight(1f))
            Cifra("$acciones", "acciones", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "$congeladas apps congeladas · $sistemas sistemas en el TOP500 · versión $version",
            fontSize = 10.sp, lineHeight = 15.sp, color = Cristal.textoTenue,
        )

        // ── 8. Pie: una línea. Sin caja de tarjeta.
        Spacer(Modifier.height(26.dp))
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
        Spacer(Modifier.height(12.dp))
        Text(
            "GLTools Reborn · código abierto · sin cuentas ni telemetría",
            fontSize = 10.sp, color = Cristal.textoTenue,
        )
    }
}

/** El estado, como titular. La barra de color a la izquierda es el único acento. */

// ═══════════════════════════════════════════════════════════════════════════════════════
// Lo aprendido mirando cómo lo hacen las apps que sí están bien hechas
// ═══════════════════════════════════════════════════════════════════════════════════════
//
// **Hardware Monitor** (Tools, 1.1.0) resuelve el problema que esta app tenía sin resolver:
// tiene siete grupos de datos y no caben. Su solución es un carrusel de píldoras —«Sistema»,
// «Batería», «Térmico», «CPU»— con la elegida encendida en el color de marca, y debajo un
// único panel que cambia. La píldora mide **una línea de alto**, entra la que quieres y se
// asoma la siguiente, así que se entiende que hay más sin necesidad de una flecha.
//
// **LSPosed** (Root & System, 2.2.0) demuestra lo contrario y también sirve: cuando un dato es
// el asunto, no hace falta tarjeta. «Dispositivo / Samsung Galaxy A33 5G» es un par
// etiqueta-arriba, valor-abajo, sin caja, y con eso la pantalla ya tiene jerarquía.
//
// **Androoster** (Root, 1.08) usa rejilla de baldosas con icono grande para las tres acciones
// principales: se pulsa sin leer. Y **SceneF** (Tools) separa lo que son grupos de ajustes en
// bloques con título, en vez de una lista interminable de interruptores.

/** El carrusel de medidores. Una píldora encendida; las demás, apagadas. Se desplaza. */
@Composable
internal fun PildorasMedidor(
    elegido: Int,
    onMedidor: (Int) -> Unit,
    nombres: List<Any> = listOf("Identidad", "Sonda", "Arquitectura", "Sistema", "Apps"),
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        nombres.forEachIndexed { i, elemento ->
            // Una píldora puede ser sólo un nombre, o un nombre con su cuenta al lado
            // («Verificados 12»). La cuenta es lo que hace útil un filtro.
            val nombre = (elemento as? Pair<*, *>)?.first?.toString() ?: elemento.toString()
            val cuenta = (elemento as? Pair<*, *>)?.second?.toString()
            val encendida = i == elegido
            // Muelle al encender: el mismo movimiento que en la barra de abajo, para que
            // toda la app responda con la misma física.
            val escala by animateFloatAsState(
                targetValue = if (encendida) 1f else 0.97f,
                animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
                label = "pildora",
            )
            Box(
                Modifier
                    .scale(escala)
                    .clip(RoundedCornerShape(50))
                    .background(if (encendida) Cristal.marca else Cristal.superficie)
                    .clickable { onMedidor(i) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        nombre,
                        fontSize = 12.sp,
                        fontWeight = if (encendida) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (encendida) Cristal.fondoAbajo else Cristal.textoSuave,
                        maxLines = 1,
                    )
                    if (cuenta != null) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            cuenta,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (encendida) Cristal.fondoAbajo.copy(alpha = 0.7f)
                            else Cristal.textoTenue,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** Identidad: los cuatro campos que un juego mira primero. Pares etiqueta/valor, sin caja. */
@Composable
private fun PanelIdentidad(d: Sonda.Datos?, actual: Map<String, String>) {
    Column(Modifier.fillMaxWidth()) {
        FilaDato("Modelo", actual["ro.product.model"]?.ifBlank { null } ?: d?.modelo ?: "sin declarar")
        Linea()
        FilaDato("Marca", actual["ro.product.brand"]?.ifBlank { null } ?: d?.marca ?: "sin declarar")
        Linea()
        FilaDato("Device", actual["ro.product.device"]?.ifBlank { null } ?: d?.device ?: "sin declarar")
        Linea()
        FilaDato("OpenGL ES", actual["ro.opengles.version"]?.ifBlank { null } ?: d?.openglEs ?: "sin declarar")
    }
}

/** La sonda: qué declara el teléfono de sí mismo. Pares etiqueta/valor, sin cajas. */
@Composable
private fun PanelSonda(d: Sonda.Datos?) {
    Column(Modifier.fillMaxWidth()) {
        if (d == null) {
            Text("La sonda no ha podido leer este teléfono todavía.",
                fontSize = 11.5.sp, lineHeight = 17.sp, color = Cristal.textoTenue)
            return@Column
        }
        val pares = listOf(
            "Fabricante" to d.fabricante,
            "Modelo" to d.modelo,
            "Board" to d.board,
            "Hardware" to d.hardware,
            "Plataforma" to d.plataforma,
            "SoC" to "${d.socFabricante} ${d.socModelo}".trim(),
        ).filter { it.second.isNotBlank() }
        pares.forEachIndexed { i, (etiqueta, valor) ->
            if (i > 0) Linea()
            FilaDato(etiqueta, valor)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "${d.camposLeidos} campos leídos de verdad. Los que falten se quedan en blanco: " +
                "no se inventan.",
            fontSize = 10.5.sp, lineHeight = 16.sp, color = Cristal.textoTenue,
        )
    }
}

/** Arquitectura: lo que los juegos miran para decidir qué binario cargar. */
@Composable
private fun PanelArquitectura(d: Sonda.Datos?) {
    Column(Modifier.fillMaxWidth()) {
        if (d == null || d.arquitectura.isEmpty()) {
            Text("Sin arquitecturas declaradas: este teléfono no las expone por lectura directa.",
                fontSize = 11.5.sp, lineHeight = 17.sp, color = Cristal.textoTenue)
            return@Column
        }
        d.arquitectura.forEachIndexed { i, abi ->
            if (i > 0) Linea()
            FilaDato("ABI ${i + 1}", abi)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Un juego elige el binario por la primera ABI de la lista. Cambiar el orden sin " +
                "cambiar lo que el procesador puede ejecutar es lo que rompe apps.",
            fontSize = 10.5.sp, lineHeight = 16.sp, color = Cristal.textoTenue,
        )
    }
}

/** Sistema: versión, características y huella. Y lo que se ha podido comprobar. */
@Composable
private fun PanelSistema(d: Sonda.Datos?, actual: Map<String, String>, compat: String) {
    Column(Modifier.fillMaxWidth()) {
        FilaDato("Android", d?.let { "${it.versionAndroid} (API ${it.sdk})" } ?: "sin leer")
        Linea()
        FilaDato("Características", d?.caracteristicas?.ifBlank { "sin declarar" } ?: "sin leer")
        Linea()
        FilaDato("Huella", actual["ro.build.fingerprint"]?.ifBlank { null } ?: "sin declarar")
        if (compat.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(compat, fontSize = 10.5.sp, lineHeight = 16.sp, color = Cristal.textoTenue)
        }
    }
}

/** Apps y rendimiento: las dos cifras que se pueden tocar sin root. */
@Composable
private fun PanelApps(apps: Int, congeladas: Int, fichas: Int) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$apps", fontSize = 30.sp, fontWeight = FontWeight.Light, color = Cristal.texto)
            Spacer(Modifier.width(6.dp))
            Text("apps instaladas", fontSize = 11.sp, color = Cristal.textoSuave,
                modifier = Modifier.padding(bottom = 6.dp))
            Spacer(Modifier.weight(1f))
            Text("$congeladas", fontSize = 30.sp, fontWeight = FontWeight.Light,
                color = Cristal.ambar)
            Spacer(Modifier.width(6.dp))
            Text("congeladas", fontSize = 11.sp, color = Cristal.textoSuave,
                modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(Modifier.height(10.dp))
        FilaDato("Fichas de dispositivo en el APK", "$fichas")
        Spacer(Modifier.height(10.dp))
        Text(
            "Congelar una app no la borra: queda parada hasta que la descongelas. Sin root, " +
                "es lo más cerca que se puede estar de «que no gaste batería».",
            fontSize = 10.5.sp, lineHeight = 16.sp, color = Cristal.textoTenue,
        )
    }
}

/**
 * Baldosa de acción: icono dibujado, título y una línea. El icono es lo que hace que se pulse
 * sin leer —lo que hacía Androoster y lo que aquí faltaba—. El color vive en el icono, no en
 * un fondo de color: así cuatro baldosas seguidas no parecen cuatro botones de plantilla.
 */
@Composable
internal fun Baldosa(
    titulo: String,
    detalle: String,
    acento: Color,
    modifier: Modifier = Modifier,
    icono: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Cristal.superficie)
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(acento.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { icono() }
            Spacer(Modifier.weight(1f))
            Iconos.Flecha(Modifier, tamano = 13.dp, color = Cristal.textoTenue)
        }
        Spacer(Modifier.height(10.dp))
        Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Cristal.texto, maxLines = 1)
        Text(detalle, fontSize = 10.5.sp, lineHeight = 14.sp, color = Cristal.textoTenue, maxLines = 1)
    }
}

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
        // Sin truncar: si el valor es largo va arriba/abajo, no cortado a la derecha.
        if (valor.length > 30) {
            Text(etiqueta, fontSize = 11.sp, color = Cristal.textoSuave)
            Spacer(Modifier.height(3.dp))
            Text(
                valor, fontSize = 11.5.sp, lineHeight = 16.sp, color = Cristal.texto,
                fontFamily = FontFamily.Monospace,
            )
        } else {
            Text(etiqueta, fontSize = 11.5.sp, color = Cristal.textoSuave)
            Spacer(Modifier.weight(1f))
            Text(valor, fontSize = 11.5.sp, color = Cristal.texto, fontFamily = FontFamily.Monospace)
        }
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
