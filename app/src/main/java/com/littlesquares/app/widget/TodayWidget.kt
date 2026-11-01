package com.littlesquares.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.littlesquares.app.data.AppData
import com.littlesquares.app.data.Habit
import com.littlesquares.app.data.Repo
import com.littlesquares.app.data.isDone
import com.littlesquares.app.data.streak
import com.littlesquares.app.data.todayEpoch
import com.littlesquares.app.data.weekLetters
import com.littlesquares.app.data.weekStart
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val SMALL = DpSize(110.dp, 110.dp)
private val WIDE = DpSize(220.dp, 110.dp)
private val LARGE = DpSize(220.dp, 220.dp)

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

/**
 * "Today" widget. Picks its layout from size and habit count:
 * 1 habit → big check; up to 3 → list; 6+ → emoji grid; 4×4 → this week's grid.
 */
class TodayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Repo.init(context)
        provideContent {
            val data by Repo.state.collectAsState()
            TodayContent(data)
        }
    }
}

@Composable
private fun TodayContent(data: AppData) {
    val ctx = LocalContext.current
    val size = LocalSize.current
    val pal = WPal(data.settings.theme)
    val today = todayEpoch()
    val habits = data.active
    Box(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(pal.surface)
            .cornerRadius(WIDGET_RADIUS)
            .clickable(openApp(ctx))
            .padding(14.dp),
    ) {
        when {
            habits.isEmpty() -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Text("Today", style = wText(15.sp, pal.ink))
                Spacer(GlanceModifier.height(4.dp))
                Text("Tap to add your first habit", style = wText(13.sp, pal.ink2, bold = false))
            }
            size.height >= LARGE.height -> WeekGrid(data, habits, today, pal)
            habits.size == 1 -> SingleHabit(habits[0], today, pal)
            habits.size <= 3 || size.width >= WIDE.width -> HabitList(habits, today, pal, wide = size.width >= WIDE.width)
            else -> EmojiGrid(habits, today, pal)
        }
    }
}

@Composable
fun SingleHabit(h: Habit, today: Long, pal: WPal) {
    val ctx = LocalContext.current
    val s = h.streak(today)
    Column(GlanceModifier.fillMaxSize().clickable(openApp(ctx, h.id))) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(h.emoji, style = TextStyle(fontSize = 26.sp))
            Spacer(GlanceModifier.defaultWeight())
            Text(if (s == 1) "1 day" else "$s days", style = wText(12.sp, pal.ink2))
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(h.name, style = wText(15.sp, pal.ink), maxLines = 2, modifier = GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(8.dp))
            WCheck(h.color, h.isDone(today), 56.dp, 30.dp, pal, toggleAction(h.id))
        }
    }
}

@Composable
private fun Header(title: String, right: String, pal: WPal) {
    Row(GlanceModifier.fillMaxWidth().height(20.dp).padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = wText(13.sp, pal.ink))
        Spacer(GlanceModifier.defaultWeight())
        Text(right, style = wText(12.sp, pal.ink2))
    }
}

@Composable
private fun HabitList(habits: List<Habit>, today: Long, pal: WPal, wide: Boolean) {
    val shown = habits.take(3)
    val n = habits.count { it.isDone(today) }
    Column(GlanceModifier.fillMaxSize()) {
        Header("Today", "$n/${habits.size}", pal)
        Spacer(GlanceModifier.height(4.dp))
        shown.forEach { h ->
            Row(GlanceModifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(h.emoji, style = TextStyle(fontSize = 16.sp))
                Spacer(GlanceModifier.width(8.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(h.name, style = wText(13.sp, pal.ink), maxLines = 1)
                    if (wide) {
                        val s = h.streak(today)
                        Text(if (s > 0) "$s-day streak" else "Start today", style = wText(11.sp, pal.ink2, bold = false), maxLines = 1)
                    }
                }
                WCheck(h.color, h.isDone(today), 36.dp, 20.dp, pal, toggleAction(h.id))
            }
        }
        if (habits.size > shown.size) {
            Text("+${habits.size - shown.size} more", style = wText(11.sp, pal.ink3))
        }
    }
}

@Composable
private fun EmojiGrid(habits: List<Habit>, today: Long, pal: WPal) {
    val n = habits.count { it.isDone(today) }
    Column(GlanceModifier.fillMaxSize()) {
        Header("Today", "$n/${habits.size}", pal)
        Spacer(GlanceModifier.defaultWeight())
        habits.take(6).chunked(3).forEach { row ->
            Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                row.forEachIndexed { i, h ->
                    if (i > 0) Spacer(GlanceModifier.width(8.dp))
                    WEmojiCheck(h.emoji, h.color, h.isDone(today), 40.dp, pal, toggleAction(h.id))
                }
            }
            Spacer(GlanceModifier.height(8.dp))
        }
    }
}

@Composable
private fun WeekGrid(data: AppData, habits: List<Habit>, today: Long, pal: WPal) {
    val monday = data.settings.weekStartsMonday
    val start = LocalDate.ofEpochDay(today).weekStart(monday)
    val days = (0L until 7L).map { start.plusDays(it).toEpochDay() }
    val shown = habits.take(6)
    val cellH = if (shown.size <= 3) 40.dp else 24.dp
    val n = habits.count { it.isDone(today) }
    val fmt = DateTimeFormatter.ofPattern("d MMM")
    Column(GlanceModifier.fillMaxSize()) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("This week", style = wText(16.sp, pal.ink))
            Spacer(GlanceModifier.defaultWeight())
            Text("${start.format(fmt)} – ${start.plusDays(6).format(fmt)}", style = wText(12.sp, pal.ink2, bold = false))
        }
        Spacer(GlanceModifier.height(10.dp))
        DayHeader(days, today, weekLetters(monday), pal)
        Spacer(GlanceModifier.height(6.dp))
        shown.forEach { h ->
            Row(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(h.emoji, style = TextStyle(fontSize = 17.sp), modifier = GlanceModifier.width(38.dp))
                days.forEach { d ->
                    val off = today - d
                    Box(GlanceModifier.defaultWeight().height(cellH).padding(horizontal = 2.dp)) {
                        when {
                            off < 0 -> WCell(pal.surface2, null, GlanceModifier.fillMaxSize(), 9.dp)
                            else -> WCell(
                                if (h.isDone(d)) pal.pastel(h.color) else pal.tint(h.color),
                                if (off == 0L) pal.ring(h.color) else null,
                                GlanceModifier.fillMaxSize(),
                                9.dp,
                                if (off in 0..3) toggleAction(h.id, d) else null,
                            )
                        }
                    }
                }
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth()) {
            Text("$n of ${habits.size} done today", style = wText(12.sp, pal.ink2))
            Spacer(GlanceModifier.defaultWeight())
            if (habits.size > shown.size) Text("+${habits.size - shown.size} more", style = wText(12.sp, pal.ink2))
        }
    }
}

@Composable
fun DayHeader(days: List<Long>, today: Long, letters: List<String>, pal: WPal, lead: androidx.compose.ui.unit.Dp = 38.dp) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(GlanceModifier.width(lead))
        days.forEachIndexed { i, d ->
            val isToday = d == today
            Box(GlanceModifier.defaultWeight().padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
                Box(
                    GlanceModifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(if (isToday) pal.primary else pal.transparent)
                        .cornerRadius(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(letters[i], style = wText(11.sp, if (isToday) pal.onPrimary else pal.ink3))
                }
            }
        }
    }
}
