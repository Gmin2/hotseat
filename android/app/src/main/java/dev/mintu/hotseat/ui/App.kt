package dev.mintu.hotseat.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.mintu.hotseat.data.Mock
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
fun App(startTab: Int = 0) {
    var tab by remember { mutableIntStateOf(startTab) }
    val practice = rememberPractice()

    val mood = if (tab == 0) practice.mood else Mood.Day
    val dark = mood == Mood.Night || mood == Mood.Overcast
    val level = if (tab == 0) practice.level else 0f

    val activity = LocalActivity.current as? ComponentActivity
    LaunchedEffect(dark) {
        val style = if (dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT) else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
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

            Column(Modifier.align(Alignment.BottomCenter)) {
                TabBar(tab, { tab = it })
                Spacer(Modifier.height(Dimens.tabBarBottom))
            }

            RoundSheet(practice)
            ReportSheet(practice)
        }
    }
}
