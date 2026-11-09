package io.github.labandita.glreborn.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Diseño nuevo, y feo a propósito de lo que imita.
 *
 * El GLTools original era una lista gris de casillas. Esto es lo contrario: fondo casi
 * negro, verde de terminal para lo que está comprobado y ámbar para lo que no, y el
 * estado del teléfono siempre visible arriba. El color no decora: avisa.
 *   verde  → escrito y confirmado
 *   ámbar  → se intentó y no se pudo / falta dato
 *   rojo   → no hay vía, no se aplicó nada
 */
object Colores {
    val fondo = Color(0xFF0B0E0C)
    val superficie = Color(0xFF141A16)
    val superficieAlta = Color(0xFF1E2721)
    val verde = Color(0xFF3DDC84)
    val ambar = Color(0xFFFFB020)
    val rojo = Color(0xFFFF5A5F)
    val texto = Color(0xFFE8F0EA)
    val textoSuave = Color(0xFF8FA396)
    val borde = Color(0xFF2A3A30)
}

private val esquema = darkColorScheme(
    primary = Colores.verde,
    onPrimary = Colores.fondo,
    secondary = Colores.ambar,
    background = Colores.fondo,
    onBackground = Colores.texto,
    surface = Colores.superficie,
    onSurface = Colores.texto,
    error = Colores.rojo,
)

private val tipografia = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp),
)

@Composable
fun TemaReborn(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquema, typography = tipografia, content = contenido)
}
