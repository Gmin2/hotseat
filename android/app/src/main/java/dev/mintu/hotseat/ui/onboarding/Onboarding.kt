package dev.mintu.hotseat.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.mintu.hotseat.data.Profile
import dev.mintu.hotseat.ui.art.OnboardingArt
import dev.mintu.hotseat.ui.art.SvgArt
import dev.mintu.hotseat.ui.art.SvgArtwork
import dev.mintu.hotseat.ui.brand.IdleMascot
import dev.mintu.hotseat.ui.components.Mood
import dev.mintu.hotseat.ui.components.SkyBackdrop
import dev.mintu.hotseat.ui.practice.PrimaryButton
import dev.mintu.hotseat.ui.theme.Dimens
import dev.mintu.hotseat.ui.theme.HotseatTheme
import dev.mintu.hotseat.ui.theme.loop
import kotlinx.coroutines.launch

private data class Page(val art: SvgArt, val kicker: String, val headline: String, val body: String)

private val pages = listOf(
    Page(
        OnboardingArt.Welcome,
        "Welcome to Hotseat",
        "Practice interviews out loud.",
        "A voice interviewer asks, listens and follows up, like a real one would.",
    ),
    Page(
        OnboardingArt.Listen,
        "Talk it through",
        "It listens, then digs deeper.",
        "Answer in your own words. The next question comes from what you actually said.",
    ),
    Page(
        OnboardingArt.Report,
        "After every interview",
        "See what landed and what to fix.",
        "A score, a STAR breakdown and a stronger way to say each answer.",
    ),
    Page(
        OnboardingArt.Ready,
        "Before you start",
        "Tell your interviewer who you are.",
        "",
    ),
)

/** First run: three pages on what Hotseat does, then name, target role and the microphone. */
@Composable
fun Onboarding(profile: Profile, onDone: (name: String, role: String) -> Unit) {
    val t = HotseatTheme.type
    val p = HotseatTheme.palette
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { pages.size }
    val last = pages.lastIndex

    var name by remember { mutableStateOf(profile.name) }
    var role by remember { mutableStateOf(profile.role) }
    var mic by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var micAsked by remember { mutableStateOf(false) }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        mic = it
        micAsked = true
    }

    BackHandler(enabled = pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
    }

    Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, indication = null) {}) {
        SkyBackdrop(Mood.Morning, 0f)

        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = Dimens.chipInset, end = Dimens.gutter, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IdleMascot(40.dp)
                BasicText("hotseat", Modifier.padding(start = 8.dp), style = t.meta.copy(color = p.text))
                Spacer(Modifier.weight(1f))
                if (pager.currentPage < last) {
                    BasicText(
                        "Skip",
                        Modifier
                            .clip(CircleShape)
                            .clickable { scope.launch { pager.animateScrollToPage(last) } }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        style = t.meta.copy(color = p.meta),
                    )
                }
            }

            HorizontalPager(pager, Modifier.weight(1f)) { index ->
                val page = pages[index]
                val settled = pager.settledPage == index
                val reveal = remember { Animatable(0f) }
                LaunchedEffect(settled) {
                    if (settled && reveal.value < 1f) reveal.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
                }
                val float = loop(2600, "onboarding float", reverse = true, easing = FastOutSlowInEasing, still = 0.5f)

                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    SvgArtwork(
                        page.art,
                        Modifier
                            .padding(horizontal = 36.dp)
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .aspectRatio(if (index == last) 1.5f else 1.1f)
                            .semantics { contentDescription = page.headline },
                        reveal = reveal.value,
                        lift = (float - 0.5f) * 10f,
                    )
                    val textIn = ((reveal.value - 0.35f) / 0.4f).coerceIn(0f, 1f)
                    Column(
                        Modifier
                            .padding(horizontal = Dimens.gutter)
                            .graphicsLayer {
                                alpha = textIn
                                translationY = (1f - textIn) * 18.dp.toPx()
                            },
                    ) {
                        BasicText(page.kicker.uppercase(), style = t.tabLabel.copy(color = p.text.copy(alpha = 0.55f)))
                        BasicText(page.headline, Modifier.padding(top = 6.dp), style = t.headline.copy(color = p.text))
                        if (page.body.isNotEmpty()) {
                            BasicText(page.body, Modifier.padding(top = 10.dp), style = t.meta.copy(color = p.meta, lineHeight = t.meta.lineHeight * 1.2f))
                        }
                        if (index == last) {
                            Spacer(Modifier.height(14.dp))
                            Field("Name", name, "What should the interviewer call you") { name = it.take(40) }
                            Spacer(Modifier.height(10.dp))
                            Field("Target role", role, "Android engineer") { role = it.take(60) }
                            Spacer(Modifier.height(10.dp))
                            MicRow(mic, micAsked) { askMic.launch(Manifest.permission.RECORD_AUDIO) }
                            BasicText(
                                "Interviews stay on this phone. Your voice goes to OpenAI only while an interview runs.",
                                Modifier.padding(top = 12.dp, bottom = 8.dp),
                                style = t.caption.copy(color = p.meta),
                            )
                        }
                    }
                }
            }

            Column(Modifier.padding(horizontal = Dimens.gutter).padding(top = 8.dp, bottom = 16.dp)) {
                Steps(pages.size, pager.currentPage, Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp))
                PrimaryButton(if (pager.currentPage == last) "Start practicing" else "Continue") {
                    if (pager.currentPage == last) onDone(name.trim(), role.trim().ifEmpty { "Android engineer" })
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }
            }
        }
    }
}

@Composable
private fun Steps(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val active = i == current
            val width by animateDpAsState(if (active) 22.dp else 7.dp, spring(dampingRatio = 0.7f, stiffness = 420f), label = "step width")
            val color by animateColorAsState(if (active) Color(0xFF141210) else Color(0x33141210), tween(200), label = "step colour")
            Box(Modifier.width(width).height(7.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun Field(label: String, value: String, hint: String, onChange: (String) -> Unit) {
    val t = HotseatTheme.type
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Color(0x14000000), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        BasicText(label, style = t.caption.copy(color = Color(0xFF848484)))
        Box(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            if (value.isEmpty()) BasicText(hint, style = t.meta.copy(color = Color(0xFFB0B0B8)))
            BasicTextField(value, onChange, Modifier.fillMaxWidth(), textStyle = t.meta.copy(color = Color.Black), singleLine = true)
        }
    }
}

@Composable
private fun MicRow(granted: Boolean, asked: Boolean, onAsk: () -> Unit) {
    val t = HotseatTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Color(0x14000000), RoundedCornerShape(18.dp))
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText("Microphone", style = t.meta.copy(color = Color.Black))
            BasicText(
                when {
                    granted -> "Ready, the interviewer can hear you"
                    asked -> "Not allowed yet, you can allow it before your first interview"
                    else -> "Needed so the interviewer can hear you"
                },
                style = t.caption.copy(color = if (granted) Color(0xFF1F9D55) else Color(0xFF848484)),
            )
        }
        Box(
            Modifier
                .clip(CircleShape)
                .background(if (granted) Color(0xFFE8F6EE) else Color(0xFF141210))
                .clickable(enabled = !granted, onClick = onAsk)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            BasicText(if (granted) "Allowed" else "Allow", style = t.pill.copy(color = if (granted) Color(0xFF1F9D55) else Color.White, fontSize = t.pill.fontSize * 0.9f))
        }
    }
}
