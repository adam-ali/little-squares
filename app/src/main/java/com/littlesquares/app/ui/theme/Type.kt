@file:OptIn(ExperimentalTextApi::class)

package com.littlesquares.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.littlesquares.app.R

/** Nunito variable font, pinned to the weights the design uses. */
val Nunito = FontFamily(
    listOf(300, 400, 500, 600, 700, 800).map { w ->
        Font(
            R.font.nunito,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    },
)

/** Text style shorthand: size in sp, weight, colour, line-height multiplier, letter spacing in sp. */
fun ts(
    size: Int,
    weight: Int = 400,
    color: Color = Color.Unspecified,
    lh: Float? = null,
    ls: Float = 0f,
): TextStyle = TextStyle(
    fontFamily = Nunito,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    color = color,
    lineHeight = lh?.let { (size * it).sp } ?: TextUnit.Unspecified,
    letterSpacing = ls.sp,
)
