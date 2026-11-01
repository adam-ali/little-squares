package com.littlesquares.app.reminders

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.littlesquares.app.widget.MonthWidget
import com.littlesquares.app.widget.StreakWidget
import com.littlesquares.app.widget.StripWidget
import com.littlesquares.app.widget.TodayWidget
import com.littlesquares.app.widget.WeekWidget

/** Everything outside the app UI that mirrors the data: widgets, alarms, visible notifications. */
object Effects {
    suspend fun refresh(ctx: Context) {
        runCatching { TodayWidget().updateAll(ctx) }
        runCatching { StreakWidget().updateAll(ctx) }
        runCatching { StripWidget().updateAll(ctx) }
        runCatching { WeekWidget().updateAll(ctx) }
        runCatching { MonthWidget().updateAll(ctx) }
        runCatching { Reminders.rescheduleAll(ctx) }
        runCatching { Notifier.refreshActive(ctx) }
    }
}
