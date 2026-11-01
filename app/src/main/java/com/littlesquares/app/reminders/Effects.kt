package com.littlesquares.app.reminders

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.littlesquares.app.widget.StreakWidget
import com.littlesquares.app.widget.TodayWidget

/** Everything outside the app UI that mirrors the data: widgets, alarms, visible notifications. */
object Effects {
    suspend fun refresh(ctx: Context) {
        runCatching { TodayWidget().updateAll(ctx) }
        runCatching { StreakWidget().updateAll(ctx) }
        runCatching { Reminders.rescheduleAll(ctx) }
        runCatching { Notifier.refreshActive(ctx) }
    }
}
