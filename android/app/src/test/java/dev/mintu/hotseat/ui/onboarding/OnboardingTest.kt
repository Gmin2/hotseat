package dev.mintu.hotseat.ui.onboarding

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import dev.mintu.hotseat.data.Profile
import dev.mintu.hotseat.ui.art.OnboardingArt
import dev.mintu.hotseat.ui.theme.HotseatTheme
import dev.mintu.hotseat.ui.theme.LocalReduceMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
class OnboardingTest {
    @get:Rule
    val rule = createComposeRule()

    private var done: Pair<String, String>? = null

    private fun show() = rule.setContent {
        CompositionLocalProvider(LocalReduceMotion provides true) {
            HotseatTheme { Onboarding(Profile()) { name, role -> done = name to role } }
        }
    }

    @Test
    fun continueThroughEveryPageThenFillInTheProfile() {
        show()
        rule.onNodeWithText("Practice interviews out loud.").assertExists()
        rule.onRoot().captureRoboImage("build/onboarding/page-1.png")
        rule.onNodeWithText("Continue").performClick()
        rule.onNodeWithText("It listens, then digs deeper.").assertExists()
        rule.onNodeWithText("Continue").performClick()
        rule.onNodeWithText("See what landed and what to fix.").assertExists()
        rule.onNodeWithText("Continue").performClick()
        rule.onNodeWithText("Tell your interviewer who you are.").assertExists()
        rule.onNodeWithText("Skip").assertDoesNotExist()
        assertNull(done)

        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("  Riya ")
        fields[1].performTextClearance()
        rule.onRoot().captureRoboImage("build/onboarding/page-4.png")
        rule.onNodeWithText("Start practicing").performClick()
        // blank role falls back to the default, names are trimmed
        assertEquals("Riya" to "Android engineer", done)
    }

    @Test
    fun skipJumpsToTheProfilePage() {
        show()
        rule.onNodeWithText("Skip").performClick()
        rule.onNodeWithText("Start practicing").assertExists()
        rule.onNodeWithText("Start practicing").performClick()
        assertEquals("" to "Android engineer", done)
    }

    @Test
    fun everyIllustrationFlattenedToPlainPaths() {
        listOf(OnboardingArt.Welcome, OnboardingArt.Listen, OnboardingArt.Report, OnboardingArt.Ready).forEach { art ->
            assertTrue(art.shapes.size in 30..160)
            assertTrue(art.shapes.all { it.d.isNotBlank() && (it.fill != null || it.stroke != null) })
            assertTrue(art.shapes.any { it.stroke == 0xFF141210 })
        }
    }
}
