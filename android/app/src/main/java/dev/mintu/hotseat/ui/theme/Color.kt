package dev.mintu.hotseat.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Immutable
data class Sky(
    val top: Color,
    val upper: Color,
    val mid: Color,
    val low: Color,
    val floor: Color,
) {
    companion object {
        val Day = Sky(Color(0xFF6793DF), Color(0xFF4989E9), Color(0xFFA5CAFA), Color(0xFFB3D3F8), Color(0xFFFFFFFF))
        val Dusk = Sky(Color(0xFF834C5C), Color(0xFFA7372F), Color(0xFFED945D), Color(0xFFE68555), Color(0xFFBCB8B8))
        val Night = Sky(Color(0xFF383B43), Color(0xFF0E121D), Color(0xFF1A1E2E), Color(0xFF11131D), Color(0xFF040810))
        // light skies for the conversation, so the chat reads on a calm page
        val Morning = Sky(Color(0xFFD6E4FB), Color(0xFFE3ECFC), Color(0xFFEEF3FD), Color(0xFFF5F8FE), Color(0xFFFAFBFF))
        val Mist = Sky(Color(0xFFE2DDF9), Color(0xFFEAE6FB), Color(0xFFF2F0FD), Color(0xFFF7F6FE), Color(0xFFFBFAFF))
        val Overcast = Sky(Color(0xFF7D8490), Color(0xFF747C8A), Color(0xFF616A7A), Color(0xFF5B6474), Color(0xFF4A5360))
    }
}

fun lerp(a: Sky, b: Sky, t: Float) = Sky(
    lerp(a.top, b.top, t),
    lerp(a.upper, b.upper, t),
    lerp(a.mid, b.mid, t),
    lerp(a.low, b.low, t),
    lerp(a.floor, b.floor, t),
)

@Immutable
data class Palette(
    val text: Color,
    val display: Color,
    val meta: Color,
    val caption: Color,
    val tick: Color,
    val tickActive: Color,
    val playhead: Color,
    val playButton: Color,
    val playGlyph: Color,
    val pill: Color,
    val pillText: Color,
    val chip: Color,
    val glass: Color,
    val glassEdge: Color,
    val glassGlyph: Color,
    val tabBar: Color,
    val tabActive: Color,
    val tabTint: Color,
    val tabLabel: Color,
) {
    companion object {
        val Light = Palette(
            text = Color(0xFF000000),
            display = Color(0xFF000000),
            meta = Color(0xFF3E3E3E),
            caption = Color(0xFF848484),
            tick = Color(0xFF9A9A9A),
            tickActive = Color(0xFF292929),
            playhead = Color(0xFFE03143),
            playButton = Color(0xFF292929),
            playGlyph = Color(0xFFFFFFFF),
            pill = Color(0xFF1F1F1F),
            pillText = Color(0xFFFFFFFF),
            chip = Color(0xFF7CC6FF),
            glass = Color(0xE6FFFFFF),
            glassEdge = Color(0x66FFFFFF),
            glassGlyph = Color(0xFF000000),
            tabBar = Color(0xFFFEFEFE),
            tabActive = Color(0xFFE5E5E5),
            tabTint = Color(0xFF0A64E4),
            tabLabel = Color(0xFF1C1C1C),
        )

        val Dark = Palette(
            text = Color(0xFFFFFFFF),
            display = Color(0xFFF3EAC8),
            meta = Color(0xFFA6A8AE),
            caption = Color(0xFF52545B),
            tick = Color(0xFF53555E),
            tickActive = Color(0xFFE6E5E9),
            playhead = Color(0xFFE03143),
            playButton = Color(0xFFE6E5E9),
            playGlyph = Color(0xFF000000),
            pill = Color(0xFFFFFFFF),
            pillText = Color(0xFF000000),
            chip = Color(0xFF1A1E2C),
            glass = Color(0xFF131519),
            glassEdge = Color(0x1FFFFFFF),
            glassGlyph = Color(0xFFE7EAEF),
            tabBar = Color(0xFF191A21),
            tabActive = Color(0xFF2E323A),
            tabTint = Color(0xFF168AFF),
            tabLabel = Color(0xFFCFD1D9),
        )
    }
}
