package dev.mintu.hotseat.ui.brand

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.mintu.hotseat.ui.components.TypedText
import dev.mintu.hotseat.ui.theme.HotseatTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// same beats as the owl onboarding it is based on, in ms from the moment the splash hands over
private object Beat {
    const val HALF_BLINK = 600L
    const val SHUT = 830L
    const val WAKE = 1050L
    const val GLANCE_LEFT = 1450L
    const val GLANCE_RIGHT = 1900L
    const val SETTLE = 2350L
    const val LEAVE = 3400L
}

/**
 * Picks up from the system splash, which shows the same bird asleep on its sticker in the same spot.
 * It half blinks, shuts, wakes with a hop and a flap, glances left and right, settles, the name types in, and it lifts away.
 */
@Composable
fun Intro(onFinished: () -> Unit) {
    val t = HotseatTheme.type
    val open = remember { Animatable(0f) }
    val look = remember { Animatable(0f) }
    val wing = remember { Animatable(0f) }
    val hop = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    var named by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                delay(Beat.HALF_BLINK)
                open.animateTo(0.35f, tween(160))
                delay(Beat.SHUT - Beat.HALF_BLINK - 160)
                open.animateTo(0f, tween(140))
                delay(Beat.WAKE - Beat.SHUT - 140)
                open.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 420f))
            }
            launch {
                delay(Beat.WAKE)
                launch { hop.animateTo(1f, tween(170, easing = FastOutSlowInEasing)); hop.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 380f)) }
                repeat(2) {
                    wing.animateTo(1f, tween(140))
                    wing.animateTo(0f, tween(150))
                }
            }
            launch {
                delay(Beat.GLANCE_LEFT)
                look.animateTo(-8f, spring(dampingRatio = 0.8f, stiffness = 500f))
                delay(Beat.GLANCE_RIGHT - Beat.GLANCE_LEFT)
                look.animateTo(8f, spring(dampingRatio = 0.8f, stiffness = 500f))
                delay(Beat.SETTLE - Beat.GLANCE_RIGHT)
                look.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 420f))
                named = true
            }
        }
        delay(Beat.LEAVE - Beat.SETTLE)
        exit.animateTo(1f, tween(380))
        onFinished()
    }

    val box = MascotArt.SPLASH_BOX_DP.dp
    // the splash centres the sticker, not the 512 box, so shift by the sticker's offset from the box centre
    val shift = box * ((MascotArt.STICKER_CY - MascotArt.BOX / 2) / MascotArt.BOX)

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - exit.value
                translationY = -exit.value * 40.dp.toPx()
            }
            .background(Color(0xFF5A8EE4)),
        contentAlignment = Alignment.Center,
    ) {
        Mascot(
            box,
            MascotPose(open = open.value, look = look.value, wing = wing.value, lift = hop.value * 28f),
            Modifier.offset(y = -shift),
            sticker = true,
        )
        TypedText(
            if (named) "hotseat" else "",
            t.headline.copy(color = Color.White),
            Modifier.padding(top = box + 36.dp),
        )
    }
}
