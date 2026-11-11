package io.github.labandita.glreborn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.labandita.glreborn.core.Autodiagnostico
import io.github.labandita.glreborn.core.Capacidad
import io.github.labandita.glreborn.core.DiagnosticoCompleto
import io.github.labandita.glreborn.core.FichaAntutu
import io.github.labandita.glreborn.core.Informe
import io.github.labandita.glreborn.core.Medida
import io.github.labandita.glreborn.core.PerfilDiagnostico
import io.github.labandita.glreborn.core.ReglasDiagnostico

/**
 * Auto-diagnóstico, ranking AnTuTu y modo penetración: tres pantallas, un solo idioma visual.
 *
 * Nada de cajas dentro de cajas ni de un borde por dato: las medidas van en filas con un hilo, la
 * barra sólo cuando un número se entiende mejor con barra, y lo que no se pudo medir se pinta en
 * gris con su motivo, no con un cero.
 */

// ── Auto-diagnóstico ─────────────────────────────────────────────────────────

@Composable
fun ContenidoDiagnostico(
    diag: DiagnosticoCompleto?,
    perfil: PerfilDiagnostico,
    corriendo: Boolean,
    onCorrer: () -> Unit,
    onPerfil: (PerfilDiagnostico) -> Unit,
    rutas: List<Pair<String, String>>,
    onGuardar: () -> Unit,
    puedeCompartir: Boolean,
    onCompartir: () -> Unit,
    perfiles: List<io.github.labandita.glreborn.core.PerfilGuardado> = emptyList(),
    onElegirPerfil: (String) -> Unit = {},
    onBorrarPerfil: (String) -> Unit = {},
    onCrearPerfil: (String) -> Unit = {},
    avisosPerfiles: List<String> = emptyList(),
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Aviso(
            "Estas pruebas se ejecutan AHORA en tu teléfono y dan números de tu aparato, no " +
                "puntuaciones comparables. Lo que Android no deja leer sin root (temperatura de " +
                "CPU, procesos, GPU) se marca «no medido» con su motivo: no se estima.",
            Cristal.marca,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Boton(
                if (corriendo) "Midiendo…" else "Medir ahora",
                if (corriendo) Cristal.superficieMasAlta else Cristal.verde,
                Modifier.weight(1f),
            ) { if (!corriendo) onCorrer() }
            Boton(
                if (perfil.profundo) "Completo" else "Rápido",
                Cristal.superficieMasAlta, Modifier.weight(1f),
            ) { onPerfil(perfil.copy(profundo = !perfil.profundo)) }
        }

        Boton("Guardar informe ahora", Cristal.superficieMasAlta, Modifier.fillMaxWidth()) {
            onGuardar()
        }
        // Compartir el último informe. Sin esto, en Android 8 y 9 el .md se guardaba dentro de
        // la app y no había manera de sacarlo del teléfono: quedaba escrito y perdido.
        Boton(
            if (puedeCompartir) "Compartir el último informe" else "Aún no hay informe que compartir",
            if (puedeCompartir) Cristal.marca else Cristal.superficieMasAlta,
            Modifier.fillMaxWidth(),
        ) { if (puedeCompartir) onCompartir() }
        Text(
            "El informe se genera solo al cerrar la app, y también con «Guardar informe ahora». " +
                "Se guarda en Documentos/GLTools Reborn y además se puede mandar por correo o " +
                "mensajería: sale de tu teléfono sólo si tú eliges a dónde.",
            fontSize = 11.sp, lineHeight = 17.sp, color = Cristal.textoSuave,
        )

        // ── Perfiles con nombre. Varios, en JSON, y se puede crear uno nuevo con el nombre que
        //    quieras a partir del que está activo. Antes había uno solo y no se podía tocar.
        Etiqueta("PERFILES DE MEDICIÓN")
        perfiles.forEach { pg ->
            Fila {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        pg.nombre,
                        fontSize = 13.sp, color = if (pg.activo) Cristal.marca else Cristal.texto,
                        fontWeight = if (pg.activo) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Text(
                        (if (pg.perfil.profundo) "completo" else "rápido") +
                            " · CPU ${pg.perfil.msCpu} ms · " +
                            "${pg.perfil.pruebas.size} pruebas · memoria ${pg.perfil.umbralMemoriaPct} %" +
                            (if (pg.perfil.alCerrar) " · informe al cerrar" else ""),
                        fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.textoSuave,
                    )
                }
                if (!pg.activo) {
                    Boton("Usar", Cristal.superficieMasAlta, Modifier.width(72.dp)) {
                        onElegirPerfil(pg.nombre)
                    }
                    Spacer(Modifier.width(6.dp))
                    Boton("Borrar", Cristal.superficieMasAlta, Modifier.width(76.dp)) {
                        onBorrarPerfil(pg.nombre)
                    }
                } else {
                    Text("en uso", fontSize = 11.sp, color = Cristal.marca)
                }
            }
            Hilo()
        }
        val nombreNuevo = remember { androidx.compose.runtime.mutableStateOf("") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CajaTexto(
                valor = nombreNuevo.value,
                onValor = { nombreNuevo.value = it },
                pista = "nombre del perfil nuevo",
                modifier = Modifier.weight(1f),
            )
            Boton("Crear", Cristal.marca, Modifier.width(88.dp)) {
                if (nombreNuevo.value.isNotBlank()) {
                    onCrearPerfil(nombreNuevo.value)
                    nombreNuevo.value = ""
                }
            }
        }
        Text(
            "El perfil nuevo copia los ajustes del que esté en uso y se puede editar después con " +
                "los controles de abajo. Se guardan en perfiles-diagnostico.json, dentro de la " +
                "app: ni un dato sale del teléfono.",
            fontSize = 11.sp, lineHeight = 17.sp, color = Cristal.textoSuave,
        )
        avisosPerfiles.forEach { a ->
            Text("· $a", fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.ambar)
        }

        Etiqueta("PERFIL DE MEDICIÓN")
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Fila { TextoPerfil("Duración de CPU", "${perfil.msCpu} ms") }
            Hilo()
            Fila {
                TextoPerfil("Cerrar la app genera informe", if (perfil.alCerrar) "sí" else "no")
            }
            Hilo()
            Fila { TextoPerfil("Aviso con sonido", if (perfil.conSonido) "sí" else "no") }
            Hilo()
            Fila {
                TextoPerfil(
                    "Umbrales",
                    "memoria ${perfil.umbralMemoriaPct} % · batería ${perfil.umbralBateriaC.toInt()} °C",
                )
            }
        }
        // Los umbrales se avisaban pero no se podían cambiar: un número que se enseña y no se
        // puede tocar es un número decorativo.
        PildorasMedidor(
            elegido = when (perfil.umbralMemoriaPct) {
                in 0..80 -> 0
                in 81..89 -> 1
                in 90..94 -> 2
                else -> 3
            },
            onMedidor = { i ->
                onPerfil(perfil.copy(umbralMemoriaPct = when (i) { 0 -> 80; 1 -> 85; 2 -> 90; else -> 95 }))
            },
            nombres = listOf("Aviso 80 %", "85 %", "90 %", "95 %"),
        )
        PildorasMedidor(
            elegido = when {
                perfil.umbralBateriaC <= 38.0 -> 0
                perfil.umbralBateriaC <= 42.0 -> 1
                else -> 2
            },
            onMedidor = { i ->
                onPerfil(perfil.copy(umbralBateriaC = when (i) { 0 -> 38.0; 1 -> 42.0; else -> 46.0 }))
            },
            nombres = listOf("Batería 38 °C", "42 °C", "46 °C"),
        )
        PildorasMedidor(
            elegido = when (perfil.msCpu) {
                in 0..200 -> 0
                in 201..600 -> 1
                else -> 2
            },
            onMedidor = { i ->
                onPerfil(perfil.copy(msCpu = when (i) { 0 -> 150; 1 -> 400; else -> 1000 }))
            },
            nombres = listOf("Rápida", "Normal", "Larga"),
        )

        // ── Qué pruebas se ejecutan. Desde la 1.14.0 el perfil se respeta de verdad, pero no había
        //    manera de marcarlas: con menos de nueve pruebas sólo se llegaba importando un JSON a
        //    mano. Filas con su hilo, como el resto de la pantalla: nada de una caja por prueba.
        //    Se distingue «marcada» de «se ejecuta», porque con el perfil en rápido la RAM y la
        //    escritura no corren por mucho que estén marcadas, y eso no se puede esconder.
        Etiqueta("QUÉ PRUEBAS SE EJECUTAN")
        val avisoPruebas = remember { androidx.compose.runtime.mutableStateOf("") }
        val corren = ReglasDiagnostico.seleccion(perfil.pruebas, perfil.profundo)
        ReglasDiagnostico.ETIQUETAS.forEach { entrada ->
            val clave = entrada.first
            val marcada = clave in perfil.pruebas
            val corre = clave in corren
            Fila(onClick = {
                val (nuevo, aviso) = ReglasDiagnostico.alternar(perfil.pruebas, clave)
                avisoPruebas.value = aviso
                if (aviso.isBlank()) onPerfil(perfil.copy(pruebas = nuevo))
            }) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        entrada.second.first,
                        fontSize = 12.sp,
                        color = if (corre) Cristal.texto else Cristal.textoTenue,
                        fontWeight = if (corre) FontWeight.Medium else FontWeight.Normal,
                    )
                    Text(
                        entrada.second.second,
                        fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.textoSuave,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        corre -> "se ejecuta"
                        marcada -> "marcada, no corre"
                        else -> "no marcada"
                    },
                    fontSize = 11.sp,
                    color = when {
                        corre -> Cristal.marca
                        marcada -> Cristal.ambar
                        else -> Cristal.textoTenue
                    },
                    fontFamily = FontFamily.Monospace,
                )
            }
            Hilo()
        }
        Text(
            "${perfil.pruebas.size} de ${ReglasDiagnostico.ETIQUETAS.size} marcadas · " +
                "${corren.size} se ejecutan con el perfil en " +
                (if (perfil.profundo) "completo" else "rápido"),
            fontSize = 11.sp, lineHeight = 17.sp, color = Cristal.textoSuave,
        )
        if (avisoPruebas.value.isNotBlank()) {
            Text("· ${avisoPruebas.value}", fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.ambar)
        }

        if (diag != null) {
            Etiqueta("RESULTADO DE LA ÚLTIMA MEDICIÓN")
            diag.medidas.forEach { m ->
                FilaMedida(m)
                Hilo()
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Duración total: ${diag.duracionMs} ms. Se midió con ${diag.equipo.size} datos de " +
                    "equipo, que van en la cabecera del informe.",
                fontSize = 11.sp, lineHeight = 17.sp, color = Cristal.textoSuave,
            )
            if (diag.errores.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Etiqueta("FALLOS DURANTE LA MEDICIÓN")
                diag.errores.forEach { e ->
                    Text("· $e", fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.ambar,
                        fontFamily = FontFamily.Monospace)
                }
            }
        } else {
            Text(
                "Todavía no se midió nada en esta sesión. Al cerrar la app se genera el informe " +
                    "igual, con lo que haya en ese momento.",
                fontSize = 13.sp, lineHeight = 19.sp, color = Cristal.textoSuave,
            )
        }

        if (rutas.isNotEmpty()) {
            Etiqueta("ARCHIVOS ESCRITOS")
            rutas.forEach { (ruta, fallo) ->
                Text(
                    if (fallo.isBlank()) ruta else "no se pudo: $fallo",
                    fontSize = 11.sp, lineHeight = 16.sp,
                    color = if (fallo.isBlank()) Cristal.marca else Cristal.rojo,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun FilaMedida(m: Medida) {
    Column(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(m.nombre, fontSize = 13.sp, color = Cristal.texto, fontWeight = FontWeight.Medium)
            Text(
                if (m.medida) "${Informe.barra(m.valor ?: 0.0, tope(m), 10)} ${formato(m)} ${m.unidad}"
                else "no medido",
                fontSize = 12.sp,
                color = if (m.medida) Cristal.marca else Cristal.textoTenue,
                fontFamily = FontFamily.Monospace,
            )
        }
        Text(
            if (m.detalle.isNotBlank()) m.detalle else m.motivoSinDato,
            fontSize = 11.sp, lineHeight = 16.sp,
            color = if (m.medida) Cristal.textoSuave else Cristal.textoTenue,
        )
    }
}

private fun tope(m: Medida): Double = when (m.unidad) {
    "%" -> 100.0
    else -> maxOf(m.valor ?: 1.0, 1.0)
}

private fun formato(m: Medida): String {
    val v = m.valor ?: return "—"
    return when {
        m.unidad == "%" -> v.toInt().toString()
        v >= 1000 -> "%,.0f".format(v)
        v >= 100 -> "%.0f".format(v)
        else -> "%.2f".format(v)
    }
}

@Composable
private fun TextoPerfil(k: String, v: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k, fontSize = 12.sp, color = Cristal.textoSuave)
        Text(v, fontSize = 12.sp, color = Cristal.texto, fontFamily = FontFamily.Monospace)
    }
}

// ── Ranking AnTuTu ──────────────────────────────────────────────────────────

@Composable
fun ContenidoRanking(
    top: List<FichaAntutu>,
    ademas: List<FichaAntutu>,
    consultado: String,
    onAplicar: (FichaAntutu) -> Unit,
    onCrearPerfil: (FichaAntutu) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Aviso(
            "Lista copiada de mobolist.net el $consultado. La puntuación es la que publica esa " +
                "página; cambia con el tiempo y con la versión de AnTuTu. Aplicar un perfil hace " +
                "que el teléfono DECLARE ese modelo, no que rinda como él.",
            Cristal.marca,
        )
        if (top.isEmpty()) {
            Text(
                "El ranking no está en este APK: el archivo de datos no se pudo leer. No se " +
                    "inventa una lista.",
                fontSize = 13.sp, color = Cristal.rojo,
            )
        }
        Etiqueta("LOS ${top.size} DEL RANKING MUNDIAL")
        top.forEach { f ->
            FilaRanking(f, onAplicar, onCrearPerfil)
            Hilo()
        }
        if (ademas.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Etiqueta("ADEMÁS, SIN PUNTUACIÓN PUBLICADA")
            Text(
                "Estos modelos no están en esa lista o no publican puntos. Se incluyen igual, con " +
                    "sus datos reales de ficha técnica, porque un perfil no necesita una nota.",
                fontSize = 11.sp, lineHeight = 17.sp, color = Cristal.textoSuave,
            )
            ademas.forEach { f ->
                FilaRanking(f, onAplicar, onCrearPerfil)
                Hilo()
            }
        }
    }
}

@Composable
private fun FilaRanking(
    f: FichaAntutu,
    onAplicar: (FichaAntutu) -> Unit,
    onCrearPerfil: (FichaAntutu) -> Unit,
) {
    val aplicable = f.device.isNotBlank()
    Column(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                (if (f.puesto > 0) "#${f.puesto} " else "") + f.nombre,
                fontSize = 13.sp, color = Cristal.texto, fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (f.antutu != null) Text(
                "%,d".format(f.antutu), fontSize = 12.sp, color = Cristal.marca,
                fontFamily = FontFamily.Monospace,
            )
        }
        Text(
            listOfNotNull(
                f.marca.takeIf { it.isNotBlank() },
                f.chipset.takeIf { it.isNotBlank() },
                f.gpu.takeIf { it.isNotBlank() },
                f.ram.takeIf { it.isNotBlank() },
                if (f.device.isNotBlank()) "device: ${f.device}" else "sin código de dispositivo",
            ).joinToString(" · "),
            fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.textoSuave,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (aplicable) {
                Text(
                    "aplicar", fontSize = 12.sp, color = Cristal.verde, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onAplicar(f) },
                )
            } else {
                Text("no aplicable: falta el device", fontSize = 11.sp, color = Cristal.ambar)
            }
            Text(
                "crear perfil", fontSize = 12.sp, color = Cristal.marca, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onCrearPerfil(f) },
            )
        }
    }
}

// ── Modo penetración ────────────────────────────────────────────────────────

@Composable
fun ContenidoPenetracion(
    capacidades: List<Capacidad>,
    resumen: String,
    sinShell: Boolean,
    onProbar: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Aviso(
            "Esto comprueba qué puede hacer la app en ESTE teléfono, ejecutando cada comando de " +
                "verdad. No es root, no escala privilegios y no parchea el sistema: si el " +
                "fabricante ignora un ajuste, se ve aquí en ámbar, no en verde.",
            Cristal.ambar,
        )
        Boton(
            if (capacidades.isEmpty()) "Comprobar el teléfono" else "Volver a comprobar",
            Cristal.marca, Modifier.fillMaxWidth(),
        ) { onProbar() }
        if (sinShell) {
            Text(
                "Shizuku no está activo, así que ahora mismo no hay vía: las comprobaciones " +
                    "saldrán «sin vía». Enciéndelo y vuelve a probar.",
                fontSize = 12.sp, lineHeight = 18.sp, color = Cristal.ambar,
            )
        }
        if (capacidades.isNotEmpty()) {
            Etiqueta("LO QUE SE COMPROBÓ")
            Text(resumen, fontSize = 12.sp, lineHeight = 18.sp, color = Cristal.texto)
            Spacer(Modifier.height(2.dp))
            capacidades.forEach { c ->
                FilaCapacidad(c)
                Hilo()
            }
        }
    }
}

@Composable
private fun FilaCapacidad(c: Capacidad) {
    val color = when (c.estado) {
        Capacidad.Estado.COMPLETO -> Cristal.verde
        Capacidad.Estado.LIMITADO -> Cristal.ambar
        Capacidad.Estado.SIN_PERMISO -> Cristal.ambar
        Capacidad.Estado.NO -> Cristal.rojo
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(c.nombre, fontSize = 13.sp, color = Cristal.texto, modifier = Modifier.weight(1f))
            Text(
                when (c.estado) {
                    Capacidad.Estado.COMPLETO -> "completo"
                    Capacidad.Estado.LIMITADO -> "limitado"
                    Capacidad.Estado.SIN_PERMISO -> "sin permiso"
                    Capacidad.Estado.NO -> "no"
                },
                fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold,
            )
        }
        Text(c.detalle, fontSize = 11.sp, lineHeight = 16.sp, color = Cristal.textoSuave)
        Text(
            "$ ${c.comando}", fontSize = 10.sp, lineHeight = 15.sp, color = Cristal.textoTenue,
            fontFamily = FontFamily.Monospace,
        )
    }
}
