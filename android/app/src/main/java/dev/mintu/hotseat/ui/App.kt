package dev.mintu.hotseat.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.mintu.hotseat.live.LiveClient
import dev.mintu.hotseat.live.LiveProbe
import androidx.compose.ui.unit.dp
import dev.mintu.hotseat.data.Mock
import dev.mintu.hotseat.ui.brand.Intro
import dev.mintu.hotseat.ui.components.Mood
import dev.mintu.hotseat.ui.components.SkyBackdrop
import dev.mintu.hotseat.ui.components.TabBar
import dev.mintu.hotseat.ui.practice.Practice
import dev.mintu.hotseat.ui.practice.PracticeScreen
import dev.mintu.hotseat.ui.practice.ReportSheet
import dev.mintu.hotseat.ui.practice.RoundSheet
import dev.mintu.hotseat.ui.tabs.ProgressTab
import dev.mintu.hotseat.ui.tabs.SessionsTab
import dev.mintu.hotseat.ui.tabs.YouTab
import dev.mintu.hotseat.ui.theme.Dimens
import dev.mintu.hotseat.ui.theme.HotseatTheme
import android.graphics.Color as AndroidColor

@Composable
fun App(startTab: Int = 0, intro: Boolean = false, demo: Boolean = false) {
    var tab by remember { mutableIntStateOf(startTab) }
    var showIntro by remember { mutableStateOf(intro) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { LiveProbe.api(context) }
    val practice = remember {
        Practice(
            scope = scope,
            newSession = { LiveClient(context, api, scope) },
            score = { round, seconds, turns -> api.report(round, seconds, turns) },
        ).also { it.demo = demo }
    }

    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) practice.start() else practice.micDenied()
    }
    val startWithMic = {
        if (practice.demo || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) practice.start()
        else mic.launch(Manifest.permission.RECORD_AUDIO)
    }

    // leaving the app mid interview ends it, a session should never keep the mic or the meter running in the background
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) practice.end() }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            practice.dispose()
        }
    }

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
                    0 -> PracticeScreen(practice, onNeedMic = startWithMic)
                    1 -> SessionsTab(onOpen = { session ->
                        practice.showDemo(Mock.rounds.indexOfFirst { it.title == session.round }.coerceAtLeast(0))
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

            RoundSheet(practice, onStart = startWithMic)
            ReportSheet(practice)

            if (showIntro) Intro(onFinished = { showIntro = false })
        }
    }
}
