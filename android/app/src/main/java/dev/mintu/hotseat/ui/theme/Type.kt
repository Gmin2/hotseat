package dev.mintu.hotseat.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.mintu.hotseat.R

private fun inter(weight: FontWeight) = Font(
    R.font.inter,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Inter = FontFamily(
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
    inter(FontWeight.SemiBold),
)

@Immutable
data class Type(
    val display: TextStyle,
    val headline: TextStyle,
    val meta: TextStyle,
    val caption: TextStyle,
    val pill: TextStyle,
    val tabLabel: TextStyle,
) {
    companion object {
        val Default = Type(
            display = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 90.sp, lineHeight = 90.sp, letterSpacing = (-0.03).em),
            headline = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 46.sp, letterSpacing = (-0.01).em),
            meta = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
            caption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
            pill = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
            tabLabel = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, lineHeight = 12.sp),
        )
    }
}
