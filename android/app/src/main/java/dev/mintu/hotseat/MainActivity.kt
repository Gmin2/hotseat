package dev.mintu.hotseat

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mintu.hotseat.ui.hero.HeroScreen
import dev.mintu.hotseat.ui.hero.HeroState
import dev.mintu.hotseat.ui.theme.HotseatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            HotseatTheme {
                var tab by remember { mutableIntStateOf(0) }
                HeroScreen(HeroState(), selectedTab = tab, onTab = { tab = it })
            }
        }
    }
}
