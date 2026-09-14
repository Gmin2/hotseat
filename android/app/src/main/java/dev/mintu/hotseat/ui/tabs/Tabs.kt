package dev.mintu.hotseat.ui.tabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.mintu.hotseat.R
import dev.mintu.hotseat.data.Mock
import dev.mintu.hotseat.data.Profile
import dev.mintu.hotseat.data.Saved
import dev.mintu.hotseat.data.SavedSession
import dev.mintu.hotseat.data.SessionRowData
import dev.mintu.hotseat.data.progress
import dev.mintu.hotseat.data.row
import dev.mintu.hotseat.data.sampleRows
import dev.mintu.hotseat.ui.art.IsoScene
import dev.mintu.hotseat.ui.art.IsoScenes
import dev.mintu.hotseat.ui.art.TabScene
import dev.mintu.hotseat.ui.components.Hairline
import dev.mintu.hotseat.ui.components.RollingText
import dev.mintu.hotseat.ui.components.Segmented
import dev.mintu.hotseat.ui.components.Sheet
import dev.mintu.hotseat.ui.components.StatusChip
import dev.mintu.hotseat.ui.components.ChipState
import dev.mintu.hotseat.ui.components.dots
import dev.mintu.hotseat.ui.icons.InkIcon
import dev.mintu.hotseat.ui.icons.InkIcons
import dev.mintu.hotseat.ui.icons.InkMotion
import dev.mintu.hotseat.ui.practice.ScorePill
import dev.mintu.hotseat.ui.theme.Dimens
import dev.mintu.hotseat.ui.theme.HotseatTheme
import kotlinx.coroutines.delay

/** Shared layout for the non practice tabs: title over the sky, a white card that holds the content. */
@Composable
private fun TabPage(kicker: String, title: String, number: String, caption: String, art: IsoScene, content: @Composable ColumnScope.() -> Unit) {
    val p = HotseatTheme.palette
    val t = HotseatTheme.type
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(top = 12.dp),
    ) {
        StatusChip(ChipState.Ready, Modifier.padding(start = Dimens.chipInset))
        BasicText(kicker.uppercase(), Modifier.padding(start = Dimens.gutter, top = 24.dp), style = t.tabLabel.copy(color = p.text.copy(alpha = 0.55f)))
        BasicText(title, Modifier.padding(start = Dimens.gutter, end = Dimens.gutter, top = 6.dp), style = t.headline.copy(color = p.text))
        TabScene(art, Modifier.padding(horizontal = 56.dp, vertical = 12.dp).fillMaxWidth())
        RollingText(number, t.display.copy(color = p.display), Modifier.padding(start = Dimens.gutter))
        BasicText(caption, Modifier.padding(start = Dimens.gutter, top = 2.dp), style = t.meta.copy(color = p.meta))
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            content = content,
        )
        Spacer(Modifier.height(Dimens.tabBarBottom + Dimens.tabBarHeight + 24.dp))
    }
}

@Composable
fun SessionsTab(saved: Saved, onOpenSaved: (SavedSession) -> Unit, onOpenSample: (Int) -> Unit) {
    val real = saved.sessions
    val minutes = (real.sumOf { it.seconds } / 60).toInt()
    TabPage(
        kicker = "Sessions",
        title = if (real.isEmpty()) "Your interviews land here." else "Every run, replayable.",
        number = real.size.toString(),
        caption = if (real.isEmpty()) "no interviews yet" else dots(if (real.size == 1) "interview" else "interviews", "$minutes min"),
        art = IsoScenes.Sessions,
    ) {
        if (real.isEmpty()) {
            Header("Samples to look around")
            Mock.sampleRows().forEachIndexed { i, row ->
                if (i > 0) Hairline()
                SessionRow(row, i) { onOpenSample(Mock.rounds.indexOfFirst { it.title == row.title }.coerceAtLeast(0)) }
            }
        } else {
            real.forEachIndexed { i, session ->
                if (i > 0) Hairline()
                SessionRow(session.row(saved.profile.role), i) { onOpenSaved(session) }
            }
        }
    }
}

@Composable
private fun SessionRow(s: SessionRowData, index: Int, onClick: () -> Unit) {
    val t = HotseatTheme.type
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, spring(dampingRatio = 0.6f, stiffness = 600f), label = "row")
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 45L)
        enter.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 300f))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = enter.value
                translationY = (1f - enter.value) * 24.dp.toPx()
                scaleX = scale
                scaleY = scale
            }
            .clickable(press, indication = null, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(52.dp)) {
            BasicText(s.day.uppercase(), style = t.tabLabel.copy(color = Color(0xFF848484)))
            BasicText(s.date, style = t.caption.copy(color = Color(0xFF1C1C1C)))
        }
        Column(Modifier.weight(1f)) {
            BasicText(s.title, style = t.meta.copy(color = Color(0xFF000000)))
            BasicText(s.subtitle, style = t.caption.copy(color = Color(0xFF848484)), maxLines = 1)
        }
        MiniTicks(s.trend, Modifier.padding(horizontal = 10.dp))
        ScorePill(s.score)
    }
}

@Composable
private fun MiniTicks(values: List<Int>, modifier: Modifier = Modifier) {
    Canvas(modifier.size(46.dp, 22.dp)) {
        val pitch = size.width / maxOf(1, values.size)
        values.forEachIndexed { i, v ->
            val h = size.height * (0.25f + 0.75f * (v - 40) / 60f).coerceIn(0.2f, 1f)
            val x = i * pitch + pitch / 2
            val last = i == values.lastIndex
            drawLine(if (last) Color(0xFFE03143) else Color(0xFF9A9A9A), Offset(x, size.height - h), Offset(x, size.height), 2.dp.toPx())
        }
    }
}

@Composable
fun ProgressTab(saved: Saved) {
    val t = HotseatTheme.type
    val data = progress(saved)
    val latest = data.scores.lastOrNull() ?: 0
    val change = latest - (data.scores.firstOrNull() ?: 0)
    TabPage(
        kicker = if (data.sample) "Progress · sample" else "Progress",
        title = when {
            data.sample -> "Here is what progress will look like."
            change > 0 -> "You are getting sharper."
            data.scores.size == 1 -> "Your first score is in."
            else -> "Keep going, it adds up."
        },
        number = latest.toString(),
        caption = dots("latest score", if (data.scores.size > 1) "${if (change >= 0) "up" else "down"} ${kotlin.math.abs(change)}" else "one interview"),
        art = IsoScenes.Progress,
    ) {
        Header(if (data.scores.size > 1) "Last ${data.scores.size} interviews" else "Scores")
        ScoreTicks(data.scores)
        Hairline(Modifier.padding(top = 12.dp))
        Header(if (data.sample) "Skills" else "STAR, across your answers")
        data.skills.forEach { SkillRow(it.name, it.score) }
        Hairline(Modifier.padding(top = 12.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            var pulse by remember { mutableIntStateOf(0) }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFFF1E6)).clickable { pulse++ },
                contentAlignment = Alignment.Center,
            ) { InkIcon(InkIcons.Streak, 20.dp, Color(0xFFE08A1E), motion = InkMotion.Wiggle, pulse = pulse) }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                BasicText(if (data.streakDays == 1) "1 day streak" else "${data.streakDays} day streak", style = t.meta.copy(color = Color.Black))
                BasicText(if (data.streakDays == 0) "Do an interview today to start one" else "Practice today to keep it going", style = t.caption.copy(color = Color(0xFF848484)))
            }
            BasicText("${data.minutes} min", style = t.meta.copy(color = Color(0xFF3E3E3E)))
        }
    }
}

@Composable
private fun Header(text: String) {
    BasicText(text.uppercase(), Modifier.padding(top = 16.dp, bottom = 10.dp), style = HotseatTheme.type.tabLabel.copy(color = Color(0xFF848484)))
}

@Composable
private fun ScoreTicks(scores: List<Int>) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(900)) }
    var picked by remember(scores.size) { mutableIntStateOf(scores.lastIndex) }
    Column {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            val pitch = size.width / scores.size
            scores.forEachIndexed { i, s ->
                val h = size.height * ((s - 40) / 60f).coerceIn(0.05f, 1f) * grow.value
                val x = i * pitch + pitch / 2
                val color = if (i == picked) Color(0xFFE03143) else Color(0xFF292929)
                drawLine(color.copy(alpha = if (i == picked) 1f else 0.85f), Offset(x, size.height), Offset(x, size.height - h), 6.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            scores.forEachIndexed { i, s ->
                Box(
                    Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, indication = null) { picked = i },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(s.toString(), style = HotseatTheme.type.tabLabel.copy(color = if (i == picked) Color(0xFFE03143) else Color(0xFF9A9A9A)))
                }
            }
        }
    }
}

@Composable
private fun SkillRow(name: String, score: Int) {
    val t = HotseatTheme.type
    val fill = remember { Animatable(0f) }
    LaunchedEffect(score) { fill.animateTo(score / 100f, tween(800)) }
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(name, Modifier.width(96.dp), style = t.meta.copy(color = Color.Black))
        Canvas(Modifier.weight(1f).height(18.dp)) {
            val count = 28
            val pitch = size.width / count
            for (i in 0 until count) {
                val on = i < (fill.value * count).toInt()
                val x = i * pitch + pitch / 2
                val h = if (on) size.height else size.height * 0.5f
                drawLine(if (on) Color(0xFF292929) else Color(0xFF9A9A9A).copy(alpha = 0.5f), Offset(x, (size.height - h) / 2), Offset(x, (size.height + h) / 2), 1.6.dp.toPx())
            }
        }
        BasicText(score.toString(), Modifier.width(36.dp).padding(start = 10.dp), style = t.meta.copy(color = Color(0xFF3E3E3E)))
    }
}

@Composable
fun YouTab(saved: Saved, onProfile: ((Profile) -> Profile) -> Unit, onDeleteAll: () -> Unit) {
    val t = HotseatTheme.type
    val profile = saved.profile
    val lengths = listOf(10, 15, 20)
    TabPage(
        kicker = "You",
        title = listOf(profile.name.trim(), profile.role.trim()).filter { it.isNotEmpty() }.joinToString(", ").ifEmpty { "Your interviews, your way." } + ".",
        number = progress(saved).let { if (it.sample) "0" else it.questions.toString() },
        caption = "questions answered",
        art = IsoScenes.You,
    ) {
        Header("Profile")
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
    }
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
