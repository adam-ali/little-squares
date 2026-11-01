package com.littlesquares.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.littlesquares.app.MainActivity
import com.littlesquares.app.R
import com.littlesquares.app.data.Habit
import com.littlesquares.app.data.Repo
import com.littlesquares.app.data.isDone
import com.littlesquares.app.data.streak
import com.littlesquares.app.data.todayEpoch
import com.littlesquares.app.ui.theme.LightLs

object Notifier {
    const val CH_SOUND = "reminders"
    const val CH_QUIET = "reminders_quiet"
    private const val HABIT_ID = 100
    private const val NUDGE_ID = 200
    private const val NUDGE_TAG = "nudge"
    const val EXTRA_HABIT = "habit_id"

    fun canNotify(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_SOUND, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Daily habit reminders with a gentle sound"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_QUIET, "Quiet reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Daily habit reminders without sound"
                setSound(null, null)
                enableVibration(false)
            },
        )
    }

    private fun habitUri(id: String) = Uri.parse("littlesquares://habit/$id")

    fun openAppIntent(ctx: Context, habitId: String?): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (habitId != null) {
                putExtra(EXTRA_HABIT, habitId)
                data = habitUri(habitId)
            }
        }
        return PendingIntent.getActivity(ctx, habitId.hashCode(), i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun receiverIntent(ctx: Context, action: String, habitId: String?): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java).apply {
            this.action = action
            if (habitId != null) {
                putExtra(EXTRA_HABIT, habitId)
                data = habitUri(habitId)
            }
        }
        return PendingIntent.getBroadcast(ctx, (action + habitId).hashCode(), i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** Rounded tile with the habit's emoji, for the notification's large icon. */
    private fun emojiBitmap(h: Habit): Bitmap {
        val size = 144
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LightLs.tint(h.color, 35).toArgb() }
        c.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), 48f, 48f, bg)
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 72f; textAlign = Paint.Align.CENTER }
        val y = size / 2f - (tp.descent() + tp.ascent()) / 2f
        c.drawText(h.emoji, size / 2f, y, tp)
        return bmp
    }

    /**
     * Posts (or refreshes) a habit reminder. When [alert] is false the update is silent —
     * used after "Mark done" so the card flips to its done state without buzzing again.
     */
    fun showHabit(ctx: Context, h: Habit, alert: Boolean) {
        if (!canNotify(ctx)) return
        val today = todayEpoch()
        val done = h.isDone(today)
        val s = h.streak(today)
        val channel = if (Repo.data.settings.gentleSound) CH_SOUND else CH_QUIET
        val title = if (done) "${h.name} · done" else h.name
        val body = when {
            done -> if (s == 1) "First square filled. See you tomorrow." else "$s-day streak. See you tomorrow."
            s > 0 -> "Your $s-day streak is waiting."
            else -> "A small step today starts a streak."
        }
        val b = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_squares)
            .setColor(LightLs.pastel(h.color).toArgb())
            .setLargeIcon(emojiBitmap(h))
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppIntent(ctx, h.id))
            .setOnlyAlertOnce(!alert)
            .setSilent(!alert)
            .setAutoCancel(true)
            .addAction(
                if (done) R.drawable.ic_stat_squares else R.drawable.ic_check,
                if (done) "Undo" else "Mark done",
                receiverIntent(ctx, ReminderReceiver.ACTION_MARK, h.id),
            )
        if (done) {
            b.setTimeoutAfter(15_000)
        } else {
            b.addAction(R.drawable.ic_stat_squares, "Snooze 1 h", receiverIntent(ctx, ReminderReceiver.ACTION_SNOOZE, h.id))
        }
        notify(ctx, h.id, HABIT_ID, b.build())
    }

    fun showNudge(ctx: Context, left: List<Habit>, alert: Boolean) {
        if (!canNotify(ctx) || left.isEmpty()) return
        val channel = if (Repo.data.settings.gentleSound) CH_SOUND else CH_QUIET
        val title = if (left.size == 1) "One habit left today" else "${left.size} habits left today"
        val body = left.joinToString("  ") { "${it.emoji} ${it.name}" }
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_squares)
            .setColor(LightLs.pastels[5].toArgb())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openAppIntent(ctx, null))
            .setOnlyAlertOnce(!alert)
            .setSilent(!alert)
            .setAutoCancel(true)
            .build()
        notify(ctx, NUDGE_TAG, NUDGE_ID, n)
    }

    private fun notify(ctx: Context, tag: String, id: Int, n: android.app.Notification) {
        if (!canNotify(ctx)) return
        try {
            NotificationManagerCompat.from(ctx).notify(tag, id, n)
        } catch (_: SecurityException) {
        }
    }

    fun cancelHabit(ctx: Context, habitId: String) = NotificationManagerCompat.from(ctx).cancel(habitId, HABIT_ID)

    /** Keep any visible notifications in step with the latest data. */
    fun refreshActive(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        val today = todayEpoch()
        val data = Repo.data
        nm.activeNotifications.forEach { sbn ->
            val tag = sbn.tag ?: return@forEach
            when {
                sbn.id == NUDGE_ID && tag == NUDGE_TAG -> {
                    val left = data.active.filter { !it.isDone(today) }
                    if (left.isEmpty()) nm.cancel(NUDGE_TAG, NUDGE_ID) else showNudge(ctx, left, alert = false)
                }
                sbn.id == HABIT_ID -> {
                    val h = data.habit(tag)
                    if (h == null || h.archived) nm.cancel(tag, HABIT_ID) else showHabit(ctx, h, alert = false)
                }
            }
        }
    }
}
