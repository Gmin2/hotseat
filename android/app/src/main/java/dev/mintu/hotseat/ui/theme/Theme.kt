package dev.mintu.hotseat.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalPalette = staticCompositionLocalOf { Palette.Light }
private val LocalType = staticCompositionLocalOf { Type.Default }

object HotseatTheme {
    val palette: Palette
        @Composable get() = LocalPalette.current

    val type: Type
        @Composable get() = LocalType.current
}

@Composable
fun HotseatTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalPalette provides if (dark) Palette.Dark else Palette.Light,
        LocalType provides Type.Default,
        content = content,
    )
}
