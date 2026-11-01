package com.littlesquares.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.littlesquares.app.data.Pastel

@Immutable
data class LsColors(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val primary: Color,
    val onPrimary: Color,
    val sel: Color,
    val onPastel: Color,
    val danger: Color,
    val wallTop: Color,
    val wallBottom: Color,
    val pastels: List<Color>,
) {
    fun pastel(p: Pastel): Color = pastels[p.ordinal]

    /** Habit colour mixed into Surface — the "faint" square. */
    fun tint(p: Pastel, pct: Int = 26): Color = mix(pastel(p), surface, pct / 100f)

    /** Outline for today's square. */
    fun todayRing(p: Pastel): Color = mix(pastel(p), ink2, .35f)

    /** Faint check icon on an unchecked circle. */
    fun checkIdle(p: Pastel): Color = mix(pastel(p), ink3, .5f)
}

private fun c(v: Long) = Color(v)

val LightLs = LsColors(
    dark = false,
    bg = c(0xFFFBF8F3), surface = c(0xFFFFFFFF), surface2 = c(0xFFF3EEE7), line = c(0xFFE6DED3),
    ink = c(0xFF2C2926), ink2 = c(0xFF6B645C), ink3 = c(0xFFA1998F),
    primary = c(0xFF2C2926), onPrimary = c(0xFFFBF8F3), sel = c(0xFFE8E0F7),
    onPastel = c(0xFF2C2926), danger = c(0xFFB0435A),
    wallTop = c(0xFFE9E0F4), wallBottom = c(0xFFF5E6DA),
    pastels = listOf(0xFFF5BA9C, 0xFFF1D68C, 0xFFB0D4A7, 0xFFA0D7C9, 0xFFAAC8EE, 0xFFC6B7EE, 0xFFF0ABC2, 0xFFDCC5A8).map(::c),
)

val DarkLs = LsColors(
    dark = true,
    bg = c(0xFF1C1B1E), surface = c(0xFF272629), surface2 = c(0xFF302E33), line = c(0xFF3D3A40),
    ink = c(0xFFEDE7E0), ink2 = c(0xFFAEA79F), ink3 = c(0xFF7C766F),
    primary = c(0xFFEDE7E0), onPrimary = c(0xFF1C1B1E), sel = c(0xFF3E3849),
    onPastel = c(0xFF1C1B1E), danger = c(0xFFE59AAB),
    wallTop = c(0xFF2B2734), wallBottom = c(0xFF352C29),
    pastels = listOf(0xFFC99880, 0xFFC7B37C, 0xFF92B08C, 0xFF87B3A9, 0xFF8EA7C8, 0xFFA598C7, 0xFFC790A3, 0xFFB6A38D).map(::c),
)

val LocalLs = staticCompositionLocalOf { LightLs }
