package com.littlesquares.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlesquares.app.data.Habit
import com.littlesquares.app.data.Pastel
import com.littlesquares.app.data.Repo
import com.littlesquares.app.data.letter
import com.littlesquares.app.data.todayEpoch
import com.littlesquares.app.ui.components.DayCell
import com.littlesquares.app.ui.components.EmojiPickerDialog
import com.littlesquares.app.ui.components.FieldLabel
import com.littlesquares.app.ui.components.NameField
import com.littlesquares.app.ui.components.PillButton
import com.littlesquares.app.ui.components.Screen
import com.littlesquares.app.ui.components.Swatches
import com.littlesquares.app.ui.theme.LocalLs
import com.littlesquares.app.ui.theme.ts
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.sin

/** Same pseudo-random hash the design uses, so the hero grid looks identical. */
fun designHash(a: Int, b: Int): Double {
    val x = sin(a * 127.1 + b * 311.7) * 43758.5453
    return x - floor(x)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    val finish = {
        Repo.updateSettings { it.copy(onboarded = true) }
        onDone()
    }
    val goTo = { page: Int -> scope.launch { pager.animateScrollToPage(page) }; Unit }
    Screen {
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            when (page) {
                0 -> WelcomePage(onNext = { goTo(1) }, onSkip = { goTo(2) })
                1 -> GridPage(onNext = { goTo(2) }, onSkip = { goTo(2) })
                else -> FirstHabitPage(
                    onAdd = { Repo.upsert(it); finish() },
                    onLater = finish,
                )
            }
        }
    }
}

@Composable
private fun Dots(active: Int) {
    val ls = LocalLs.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            val w by animateDpAsState(if (i == active) 22.dp else 8.dp, label = "dot")
            Box(
                Modifier
                    .size(w, 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (i == active) ls.ink else ls.line),
            )
        }
    }
}

@Composable
private fun IntroFrame(
    page: Int,
    button: String,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    hero: @Composable () -> Unit,
    text: @Composable () -> Unit,
) {
    val ls = LocalLs.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 28.dp, end = 28.dp, top = 8.dp, bottom = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                "Skip",
                style = ts(15, 700, ls.ink2),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onSkip)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { hero() }
        text()
        Spacer(Modifier.height(32.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dots(page)
            PillButton(
                button,
                onClick = onNext,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                padding = androidx.compose.foundation.layout.PaddingValues(start = 26.dp, end = 22.dp),
            )
        }
    }
}

@Composable
private fun WelcomePage(onNext: () -> Unit, onSkip: () -> Unit) {
    val ls = LocalLs.current
    IntroFrame(
        page = 0, button = "Get started", onNext = onNext, onSkip = onSkip,
        hero = { HeroGrid() },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("WELCOME TO LITTLE SQUARES", style = ts(13, 800, ls.ink2, ls = 1.4f))
                Text("Small days, quietly kept.", style = ts(32, 800, ls.ink, lh = 1.15f, ls = -.5f))
                Text(
                    "Tick off a few habits each day and watch your squares fill in. No points, no pressure.",
                    style = ts(16, 400, ls.ink2, lh = 1.5f),
                )
            }
        },
    )
}

@Composable
private fun HeroGrid() {
    val ls = LocalLs.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        repeat(7) { r ->
            val p = Pastel.entries[r]
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(9) { c ->
                    val filled = designHash(r + 3, c + 5) > .42
                    val color by animateColorAsState(
                        if (shown && filled) ls.pastel(p) else ls.tint(p, 30),
                        tween(420, delayMillis = (r * 9 + c) * 14),
                        label = "hero",
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color),
                    )
                }
            }
        }
    }
}

@Composable
private fun GridPage(onNext: () -> Unit, onSkip: () -> Unit) {
    val ls = LocalLs.current
    val lav = Pastel.LAVENDER
    var todayDone by remember { mutableStateOf(false) }
    val today = todayEpoch()
    IntroFrame(
        page = 1, button = "Next", onNext = onNext, onSkip = onSkip,
        hero = {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(ls.surface)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ls.tint(lav, 32)),
                            contentAlignment = Alignment.Center,
                        ) { Text("📖", fontSize = 20.sp) }
                        Text("Read 10 pages", style = ts(16, 700, ls.ink))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (off in 6 downTo 0) {
                            val d = off > 0 || todayDone
                            DayCell(
                                bg = if (d) ls.pastel(lav) else ls.tint(lav),
                                modifier = Modifier.weight(1f).height(34.dp),
                                radius = 11.dp,
                                ring = if (off == 0) ls.todayRing(lav) else null,
                                label = (today - off).letter(),
                                labelSize = 12,
                                labelColor = if (d) ls.onPastel else ls.ink2,
                                onClick = if (off == 0) ({ todayDone = !todayDone }) else null,
                            )
                        }
                    }
                }
                Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Legend(ls.tint(lav), null, "Faint — not done yet")
                    Legend(ls.pastel(lav), null, "Filled — done that day")
                    Legend(ls.tint(lav), ls.todayRing(lav), "Outlined — today")
                }
            }
        },
        text = {
            Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Every square is a day.", style = ts(32, 800, ls.ink, lh = 1.15f, ls = -.5f))
                Text(
                    "Tap a habit to fill in today. Forgot yesterday? You can fill in the last three days, too.",
                    style = ts(16, 400, ls.ink2, lh = 1.5f),
                )
            }
        },
    )
}

@Composable
private fun Legend(bg: androidx.compose.ui.graphics.Color, ring: androidx.compose.ui.graphics.Color?, text: String) {
    val ls = LocalLs.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(bg)
                .then(if (ring != null) Modifier.border(2.dp, ring, RoundedCornerShape(9.dp)) else Modifier),
        )
        Text(text, style = ts(15, 600, ls.ink2))
    }
}

private data class Suggestion(val emoji: String, val label: String, val name: String, val color: Pastel)

private val SUGGESTIONS = listOf(
    Suggestion("📖", "Read", "Read 10 pages", Pastel.LAVENDER),
    Suggestion("🌿", "Walk", "Morning walk", Pastel.SAGE),
    Suggestion("🧘", "Stretch", "Stretch", Pastel.MINT),
    Suggestion("✍️", "Journal", "Journal", Pastel.PEACH),
    Suggestion("🌙", "Sleep early", "Sleep early", Pastel.ROSE),
    Suggestion("💊", "Vitamins", "Vitamins", Pastel.BUTTER),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FirstHabitPage(onAdd: (Habit) -> Unit, onLater: () -> Unit) {
    val ls = LocalLs.current
    var name by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable { mutableStateOf("💧") }
    var color by rememberSaveable { mutableStateOf(Pastel.SKY) }
    var picking by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Start with one.", style = ts(32, 800, ls.ink, lh = 1.15f, ls = -.5f))
                Text("What's one small thing you'd like to do every day?", style = ts(16, 400, ls.ink2, lh = 1.5f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("Habit name")
                NameField(
                    value = name,
                    onValue = { name = it },
                    ringColor = ls.pastel(color),
                    placeholder = "Drink water",
                    emoji = emoji,
                    onEmojiClick = { picking = true },
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldLabel("Or pick one")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SUGGESTIONS.forEach { s ->
                        val sel = name == s.name
                        Row(
                            Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (sel) ls.tint(s.color, 40) else androidx.compose.ui.graphics.Color.Transparent)
                                .border(1.5.dp, if (sel) ls.pastel(s.color) else ls.line, RoundedCornerShape(20.dp))
                                .clickable { name = s.name; emoji = s.emoji; color = s.color }
                                .padding(start = 12.dp, end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(s.emoji, fontSize = 15.sp)
                            Text(s.label, style = ts(14, 700, ls.ink))
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FieldLabel("Colour")
                Swatches(color) { color = it }
            }
            Spacer(Modifier.width(1.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PillButton(
                "Add habit",
                icon = Icons.Rounded.Add,
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val today = todayEpoch()
                    onAdd(Habit(id = Repo.newId(), name = name.trim(), emoji = emoji, color = color, createdDay = today))
                },
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onLater),
                contentAlignment = Alignment.Center,
            ) { Text("I'll do this later", style = ts(15, 700, ls.ink2)) }
        }
    }
    if (picking) {
        EmojiPickerDialog(emoji, onPick = { emoji = it; picking = false }, onDismiss = { picking = false })
    }
}
