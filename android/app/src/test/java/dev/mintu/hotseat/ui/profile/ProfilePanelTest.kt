package dev.mintu.hotseat.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import dev.mintu.hotseat.data.Store
import dev.mintu.hotseat.ui.theme.HotseatTheme
import dev.mintu.hotseat.ui.theme.LocalReduceMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w408dp-h907dp-xxhdpi")
class ProfilePanelTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun opensEditsSavesAndDeleteAsksFirst() {
        val file = File(folder.root, "hotseat.json")
        val store = Store(file)
        var asked by mutableStateOf(false)
        var open by mutableStateOf(false)
        var deleted = false
        rule.setContent {
            val saved by store.saved.collectAsState()
            CompositionLocalProvider(LocalReduceMotion provides true) { HotseatTheme {
                Box(Modifier.fillMaxSize()) {
                    ProfileButton(saved.profile.name, onClick = { open = true })
                    ProfilePanel(open, saved, onClose = { open = false }, onProfile = store::updateProfile, onDeleteAll = { asked = true })
                    DeleteSheet(asked, saved.sessions.size, onCancel = { asked = false }, onDelete = {
                        store.deleteAll()
                        deleted = true
                        asked = false
                    })
                }
            } }
        }
        rule.waitForIdle()

        rule.onNodeWithContentDescription("Profile and settings").performClick()
        rule.waitForIdle()
        assertTrue(open)

        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("Riya")
        fields[1].performTextClearance()
        fields[1].performTextInput("iOS engineer")
        rule.onAllNodesWithText("Hard").onFirst().performScrollTo().performClick()
        rule.waitForIdle()

        assertEquals("Riya", store.profile.name)
        assertEquals("iOS engineer", store.profile.role)
        assertEquals(2, store.profile.difficulty)
        assertEquals("Riya", Store(file).profile.name)
        // the button shows the first letter once there is a name
        rule.onNodeWithText("R").assertExists()

        rule.onAllNodesWithText("Delete all data").onFirst().performScrollTo().performClick()
        rule.waitForIdle()
        assertTrue(asked)
        assertTrue(file.exists())
        rule.onAllNodes(hasText("Keep my data")).onFirst().performClick()
        rule.waitForIdle()
        assertFalse(asked)
        assertTrue(file.exists())

        rule.onAllNodesWithText("Delete all data").onFirst().performScrollTo().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("Delete all data")[1].performClick()
        rule.waitForIdle()
        assertTrue(deleted)
        assertFalse(file.exists())
        assertEquals("", store.profile.name)

        rule.onNodeWithContentDescription("Close profile").performClick()
        rule.waitForIdle()
        assertFalse(open)
    }
}
