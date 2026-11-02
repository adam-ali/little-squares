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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.littlesquares.app.data.AppData
import com.littlesquares.app.data.Habit
import com.littlesquares.app.data.Pastel
import com.littlesquares.app.data.Repo
import com.littlesquares.app.data.isDone
import com.littlesquares.app.data.todayEpoch
import com.littlesquares.app.data.weekLetters
import com.littlesquares.app.data.weekStart
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// Glance rows/columns render at most 10 children; every container here stays under that.

private fun weekDays(data: AppData, today: Long): List<Long> {
    val start = LocalDate.ofEpochDay(today).weekStart(data.settings.weekStartsMonday)
    return (0L until 7L).map { start.plusDays(it).toEpochDay() }
}

private fun level(habits: List<Habit>, day: Long): Int =
    if (habits.isEmpty()) 0 else (habits.count { it.isDone(day) } * 4f / habits.size).roundToInt()

/**
 * Combined day square with an optional label: future days are outlined,
 * today gets the lavender ring, past days are shaded by how much was done.
 */
@Composable
private fun HeatSquare(
    habits: List<Habit>,
    day: Long,
    today: Long,
    label: String?,
    pal: WPal,
    modifier: GlanceModifier,
    radius: Dp,
) {
    val lv = level(habits, day)
    val future = day > today
    val fill: ColorProvider = if (future) pal.surface else pal.heat(HEAT[lv])
    val ink = when {
        future -> pal.ink3
        lv >= 3 -> pal.onPastel
        else -> pal.ink2
    }
    val ring: ColorProvider? = when {
        future -> pal.line
        day == today -> pal.ring(Pastel.LAVENDER)
        else -> null
    }
    val content: @Composable () -> Unit = {
        if (label != null) Text(label, style = wText(11.sp, ink))
    }
    if (ring != null) {
        Box(modifier.background(ring).cornerRadius(radius).padding(if (future) 1.5.dp else 2.dp)) {
            Box(GlanceModifier.fillMaxSize().background(fill).cornerRadius(radius - 2.dp), contentAlignment = Alignment.Center) { content() }
        }
    } else {
        Box(modifier.background(fill).cornerRadius(radius), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun Frame(pal: WPal, glass: Boolean, radius: Dp, padStart: Dp, padEnd: Dp, padV: Dp, action: androidx.glance.action.Action, content: @Composable () -> Unit) {
    Box(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(if (glass) pal.glass else pal.surface)
            .cornerRadius(radius)
            .clickable(action)
            .padding(start = padStart, end = padEnd, top = padV, bottom = padV),
        contentAlignment = Alignment.CenterStart,
    ) { content() }
}

// ---------- 5×1 · this week, all habits · tap opens Calendar ----------

class WeekStripWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekStripWidget()
}

class WeekStripWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Repo.init(context)
        provideContent {
            val data by Repo.state.collectAsState()
            WeekStrip(data)
        }
    }
}

@Composable
private fun WeekStrip(data: AppData) {
    val ctx = LocalContext.current
    val size = LocalSize.current
    val pal = WPal(data.settings.theme)
    val today = todayEpoch()
    val habits = data.active
    val days = weekDays(data, today)
    val past = days.filter { it <= today }
    val done = habits.sumOf { h -> past.count { h.isDone(it) } }
    val letters = weekLetters(data.settings.weekStartsMonday)
    val square = (size.height.value - 28f).coerceIn(24f, 40f)
    val showCount = size.width.value >= 300f
    Frame(pal, glass = false, radius = WIDGET_RADIUS, padStart = if (showCount) 22.dp else 12.dp, padEnd = if (showCount) 18.dp else 12.dp, padV = 6.dp, action = openCalendar(ctx)) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (showCount) {
                Column(GlanceModifier.defaultWeight()) {
                    Text("This week", style = wText(12.sp, pal.ink2), maxLines = 1)
                    Text("$done/${past.size * habits.size}", style = wText(22.sp, pal.ink), maxLines = 1)
                }
            }
            Row(if (showCount) GlanceModifier else GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                days.forEachIndexed { i, d ->
                    val gap = GlanceModifier.padding(start = if (i > 0) 6.dp else 0.dp)
                    if (showCount) {
                        Box(gap) { HeatSquare(habits, d, today, letters[i], pal, GlanceModifier.size(square.dp), 12.dp) }
                    } else {
                        Box(gap.defaultWeight()) {
                            HeatSquare(habits, d, today, letters[i], pal, GlanceModifier.fillMaxWidth().height(square.dp), 12.dp)
                        }
                    }
                }
            }
        }
    }
}

// ---------- 1×1 · today: one small square per habit ----------

class TodayDotsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayDotsWidget()
}

class TodayDotsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Repo.init(context)
        provideContent {
            val data by Repo.state.collectAsState()
            TodayDots(data)
        }
    }
}

@Composable
private fun TodayDots(data: AppData) {
    val ctx = LocalContext.current
    val size = LocalSize.current
    val pal = WPal(data.settings.theme)
    val today = todayEpoch()
    val habits = data.active.take(9)
    val side = minOf(size.width.value, size.height.value)
    val dot = ((side - 24f) / 3f - 5f).coerceIn(10f, 22f)
    Box(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(pal.surface)
            .cornerRadius(24.dp)
            .clickable(openApp(ctx))
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            habits.chunked(3).forEachIndexed { r, row ->
                Row(GlanceModifier.padding(top = if (r > 0) 5.dp else 0.dp)) {
                    row.forEachIndexed { i, h ->
                        Box(GlanceModifier.padding(start = if (i > 0) 5.dp else 0.dp)) {
                            Box(
                                GlanceModifier
                                    .size(dot.dp)
                                    .background(if (h.isDone(today)) pal.pastel(h.color) else pal.tint(h.color, 28))
                                    .cornerRadius((dot / 3f).dp),
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

// ---------- Lock screen glances (read-only) ----------

class LockTodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LockTodayWidget()
}

class LockTodayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Repo.init(context)
        provideContent {
            val data by Repo.state.collectAsState()
            LockToday(data)
        }
    }
}

@Composable
private fun LockToday(data: AppData) {
    val ctx = LocalContext.current
    val pal = WPal(data.settings.theme)
    val today = todayEpoch()
    val habits = data.active.take(7)
    Frame(pal, glass = true, radius = 24.dp, padStart = 14.dp, padEnd = 14.dp, padV = 10.dp, action = openApp(ctx)) {
        if (habits.isEmpty()) {
            Text("No habits yet", style = wText(13.sp, pal.ink2))
            return@Frame
        }
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            for (i in 0 until 7) {
                val h = habits.getOrNull(i)
                Column(
                    GlanceModifier.defaultWeight().padding(horizontal = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (h != null) {
                        Text(h.emoji, style = TextStyle(fontSize = 16.sp))
                        Spacer(GlanceModifier.height(6.dp))
                        Box(
                            GlanceModifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .background(if (h.isDone(today)) pal.pastel(h.color) else pal.tint(h.color, 28))
                                .cornerRadius(9.dp),
                        ) {}
                    }
                }
            }
        }
    }
}

class LockWeekWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LockWeekWidget()
}

class LockWeekWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Repo.init(context)
        provideContent {
            val data by Repo.state.collectAsState()
            LockWeek(data)
        }
    }
}

@Composable
private fun LockWeek(data: AppData) {
    val ctx = LocalContext.current
    val pal = WPal(data.settings.theme)
    val today = todayEpoch()
    val habits = data.active
    val days = weekDays(data, today)
    val past = days.filter { it <= today }
    val done = habits.sumOf { h -> past.count { h.isDone(it) } }
    val letters = weekLetters(data.settings.weekStartsMonday)
    Frame(pal, glass = true, radius = 24.dp, padStart = 14.dp, padEnd = 14.dp, padV = 12.dp, action = openCalendar(ctx)) {
        Column(GlanceModifier.fillMaxSize()) {
            Row(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("This week", style = wText(13.sp, pal.ink))
                Spacer(GlanceModifier.defaultWeight())
                Text("$done/${past.size * habits.size}", style = wText(12.sp, pal.ink2))
            }
            Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                days.forEachIndexed { i, d ->
                    Box(GlanceModifier.defaultWeight().padding(horizontal = 3.dp)) {
                        HeatSquare(habits, d, today, letters[i], pal, GlanceModifier.fillMaxSize(), 10.dp)
                    }
                }
            }
        }
    }
}

class LockMonthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LockMonthWidget()
}

class LockMonthWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Repo.init(context)
        provideContent {
            val data by Repo.state.collectAsState()
            LockMonth(data)
        }
    }
}

@Composable
private fun LockMonth(data: AppData) {
    val ctx = LocalContext.current
    val pal = WPal(data.settings.theme)
    val today = todayEpoch()
    val habits = data.active
    val date = LocalDate.ofEpochDay(today)
    val month = YearMonth.from(date)
    val first = month.atDay(1)
    val lead = (first.toEpochDay() - first.weekStart(data.settings.weekStartsMonday).toEpochDay()).toInt()
    val rows = (lead + month.lengthOfMonth() + 6) / 7
    val elapsed = (today - first.toEpochDay() + 1).toInt()
    val checks = habits.sumOf { h -> (0 until elapsed).count { h.isDone(first.toEpochDay() + it) } }
    val pct = if (habits.isEmpty()) 0 else (checks * 100f / (elapsed * habits.size)).roundToInt()
    Frame(pal, glass = true, radius = 24.dp, padStart = 14.dp, padEnd = 14.dp, padV = 12.dp, action = openCalendar(ctx)) {
        Column(GlanceModifier.fillMaxSize()) {
            Row(GlanceModifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(date.format(DateTimeFormatter.ofPattern("MMMM")), style = wText(13.sp, pal.ink))
                Spacer(GlanceModifier.defaultWeight())
                Text("$pct%", style = wText(12.sp, pal.ink2))
            }
            Column(GlanceModifier.fillMaxWidth().defaultWeight()) {
                for (r in 0 until rows) {
                    Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                        for (c in 0 until 7) {
                            val i = r * 7 + c - lead
                            Box(GlanceModifier.defaultWeight().fillMaxSize().padding(2.dp)) {
                                if (i in 0 until month.lengthOfMonth()) {
                                    HeatSquare(habits, first.toEpochDay() + i, today, null, pal, GlanceModifier.fillMaxSize(), 5.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
