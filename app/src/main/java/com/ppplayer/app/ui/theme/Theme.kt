package com.ppplayer.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ppplayer.app.R
import com.ppplayer.app.data.ThemeMode

// Brand ramp sampled from the PPPlayer mark: cyan → blue → violet → magenta.
val BrandCyan = Color(0xFF2AD4F5)
val BrandBlue = Color(0xFF2B5BF0)
val BrandViolet = Color(0xFF7A45F2)
val BrandMagenta = Color(0xFFD957F0)
val BrandNavy = Color(0xFF1B2B6B)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9D8CFF),
    onPrimary = Color(0xFF14104A),
    primaryContainer = Color(0xFF2D2A72),
    onPrimaryContainer = Color(0xFFE3DEFF),
    secondary = Color(0xFF5FDDF7),
    onSecondary = Color(0xFF00303B),
    secondaryContainer = Color(0xFF12394A),
    onSecondaryContainer = Color(0xFFC4F3FF),
    tertiary = Color(0xFFE894F5),
    onTertiary = Color(0xFF45104F),
    background = Color(0xFF0A0C16),
    onBackground = Color(0xFFE8EAF6),
    surface = Color(0xFF0F1220),
    onSurface = Color(0xFFE8EAF6),
    surfaceVariant = Color(0xFF1E2238),
    onSurfaceVariant = Color(0xFF9CA3C4),
    surfaceContainerLowest = Color(0xFF080A12),
    surfaceContainerLow = Color(0xFF12152A),
    surfaceContainer = Color(0xFF171B33),
    surfaceContainerHigh = Color(0xFF1F2440),
    surfaceContainerHighest = Color(0xFF282E50),
    outline = Color(0xFF3A4066),
    outlineVariant = Color(0xFF242944),
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF4A0A0A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3B46E0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E3FF),
    onPrimaryContainer = Color(0xFF141A66),
    secondary = Color(0xFF0086A8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3F4FD),
    onSecondaryContainer = Color(0xFF003544),
    tertiary = Color(0xFFA233BF),
    onTertiary = Color.White,
    background = Color(0xFFF3F5FF),
    onBackground = Color(0xFF141A3D),
    surface = Color(0xFFFBFBFF),
    onSurface = Color(0xFF141A3D),
    surfaceVariant = Color(0xFFE4E7F7),
    onSurfaceVariant = Color(0xFF596085),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F7FF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFECEFFC),
    surfaceContainerHighest = Color(0xFFE2E6F8),
    outline = Color(0xFFB5BBD9),
    outlineVariant = Color(0xFFDADFF2),
    error = Color(0xFFC22B2B),
    onError = Color.White,
)

@Immutable
data class BrandColors(
    /** The mark's full gradient. Reserved for the few things that should carry the brand. */
    val gradient: Brush,
    /** Soft wash behind the home header. */
    val glow: Color,
    /** Text and icons placed on [gradient]. */
    val onGradient: Color,
    /** Dark scrim colours for content over video frames. */
    val scrim: Color,
)

private val BrandGradient = Brush.linearGradient(listOf(BrandCyan, BrandBlue, BrandViolet, BrandMagenta))

private val DarkBrand = BrandColors(BrandGradient, Color(0xFF3B2FB8), Color.White, Color(0xCC05060C))
private val LightBrand = BrandColors(BrandGradient, Color(0xFFB9C4FF), Color.White, Color(0xCC05060C))

val LocalBrandColors = staticCompositionLocalOf { DarkBrand }

@OptIn(ExperimentalTextApi::class)
private fun readex(weight: FontWeight) = Font(
    R.font.readex_pro,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Readex = FontFamily(
    readex(FontWeight.Light),
    readex(FontWeight.Normal),
    readex(FontWeight.Medium),
    readex(FontWeight.SemiBold),
    readex(FontWeight.Bold),
)

/** Tabular digits for durations and counters so they don't wobble while playing. */
const val TABULAR = "tnum"

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Readex, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 42.sp),
    headlineMedium = TextStyle(fontFamily = Readex, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Readex, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Readex, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = Readex, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Readex, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = Readex, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Readex, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Readex, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Readex, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Readex, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Readex, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun PPPlayerTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalBrandColors provides if (dark) DarkBrand else LightBrand) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object Brand {
    val colors: BrandColors
        @Composable get() = LocalBrandColors.current
}
