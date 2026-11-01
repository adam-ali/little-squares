package com.littlesquares.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

fun todayEpoch(): Long = LocalDate.now().toEpochDay()

fun Habit.isDone(day: Long): Boolean = day in done

/** Counts back from today if done, otherwise from yesterday — today never breaks a streak. */
fun Habit.streak(today: Long): Int {
    var day = if (isDone(today)) today else today - 1
    var n = 0
    while (day in done) { n++; day-- }
    return n
}

fun Habit.bestStreak(): Int {
    var best = 0
    var run = 0
    var prev = Long.MIN_VALUE
    for (d in done.sorted()) {
        run = if (d == prev + 1) run + 1 else 1
        best = maxOf(best, run)
        prev = d
    }
    return best
}

/** First day this habit could have been done (creation or earliest backfill). */
fun Habit.firstDay(): Long = minOf(createdDay, done.minOrNull() ?: createdDay)

fun Habit.doneBetween(from: Long, to: Long): Int = done.count { it in from..to }

fun streakText(s: Int): String = if (s > 0) "$s-day streak" else "Start a streak today"

fun LocalDate.weekStart(mondayFirst: Boolean): LocalDate {
    val idx = if (mondayFirst) dayOfWeek.value - 1 else dayOfWeek.value % 7
    return minusDays(idx.toLong())
}

/** Day letters in display order. */
fun weekLetters(mondayFirst: Boolean): List<String> {
    val first = if (mondayFirst) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    return (0L until 7L).map { first.plus(it).letter() }
}

fun DayOfWeek.letter(): String = getDisplayName(TextStyle.NARROW, Locale.getDefault())

fun Long.letter(): String = LocalDate.ofEpochDay(this).dayOfWeek.letter()
