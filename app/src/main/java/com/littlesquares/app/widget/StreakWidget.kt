package com.littlesquares.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
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
import androidx.glance.text.TextStyle
import androidx.lifecycle.lifecycleScope
import com.littlesquares.app.data.AppData
import com.littlesquares.app.data.Habit
import com.littlesquares.app.data.Repo
import com.littlesquares.app.data.isDone
import com.littlesquares.app.data.streak
import com.littlesquares.app.data.todayEpoch
import com.littlesquares.app.data.weekLetters
import com.littlesquares.app.data.weekStart
import com.littlesquares.app.ui.components.EmojiTile
import com.littlesquares.app.ui.components.Screen
import com.littlesquares.app.ui.components.TopBar
import com.littlesquares.app.ui.theme.LittleSquaresTheme
import com.littlesquares.app.ui.theme.LocalLs
import com.littlesquares.app.ui.theme.ts
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.Row as CRow
import androidx.compose.foundation.layout.padding as cPadding
import androidx.compose.material3.Text as CText
import androidx.compose.ui.Alignment as CAlignment
import androidx.compose.ui.Modifier as CModifier

private val S_SMALL = DpSize(110.dp, 110.dp)
private val S_WIDE = DpSize(250.dp, 110.dp)
private val S_LARGE = DpSize(250.dp, 250.dp)

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}

/** One habit: big check (2×2), streak + last 8 weeks (4×2), last 4 weeks (4×4). */
class StreakWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(S_SMALL, S_WIDE, S_LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Repo.init(context)
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val data by Repo.state.collectAsState()
            StreakContent(data, widgetId)
        }
    }
}

@Composable
private fun StreakContent(data: AppData, widgetId: Int) {
    val ctx = LocalContext.current
    val size = LocalSize.current
    val pal = WPal(data.settings.theme)
    val today = todayEpoch()
    val h = data.habit(data.settings.widgetHabits[widgetId])?.takeIf { !it.archived } ?: data.active.firstOrNull()
    Box(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(pal.surface)
            .cornerRadius(WIDGET_RADIUS)
            .clickable(openApp(ctx, h?.id))
            .padding(16.dp),
    ) {
        when {
            h == null -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Text("Little Squares", style = wText(15.sp, pal.ink))
                Spacer(GlanceModifier.height(4.dp))
                Text("Add a habit in the app first", style = wText(13.sp, pal.ink2, bold = false))
            }
            size.height >= S_LARGE.height -> FourWeeks(h, today, data.settings.weekStartsMonday, pal)
            size.width >= S_WIDE.width -> EightWeeks(h, today, data.settings.weekStartsMonday, pal)
            else -> SingleHabit(h, today, pal)
        }
    }
}

@Composable
private fun EightWeeks(h: Habit, today: Long, monday: Boolean, pal: WPal) {
    val start = LocalDate.ofEpochDay(today).weekStart(monday).minusWeeks(7).toEpochDay()
    Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(GlanceModifier.defaultWeight().fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(h.emoji, style = TextStyle(fontSize = 20.sp))
                Spacer(GlanceModifier.width(8.dp))
                Text(h.name, style = wText(14.sp, pal.ink), maxLines = 1)
            }
            Spacer(GlanceModifier.defaultWeight())
            Text("${h.streak(today)}", style = wText(44.sp, pal.ink))
            Text("day streak", style = wText(13.sp, pal.ink2))
        }
        Spacer(GlanceModifier.width(12.dp))
        Row {
            for (w in 0 until 8) {
                if (w > 0) Spacer(GlanceModifier.width(4.dp))
                Column {
                    for (r in 0 until 7) {
                        if (r > 0) Spacer(GlanceModifier.height(4.dp))
                        val d = start + w * 7 + r
                        val off = today - d
                        when {
                            off < 0 -> WCell(pal.surface2, null, GlanceModifier.size(12.dp), 4.dp)
                            else -> WCell(
                                if (h.isDone(d)) pal.pastel(h.color) else pal.tint(h.color),
                                if (off == 0L) pal.ring(h.color) else null,
                                GlanceModifier.size(12.dp),
                                4.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FourWeeks(h: Habit, today: Long, monday: Boolean, pal: WPal) {
    val thisWeek = LocalDate.ofEpochDay(today).weekStart(monday)
    val fmt = DateTimeFormatter.ofPattern("d MMM")
    val s = h.streak(today)
    Column(GlanceModifier.fillMaxSize()) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(h.emoji, style = TextStyle(fontSize = 18.sp))
            Spacer(GlanceModifier.width(8.dp))
            Text(h.name, style = wText(16.sp, pal.ink), maxLines = 1, modifier = GlanceModifier.defaultWeight())
            Text("Last 4 weeks", style = wText(12.sp, pal.ink2, bold = false))
        }
        Spacer(GlanceModifier.height(10.dp))
        DayHeader((0L until 7L).map { thisWeek.plusDays(it).toEpochDay() }, today, weekLetters(monday), pal, lead = 44.dp)
        Spacer(GlanceModifier.height(4.dp))
        for (w in 3 downTo 0) {
            val ws = thisWeek.minusWeeks(w.toLong())
            Row(GlanceModifier.fillMaxWidth().defaultWeight().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(ws.format(fmt), style = wText(10.sp, pal.ink2), modifier = GlanceModifier.width(44.dp), maxLines = 1)
                for (i in 0L until 7L) {
                    val d = ws.plusDays(i).toEpochDay()
                    val off = today - d
                    Box(GlanceModifier.defaultWeight().fillMaxHeight().padding(horizontal = 2.dp)) {
                        when {
                            off < 0 -> WCell(pal.surface2, null, GlanceModifier.fillMaxSize(), 10.dp)
                            else -> WCell(
                                if (h.isDone(d)) pal.pastel(h.color) else pal.tint(h.color),
                                if (off == 0L) pal.ring(h.color) else null,
                                GlanceModifier.fillMaxSize(),
                                10.dp,
                                if (off in 0..3) toggleAction(h.id, d) else null,
                            )
                        }
                    }
                }
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (s > 0) "$s-day streak" else "Start a streak today", style = wText(13.sp, pal.ink2), modifier = GlanceModifier.defaultWeight())
            WCheck(h.color, h.isDone(today), 36.dp, 20.dp, pal, toggleAction(h.id))
        }
    }
}

/** Lets the user pick which habit a streak widget shows. */
class StreakWidgetConfigActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        Repo.init(this)
        setContent {
            val data by Repo.state.collectAsState()
            LittleSquaresTheme(data.settings.theme) { Picker(data) }
        }
    }

    private fun pick(habitId: String) {
        Repo.updateSettings { it.copy(widgetHabits = it.widgetHabits + (widgetId to habitId)) }
        lifecycleScope.launch {
            runCatching {
                val gid = GlanceAppWidgetManager(this@StreakWidgetConfigActivity).getGlanceIdBy(widgetId)
                StreakWidget().update(this@StreakWidgetConfigActivity, gid)
            }
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }
    }

    @Composable
    private fun Picker(data: AppData) {
        val ls = LocalLs.current
        Screen {
            TopBar("Choose a habit", Icons.Rounded.Close, { finish() })
            if (data.active.isEmpty()) {
                CText(
                    "Add a habit in Little Squares first, then add this widget again.",
                    style = ts(15, 400, ls.ink2, lh = 1.5f),
                    modifier = CModifier.cPadding(horizontal = 24.dp),
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(data.active, key = { it.id }) { h ->
                    CRow(
                        CModifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(ls.surface)
                            .clickable { pick(h.id) }
                            .cPadding(12.dp),
                        verticalAlignment = CAlignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        EmojiTile(h.emoji, ls.tint(h.color, 32), 46.dp, 16.dp, 23)
                        CText(h.name, style = ts(17, 700, ls.ink))
                    }
                }
            }
        }
    }
}
