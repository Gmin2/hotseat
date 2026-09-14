package dev.mintu.hotseat.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.mintu.hotseat.data.Mock
import dev.mintu.hotseat.ui.brand.Intro
import dev.mintu.hotseat.ui.components.Mood
import dev.mintu.hotseat.ui.components.SkyBackdrop
import dev.mintu.hotseat.ui.components.TabBar
import dev.mintu.hotseat.ui.practice.Phase
import dev.mintu.hotseat.ui.practice.PracticeScreen
import dev.mintu.hotseat.ui.practice.ReportSheet
import dev.mintu.hotseat.ui.practice.RoundSheet
import dev.mintu.hotseat.ui.practice.rememberPractice
import dev.mintu.hotseat.ui.tabs.ProgressTab
import dev.mintu.hotseat.ui.tabs.SessionsTab
import dev.mintu.hotseat.ui.tabs.YouTab
import dev.mintu.hotseat.ui.theme.Dimens
import dev.mintu.hotseat.ui.theme.HotseatTheme
import android.graphics.Color as AndroidColor

@Composable
fun App(startTab: Int = 0, intro: Boolean = false) {
    var tab by remember { mutableIntStateOf(startTab) }
    var showIntro by remember { mutableStateOf(intro) }
    val practice = rememberPractice()

    val mood = if (tab == 0) practice.mood else Mood.Day
    val dark = mood == Mood.Night || mood == Mood.Overcast
    val level = if (tab == 0) practice.level else 0f
    val top by animateColorAsState(mood.sky.top, tween(mood.ms), label = "scrim top")
    // tabs sit on a white card, practice sits on the sky floor
    val floor by animateColorAsState(if (tab == 0) mood.sky.floor else Color.White, tween(mood.ms), label = "scrim floor")

    val activity = LocalActivity.current as? ComponentActivity
    LaunchedEffect(dark, showIntro) {
        val style = if (dark || showIntro) SystemBarStyle.dark(AndroidColor.TRANSPARENT) else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        activity?.enableEdgeToEdge(style, style)
    }

    HotseatTheme(dark = dark) {
        Box(Modifier.fillMaxSize()) {
            SkyBackdrop(mood, level)

            AnimatedContent(
                tab,
                transitionSpec = { (fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.98f)).togetherWith(fadeOut(tween(160))) },
                label = "tab",
            ) { current ->
                when (current) {
                    0 -> PracticeScreen(practice)
                    1 -> SessionsTab(onOpen = {
                        practice.elapsed = Mock.totalMs
                        practice.playing = false
                        practice.phase = Phase.Report
                        tab = 0
                    })
                    2 -> ProgressTab()
                    else -> YouTab()
                }
            }

            // scrolled content fades out under the status bar and under the floating tab bar
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars.add(WindowInsets(top = 20.dp)))
                    .background(Brush.verticalGradient(listOf(top, top.copy(alpha = 0.85f), top.copy(alpha = 0f)))),
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(Dimens.tabBarBottom + Dimens.tabBarHeight + 40.dp)
                    .background(Brush.verticalGradient(listOf(floor.copy(alpha = 0f), floor.copy(alpha = 0.9f), floor))),
            )

            Column(Modifier.align(Alignment.BottomCenter)) {
                TabBar(tab, { tab = it })
                Spacer(Modifier.height(Dimens.tabBarBottom))
            }

            RoundSheet(practice)
            ReportSheet(practice)

            if (showIntro) Intro(onFinished = { showIntro = false })
        }
    }
}
