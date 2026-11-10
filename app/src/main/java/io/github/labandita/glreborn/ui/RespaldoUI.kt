package io.github.labandita.glreborn.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.labandita.glreborn.core.PerfilDispositivo
import io.github.labandita.glreborn.core.Respaldo

/**
 * «Llevarte los perfiles»: exportar e importar, sin nube y sin cuenta.
 *
 * Usa el selector de archivos del sistema (`ActivityResultContracts.CreateDocument` y
 * `OpenDocument`), así que el usuario elige dónde guardar y de dónde leer: la app **no pide
 * permiso de almacenamiento** y no sabe nada de sus carpetas.
 *
 * El resultado se cuenta tal cual: cuántos perfiles se han escrito o leído, y **cuántos se han
 * descartado y por qué**. Importar «a medias» sin decirlo sería otra forma de mentir.
 */
@Composable
internal fun SeccionRespaldo(
    perfiles: List<PerfilDispositivo>,
    versionApp: String,
    modeloTelefono: String,
    onImportar: (List<PerfilDispositivo>) -> Unit,
) {
    val contexto = LocalContext.current
    var ultimo by remember { mutableStateOf("") }

    val modelo = remember(perfiles, versionApp, modeloTelefono) {
        Respaldo.escribir(perfiles, versionApp, modeloTelefono)
    }

    // Guardar: el sistema pregunta dónde.
    val guardar = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) {
            ultimo = "No se ha guardado nada."
            return@rememberLauncherForActivityResult
        }
        val escrito = runCatching {
            contexto.contentResolver.openOutputStream(uri)?.use { salida ->
                salida.write(modelo.toByteArray(Charsets.UTF_8))
            } ?: error("no se pudo abrir el fichero")
        }
        ultimo = if (escrito.isSuccess) {
            "Guardados ${perfiles.size} perfiles."
        } else {
            "No se pudo escribir: ${escrito.exceptionOrNull()?.message ?: "error desconocido"}"
        }
        Toast.makeText(contexto, ultimo, Toast.LENGTH_LONG).show()
    }

    // Leer: el sistema pregunta de dónde.
    val abrir = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            ultimo = "No se ha leído nada."
            return@rememberLauncherForActivityResult
        }
        val texto = runCatching {
            contexto.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: error("no se pudo abrir el fichero")
        }
        texto.fold(
            onSuccess = { contenido ->
                val lectura = Respaldo.leer(contenido)
                ultimo = lectura.resumen +
                    if (lectura.origen.isNotBlank()) " · de ${lectura.origen}" else ""
                if (lectura.perfiles.isNotEmpty()) onImportar(lectura.perfiles)
                Toast.makeText(contexto, lectura.resumen, Toast.LENGTH_LONG).show()
            },
            onFailure = { e ->
                ultimo = "No se pudo leer: ${e.message ?: "error desconocido"}"
                Toast.makeText(contexto, ultimo, Toast.LENGTH_LONG).show()
            },
        )
    }

    Grupo(rotulo = "LLEVARTE LOS PERFILES") {
        Column(Modifier.fillMaxWidth().let { it }.padding(14.dp)) {
            Text(
                "${perfiles.size} perfiles propios",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Cristal.texto,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "Un fichero JSON legible, sin cuenta y sin servidor. Sirve para cambiar de " +
                    "teléfono o para no perder lo que has generado tú.",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Cristal.textoSuave,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonPlano("Exportar", Cristal.marca, Modifier.weight(1f)) {
                    guardar.launch(Respaldo.nombreDeFichero())
                }
                BotonPlano("Importar", Cristal.superficieMasAlta, Modifier.weight(1f)) {
                    abrir.launch(arrayOf("application/json", "text/plain", "*/*"))
                }
            }
            if (ultimo.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(ultimo, fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.textoSuave)
            }
        }
    }
}
