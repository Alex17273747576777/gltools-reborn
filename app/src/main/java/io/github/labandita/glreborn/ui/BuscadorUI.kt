package io.github.labandita.glreborn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.labandita.glreborn.core.BusquedaGlobal

/**
 * La caja de búsqueda y su lista de resultados.
 *
 * Detalles que importan en un teléfono y que se aprenden usándolo:
 *  - La caja **no lleva borde**: es una superficie con tono, como los grupos.
 *  - Los resultados dicen **de qué tipo son** (sección, perfil, dispositivo) con una palabra en
 *    versalitas, no con un icono que hay que aprender.
 *  - Si no hay nada, **no hay un vacío**: se ofrecen búsquedas que sí existen.
 *  - Se busca a partir de **dos letras**: con una sola, la lista sería el catálogo entero.
 */
@Composable
internal fun CajaBusqueda(
    texto: String,
    onTexto: (String) -> Unit,
    resultados: List<BusquedaGlobal.Hallazgo>,
    onIr: (BusquedaGlobal.Destino) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Cristal.superficieAlta)
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Buscar",
                fontSize = 12.sp,
                color = Cristal.textoTenue,
                modifier = Modifier.width(52.dp),
            )
            Box(Modifier.weight(1f)) {
                if (texto.isEmpty()) {
                    Text(
                        "perfiles, ajustes, acciones, modelos…",
                        fontSize = 13.sp,
                        color = Cristal.textoTenue,
                    )
                }
                BasicTextField(
                    value = texto,
                    onValueChange = onTexto,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp, color = Cristal.texto),
                    cursorBrush = SolidColor(Cristal.marca),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (texto.isNotEmpty()) {
                Text(
                    "borrar",
                    fontSize = 11.sp,
                    color = Cristal.marca,
                    modifier = Modifier.clickable { onTexto("") }.padding(start = 8.dp),
                )
            }
        }

        if (texto.length >= 2) {
            Spacer(Modifier.height(8.dp))
            if (resultados.isEmpty()) {
                SinResultados(texto)
            } else {
                Grupo(rotulo = "${resultados.size} RESULTADOS") {
                    resultados.forEachIndexed { i, h ->
                        if (i > 0) Hilo()
                        Fila(onClick = { onIr(h.ir) }) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    h.titulo,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Cristal.texto,
                                )
                                if (h.detalle.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        h.detalle,
                                        fontSize = 11.sp,
                                        color = Cristal.textoTenue,
                                        maxLines = 2,
                                    )
                                }
                            }
                            Text(
                                when (h.tipo) {
                                    BusquedaGlobal.Tipo.SECCION -> "SECCIÓN"
                                    BusquedaGlobal.Tipo.PERFIL -> "PERFIL"
                                    BusquedaGlobal.Tipo.DISPOSITIVO -> "FICHA"
                                    BusquedaGlobal.Tipo.ACCION -> "ACCIÓN"
                                },
                                fontSize = 8.5.sp,
                                letterSpacing = 0.9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Cristal.textoTenue,
                            )
                            Spacer(Modifier.width(8.dp))
                            Iconos.Flecha(Modifier, tamano = 13.dp, color = Cristal.textoTenue)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Nada de «no hay resultados» y punto: se dice **por qué** y se ofrecen búsquedas que sí existen.
 * Un estado vacío es una oportunidad de enseñar, no un callejón.
 */
@Composable
private fun SinResultados(texto: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            "Nada que case con «$texto»",
            fontSize = 12.5.sp,
            color = Cristal.texto,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Se busca en todo lo que la app sabe: secciones, perfiles, acciones y las " +
                "54.000 fichas del catálogo. Prueba con una palabra suelta.",
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = Cristal.textoTenue,
        )
        Spacer(Modifier.height(8.dp))
        // Sin `take(4)`: las sugerencias ya son cortas, y cortar una lista es truncar. Si un día
        // crecen, el desplazamiento horizontal se encarga.
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BusquedaGlobal.sugerencias().forEach { s ->
                Text(
                    s,
                    fontSize = 11.sp,
                    color = Cristal.marca,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Cristal.superficieAlta)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}
