package dev.mintu.hotseat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.mintu.hotseat.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // no fade on the system splash, the compose intro draws the same mark in the same spot
        installSplashScreen().setOnExitAnimationListener { it.remove() }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // adb shell am start -n dev.mintu.hotseat/.MainActivity --ei tab 2 opens a tab directly
        val tab = intent.getIntExtra("tab", 0)
        val intro = !intent.hasExtra("tab")
        setContent { App(startTab = tab, intro = intro) }
    }
}
