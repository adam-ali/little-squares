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
