package com.littlesquares.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlesquares.app.data.AppData
import com.littlesquares.app.data.Habit
import com.littlesquares.app.data.Pastel
import com.littlesquares.app.data.Repo
import com.littlesquares.app.data.doneBetween
import com.littlesquares.app.data.isDone
import com.littlesquares.app.data.weekLetters
import com.littlesquares.app.data.weekStart
import com.littlesquares.app.ui.components.DayCell
import com.littlesquares.app.ui.components.IconBtn
import com.littlesquares.app.ui.components.LsIcon
import com.littlesquares.app.ui.components.Screen
import com.littlesquares.app.ui.components.Segmented
import com.littlesquares.app.ui.components.TopBar
import com.littlesquares.app.ui.components.rememberToday
import com.littlesquares.app.ui.theme.LocalLs
import com.littlesquares.app.ui.theme.LsColors
import com.littlesquares.app.ui.theme.mix
import com.littlesquares.app.ui.theme.ts
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private const val WEEK = 0
private const val MONTH = 1
private val MIX = listOf(10, 30, 52, 76, 100)

@Composable
fun CalendarScreen(data: AppData, onBack: () -> Unit, startOnMonth: Boolean = false) {
    val ls = LocalLs.current
    val today = rememberToday()
    val mondayFirst = data.settings.weekStartsMonday
    var mode by rememberSaveable { mutableIntStateOf(if (startOnMonth) MONTH else WEEK) }
    var weekOff by rememberSaveable { mutableIntStateOf(0) }
    var monthOff by rememberSaveable { mutableIntStateOf(0) }
    val offset = if (mode == WEEK) weekOff else monthOff
    val move = { delta: Int ->
        if (mode == WEEK) weekOff = (weekOff + delta).coerceAtMost(0) else monthOff = (monthOff + delta).coerceAtMost(0)
    }

    val todayDate = LocalDate.ofEpochDay(today)
    val weekStart = todayDate.weekStart(mondayFirst).plusWeeks(weekOff.toLong())
    val month = YearMonth.from(todayDate).plusMonths(monthOff.toLong())
    val title = if (mode == WEEK) {
        val end = weekStart.plusDays(6)
        if (weekStart.month == end.month) "${weekStart.dayOfMonth} – ${end.format(DateTimeFormatter.ofPattern("d MMMM"))}"
        else "${weekStart.format(DateTimeFormatter.ofPattern("d MMM"))} – ${end.format(DateTimeFormatter.ofPattern("d MMM"))}"
    } else {
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    Screen {
        TopBar("Calendar", Icons.AutoMirrored.Rounded.ArrowBack, onBack)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Segmented(listOf(WEEK, MONTH), mode, { if (it == WEEK) "Week" else "Month" }, { mode = it }, ls.surface, shadow = true)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconBtn(Icons.Rounded.ChevronLeft, { move(-1) }, size = 44.dp, contentDescription = "Previous")
                Text(title, style = ts(17, 800, ls.ink))
                IconBtn(Icons.Rounded.ChevronRight, { move(1) }, enabled = offset < 0, size = 44.dp, contentDescription = "Next")
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(mode) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = {
                                if (total > 120f) move(-1) else if (total < -120f) move(1)
                            },
                        ) { _, dx -> total += dx }
                    },
            ) {
                AnimatedContent(
                    targetState = mode to offset,
                    transitionSpec = {
                        val dir = if (targetState.second > initialState.second) 1 else -1
                        if (targetState.first != initialState.first) fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                        else (slideInHorizontally(tween(260)) { it / 4 * dir } + fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally(tween(200)) { -it / 4 * dir } + fadeOut(tween(160)))
                    },
                    label = "calendar",
                ) { (m, _) ->
                    if (m == WEEK) WeekView(data, weekStart, today, mondayFirst)
                    else MonthView(data, month, today, mondayFirst)
                }
            }
        }
    }
}

@Composable
private fun EmptyCalendar() {
    val ls = LocalLs.current
    Text("Add a habit to see your calendar.", style = ts(15, 600, ls.ink2), modifier = Modifier.padding(6.dp))
}

@Composable
private fun WeekView(data: AppData, start: LocalDate, today: Long, mondayFirst: Boolean) {
    val ls = LocalLs.current
    val habits = data.active
    val days = (0L until 7L).map { start.plusDays(it).toEpochDay() }
    val letters = weekLetters(mondayFirst)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (habits.isEmpty()) { EmptyCalendar(); return@Column }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(ls.surface)
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(76.dp))
                days.forEachIndexed { i, d ->
                    val isToday = d == today
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(letters[i], style = ts(11, 800, ls.ink3))
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (isToday) ls.primary else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${LocalDate.ofEpochDay(d).dayOfMonth}", style = ts(13, 800, if (isToday) ls.onPrimary else ls.ink2))
                        }
                    }
                }
            }
            habits.forEach { h ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.width(76.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(h.emoji, fontSize = 15.sp)
                        Text(h.name, style = ts(13, 700, ls.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    days.forEach { d -> GridCell(ls, h, d, today, Modifier.weight(1f).height(32.dp), 10.dp, interactive = true) }
                }
            }
        }
        val past = days.filter { it <= today }
        val total = past.size * habits.size
        val done = habits.sumOf { h -> past.count { h.isDone(it) } }
        val summary = if (days.last() < today) "$done of $total check-ins that week" else "$done of $total check-ins so far this week"
        Text(summary, style = ts(14, 700, ls.ink2), modifier = Modifier.padding(horizontal = 6.dp))
        SwipeHint("Swipe to move between weeks")
    }
}

@Composable
private fun SwipeHint(text: String) {
    val ls = LocalLs.current
    Row(
        Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LsIcon(Icons.Rounded.Swipe, ls.ink3, 18.dp)
        Text(text, style = ts(13, 600, ls.ink3))
    }
}

@Composable
private fun GridCell(
    ls: LsColors,
    h: Habit,
    day: Long,
    today: Long,
    modifier: Modifier,
    radius: androidx.compose.ui.unit.Dp,
    interactive: Boolean = false,
) {
    val off = today - day
    if (off < 0) {
        DayCell(Color.Transparent, modifier, radius, ring = ls.line, ringWidth = 1.5.dp)
        return
    }
    val d = h.isDone(day)
    DayCell(
        bg = if (d) ls.pastel(h.color) else ls.tint(h.color),
        modifier = modifier,
        radius = radius,
        ring = if (off == 0L) ls.todayRing(h.color) else null,
        onClick = if (interactive && off in 0..3) ({ Repo.toggle(h.id, day) }) else null,
    )
}

@Composable
private fun MonthView(data: AppData, month: YearMonth, today: Long, mondayFirst: Boolean) {
    val ls = LocalLs.current
    val habits = data.active
    val first = month.atDay(1)
    val lead = (first.toEpochDay() - first.weekStart(mondayFirst).toEpochDay()).toInt()
    val slots = ((lead + month.lengthOfMonth() + 6) / 7) * 7
    val dayAt = { i: Int -> if (i - lead in 0 until month.lengthOfMonth()) first.plusDays((i - lead).toLong()).toEpochDay() else null }
    val letters = weekLetters(mondayFirst)
    val lav = Pastel.LAVENDER

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (habits.isEmpty()) { EmptyCalendar(); return@Column }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(ls.surface)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("All habits", style = ts(14, 800, ls.ink))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Less", style = ts(11, 700, ls.ink3), modifier = Modifier.padding(end = 2.dp))
                    MIX.forEach { p ->
                        Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(mix(ls.pastel(lav), ls.surface, p / 100f)))
                    }
                    Text("More", style = ts(11, 700, ls.ink3), modifier = Modifier.padding(start = 2.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                letters.forEach { Text(it, style = ts(11, 800, ls.ink3), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            }
            for (r in 0 until slots / 7) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (c in 0 until 7) {
                        val day = dayAt(r * 7 + c)
                        val m = Modifier.weight(1f).height(32.dp)
                        when {
                            day == null -> Spacer(m)
                            day > today -> DayCell(
                                Color.Transparent, m, 10.dp, ring = ls.line, ringWidth = 1.5.dp,
                                label = "${LocalDate.ofEpochDay(day).dayOfMonth}", labelColor = ls.ink3, labelSize = 12, labelWeight = 700,
                            )
                            else -> {
                                val lv = (habits.count { it.isDone(day) } * 4f / habits.size).roundToInt()
                                DayCell(
                                    mix(ls.pastel(lav), ls.surface, MIX[lv] / 100f), m, 10.dp,
                                    ring = if (day == today) ls.todayRing(lav) else null,
                                    label = "${LocalDate.ofEpochDay(day).dayOfMonth}",
                                    labelColor = if (lv >= 3) ls.onPastel else ls.ink2, labelSize = 12, labelWeight = 700,
                                )
                            }
                        }
                    }
                }
            }
        }
        Text("By habit", style = ts(14, 800, ls.ink), modifier = Modifier.padding(horizontal = 6.dp))
        val monthStart = first.toEpochDay()
        val monthEnd = minOf(month.atEndOfMonth().toEpochDay(), today)
        val elapsed = (monthEnd - monthStart + 1).coerceAtLeast(1)
        habits.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { h ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(ls.surface)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(h.emoji, fontSize = 14.sp)
                            val pct = if (monthEnd < monthStart) 0 else (h.doneBetween(monthStart, monthEnd) * 100f / elapsed).roundToInt()
                            Text("$pct%", style = ts(11, 800, ls.ink2))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            for (r in 0 until slots / 7) {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    for (c in 0 until 7) {
                                        val day = dayAt(r * 7 + c)
                                        val m = Modifier.weight(1f).aspectRatio(1f)
                                        if (day == null) Spacer(m) else GridCell(ls, h, day, today, m, 3.dp)
                                    }
                                }
                            }
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(8.dp))
        SwipeHint("Swipe to move between months")
    }
}
