package com.littlesquares.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.littlesquares.app.data.Repo
import com.littlesquares.app.data.isDone
import com.littlesquares.app.data.todayEpoch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_REMIND = "com.littlesquares.app.REMIND"
        const val ACTION_SNOOZE_FIRE = "com.littlesquares.app.SNOOZE_FIRE"
        const val ACTION_NUDGE = "com.littlesquares.app.NUDGE"
        const val ACTION_MIDNIGHT = "com.littlesquares.app.MIDNIGHT"
        const val ACTION_MARK = "com.littlesquares.app.MARK"
        const val ACTION_SNOOZE = "com.littlesquares.app.SNOOZE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val ctx = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                handle(ctx, intent)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(ctx: Context, intent: Intent) {
        Repo.init(ctx)
        val id = intent.getStringExtra(Notifier.EXTRA_HABIT)
        val today = todayEpoch()
        val data = Repo.data
        when (intent.action) {
            ACTION_REMIND, ACTION_SNOOZE_FIRE -> {
                val h = data.habit(id)
                val wanted = h != null && !h.archived && (h.reminderOn || intent.action == ACTION_SNOOZE_FIRE)
                if (h != null && wanted && !h.isDone(today)) Notifier.showHabit(ctx, h, alert = true)
                Reminders.rescheduleAll(ctx)
            }
            ACTION_NUDGE -> {
                val left = data.active.filter { !it.isDone(today) }
                if (data.settings.eveningNudge) Notifier.showNudge(ctx, left, alert = true)
                Reminders.rescheduleAll(ctx)
            }
            ACTION_MARK -> {
                if (id != null) Repo.toggle(id, today)
                Effects.refresh(ctx)
            }
            ACTION_SNOOZE -> {
                if (id != null) {
                    Notifier.cancelHabit(ctx, id)
                    Reminders.snooze(ctx, id)
                }
            }
            ACTION_MIDNIGHT -> Effects.refresh(ctx)
        }
    }
}

/** Re-arms alarms after reboot, app update or clock changes. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val ctx = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                Repo.init(ctx)
                Effects.refresh(ctx)
            } finally {
                pending.finish()
            }
        }
    }
}
