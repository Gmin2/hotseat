package dev.mintu.hotseat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.mintu.hotseat.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // adb shell am start -n dev.mintu.hotseat/.MainActivity --ei tab 2 opens a tab directly
        val tab = intent.getIntExtra("tab", 0)
        setContent { App(startTab = tab) }
    }
}
