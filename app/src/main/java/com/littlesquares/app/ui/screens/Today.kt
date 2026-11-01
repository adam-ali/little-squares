package com.littlesquares.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.littlesquares.app.data.AppData
import com.littlesquares.app.data.Habit
import com.littlesquares.app.data.Repo
import com.littlesquares.app.data.isDone
import com.littlesquares.app.data.streak
import com.littlesquares.app.data.streakText
import com.littlesquares.app.data.doneBetween
import com.littlesquares.app.data.weekLetters
import com.littlesquares.app.data.weekStart
import com.littlesquares.app.ui.components.DayCell
import com.littlesquares.app.ui.theme.mix
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.roundToInt
import com.littlesquares.app.ui.components.CheckButton
import com.littlesquares.app.ui.components.EmojiTile
import com.littlesquares.app.ui.components.HabitStrip
import com.littlesquares.app.ui.components.IconBtn
import com.littlesquares.app.ui.components.LsIcon
import com.littlesquares.app.ui.components.Screen
import com.littlesquares.app.ui.components.rememberToday
import com.littlesquares.app.ui.theme.LocalLs
import com.littlesquares.app.ui.theme.ts
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    data: AppData,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onCalendar: () -> Unit,
    onSettings: () -> Unit,
    onMonth: () -> Unit = onCalendar,
) {
    val ls = LocalLs.current
    val today = rememberToday()
    val habits = data.active
    val dateText = LocalDate.ofEpochDay(today).format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))

    Screen {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(start = 24.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(dateText, style = ts(14, 700, ls.ink2), modifier = Modifier.weight(1f))
                    IconBtn(Icons.Rounded.CalendarMonth, onCalendar, tint = ls.ink2, contentDescription = "Calendar")
                    IconBtn(Icons.Rounded.Settings, onSettings, tint = ls.ink2, contentDescription = "Settings")
                }
                Text("Today", style = ts(34, 800, ls.ink, lh = 1.1f, ls = -.6f), modifier = Modifier.padding(horizontal = 24.dp))

                if (habits.isEmpty()) {
                    EmptyHome(Modifier.weight(1f))
                } else {
                    ProgressRow(habits, today)
                    LazyColumn(
                        Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item(key = "month") {
                            MonthCard(habits, today, data.settings.weekStartsMonday, onMonth, Modifier.animateItem())
                        }
                        items(habits, key = { it.id }) { h ->
                            HabitCard(h, today, onOpen = { onOpen(h.id) }, modifier = Modifier.animateItem())
                        }
                    }
                }
            }
            val fabMod = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 16.dp)
            if (habits.isEmpty()) {
                Row(
                    fabMod
                        .height(60.dp)
                        .shadow(10.dp, RoundedCornerShape(30.dp))
                        .clip(RoundedCornerShape(30.dp))
                        .background(ls.primary)
                        .clickable(onClick = onAdd)
                        .padding(start = 18.dp, end = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LsIcon(Icons.Rounded.Add, ls.onPrimary, 26.dp)
                    Text("New habit", style = ts(16, 800, ls.onPrimary))
                }
            } else {
                Box(
                    fabMod
                        .size(60.dp)
                        .shadow(10.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(ls.primary)
                        .clickable(onClickLabel = "New habit", onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) { LsIcon(Icons.Rounded.Add, ls.onPrimary, 28.dp) }
            }
        }
    }
}

@Composable
private fun ProgressRow(habits: List<Habit>, today: Long) {
    val ls = LocalLs.current
    val n = habits.count { it.isDone(today) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            habits.forEach { h ->
                val bg by animateColorAsState(
                    if (h.isDone(today)) ls.pastel(h.color) else ls.line,
                    tween(300),
                    label = "seg",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(bg),
                )
            }
        }
        Text(if (n == habits.size) "All done" else "$n of ${habits.size}", style = ts(14, 800, ls.ink2))
    }
}

@Composable
fun HabitCard(h: Habit, today: Long, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val ls = LocalLs.current
    val done = h.isDone(today)
    val cardBg by animateColorAsState(if (done) ls.tint(h.color, 18) else ls.surface, tween(350), label = "card")
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cardBg)
            .clickable(onClick = onOpen)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            EmojiTile(h.emoji, ls.tint(h.color, if (done) 45 else 32), 46.dp, 16.dp, 23)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(h.name, style = ts(17, 700, ls.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(streakText(h.streak(today)), style = ts(13, 600, ls.ink2))
            }
            CheckButton(done, h.color, 52.dp, 28.dp, onClick = { Repo.toggle(h.id, today) })
        }
        HabitStrip(h, today, height = 24.dp, radius = 9.dp)
    }
}

@Composable
private fun EmptyHome(modifier: Modifier) {
    val ls = LocalLs.current
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, bottom = 80.dp),
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
        Spacer(Modifier.height(0.dp))
    }
}

private val MONTH_MIX = listOf(10, 30, 52, 76, 100)

/**
 * Compact month at a glance: each day shaded by how many habits were done.
 * No outlines — unchecked days are just a faint tint, future days are blank.
 */
@Composable
private fun MonthCard(habits: List<Habit>, today: Long, mondayFirst: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val ls = LocalLs.current
    val lav = com.littlesquares.app.data.Pastel.LAVENDER
    val date = LocalDate.ofEpochDay(today)
    val month = java.time.YearMonth.from(date)
    val first = month.atDay(1)
    val lead = (first.toEpochDay() - first.weekStart(mondayFirst).toEpochDay()).toInt()
    val rows = (lead + month.lengthOfMonth() + 6) / 7
    val monthStart = first.toEpochDay()
    val checks = habits.sumOf { it.doneBetween(monthStart, today) }
    val pct = if (habits.isEmpty()) 0 else (checks * 100f / (habits.size * (today - monthStart + 1))).roundToInt()
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(ls.surface)
            .clickable(onClickLabel = "Open calendar", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(date.format(DateTimeFormatter.ofPattern("MMMM")), style = ts(15, 800, ls.ink), modifier = Modifier.weight(1f))
            Text("$pct% this month", style = ts(12, 700, ls.ink2))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            weekLetters(mondayFirst).forEach {
                Text(it, style = ts(10, 800, ls.ink3), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        for (r in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in 0 until 7) {
                    val i = r * 7 + c - lead
                    val m = Modifier.weight(1f).height(22.dp)
                    if (i !in 0 until month.lengthOfMonth()) { Spacer(m); continue }
                    val day = monthStart + i
                    if (day > today) {
                        Box(m, contentAlignment = Alignment.Center) { Text("${i + 1}", style = ts(10, 700, ls.ink3)) }
                        continue
                    }
                    val lv = (habits.count { it.isDone(day) } * 4f / habits.size).roundToInt()
                    val isToday = day == today
                    DayCell(
                        bg = mix(ls.pastel(lav), ls.surface, MONTH_MIX[lv] / 100f),
                        modifier = m,
                        radius = 7.dp,
                        label = "${i + 1}",
                        labelSize = 10,
                        labelWeight = if (isToday) 900 else 700,
                        labelColor = if (isToday) ls.ink else if (lv >= 3) ls.onPastel else ls.ink2,
                    )
                }
            }
        }
    }
}
