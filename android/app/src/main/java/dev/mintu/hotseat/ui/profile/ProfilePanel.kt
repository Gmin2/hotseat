package dev.mintu.hotseat.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.mintu.hotseat.data.Mock
import dev.mintu.hotseat.data.Profile
import dev.mintu.hotseat.data.Saved
import dev.mintu.hotseat.data.progress
import dev.mintu.hotseat.ui.brand.IdleMascot
import dev.mintu.hotseat.ui.components.Hairline
import dev.mintu.hotseat.ui.components.Segmented
import dev.mintu.hotseat.ui.components.Sheet
import dev.mintu.hotseat.ui.components.dots
import dev.mintu.hotseat.ui.icons.InkIcon
import dev.mintu.hotseat.ui.icons.InkIcons
import dev.mintu.hotseat.ui.icons.InkMotion
import dev.mintu.hotseat.ui.theme.Dimens
import dev.mintu.hotseat.ui.theme.HotseatTheme

private val Blue = Color(0xFF2F6CE5)

/** Round button in the top right corner, the first letter of your name, opens the profile panel. */
@Composable
fun ProfileButton(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "profile press")
    val initial = name.trim().firstOrNull()?.uppercaseChar()
    Box(
        modifier
            .size(Dimens.chipHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(10.dp, CircleShape, ambientColor = Color(0x223B5BA8), spotColor = Color(0x223B5BA8))
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Color(0x14000000), CircleShape)
            .clickable(press, indication = null, onClick = onClick)
            .semantics { contentDescription = "Profile and settings" },
        contentAlignment = Alignment.Center,
    ) {
        if (initial != null) {
            BasicText(initial.toString(), style = HotseatTheme.type.pill.copy(color = Blue))
        } else {
            InkIcon(InkIcons.You, 22.dp, Blue)
        }
    }
}

/**
 * Slides in from the right over a dim scrim: who you are, how the interviewer behaves, and your data.
 * Tap outside, swipe back or press back to close.
 */
@Composable
fun BoxScope.ProfilePanel(
    visible: Boolean,
    saved: Saved,
    onClose: () -> Unit,
    onProfile: ((Profile) -> Profile) -> Unit,
    onDeleteAll: () -> Unit,
) {
    BackHandler(enabled = visible, onBack = onClose)
    AnimatedVisibility(visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(200))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        )
    }
    BoxWithConstraints(Modifier.align(Alignment.CenterEnd)) {
        val width = minOf(maxWidth * 0.88f, 380.dp)
        AnimatedVisibility(
            visible,
            enter = slideInHorizontally(spring(dampingRatio = 0.86f, stiffness = 380f)) { it },
            exit = slideOutHorizontally(tween(220)) { it },
        ) {
            Column(
                Modifier
                    .width(width)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp))
                    .background(Color.White)
                    .clickable(remember { MutableInteractionSource() }, indication = null) {}
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                PanelContent(saved, onClose, onProfile, onDeleteAll)
            }
        }
    }
}

@Composable
private fun PanelContent(saved: Saved, onClose: () -> Unit, onProfile: ((Profile) -> Profile) -> Unit, onDeleteAll: () -> Unit) {
    val t = HotseatTheme.type
    val profile = saved.profile
    val lengths = listOf(10, 15, 20)
    val stats = progress(saved)
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText("Profile", Modifier.weight(1f), style = t.meta.copy(color = Color(0xFF848484)))
        CloseButton(onClose)
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(Modifier.padding(top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp).clip(CircleShape).background(Color(0xFFEAF1FD)), contentAlignment = Alignment.Center) {
                IdleMascot(46.dp)
            }
            Column(Modifier.padding(start = 14.dp)) {
                BasicText(profile.name.trim().ifEmpty { "You" }, style = t.headline.copy(color = Color.Black, fontSize = t.headline.fontSize * 0.7f, lineHeight = t.headline.lineHeight * 0.7f))
                BasicText(
                    dots(profile.role.trim().ifEmpty { Mock.role }, if (stats.sample) "no interviews yet" else "${saved.sessions.size} interviews"),
                    style = t.caption.copy(color = Color(0xFF848484)),
                )
            }
        }

        Header("About you")
        TextRow("Name", profile.name, "What should the interviewer call you") { v -> onProfile { it.copy(name = v.take(40)) } }
        Hairline()
        TextRow("Target role", profile.role, "Android engineer") { v -> onProfile { it.copy(role = v.take(60)) } }

        Header("Interviewer")
        SettingRow(InkIcons.Interviewer, "Voice", "Marin")
        Segmented(Mock.styles, profile.style, { i -> onProfile { it.copy(style = i) } }, Modifier.padding(vertical = 8.dp))

        Header("Default session")
        Segmented(Mock.difficulties, profile.difficulty, { i -> onProfile { it.copy(difficulty = i) } }, Modifier.padding(bottom = 10.dp))
        Segmented(lengths.map { "$it min" }, lengths.indexOf(profile.minutes).coerceAtLeast(0), { i -> onProfile { it.copy(minutes = lengths[i]) } }, Modifier.padding(bottom = 6.dp))

        Header("Your data")
        BasicText(
            "Interviews and your profile stay on this phone. Voice goes to OpenAI only while an interview runs, Hotseat's server keeps nothing.",
            Modifier.padding(bottom = 4.dp),
            style = t.caption.copy(color = Color(0xFF848484)),
        )
        DangerRow("Delete all data", if (saved.sessions.isEmpty()) "profile and settings" else "${saved.sessions.size} interviews, profile and settings", onDeleteAll)
        Hairline()
        SettingRow(InkIcons.Badge, "About Hotseat", "0.1.0")
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    var pulse by remember { mutableIntStateOf(0) }
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0xFFF1F1F4))
            .clickable {
                pulse++
                onClick()
            }
            .semantics { contentDescription = "Close profile" },
        contentAlignment = Alignment.Center,
    ) {
        InkIcon(InkIcons.Next, 18.dp, Color(0xFF141210), motion = InkMotion.Nudge, pulse = pulse)
    }
}

@Composable
private fun Header(text: String) {
    BasicText(text.uppercase(), Modifier.padding(top = 18.dp, bottom = 6.dp), style = HotseatTheme.type.tabLabel.copy(color = Color(0xFF848484)))
}

@Composable
private fun TextRow(label: String, value: String, hint: String, onChange: (String) -> Unit) {
    val t = HotseatTheme.type
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        BasicText(label, style = t.caption.copy(color = Color(0xFF848484)))
        Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            if (value.isEmpty()) BasicText(hint, style = t.meta.copy(color = Color(0xFFB0B0B8)))
            BasicTextField(value, onChange, Modifier.fillMaxWidth(), textStyle = t.meta.copy(color = Color.Black), singleLine = true)
        }
    }
}

@Composable
private fun DangerRow(label: String, detail: String, onClick: () -> Unit) {
    val t = HotseatTheme.type
    var pulse by remember { mutableIntStateOf(0) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, indication = null) {
                pulse++
                onClick()
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFDECEE)), contentAlignment = Alignment.Center) {
            InkIcon(InkIcons.Trash, 16.dp, Color(0xFFE03143), motion = InkMotion.Wiggle, pulse = pulse)
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            BasicText(label, style = t.meta.copy(color = Color(0xFFE03143)))
            BasicText(detail, style = t.caption.copy(color = Color(0xFF848484)))
        }
    }
}

@Composable
private fun SettingRow(icon: InkIcon, label: String, value: String) {
    val t = HotseatTheme.type
    var pulse by remember { mutableIntStateOf(0) }
    Row(
        Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, indication = null) { pulse++ }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFEFF5FE)), contentAlignment = Alignment.Center) {
            InkIcon(icon, 16.dp, Color(0xFF0A64E4), motion = InkMotion.Wiggle, pulse = pulse)
        }
        BasicText(label, Modifier.padding(start = 12.dp).weight(1f), style = t.meta.copy(color = Color.Black))
        BasicText(value, style = t.meta.copy(color = Color(0xFF848484)))
    }
}

/** Asks before wiping everything, spells out what goes. */
@Composable
fun BoxScope.DeleteSheet(visible: Boolean, interviews: Int, onCancel: () -> Unit, onDelete: () -> Unit) {
    val t = HotseatTheme.type
    Sheet(visible, onDismiss = onCancel, fraction = 1f) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFFDECEE)), contentAlignment = Alignment.Center) {
            InkIcon(InkIcons.Trash, 24.dp, Color(0xFFE03143))
        }
        BasicText("Delete everything?", Modifier.padding(top = 14.dp), style = t.headline.copy(color = Color.Black, fontSize = t.headline.fontSize * 0.8f))
        BasicText(
            "This removes " + (if (interviews > 0) "$interviews saved ${if (interviews == 1) "interview" else "interviews"} with their reports, " else "") +
                "your name, target role and settings from this phone. It cannot be undone.",
            Modifier.padding(top = 8.dp, bottom = 22.dp),
            style = t.meta.copy(color = Color(0xFF3E3E3E)),
        )
        ActionButton("Delete all data", Color(0xFFE03143), Color.White, onDelete)
        Spacer(Modifier.height(10.dp))
        ActionButton("Keep my data", Color(0xFFF1F1F4), Color.Black, onCancel)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ActionButton(text: String, fill: Color, ink: Color, onClick: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "action press")
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(fill)
            .clickable(press, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = HotseatTheme.type.pill.copy(color = ink))
    }
}
