package dev.mintu.hotseat.ui.brand

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import dev.mintu.hotseat.live.Speaker
import dev.mintu.hotseat.live.Turn
import dev.mintu.hotseat.ui.practice.ChatBubbles
import dev.mintu.hotseat.ui.practice.Typing
import dev.mintu.hotseat.ui.practice.asksSomething
import dev.mintu.hotseat.ui.theme.HotseatTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w408dp-h907dp-xxhdpi")
class MascotTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun poses() {
        val poses = listOf(
            MascotPose(open = 0f),
            MascotPose(open = 0.35f),
            MascotPose(),
            MascotPose(look = -8f),
            MascotPose(wing = 1f, lift = 28f),
            MascotPose(mic = 1f),
        )
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Column(Modifier.fillMaxSize().background(Color(0xFF5A8EE4)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                poses.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { Mascot(120.dp, it, sticker = true) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mascot(34.dp, MascotPose(), tight = true)
                    Mascot(34.dp, MascotPose())
                }
            }
        }
        rule.mainClock.advanceTimeBy(500)
        rule.onRoot().captureRoboImage("build/mascot/poses.png")
    }

    @Test
    fun chat() {
        val turns = listOf(
            Turn(Speaker.interviewer, "Hi, thanks for joining. To start, tell me a little about yourself.", 0, 3000),
            Turn(Speaker.candidate, "I am a mobile engineer, I built the offline sync in a Flutter wallet.", 4000, 9000),
            Turn(Speaker.interviewer, "What was the hardest part of that sync?", 10000, 12000),
        )
        rule.mainClock.autoAdvance = false
        rule.setContent {
            HotseatTheme {
                Box(Modifier.fillMaxSize().background(Color(0xFF6793DF))) {
                    ChatBubbles(turns, Typing.Candidate, 0.6f, Modifier.fillMaxSize())
                }
            }
        }
        rule.mainClock.advanceTimeBy(1500)
        rule.onRoot().captureRoboImage("build/mascot/chat.png")
    }

    @Test
    fun spotsQuestionsWithoutAQuestionMark() {
        assertTrue(asksSomething("Walk me through a time you disagreed with a product decision."))
        assertTrue(asksSomething("And the hardest part?"))
        assertTrue(asksSomething("Tell me about yourself"))
        assertFalse(asksSomething("Great, that is all from me. Your report is ready."))
        assertFalse(asksSomething("Hmm."))
        assertEquals(2, listOf("Hi, tell me about you.", "Nice.", "How did you test it").count(::asksSomething))
    }
}
