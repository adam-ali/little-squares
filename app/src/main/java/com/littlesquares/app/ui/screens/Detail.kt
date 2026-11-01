package com.littlesquares.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlesquares.app.data.AppData
import com.littlesquares.app.data.Habit
import com.littlesquares.app.data.Repo
import com.littlesquares.app.data.bestStreak
import com.littlesquares.app.data.doneBetween
import com.littlesquares.app.data.firstDay
import com.littlesquares.app.data.isDone
import com.littlesquares.app.data.streak
import com.littlesquares.app.data.weekStart
import com.littlesquares.app.ui.components.EmojiTile
import com.littlesquares.app.ui.components.IconBtn
import com.littlesquares.app.ui.components.PillButton
import com.littlesquares.app.ui.components.Screen
import com.littlesquares.app.ui.components.formatTime
import com.littlesquares.app.ui.components.rememberToday
import com.littlesquares.app.ui.theme.LocalLs
import com.littlesquares.app.ui.theme.ts
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DetailScreen(data: AppData, id: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val h = data.habit(id)
    if (h == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val ls = LocalLs.current
    val today = rememberToday()
    val done = h.isDone(today)

    Screen {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBtn(Icons.AutoMirrored.Rounded.ArrowBack, onBack, contentDescription = "Back")
            IconBtn(Icons.Rounded.Edit, onEdit, tint = ls.ink2, contentDescription = "Edit")
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                EmojiTile(h.emoji, ls.tint(h.color, 35), 56.dp, 20.dp, 28)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(h.name, style = ts(24, 800, ls.ink, ls = -.3f))
                    val sub = buildString {
                        append(if (h.archived) "Archived" else "Every day")
                        append(" · ")
                        append(if (h.reminderOn) "Reminder ${formatTime(h.reminderMinutes)}" else "No reminder")
                    }
                    Text(sub, style = ts(13, 600, ls.ink2))
                }
            }
            Stats(h, today)
            YearCard(h, today, data.settings.weekStartsMonday)
        }
        PillButton(
            if (done) "Done today" else "Mark today done",
            icon = if (done) Icons.Rounded.CheckCircle else Icons.Rounded.Check,
            bg = if (done) ls.pastel(h.color) else ls.primary,
            fg = if (done) ls.onPastel else ls.onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
            onClick = { Repo.toggle(h.id, today) },
        )
    }
}

@Composable
private fun Stats(h: Habit, today: Long) {
    val date = LocalDate.ofEpochDay(today)
    val monthStart = date.withDayOfMonth(1).toEpochDay()
    val monthPct = (h.doneBetween(monthStart, today) * 100f / (today - monthStart + 1)).roundToInt()
    val first = h.firstDay()
    val allPct = (h.doneBetween(first, today) * 100f / (today - first + 1).coerceAtLeast(1)).roundToInt()
    val items = listOf(
        Triple(h.streak(today).toString(), "days", "Current streak"),
        Triple(h.bestStreak().toString(), "days", "Best streak"),
        Triple("$monthPct%", "", "This month"),
        Triple("$allPct%", "", "All time"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (v, u, l) -> StatCard(v, u, l, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun StatCard(v: String, unit: String, label: String, modifier: Modifier) {
    val ls = LocalLs.current
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(ls.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            buildAnnotatedString {
                append(v)
                if (unit.isNotEmpty()) withStyle(SpanStyle(fontSize = 14.sp, color = ls.ink2, letterSpacing = 0.sp)) { append(" $unit") }
            },
            style = ts(28, 800, ls.ink, lh = 1.15f, ls = -.5f),
        )
        Text(label, style = ts(13, 700, ls.ink2))
    }
}

@Composable
private fun YearCard(h: Habit, today: Long, mondayFirst: Boolean) {
    val ls = LocalLs.current
    val thisYear = LocalDate.ofEpochDay(today).year
    val firstYear = LocalDate.ofEpochDay(h.firstDay()).year
    var year by remember { mutableIntStateOf(thisYear) }
    val count = h.doneBetween(LocalDate.of(year, 1, 1).toEpochDay(), LocalDate.of(year, 12, 31).toEpochDay())

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(ls.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Icons.Rounded.ChevronLeft, { year-- }, enabled = year > firstYear, size = 32.dp)
            Text("$year", style = ts(16, 800, ls.ink))
            IconBtn(Icons.Rounded.ChevronRight, { year++ }, enabled = year < thisYear, size = 32.dp)
            Box(Modifier.weight(1f))
            Text("$count days done", style = ts(13, 700, ls.ink2), modifier = Modifier.padding(end = 2.dp))
        }
        YearGrid(h, today, year, mondayFirst)
    }
}

/** GitHub-style year: two bands of ~27 week columns, 7 rows each. */
@Composable
private fun YearGrid(h: Habit, today: Long, year: Int, mondayFirst: Boolean) {
    val ls = LocalLs.current
    val jan1 = LocalDate.of(year, 1, 1)
    val start = jan1.weekStart(mondayFirst)
    val startDay = start.toEpochDay()
    val dec31 = LocalDate.of(year, 12, 31).toEpochDay()
    val weeks = ((dec31 - startDay) / 7 + 1).toInt()
    val cols = (weeks + 1) / 2
    // Month label goes over the first full week that starts in that month.
    val monthWeeks = (1..12).map { m ->
        val first = LocalDate.of(year, m, 1)
        val ws = first.weekStart(mondayFirst).let { if (it.isBefore(first)) it.plusWeeks(1) else it }
        m to ((ws.toEpochDay() - startDay) / 7).toInt()
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 2.dp
        val cell = (maxWidth - gap * (cols - 1)) / cols
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0 until cols, cols until weeks).forEach { band ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.fillMaxWidth().height(12.dp)) {
                        monthWeeks.filter { it.second in band }.forEach { (m, w) ->
                            Text(
                                java.time.Month.of(m).getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                style = ts(9, 800, ls.ink3, lh = 1.3f),
                                modifier = Modifier.offset(x = (cell + gap) * (w - band.first)),
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .height(cell * 7 + gap * 6),
                    ) {
                        val c = cell.toPx()
                        val g = gap.toPx()
                        val r = CornerRadius(2.5.dp.toPx())
                        for (w in band) for (row in 0 until 7) {
                            val day = startDay + w * 7 + row
                            if (day < jan1.toEpochDay() || day > dec31) continue
                            val color = when {
                                day > today -> ls.surface2
                                h.isDone(day) -> ls.pastel(h.color)
                                else -> ls.tint(h.color)
                            }
                            val tl = Offset((w - band.first) * (c + g), row * (c + g))
                            drawRoundRect(color, tl, Size(c, c), r)
                            if (day == today) {
                                val sw = 1.5.dp.toPx()
                                drawRoundRect(
                                    ls.ink2,
                                    Offset(tl.x - sw / 2, tl.y - sw / 2),
                                    Size(c + sw, c + sw),
                                    CornerRadius(r.x + sw / 2),
                                    style = Stroke(sw),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
