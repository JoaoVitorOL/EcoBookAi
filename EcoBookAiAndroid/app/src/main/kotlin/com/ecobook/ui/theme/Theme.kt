package com.ecobook.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF24584A),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFC87A43),
    onSecondary = Color(0xFF2A1709),
    tertiary = Color(0xFF37607F),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F2E7),
    onBackground = Color(0xFF18312B),
    surface = Color(0xFFFFFBF6),
    onSurface = Color(0xFF18312B),
    primaryContainer = Color(0xFFC9E1D7),
    onPrimaryContainer = Color(0xFF0A2B22),
    secondaryContainer = Color(0xFFFCE7D8),
    onSecondaryContainer = Color(0xFF5B3317),
    tertiaryContainer = Color(0xFFDDE8F4),
    onTertiaryContainer = Color(0xFF10293F),
    surfaceVariant = Color(0xFFE4DDD3),
    onSurfaceVariant = Color(0xFF4B635A),
    outline = Color(0xFF73857C),
    error = Color(0xFFB4523E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DBD7),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFAECFC0),
    onPrimary = Color(0xFF10382D),
    secondary = Color(0xFFF0B48A),
    onSecondary = Color(0xFF4C2811),
    tertiary = Color(0xFFA8C9EC),
    onTertiary = Color(0xFF0D2942),
    background = Color(0xFF091114),
    onBackground = Color(0xFFDEE4E0),
    surface = Color(0xFF111A1D),
    onSurface = Color(0xFFDEE4E0),
    primaryContainer = Color(0xFF24584A),
    onPrimaryContainer = Color(0xFFD7F0E5),
    secondaryContainer = Color(0xFF6B3F22),
    onSecondaryContainer = Color(0xFFFFDBC6),
    tertiaryContainer = Color(0xFF244A69),
    onTertiaryContainer = Color(0xFFD7E8FA),
    surfaceVariant = Color(0xFF263238),
    onSurfaceVariant = Color(0xFFBEC9C2),
    outline = Color(0xFF87938C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LocalEcoBookDarkTheme = staticCompositionLocalOf { false }

private val EcoBookTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 38.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )
)

private val EcoBookShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp)
)

@Composable
fun EcoBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalEcoBookDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = EcoBookTypography,
            shapes = EcoBookShapes,
            content = content
        )
    }
}

@Composable
fun isEcoBookDarkTheme(): Boolean = LocalEcoBookDarkTheme.current

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
