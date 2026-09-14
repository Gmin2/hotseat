package dev.mintu.hotseat.ui.brand

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.mintu.hotseat.ui.components.TypedText
import dev.mintu.hotseat.ui.theme.HotseatTheme
import kotlinx.coroutines.delay

/**
 * Picks up where the system splash leaves off (same sky and mark), plays the mark in,
 * types the name, then lifts away to reveal the app.
 */
@Composable
fun Intro(onFinished: () -> Unit) {
    val t = HotseatTheme.type
    var named by remember { mutableStateOf(false) }
    val exit = remember { Animatable(0f) }
    var drawn by remember { mutableStateOf(false) }

    LaunchedEffect(drawn) {
        if (!drawn) return@LaunchedEffect
        named = true
        delay(700)
        exit.animateTo(1f, tween(420))
        onFinished()
    }

    Column(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - exit.value
                scaleX = 1f + exit.value * 0.08f
                scaleY = 1f + exit.value * 0.08f
            }
            .background(Brush.verticalGradient(listOf(Color(Brand.SKY_TOP), Color(Brand.SKY_BOTTOM)))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        HoppingBrandMark(112.dp, Color.White, onDone = { drawn = true })
        TypedText(
            if (named) "hotseat" else "",
            t.headline.copy(color = Color.White),
            Modifier.padding(top = 18.dp),
        )
    }
}
