package com.littlesquares.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.littlesquares.app.data.AppData
import com.littlesquares.app.data.Habit
import com.littlesquares.app.data.Pastel
import com.littlesquares.app.data.Repo
import com.littlesquares.app.data.isDone
import com.littlesquares.app.data.streak
import com.littlesquares.app.data.streakText
import com.littlesquares.app.data.weekLetters
import com.littlesquares.app.data.weekStart
import com.littlesquares.app.ui.components.CheckButton
import com.littlesquares.app.ui.components.DayCell
import com.littlesquares.app.ui.components.EmojiTile
import com.littlesquares.app.ui.components.HabitStrip
import com.littlesquares.app.ui.components.LsIcon
import com.littlesquares.app.ui.components.PillButton
import com.littlesquares.app.ui.components.Screen
import com.littlesquares.app.ui.components.rememberToday
import com.littlesquares.app.ui.theme.LocalLs
import com.littlesquares.app.ui.theme.LsColors
import com.littlesquares.app.ui.theme.mix
import com.littlesquares.app.ui.theme.ts
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Combined-intensity shades: lavender mixed into Surface, 0–4 levels. */
val HEAT_MIX = listOf(12, 30, 52, 76, 100)

/** The visible month as slots (null = padding before/after), always whole weeks. */
fun monthSlots(month: YearMonth, mondayFirst: Boolean): List<Long?> {
    val first = month.atDay(1)
    val lead = (first.toEpochDay() - first.weekStart(mondayFirst).toEpochDay()).toInt()
    val total = ((lead + month.lengthOfMonth() + 6) / 7) * 7
    return (0 until total).map { i ->
        val d = i - lead
        if (d in 0 until month.lengthOfMonth()) first.toEpochDay() + d else null
    }
}

@Composable
fun TodayScreen(
    data: AppData,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val ls = LocalLs.current
    val today = rememberToday()
    val habits = data.active
    val n = habits.count { it.isDone(today) }
    val count = when {
        habits.isEmpty() -> null
        n == habits.size -> "All done"
        else -> "$n of ${habits.size}"
    }
    val dateText = LocalDate.ofEpochDay(today).format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
    val monthView = data.settings.homeMonth

    Screen(tab = true) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (count != null) "$dateText · $count" else dateText,
                    style = ts(14, 700, ls.ink2),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Today", style = ts(34, 800, ls.ink, lh = 1.1f, ls = -.6f))
            }
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ls.surface2)
                    .clickable(onClickLabel = "New habit", onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) { LsIcon(Icons.Rounded.Add, ls.ink, 26.dp) }
        }

        if (habits.isEmpty()) {
            EmptyHome(Modifier.weight(1f), onAdd)
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item(key = "summary") {
                    SummaryCard(habits, today, data.settings.weekStartsMonday, monthView, Modifier.animateItem())
                }
                items(habits, key = { it.id }) { h ->
                    HabitCard(h, today, monthView, data.settings.weekStartsMonday, onOpen = { onOpen(h.id) }, modifier = Modifier.animateItem())
                }
            }
        }
    }
}

/** One combined day square: shade by how many habits were done, outlined if future, ringed if today. */
@Composable
private fun HeatCell(
    ls: LsColors,
    habits: List<Habit>,
    day: Long,
    today: Long,
    label: String,
    height: Dp,
    labelWeight: Int,
    modifier: Modifier,
) {
    val lav = Pastel.LAVENDER
    if (day > today) {
        DayCell(
            Color.Transparent, modifier.height(height), 10.dp, ring = ls.line, ringWidth = 1.5.dp,
            label = label, labelColor = ls.ink3, labelSize = 12, labelWeight = labelWeight,
        )
        return
    }
    val lv = (habits.count { it.isDone(day) } * 4f / habits.size).roundToInt()
    DayCell(
        mix(ls.pastel(lav), ls.surface, HEAT_MIX[lv] / 100f),
        modifier.height(height),
        10.dp,
        ring = if (day == today) ls.todayRing(lav) else null,
        label = label,
        labelColor = if (lv >= 3) ls.onPastel else ls.ink2,
        labelSize = 12,
        labelWeight = labelWeight,
    )
}

/** "This week" / month summary with a Week ↔ Month switch that also changes the habit cards. */
@Composable
private fun SummaryCard(habits: List<Habit>, today: Long, mondayFirst: Boolean, monthView: Boolean, modifier: Modifier) {
    val ls = LocalLs.current
    val date = LocalDate.ofEpochDay(today)
    val weekDays = (0L until 7L).map { date.weekStart(mondayFirst).plusDays(it).toEpochDay() }
    val slots = monthSlots(YearMonth.from(date), mondayFirst)
    val shown = if (monthView) slots.filterNotNull() else weekDays
    val past = shown.filter { it <= today }
    val done = habits.sumOf { h -> past.count { h.isDone(it) } }
    val letters = weekLetters(mondayFirst)

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(ls.surface)
            .clickable(onClickLabel = if (monthView) "Show week" else "Show month") {
                Repo.updateSettings { it.copy(homeMonth = !it.homeMonth) }
            }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(if (monthView) date.format(DateTimeFormatter.ofPattern("MMMM")) else "This week", style = ts(17, 800, ls.ink))
                Text("$done of ${past.size * habits.size} check-ins", style = ts(13, 600, ls.ink2))
            }
            Row(
                Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ls.surface2)
                    .padding(start = 12.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(if (monthView) "Week" else "Month", style = ts(12, 800, ls.ink2))
                LsIcon(if (monthView) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, ls.ink2, 18.dp)
            }
        }
        AnimatedContent(monthView, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) }, label = "summary") { month ->
            if (!month) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    weekDays.forEachIndexed { i, d ->
                        HeatCell(ls, habits, d, today, letters[i], 28.dp, 800, Modifier.weight(1f))
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        letters.forEach { Text(it, style = ts(11, 800, ls.ink3), textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
                    }
                    slots.chunked(7).forEach { week ->
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            week.forEach { d ->
                                if (d == null) Spacer(Modifier.weight(1f).height(30.dp))
                                else HeatCell(ls, habits, d, today, "${LocalDate.ofEpochDay(d).dayOfMonth}", 30.dp, 700, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabitCard(
    h: Habit,
    today: Long,
    monthView: Boolean,
    mondayFirst: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ls = LocalLs.current
    val done = h.isDone(today)
    val cardBg by animateColorAsState(if (done) ls.tint(h.color, 18) else ls.surface, tween(350), label = "card")
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cardBg)
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            EmojiTile(h.emoji, ls.tint(h.color, if (done) 45 else 32), 42.dp, 15.dp, 23)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(h.name, style = ts(17, 700, ls.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(streakText(h.streak(today)), style = ts(13, 600, ls.ink2))
            }
            CheckButton(done, h.color, 46.dp, 28.dp, onClick = { Repo.toggle(h.id, today) })
        }
        if (!monthView) {
            HabitStrip(h, today, height = 22.dp, radius = 8.dp)
        } else {
            val slots = monthSlots(YearMonth.from(LocalDate.ofEpochDay(today)), mondayFirst)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                slots.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { d ->
                            val m = Modifier.weight(1f).height(14.dp)
                            when {
                                d == null -> Spacer(m)
                                d > today -> DayCell(Color.Transparent, m, 5.dp, ring = ls.line, ringWidth = 1.5.dp)
                                else -> DayCell(
                                    if (h.isDone(d)) ls.pastel(h.color) else ls.tint(h.color),
                                    m,
                                    5.dp,
                                    ring = if (d == today) ls.todayRing(h.color) else null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHome(modifier: Modifier, onAdd: () -> Unit) {
    val ls = LocalLs.current
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    repeat(7) {
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .border(1.5.dp, ls.line, RoundedCornerShape(10.dp)),
                        )
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nothing here yet", style = ts(24, 800, ls.ink, ls = -.3f))
            Text("Add a habit you'd like to keep. One is plenty to start.", style = ts(16, 400, ls.ink2, lh = 1.5f))
        }
        PillButton("New habit", onClick = onAdd, icon = Icons.Rounded.Add)
    }
}
