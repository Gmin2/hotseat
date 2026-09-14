package dev.mintu.hotseat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import dev.mintu.hotseat.live.LiveProbe
import dev.mintu.hotseat.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // no fade on the system splash, the compose intro draws the same mark in the same spot
        installSplashScreen().setOnExitAnimationListener { it.remove() }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // adb shell am start -n dev.mintu.hotseat/.MainActivity --ei tab 2 opens a tab directly
        val tab = intent.getIntExtra("tab", 0).coerceIn(0, 3)
        val intro = !intent.hasExtra("tab") && !intent.hasExtra("probe")
        if (BuildConfig.DEBUG && intent.hasExtra("probe")) {
            LiveProbe.run(this, lifecycleScope, intent.getIntExtra("probe", 20), intent.getStringExtra("round") ?: "behavioral")
        }
        // adb launches that jump to a tab skip onboarding unless asked for with --ez onboarding true
        val onboarding = intent.getBooleanExtra("onboarding", !intent.hasExtra("tab") && !intent.hasExtra("probe"))
        setContent { App(startTab = tab, intro = intro, demo = intent.getBooleanExtra("demo", false), onboarding = onboarding) }
    }
}
