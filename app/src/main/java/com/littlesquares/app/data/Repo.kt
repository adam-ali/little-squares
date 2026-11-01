package com.littlesquares.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Single source of truth shared by the app, notifications and widgets.
 * Data is small, so it lives in one JSON file and every change rewrites it.
 */
object Repo {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var file: File? = null
    private val _state = MutableStateFlow(AppData())
    val state: StateFlow<AppData> = _state.asStateFlow()
    val data: AppData get() = _state.value

    @Synchronized
    fun init(context: Context) {
        if (file != null) return
        val f = File(context.applicationContext.filesDir, "little_squares.json")
        file = f
        if (f.exists()) {
            runCatching { _state.value = json.decodeFromString(AppData.serializer(), f.readText()) }
        }
    }

    @Synchronized
    fun update(transform: (AppData) -> AppData) {
        val next = transform(_state.value)
        if (next == _state.value) return
        _state.value = next
        save(next)
    }

    private fun save(d: AppData) {
        val f = file ?: return
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), d))
        if (!tmp.renameTo(f)) { f.writeText(tmp.readText()); tmp.delete() }
    }

    fun exportJson(): String =
        Json { prettyPrint = true; encodeDefaults = true }.encodeToString(AppData.serializer(), data)

    private fun mapHabit(id: String, f: (Habit) -> Habit) = update { d ->
        d.copy(habits = d.habits.map { if (it.id == id) f(it) else it })
    }

    fun setDone(id: String, day: Long, value: Boolean) = mapHabit(id) {
        it.copy(done = if (value) it.done + day else it.done - day)
    }

    fun toggle(id: String, day: Long) {
        val h = data.habit(id) ?: return
        setDone(id, day, !h.isDone(day))
    }

    fun upsert(h: Habit) = update { d ->
        if (d.habits.any { it.id == h.id }) d.copy(habits = d.habits.map { if (it.id == h.id) h else it })
        else d.copy(habits = d.habits + h)
    }

    fun setArchived(id: String, archived: Boolean) = mapHabit(id) { it.copy(archived = archived) }

    fun delete(id: String) = update { d -> d.copy(habits = d.habits.filterNot { it.id == id }) }

    fun updateSettings(f: (Settings) -> Settings) = update { it.copy(settings = f(it.settings)) }

    fun newId(): String = UUID.randomUUID().toString()
}
