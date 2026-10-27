package com.tally.counter.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.tally.counter.data.settings.ThemeMode

/** Extra colours that Material's scheme doesn't cover. */
@Immutable
data class TallyColors(
    val badgeTop: Color,
    val badgeBottom: Color,
    val onBadge: Color,
    val success: Color,
    val warning: Color,
    val cardStroke: Color,
    val muted: Color,
    val glowAlpha: Float,
    val isDark: Boolean
)

private val DarkTallyColors = TallyColors(
    badgeTop = Color(0xFFFF5A4F),
    badgeBottom = Color(0xFFE5251B),
    onBadge = Color.White,
    success = Color(0xFF32D74B),
    warning = Color(0xFFFFB340),
    cardStroke = Color(0x1FFFFFFF),
    muted = Color(0xFF9A9AA6),
    glowAlpha = 0.30f,
    isDark = true
)

private val LightTallyColors = TallyColors(
    badgeTop = Color(0xFFFF4D42),
    badgeBottom = Color(0xFFE0261C),
    onBadge = Color.White,
    success = Color(0xFF1E9E55),
    warning = Color(0xFFB86E00),
    cardStroke = Color(0x14000000),
    muted = Color(0xFF6B6B76),
    glowAlpha = 0.18f,
    isDark = false
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFFF453A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1513),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFB9B9C6),
    onSecondary = Color(0xFF1B1B22),
    secondaryContainer = Color(0xFF2A2A33),
    onSecondaryContainer = Color(0xFFE6E6EE),
    tertiary = Color(0xFF32D74B),
    background = Color(0xFF0A0A0D),
    onBackground = Color(0xFFF2F2F5),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFF2F2F5),
    surfaceVariant = Color(0xFF1C1C23),
    onSurfaceVariant = Color(0xFF9A9AA6),
    surfaceContainerLowest = Color(0xFF08080B),
    surfaceContainerLow = Color(0xFF111116),
    surfaceContainer = Color(0xFF15151B),
    surfaceContainerHigh = Color(0xFF1B1B22),
    surfaceContainerHighest = Color(0xFF22222A),
    outline = Color(0xFF3A3A44),
    outlineVariant = Color(0xFF26262E),
    error = Color(0xFFFF6B60),
    onError = Color.White
)

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFFE0261C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF55555F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E6EE),
    onSecondaryContainer = Color(0xFF1B1B22),
    tertiary = Color(0xFF1E9E55),
    background = Color(0xFFF4F4F7),
    onBackground = Color(0xFF15151A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF15151A),
    surfaceVariant = Color(0xFFEDEDF2),
    onSurfaceVariant = Color(0xFF6B6B76),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F9FB),
    surfaceContainer = Color(0xFFF3F3F6),
    surfaceContainerHigh = Color(0xFFEDEDF1),
    surfaceContainerHighest = Color(0xFFE6E6EB),
    outline = Color(0xFFC4C4CC),
    outlineVariant = Color(0xFFE2E2E8),
    error = Color(0xFFD7261C),
    onError = Color.White
)

private val TallyTypography: Typography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

private val TallyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** Tabular (fixed-width) digits so counts don't jiggle while animating. */
val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")

val LocalTallyColors = staticCompositionLocalOf { DarkTallyColors }

@Composable
fun TallyTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(LocalTallyColors provides if (dark) DarkTallyColors else LightTallyColors) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = TallyTypography,
            shapes = TallyShapes,
            content = content
        )
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
