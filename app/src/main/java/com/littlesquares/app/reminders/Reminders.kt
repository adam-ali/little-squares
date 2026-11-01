package com.littlesquares.app.reminders

import android.app.AlarmManager
import android.content.Context
import com.littlesquares.app.data.Repo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Inexact, doze-friendly alarms: one per habit with a reminder, the evening nudge,
 * and a just-after-midnight tick so widgets roll over to the new day.
 */
object Reminders {
    private const val NUDGE_MINUTES = 20 * 60

    private fun nextAt(minutes: Int): Long {
        val now = LocalDateTime.now()
        var t = LocalDateTime.of(LocalDate.now(), LocalTime.of(minutes / 60, minutes % 60))
        if (!t.isAfter(now)) t = t.plusDays(1)
        return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun set(ctx: Context, at: Long, action: String, habitId: String?) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, Notifier.receiverIntent(ctx, action, habitId))
    }

    private fun cancel(ctx: Context, action: String, habitId: String?) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(Notifier.receiverIntent(ctx, action, habitId))
    }

    fun rescheduleAll(ctx: Context) {
        val data = Repo.data
        data.habits.forEach { h ->
            if (!h.archived && h.reminderOn) set(ctx, nextAt(h.reminderMinutes), ReminderReceiver.ACTION_REMIND, h.id)
            else cancel(ctx, ReminderReceiver.ACTION_REMIND, h.id)
        }
        if (data.settings.eveningNudge) set(ctx, nextAt(NUDGE_MINUTES), ReminderReceiver.ACTION_NUDGE, null)
        else cancel(ctx, ReminderReceiver.ACTION_NUDGE, null)
        set(ctx, nextAt(0) + 30_000, ReminderReceiver.ACTION_MIDNIGHT, null)
    }

    fun snooze(ctx: Context, habitId: String) {
        set(ctx, System.currentTimeMillis() + 60 * 60 * 1000L, ReminderReceiver.ACTION_SNOOZE_FIRE, habitId)
    }
}
