package dev.mintu.hotseat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.mintu.hotseat.ui.icons.InkIcon
import dev.mintu.hotseat.ui.icons.InkIcons
import dev.mintu.hotseat.ui.icons.InkMotion
import dev.mintu.hotseat.ui.theme.Dimens
import dev.mintu.hotseat.ui.theme.HotseatTheme

data class TabItem(val label: String, val icon: InkIcon, val motion: InkMotion)

val tabItems = listOf(
    TabItem("Practice", InkIcons.Practice, InkMotion.Bob),
    TabItem("Sessions", InkIcons.Sessions, InkMotion.Spin),
    TabItem("Progress", InkIcons.Progress, InkMotion.Draw),
    TabItem("You", InkIcons.You, InkMotion.Wiggle),
)

@Composable
fun TabBar(selected: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = HotseatTheme.palette
    val t = HotseatTheme.type
    val bar by animateColorAsState(p.tabBar, tween(500), label = "bar")
    val activeFill by animateColorAsState(p.tabActive, tween(500), label = "active")
    val pulses = remember { mutableStateListOf(0, 0, 0, 0) }
    Row(
        modifier
            .padding(horizontal = Dimens.tabBarInset)
            .fillMaxWidth()
            .height(Dimens.tabBarHeight)
            .shadow(24.dp, RoundedCornerShape(Dimens.tabBarHeight / 2), ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(Dimens.tabBarHeight / 2))
            .background(bar)
            .padding(Dimens.tabActiveInset),
    ) {
        tabItems.forEachIndexed { i, tab ->
            val active = i == selected
            val tint by animateColorAsState(if (active) p.tabTint else p.tabLabel, tween(220), label = "tint")
            val fill by animateColorAsState(if (active) activeFill else activeFill.copy(alpha = 0f), tween(220), label = "fill")
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape((Dimens.tabBarHeight - Dimens.tabActiveInset * 2) / 2))
                    .background(fill)
                    .clickable(remember { MutableInteractionSource() }, indication = null) {
                        pulses[i]++
                        onTab(i)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                InkIcon(tab.icon, 20.dp, tint, motion = tab.motion, pulse = pulses[i])
                BasicText(tab.label, Modifier.padding(top = 3.dp), style = t.tabLabel.copy(color = tint))
            }
        }
    }
}

/** White sheet that springs up from the bottom over a dimmed screen. */
@Composable
fun BoxScope.Sheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(200))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
    }
    AnimatedVisibility(
        visible,
        Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(spring(dampingRatio = 0.85f, stiffness = 380f)) { it },
        exit = slideOutVertically(tween(220)) { it },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding(),
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(5.dp).clip(CircleShape).background(Color(0xFFD9D9DE)))
            }
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Dimens.gutter, vertical = 16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Color(0x0F000000)))
}

@Composable
fun Dot(color: Color, size: Dp = 6.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}
