package com.ominime.app.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

class Pal(
    val bg: Color,
    val card: Color,
    val lift: Color,
    val text: Color,
    val mute: Color,
    val line: Color,
    val acc: Color,
    val acc2: Color,
    val glass: Color,
    val dock: Color
)

val fallback = Pal(
    Color(0xFF08080B),
    Color(0xFF141418),
    Color(0xFF1C1C22),
    Color(0xFFF6F6F8),
    Color(0xFF9A9AA6),
    Color(0x1FFFFFFF),
    Color(0xFFFF3D71),
    Color(0xFFFFB03A),
    Color(0x14FFFFFF),
    Color(0xFF121216)
)

val localpal = staticCompositionLocalOf { fallback }

object ink {
    val acc = Color(0xFFFF6A00)
    val acc2 = Color(0xFFFFC02E)
    val tones = listOf(
        Triple("orange", Color(0xFFFF6A00), Color(0xFFFFC02E)),
        Triple("magenta", Color(0xFFFF2E88), Color(0xFFFF8AC4)),
        Triple("cyan", Color(0xFF00D9F5), Color(0xFF7CF7FF)),
        Triple("lime", Color(0xFF9BFF3C), Color(0xFFDCFFA0)),
        Triple("violet", Color(0xFF9B6BFF), Color(0xFFC9A8FF)),
        Triple("amber", Color(0xFFFFC02E), Color(0xFFFFE08A)),
        Triple("crimson", Color(0xFFFF3B30), Color(0xFFFF9A94))
    )

    fun pair(key: String): Pair<Color, Color> {
        val one = tones.firstOrNull { it.first == key } ?: tones.first()
        return one.second to one.third
    }
    val good = Color(0xFF32D74B)

    fun dark(key: String = "orange"): Pal {
        val (a, b) = pair(key)
        return Pal(
            Color(0xFF07070A), Color(0xFF101116), Color(0xFF181A21),
            Color(0xFFF7F7F9), Color(0xFF8D909B), Color(0x1FFFFFFF),
            a, b, Color(0x14FFFFFF), Color(0xFF0C0D11)
        )
    }

    fun black(key: String = "orange"): Pal {
        val (a, b) = pair(key)
        return Pal(
            Color(0xFF000000), Color(0xFF09090C), Color(0xFF111318),
            Color(0xFFF7F7F9), Color(0xFF82858F), Color(0x1AFFFFFF),
            a, b, Color(0x10FFFFFF), Color(0xFF07080B)
        )
    }

    fun light(key: String = "orange"): Pal {
        val (a, b) = pair(key)
        return Pal(
            Color(0xFFF2F2F5), Color(0xFFFFFFFF), Color(0xFFFFFFFF),
            Color(0xFF0B0B0F), Color(0xFF6E727E), Color(0x14000000),
            a, b, Color(0xB3FFFFFF), Color(0xFFFFFFFF)
        )
    }
}

private val typo = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Black, fontSize = 44.sp, letterSpacing = (-1.4).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.6).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.4.sp)
)

@Composable
fun Skin(mode: Int, acc: String = "orange", content: @Composable () -> Unit) {
    val night = isSystemInDarkTheme()
    val dark = when (mode) {
        1 -> true
        2 -> false
        3 -> true
        else -> night
    }
    val p = when (mode) {
        3 -> ink.black(acc)
        else -> if (dark) ink.dark(acc) else ink.light(acc)
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val win = ((view.context as? Activity) ?: view.context.findact())?.window ?: return@SideEffect
            WindowCompat.getInsetsController(win, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(localpal provides p) {
        MaterialTheme(
            colorScheme = if (dark) darkColorScheme(
                primary = p.acc,
                onPrimary = Color.White,
                background = p.bg,
                onBackground = p.text,
                surface = p.card,
                onSurface = p.text,
                surfaceVariant = p.lift,
                onSurfaceVariant = p.mute,
                secondary = p.mute
            ) else lightColorScheme(
                primary = p.acc,
                onPrimary = Color.White,
                background = p.bg,
                onBackground = p.text,
                surface = p.card,
                onSurface = p.text,
                surfaceVariant = p.lift,
                onSurfaceVariant = p.mute,
                secondary = p.mute
            ),
            typography = typo,
            content = content
        )
    }
}
