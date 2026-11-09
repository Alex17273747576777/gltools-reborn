package io.github.labandita.glreborn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Liquid Glass — el lenguaje visual de iOS 26, aplicado aquí.
 *
 * La referencia es ReDantotsu / Kiroku (apps de anime y manga), no este sitio de
 * módulos: superficies translúcidas con desenfoque real, bordes de luz, barra
 * inferior en forma de píldora y animaciones con muelle.
 *
 * Nada de esto carga fuentes ni imágenes de internet: el fondo son degradados
 * dibujados por código, y el cristal es `blur()` de Compose sobre lo que hay detrás.
 * En modo avión se ve igual.
 */
object Cristal {
    // Fondo: degradados profundos, como el cielo de una app de anime a medianoche.
    val fondoArriba = Color(0xFF12081F)
    val fondoMedio = Color(0xFF1B1030)
    val fondoAbajo = Color(0xFF0A0612)

    // Acentos: violeta y cian, con un rosa de remate.
    val violeta = Color(0xFF9D7BFF)
    val cian = Color(0xFF5CE1E6)
    val rosa = Color(0xFFFF7AB6)
    val verde = Color(0xFF4ADE80)
    val ambar = Color(0xFFFFC24B)
    val rojo = Color(0xFFFF6B81)

    val texto = Color(0xFFF4F1FF)
    val textoSuave = Color(0xFFB5AECF)

    // El cristal: blanco muy transparente sobre el que se apoya el desenfoque.
    val vidrio = Color(0x1AFFFFFF)
    val vidrioFuerte = Color(0x26FFFFFF)
    val bordeLuz = Color(0x33FFFFFF)
    val bordeLuzAlto = Color(0x66FFFFFF)

    val degradadoFondo = Brush.verticalGradient(
        listOf(fondoArriba, fondoMedio, fondoAbajo)
    )
    val degradadoAcento = Brush.linearGradient(listOf(violeta, cian))
    val degradadoCalido = Brush.linearGradient(listOf(rosa, violeta))
}

private val esquema = darkColorScheme(
    primary = Cristal.violeta,
    onPrimary = Color(0xFF12081F),
    secondary = Cristal.cian,
    background = Cristal.fondoArriba,
    onBackground = Cristal.texto,
    surface = Cristal.fondoMedio,
    onSurface = Cristal.texto,
    error = Cristal.rojo,
)

private val tipografia = Typography(
    headlineMedium = TextStyle(fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp),
)

@Composable
fun TemaReborn(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquema, typography = tipografia, content = contenido)
}

/** Una superficie de cristal: fondo translúcido + borde de luz + esquinas grandes. */
fun Modifier.vidrio(radio: Int = 22): Modifier = this
    .clip(RoundedCornerShape(radio.dp))
    .background(Cristal.vidrio)
    .border(1.dp, Cristal.bordeLuz, RoundedCornerShape(radio.dp))

/** Cristal elevado, para lo que flota (barra inferior, hojas). */
fun Modifier.vidrioAlto(radio: Int = 26): Modifier = this
    .clip(RoundedCornerShape(radio.dp))
    .background(Cristal.vidrioFuerte)
    .border(1.dp, Cristal.bordeLuzAlto, RoundedCornerShape(radio.dp))
