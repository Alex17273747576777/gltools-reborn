package io.github.labandita.glreborn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Los grupos de superficie, en un solo sitio.
 *
 * El problema que resuelve: en la 1.9.1 la portada ya estaba bien, pero las otras cuatro
 * secciones seguían con **43 cajas redondeadas y 21 bordes** heredados del diseño viejo. Dos
 * pantallas de la misma app no pueden tener dos lenguajes: es lo que hace que una app parezca
 * un conjunto de páginas y no una app.
 *
 * La regla, aquí escrita una vez y usada en todas partes:
 *
 *  · **Un grupo** es el único sitio donde se dibuja una caja. No hay cajas dentro de cajas.
 *  · Dentro del grupo, los datos van en **filas separadas por un hilo de 1 dp**, no en cajas.
 *  · El grupo **no lleva borde**: se distingue por tono (`superficie` sobre `fondoArriba`).
 *  · Las filas que se pulsan lo dicen con un `›` dibujado, no con un borde.
 */

/** Un grupo de contenido: rótulo opcional arriba y una superficie con las filas dentro. */
@Composable
internal fun Grupo(
    rotulo: String? = null,
    modifier: Modifier = Modifier,
    tenue: Boolean = false,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        if (rotulo != null) {
            Text(
                rotulo,
                fontSize = 9.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
                color = Cristal.textoTenue,
                modifier = Modifier.padding(start = 2.dp, bottom = 7.dp),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (tenue) Cristal.superficie else Cristal.superficieAlta)
                .padding(vertical = 2.dp),
            content = contenido,
        )
    }
}

/** Una fila dentro de un grupo. El hilo de separación lo pone [Hilo], no la fila. */
@Composable
internal fun Fila(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contenido: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val base = modifier
        .fillMaxWidth()
        .then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier,
        )
        .padding(horizontal = 14.dp, vertical = 12.dp)
    Row(base, verticalAlignment = Alignment.CenterVertically, content = contenido)
}

/** El hilo de 1 dp entre filas. Sangrado a la izquierda para que no parezca una tabla. */
@Composable
internal fun Hilo(sangrado: Int = 14) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = sangrado.dp)
            .height(1.dp)
            .background(Cristal.bordeLuz),
    )
}

/** Etiqueta y valor: el par que se repite en toda la app. */
@Composable
internal fun Dato(etiqueta: String, valor: String, mono: Boolean = true, color: Color = Cristal.texto) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(etiqueta, fontSize = 12.sp, color = Cristal.textoSuave, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(
            valor,
            fontSize = 12.sp,
            color = color,
            maxLines = 1,
            fontFamily = if (mono) FontFamily.Monospace else null,
        )
    }
}

/** Un punto de color y un texto: para estados, no para adornar. */
@Composable
internal fun Estado(color: Color, texto: String, detalle: String? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)) {
        Box(
            Modifier.padding(top = 5.dp).size(6.dp).clip(CircleShape).background(color),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(texto, fontSize = 12.5.sp, color = Cristal.texto, fontWeight = FontWeight.Medium)
            if (detalle != null) {
                Text(detalle, fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.textoSuave)
            }
        }
    }
}

/** Fila que lleva a otro sitio. Se distingue por el `›` dibujado, no por un borde. */
@Composable
internal fun FilaEnlace(titulo: String, detalle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Cristal.texto)
            if (detalle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(detalle, fontSize = 11.sp, color = Cristal.textoTenue, maxLines = 2)
            }
        }
        Iconos.Flecha(Modifier, tamano = 14.dp, color = Cristal.textoTenue)
    }
}

/**
 * El aviso al principio de una sección: una barra de color y texto sobre el fondo.
 *
 * Es el patrón que quedó bien en la portada, y el que faltaba en el resto: **el estado no va
 * dentro de una tarjeta**. Una tarjeta dice «esto es un objeto»; una barra y un texto dicen
 * «esto es lo que está pasando».
 */
@Composable
internal fun AvisoSeccion(color: Color, titulo: String, detalle: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(Modifier.width(3.dp).height(42.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                titulo.uppercase(),
                fontSize = 9.5.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Spacer(Modifier.height(3.dp))
            Text(detalle, fontSize = 12.sp, lineHeight = 17.sp, color = Cristal.texto)
        }
    }
}
