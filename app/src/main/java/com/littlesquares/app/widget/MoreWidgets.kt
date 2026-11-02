package com.littlesquares.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
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
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.littlesquares.app.data.AppData
import com.littlesquares.app.data.Habit
import com.littlesquares.app.data.Repo
import com.littlesquares.app.data.isDone
import com.littlesquares.app.data.todayEpoch
import com.littlesquares.app.data.weekLetters
import com.littlesquares.app.data.weekStart
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// Note: Glance rows/columns render at most 10 children, so every container here stays under that.

@Composable
private fun WidgetFrame(pal: WPal, padH: Dp, padV: Dp, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    Box(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(pal.surface)
            .cornerRadius(WIDGET_RADIUS)
            .clickable(openApp(ctx))
            .padding(horizontal = padH, vertical = padV),
    ) { content() }
}

@Composable
private fun NoHabits(pal: WPal) {
    Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Text("Little Squares", style = wText(14.sp, pal.ink))
        Text("Tap to add your first habit", style = wText(12.sp, pal.ink2, bold = false))
    }
}

/** This week: one row per habit (up to 10), fitted to the widget's height. */
@Composable
fun WeekGrid(data: AppData, habits: List<Habit>, today: Long, pal: WPal, height: Dp) {
    val monday = data.settings.weekStartsMonday
    val start = LocalDate.ofEpochDay(today).weekStart(monday)
    val days = (0L until 7L).map { start.plusDays(it).toEpochDay() }
    val available = (height.value - 104f).coerceAtLeast(24f)
    val count = minOf(habits.size, 10, (available / 22f).toInt().coerceAtLeast(1))
    val shown = habits.take(count)
    val cellH = (available / count - 6f).coerceIn(14f, 40f).dp
    val n = habits.count { it.isDone(today) }
    val fmt = DateTimeFormatter.ofPattern("d MMM")
    Column(GlanceModifier.fillMaxSize()) {
        Row(GlanceModifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("This week", style = wText(16.sp, pal.ink))
            Spacer(GlanceModifier.defaultWeight())
            Text("${start.format(fmt)} – ${start.plusDays(6).format(fmt)}", style = wText(12.sp, pal.ink2, bold = false))
        }
        DayHeader(days, today, weekLetters(monday), pal)
        Column(GlanceModifier.fillMaxWidth().defaultWeight().padding(top = 4.dp)) {
            shown.forEach { h ->
                Row(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(h.emoji, style = TextStyle(fontSize = 16.sp), modifier = GlanceModifier.width(38.dp))
                    days.forEach { d ->
                        val off = today - d
                        Box(GlanceModifier.defaultWeight().height(cellH).padding(horizontal = 2.dp)) {
                            if (off < 0) {
                                WCell(pal.surface2, null, GlanceModifier.fillMaxSize(), 8.dp)
                            } else {
                                WCell(
                                    if (h.isDone(d)) pal.pastel(h.color) else pal.tint(h.color),
                                    if (off == 0L) pal.ring(h.color) else null,
                                    GlanceModifier.fillMaxSize(),
                                    8.dp,
                                    if (off in 0..3) toggleAction(h.id, d) else null,
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(GlanceModifier.fillMaxWidth()) {
            Text("$n of ${habits.size} done today", style = wText(12.sp, pal.ink2))
            Spacer(GlanceModifier.defaultWeight())
            if (habits.size > shown.size) Text("+${habits.size - shown.size} more", style = wText(12.sp, pal.ink2))
        }
    }
}

// ---------- 1-row strip: every habit's check in one line ----------

class StripWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StripWidget()
}

class StripWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Repo.init(context)
        provideContent {
            val data by Repo.state.collectAsState()
            StripContent(data)
        }
    }
}

@Composable
private fun StripContent(data: AppData) {
    val ctx = LocalContext.current
    val size = LocalSize.current
    val pal = WPal(data.settings.theme)
    val today = todayEpoch()
    val habits = data.active
    WidgetFrame(pal, 8.dp, 6.dp) {
        if (habits.isEmpty()) { NoHabits(pal); return@WidgetFrame }
        val circle = (size.height.value - 14f).coerceIn(28f, 48f)
        val fit = ((size.width.value - 16f) / (circle + 6f)).toInt().coerceIn(1, 10)
        val shown = if (habits.size <= fit) habits else habits.take(fit - 1)
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            shown.forEach { h ->
                Box(GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                    WEmojiCheck(h.emoji, h.color, h.isDone(today), circle.dp, pal, toggleAction(h.id))
                }
            }
            if (habits.size > shown.size) {
                Box(GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                    Box(
                        GlanceModifier
                            .size(circle.dp)
                            .background(pal.surface2)
                            .cornerRadius((circle / 2).dp)
                            .clickable(openApp(ctx)),
                        contentAlignment = Alignment.Center,
                    ) { Text("+${habits.size - shown.size}", style = wText(13.sp, pal.ink2)) }
                }
            }
        }
    }
}

// ---------- Week ----------

class WeekWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekWidget()
}

class WeekWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Repo.init(context)
        provideContent {
            val data by Repo.state.collectAsState()
            val pal = WPal(data.settings.theme)
            val height = LocalSize.current.height
            WidgetFrame(pal, 14.dp, 14.dp) {
                if (data.active.isEmpty()) NoHabits(pal)
                else WeekGrid(data, data.active, todayEpoch(), pal, height)
            }
        }
    }
}

// ---------- Month ----------

val HEAT = listOf(12, 30, 52, 76, 100)

class MonthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthWidget()
}

class MonthWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Repo.init(context)
        provideContent {
            val data by Repo.state.collectAsState()
            MonthContent(data)
        }
    }
}

@Composable
private fun MonthContent(data: AppData) {
    val pal = WPal(data.settings.theme)
    val today = todayEpoch()
    val habits = data.active
    val monday = data.settings.weekStartsMonday
    val date = LocalDate.ofEpochDay(today)
    val month = YearMonth.from(date)
    val first = month.atDay(1)
    val lead = (first.toEpochDay() - first.weekStart(monday).toEpochDay()).toInt()
    val rows = (lead + month.lengthOfMonth() + 6) / 7
    val letters = weekLetters(monday)
    WidgetFrame(pal, 12.dp, 12.dp) {
        if (habits.isEmpty()) { NoHabits(pal); return@WidgetFrame }
        val n = habits.count { it.isDone(today) }
        Column(GlanceModifier.fillMaxSize()) {
            Row(GlanceModifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(date.format(DateTimeFormatter.ofPattern("MMMM")), style = wText(15.sp, pal.ink))
                Spacer(GlanceModifier.defaultWeight())
                Text("$n/${habits.size} today", style = wText(12.sp, pal.ink2))
            }
            Row(GlanceModifier.fillMaxWidth()) {
                letters.forEach {
                    Text(
                        it,
                        style = TextStyle(color = pal.ink3, fontSize = 10.sp, fontWeight = androidx.glance.text.FontWeight.Bold, textAlign = TextAlign.Center),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
            Column(GlanceModifier.fillMaxWidth().defaultWeight()) {
                for (r in 0 until rows) {
                    Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                        for (c in 0 until 7) {
                            val i = r * 7 + c - lead
                            Box(GlanceModifier.defaultWeight().fillMaxHeight().padding(2.dp)) {
                                if (i in 0 until month.lengthOfMonth()) {
                                    val day = first.toEpochDay() + i
                                    MonthDay(i + 1, day, today, habits, pal)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthDay(n: Int, day: Long, today: Long, habits: List<Habit>, pal: WPal) {
    if (day > today) {
        Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("$n", style = wText(10.sp, pal.ink3, bold = false))
        }
        return
    }
    val lv = (habits.count { it.isDone(day) } * 4f / habits.size).roundToInt()
    val isToday = day == today
    Box(
        GlanceModifier.fillMaxSize().background(pal.heat(HEAT[lv])).cornerRadius(7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$n",
            style = wText(10.sp, if (isToday) pal.ink else if (lv >= 3) pal.onPastel else pal.ink2, bold = isToday),
        )
    }
}
