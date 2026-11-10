package io.github.labandita.glreborn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * La pantalla que aparece cuando la app se cerró la última vez.
 *
 * Antes, un cierre dejaba al usuario sin nada: icono, pantalla negra, y a adivinar. Esta
 * pantalla convierte eso en información: **qué excepción, en qué archivo, en qué línea, con
 * qué versión de Android**. Y un botón para copiarlo entero y pegarlo donde haga falta.
 *
 * Está escrita con la misma ley de diseño que el resto: jerarquía clara, icono dibujado, un
 * solo acento de color, y una acción principal. Nada de un muro de texto rojo.
 */
@Composable
fun PantallaFallo(
    informe: String,
    onCopiar: (String) -> Unit,
    onContinuar: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Cristal.fondoArriba)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 34.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                    .background(Cristal.rojo.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Iconos.Aspa(tamano = 20.dp, color = Cristal.rojo)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("SE CERRÓ LA ÚLTIMA VEZ", fontSize = 9.sp, letterSpacing = 2.2.sp,
                    color = Cristal.rojo, fontWeight = FontWeight.Bold)
                Text("Esto es lo que falló", fontSize = 22.sp, color = Cristal.texto,
                    fontWeight = FontWeight.Light)
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "La app no se ha cerrado ahora: se cerró antes, y el parte queda guardado hasta que " +
                "vosotros lo veáis. Nada de esto lleva tus datos: sólo la excepción, la versión y " +
                "el modelo del teléfono.",
            fontSize = 11.5.sp, lineHeight = 17.sp, color = Cristal.textoSuave,
        )

        Spacer(Modifier.height(16.dp))
        Etiqueta("EL PARTE")
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Cristal.superficie)
                .padding(14.dp),
        ) {
            Text(
                informe.ifBlank { "El parte quedó vacío. Copia lo que veas y mándalo igual." },
                fontSize = 10.sp, lineHeight = 15.sp,
                fontFamily = FontFamily.Monospace,
                color = Cristal.texto.copy(alpha = 0.9f),
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(15.dp))
                    .background(Cristal.marca).clickable { onCopiar(informe) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Copiar el parte", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = Cristal.fondoArriba)
            }
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(15.dp))
                    .background(Cristal.superficieMasAlta).clickable { onContinuar() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Seguir usando la app", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = Cristal.texto)
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "Al pulsar «Seguir», el parte se borra para no volver a salir. Si la app vuelve a " +
                "cerrarse, aparecerá otra vez con el fallo nuevo.",
            fontSize = 10.sp, lineHeight = 15.sp, color = Cristal.textoSuave,
        )
    }
}
