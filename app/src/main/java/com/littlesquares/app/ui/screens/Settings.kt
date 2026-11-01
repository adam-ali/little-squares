package com.littlesquares.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.littlesquares.app.BuildConfig
import com.littlesquares.app.data.AppData
import com.littlesquares.app.data.Habit
import com.littlesquares.app.data.Repo
import com.littlesquares.app.data.ThemeMode
import com.littlesquares.app.ui.components.CardDivider
import com.littlesquares.app.ui.components.ConfirmDialog
import com.littlesquares.app.ui.components.EmojiTile
import com.littlesquares.app.ui.components.IconBtn
import com.littlesquares.app.ui.components.ListRow
import com.littlesquares.app.ui.components.LsIcon
import com.littlesquares.app.ui.components.LsSwitch
import com.littlesquares.app.ui.components.Screen
import com.littlesquares.app.ui.components.SectionCard
import com.littlesquares.app.ui.components.Segmented
import com.littlesquares.app.ui.components.TimeDialog
import com.littlesquares.app.ui.components.TopBar
import com.littlesquares.app.ui.components.formatTime
import com.littlesquares.app.ui.theme.LocalLs
import com.littlesquares.app.ui.theme.ts
import java.io.File
import java.time.LocalDate

@Composable
private fun SectionLabel(text: String, top: Boolean = false) {
    Text(
        text,
        style = ts(13, 800, LocalLs.current.ink2),
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = if (top) 4.dp else 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun Chevron() = LsIcon(Icons.Rounded.ChevronRight, LocalLs.current.ink3, 22.dp)

@Composable
fun SettingsScreen(data: AppData, onBack: () -> Unit, onArchived: () -> Unit) {
    val ls = LocalLs.current
    val ctx = LocalContext.current
    val s = data.settings
    var pickTime by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    val askNotify = rememberNotificationGate { ok -> Repo.updateSettings { it.copy(eveningNudge = ok) } }

    Screen {
        TopBar("Settings", Icons.AutoMirrored.Rounded.ArrowBack, onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel("Appearance", top = true)
            SectionCard {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Theme", style = ts(16, 700, ls.ink))
                    Segmented(
                        options = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM),
                        selected = s.theme,
                        label = { when (it) { ThemeMode.LIGHT -> "Light"; ThemeMode.DARK -> "Dark"; ThemeMode.SYSTEM -> "System" } },
                        icon = { when (it) { ThemeMode.LIGHT -> Icons.Rounded.LightMode; ThemeMode.DARK -> Icons.Rounded.DarkMode; ThemeMode.SYSTEM -> Icons.Rounded.Contrast } },
                        onSelect = { m -> Repo.updateSettings { it.copy(theme = m) } },
                        selectedBg = ls.sel,
                    )
                }
                CardDivider()
                ListRow("Week starts on", onClick = { Repo.updateSettings { it.copy(weekStartsMonday = !it.weekStartsMonday) } }) {
                    Text(if (s.weekStartsMonday) "Monday" else "Sunday", style = ts(15, 700, ls.ink2))
                    Chevron()
                }
            }

            SectionLabel("Reminders")
            SectionCard {
                ListRow("Default time", onClick = { pickTime = true }) {
                    Text(formatTime(s.defaultReminderMinutes), style = ts(15, 700, ls.ink2))
                    Chevron()
                }
                CardDivider()
                ListRow("Gentle sound", onClick = { Repo.updateSettings { it.copy(gentleSound = !it.gentleSound) } }) {
                    LsSwitch(s.gentleSound) { v -> Repo.updateSettings { it.copy(gentleSound = v) } }
                }
                CardDivider()
                ListRow("Evening nudge", subtitle = "If anything's left at 20:00") {
                    LsSwitch(s.eveningNudge) { v -> if (v) askNotify() else Repo.updateSettings { it.copy(eveningNudge = false) } }
                }
            }

            SectionLabel("Your data")
            SectionCard {
                ListRow("Export data", subtitle = "CSV or JSON, all habits", onClick = { exporting = true }) {
                    LsIcon(Icons.Rounded.Download, ls.ink2, 22.dp)
                }
                CardDivider()
                ListRow("Archived habits", onClick = onArchived) {
                    Text("${data.archived.size}", style = ts(15, 700, ls.ink2))
                    Chevron()
                }
            }
            Text(
                "Little Squares ${BuildConfig.VERSION_NAME}",
                style = ts(12, 600, ls.ink3),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
        }
    }

    if (pickTime) {
        TimeDialog(s.defaultReminderMinutes, onDismiss = { pickTime = false }) { m ->
            Repo.updateSettings { it.copy(defaultReminderMinutes = m) }
            pickTime = false
        }
    }
    if (exporting) {
        AlertDialog(
            onDismissRequest = { exporting = false },
            containerColor = ls.surface,
            title = { Text("Export data", style = ts(20, 800, ls.ink)) },
            text = { Text("Share a copy of every habit and check-in.", style = ts(15, 400, ls.ink2)) },
            confirmButton = {
                TextButton(onClick = { exporting = false; Exporter.share(ctx, json = true) }) { Text("JSON", style = ts(15, 800, ls.ink)) }
            },
            dismissButton = {
                TextButton(onClick = { exporting = false; Exporter.share(ctx, json = false) }) { Text("CSV", style = ts(15, 800, ls.ink)) }
            },
        )
    }
}

object Exporter {
    fun csv(data: AppData): String = buildString {
        appendLine("habit_id,habit,emoji,colour,archived,date")
        data.habits.forEach { h ->
            val name = "\"" + h.name.replace("\"", "\"\"") + "\""
            h.done.sorted().forEach { d ->
                appendLine("${h.id},$name,${h.emoji},${h.color.name.lowercase()},${h.archived},${LocalDate.ofEpochDay(d)}")
            }
        }
    }

    fun share(ctx: Context, json: Boolean) {
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val ext = if (json) "json" else "csv"
        val f = File(dir, "little-squares-${LocalDate.now()}.$ext")
        f.writeText(if (json) Repo.exportJson() else csv(Repo.data))
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = if (json) "application/json" else "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Little Squares export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, "Export data").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
fun ArchivedScreen(data: AppData, onBack: () -> Unit) {
    val ls = LocalLs.current
    var deleting by remember { mutableStateOf<Habit?>(null) }
    Screen {
        TopBar("Archived habits", Icons.AutoMirrored.Rounded.ArrowBack, onBack)
        if (data.archived.isEmpty()) {
            Text(
                "Nothing archived. Archiving a habit hides it from Today but keeps its squares.",
                style = ts(15, 400, ls.ink2, lh = 1.5f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(data.archived, key = { it.id }) { h ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .clip(RoundedCornerShape(28.dp))
                        .background(ls.surface)
                        .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    EmojiTile(h.emoji, ls.tint(h.color, 32), 46.dp, 16.dp, 23)
                    Column(Modifier.weight(1f)) {
                        Text(h.name, style = ts(17, 700, ls.ink), maxLines = 1)
                        Text("${h.done.size} days done", style = ts(13, 600, ls.ink2))
                    }
                    IconBtn(Icons.Rounded.Unarchive, { Repo.setArchived(h.id, false) }, tint = ls.ink2, contentDescription = "Unarchive")
                    IconBtn(Icons.Rounded.Delete, { deleting = h }, tint = ls.danger, contentDescription = "Delete")
                }
            }
        }
    }
    deleting?.let { h ->
        ConfirmDialog(
            title = "Delete “${h.name}”?",
            body = "This removes the habit and all its squares for good.",
            confirm = "Delete",
            danger = true,
            onConfirm = { Repo.delete(h.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}
