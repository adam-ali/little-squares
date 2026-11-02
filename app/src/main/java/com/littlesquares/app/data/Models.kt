package com.littlesquares.app.data

import kotlinx.serialization.Serializable

/** The eight habit colours, in the design's palette order. */
@Serializable
enum class Pastel { PEACH, BUTTER, SAGE, MINT, SKY, LAVENDER, ROSE, SAND }

@Serializable
enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Serializable
data class Habit(
    val id: String,
    val name: String,
    val emoji: String,
    val color: Pastel,
    val reminderOn: Boolean = false,
    /** Minutes after midnight. */
    val reminderMinutes: Int = 9 * 60,
    val archived: Boolean = false,
    /** Epoch day the habit was created. */
    val createdDay: Long,
    /** Epoch days the habit was done. */
    val done: Set<Long> = emptySet(),
)

@Serializable
data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val weekStartsMonday: Boolean = true,
    val defaultReminderMinutes: Int = 9 * 60,
    val gentleSound: Boolean = true,
    val eveningNudge: Boolean = false,
    val onboarded: Boolean = false,
    /** Home shows the month instead of the week. */
    val homeMonth: Boolean = false,
    /** Streak widget id -> habit id. */
    val widgetHabits: Map<Int, String> = emptyMap(),
)

@Serializable
data class AppData(
    val habits: List<Habit> = emptyList(),
    val settings: Settings = Settings(),
) {
    val active: List<Habit> get() = habits.filter { !it.archived }
    val archived: List<Habit> get() = habits.filter { it.archived }
    fun habit(id: String?): Habit? = habits.firstOrNull { it.id == id }
}
