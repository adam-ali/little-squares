package com.littlesquares.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.cbrt
import kotlin.math.pow

private fun toLinear(c: Float) = if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
private fun toSrgb(c: Float) = if (c <= 0.0031308f) 12.92f * c else 1.055f * c.pow(1f / 2.4f) - 0.055f

private fun toOklab(c: Color): FloatArray {
    val r = toLinear(c.red)
    val g = toLinear(c.green)
    val b = toLinear(c.blue)
    val l = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
    val m = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
    val s = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)
    return floatArrayOf(
        0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
        1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
        0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s,
    )
}

private fun fromOklab(lab: FloatArray, alpha: Float): Color {
    val l = (lab[0] + 0.3963377774f * lab[1] + 0.2158037573f * lab[2]).pow(3)
    val m = (lab[0] - 0.1055613458f * lab[1] - 0.0638541728f * lab[2]).pow(3)
    val s = (lab[0] - 0.0894841775f * lab[1] - 1.2914855480f * lab[2]).pow(3)
    val r = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s
    val g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s
    val b = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
    return Color(
        toSrgb(r).coerceIn(0f, 1f),
        toSrgb(g).coerceIn(0f, 1f),
        toSrgb(b).coerceIn(0f, 1f),
        alpha,
    )
}

/** CSS `color-mix(in oklab, a t, b)` — [t] is the share of [a], 0..1. */
fun mix(a: Color, b: Color, t: Float): Color {
    val x = toOklab(a)
    val y = toOklab(b)
    return fromOklab(FloatArray(3) { x[it] * t + y[it] * (1 - t) }, a.alpha * t + b.alpha * (1 - t))
}
