package com.littlesquares.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.littlesquares.app.MainActivity
import com.littlesquares.app.R
import com.littlesquares.app.data.Pastel
import com.littlesquares.app.data.Repo
import com.littlesquares.app.data.ThemeMode
import com.littlesquares.app.data.todayEpoch
import com.littlesquares.app.reminders.Effects
import com.littlesquares.app.reminders.Notifier
import com.littlesquares.app.ui.theme.DarkLs
import com.littlesquares.app.ui.theme.LightLs
import com.littlesquares.app.ui.theme.LsColors
import androidx.glance.color.ColorProvider as dayNight

/** Widget colours: follow the app's theme setting, or day/night when it's "System". */
class WPal(private val mode: ThemeMode) {
    private fun cp(f: (LsColors) -> Color): ColorProvider = when (mode) {
        ThemeMode.LIGHT -> ColorProvider(f(LightLs))
        ThemeMode.DARK -> ColorProvider(f(DarkLs))
        ThemeMode.SYSTEM -> dayNight(day = f(LightLs), night = f(DarkLs))
    }

    val surface = cp { it.surface }
    val surface2 = cp { it.surface2 }
    val line = cp { it.line }
    val ink = cp { it.ink }
    val ink2 = cp { it.ink2 }
    val ink3 = cp { it.ink3 }
    val primary = cp { it.primary }
    val onPrimary = cp { it.onPrimary }
    val onPastel = cp { it.onPastel }
    val transparent = ColorProvider(Color.Transparent)
    fun pastel(p: Pastel) = cp { it.pastel(p) }
    fun tint(p: Pastel, pct: Int = 26) = cp { it.tint(p, pct) }
    fun ring(p: Pastel) = cp { it.todayRing(p) }
    fun checkIdle(p: Pastel) = cp { it.checkIdle(p) }
}

val HabitKey = ActionParameters.Key<String>("habit_id")
val DayKey = ActionParameters.Key<Long>("day")

class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Repo.init(context)
        val id = parameters[HabitKey] ?: return
        Repo.toggle(id, parameters[DayKey] ?: todayEpoch())
        Effects.refresh(context)
    }
}

fun toggleAction(id: String, day: Long? = null): Action =
    if (day == null) actionRunCallback<ToggleHabitAction>(actionParametersOf(HabitKey to id))
    else actionRunCallback<ToggleHabitAction>(actionParametersOf(HabitKey to id, DayKey to day))

fun openApp(context: Context, habitId: String? = null): Action =
    actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (habitId != null) putExtra(Notifier.EXTRA_HABIT, habitId)
        },
    )

fun wText(size: TextUnit, color: ColorProvider, bold: Boolean = true) =
    TextStyle(color = color, fontSize = size, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)

/** A day square; the "today" outline is drawn as a 2dp frame around the fill. */
@Composable
fun WCell(bg: ColorProvider, ring: ColorProvider?, modifier: GlanceModifier, radius: Dp, action: Action? = null) {
    val m = if (action != null) modifier.clickable(action) else modifier
    if (ring != null) {
        Box(m.background(ring).cornerRadius(radius).padding(2.dp)) {
            Box(GlanceModifier.fillMaxSize().background(bg).cornerRadius(radius - 2.dp)) {}
        }
    } else {
        Box(m.background(bg).cornerRadius(radius)) {}
    }
}

@Composable
fun WCheck(p: Pastel, done: Boolean, size: Dp, iconSize: Dp, pal: WPal, action: Action) {
    Box(
        GlanceModifier
            .size(size)
            .background(if (done) pal.pastel(p) else pal.tint(p, 30))
            .cornerRadius(size / 2)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_check),
            contentDescription = if (done) "Done" else "Mark done",
            modifier = GlanceModifier.size(iconSize),
            colorFilter = ColorFilter.tint(if (done) pal.onPastel else pal.checkIdle(p)),
        )
    }
}

@Composable
fun WEmojiCheck(emoji: String, p: Pastel, done: Boolean, size: Dp, pal: WPal, action: Action) {
    Box(
        GlanceModifier
            .size(size)
            .background(if (done) pal.pastel(p) else pal.tint(p, 30))
            .cornerRadius(size / 2)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, style = TextStyle(fontSize = (size.value * .45f).sp)) }
}

val WIDGET_RADIUS = 28.dp
