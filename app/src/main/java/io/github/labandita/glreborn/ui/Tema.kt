package io.github.labandita.glreborn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import io.github.labandita.glreborn.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Superficies neutras y un acento de marca; sin degradados ni alias de paleta. */
object Cristal {
    /*
      ── RELECTURA DEL TEMA, 1.8.1 ────────────────────────────────────────────────
      Lo que había era el ejemplo de manual de «diseño que parece HTML»: fondo con
      degradado violeta, acento violeta, acento cian, acento rosa, y vidrio con borde
      blanco en cada caja. Todo pesaba igual y nada comunicaba.

      Lo que hay ahora, y por qué:

      · UN solo acento funcional: el verde de «esto funciona». El cian deja de ser adorno y
        pasa a ser el color de la marca, usado SÓLO en la cabecera y en lo seleccionado.
        El rosa desaparece del sistema.
      · Fondo casi negro neutro, no violeta. Una herramienta de sistema se lee sobre negro;
        el violeta era un disfraz.
      · Tres superficies con tono creciente en vez de un borde blanco en todo: la altura se
        expresa con TONO, que es la guía de Material 3. El borde se reserva para lo que
        necesita un límite de verdad.
      · El vidrio sobrevive SÓLO en lo que flota (la barra de abajo). Ya no hay vidrio en
        cada tarjeta: eso era lo que aplastaba la jerarquía.
      · Verde, ámbar y rojo se quedan porque comunican (se puede / cuidado / no se puede).
    */

    // Fondo: neutro y profundo. Sin violeta.
    val fondoArriba = Color(0xFF0B0D10)
    val fondoMedio = Color(0xFF0E1116)
    val fondoAbajo = Color(0xFF080A0D)

    // Superficie de contenido: tres pasos de tono. La altura se ve, no se adivina.
    val superficie = Color(0xFF14181E)
    val superficieAlta = Color(0xFF1B2028)
    val superficieMasAlta = Color(0xFF232A34)

    // El único acento de marca (cabecera y selección).
    val marca = Color(0xFF4C9AFF)

    // Estados. Comunican, no decoran.
    val verde = Color(0xFF3DDC84)
    val ambar = Color(0xFFFFB020)
    val rojo = Color(0xFFFF5A6E)


    val texto = Color(0xFFEDF1F7)
    val textoSuave = Color(0xFF98A2B3)
    val textoTenue = Color(0xFF667085)

    // Bordes: discretos. Antes eran blancos al 20-40 % en todas partes.
    val bordeLuz = Color(0x14FFFFFF)
    val bordeLuzAlto = Color(0x24FFFFFF)

    // Vidrio: sólo para lo que flota.
    val vidrio = Color(0xFF14181E)
    val vidrioFuerte = Color(0xFF232A34)

}

private val esquema = darkColorScheme(
    primary = Cristal.marca,
    onPrimary = Cristal.fondoAbajo,
    secondary = Cristal.marca,
    background = Cristal.fondoArriba,
    onBackground = Cristal.texto,
    surface = Cristal.fondoMedio,
    onSurface = Cristal.texto,
    error = Cristal.rojo,
)

/**
 * La tipografía deja de ser la del sistema.
 *
 * Hasta la 1.9.1 la app usaba la fuente por defecto de Android, que es la del sistema
 * operativo y por eso la hace indistinguible: misma letra que los ajustes, que el navegador,
 * que cualquier cosa. Con eso, por muy bien ordenada que esté la pantalla, sigue pareciendo
 * una página web del teléfono.
 *
 * Se usa **Atkinson Hyperlegible Next**, licencia SIL OFL 1.1 (licencia completa en
 * `assets/OFL-AtkinsonNext.txt`), regular/medium/semibold/bold en `res/font/`. La eligió el
 * Braille Institute para que se lea con poca vista: trazos abiertos, la «l» y la «1» distintas,
 * la «I» con remates. En un panel de datos con números de modelo y cadenas de versión eso no es
 * estética, es que se lea.
 *
 * Sólo se usa la fuente donde hay texto de interfaz. El monoespaciado de los datos técnicos
 * sigue siendo el del sistema: ahí lo que importa es que los dígitos cuadren en columna.
 */
private val atkinson = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_medium, FontWeight.Medium),
    Font(R.font.atkinson_semibold, FontWeight.SemiBold),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

private val tipografia = Typography(
    displayLarge = TextStyle(fontFamily = atkinson, fontSize = 34.sp, fontWeight = FontWeight.Light, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = atkinson, fontSize = 27.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp, letterSpacing = (-0.6).sp),
    headlineSmall = TextStyle(fontFamily = atkinson, fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = atkinson, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = atkinson, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = atkinson, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = atkinson, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = atkinson, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = atkinson, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = atkinson, fontSize = 13.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontFamily = atkinson, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = atkinson, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp),
)

/** La familia, para las pantallas que escriben `Text(...)` con estilo propio. */
val familiaAtkinson: FontFamily = atkinson

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
