package com.littlesquares.app.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.littlesquares.app.data.AppData
import com.littlesquares.app.data.Habit
import com.littlesquares.app.data.Pastel
import com.littlesquares.app.data.Repo
import com.littlesquares.app.data.todayEpoch
import com.littlesquares.app.reminders.Notifier
import com.littlesquares.app.ui.components.CardDivider
import com.littlesquares.app.ui.components.ConfirmDialog
import com.littlesquares.app.ui.components.EmojiPickerDialog
import com.littlesquares.app.ui.components.EmojiTile
import com.littlesquares.app.ui.components.FieldLabel
import com.littlesquares.app.ui.components.ListRow
import com.littlesquares.app.ui.components.LsIcon
import com.littlesquares.app.ui.components.LsSwitch
import com.littlesquares.app.ui.components.NameField
import com.littlesquares.app.ui.components.PillButton
import com.littlesquares.app.ui.components.Screen
import com.littlesquares.app.ui.components.SectionCard
import com.littlesquares.app.ui.components.Swatches
import com.littlesquares.app.ui.components.TimeDialog
import com.littlesquares.app.ui.components.TopBar
import com.littlesquares.app.ui.components.is24h
import com.littlesquares.app.ui.theme.LocalLs
import com.littlesquares.app.ui.theme.ts

/** Asks for the notification permission (Android 13+) before turning a reminder on. */
@Composable
fun rememberNotificationGate(onResult: (Boolean) -> Unit): (() -> Unit) {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(ctx, "Notifications are off for Little Squares", Toast.LENGTH_SHORT).show()
        onResult(granted)
    }
    return {
        if (Build.VERSION.SDK_INT >= 33 && !Notifier.canNotify(ctx)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onResult(true)
        }
    }
}

@Composable
fun EditHabitScreen(data: AppData, id: String?, onClose: () -> Unit, onGone: () -> Unit) {
    val ls = LocalLs.current
    val ctx = LocalContext.current
    val existing = remember(id) { data.habit(id) }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var emoji by rememberSaveable { mutableStateOf(existing?.emoji ?: "🌱") }
    var color by rememberSaveable { mutableStateOf(existing?.color ?: Pastel.LAVENDER) }
    var reminderOn by rememberSaveable { mutableStateOf(existing?.reminderOn ?: false) }
    var minutes by rememberSaveable { mutableIntStateOf(existing?.reminderMinutes ?: data.settings.defaultReminderMinutes) }
    var pickEmoji by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val askNotify = rememberNotificationGate { reminderOn = it }

    Screen {
        TopBar(if (existing == null) "New habit" else "Edit habit", Icons.Rounded.Close, onClose)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.clickable { pickEmoji = true }) {
                    EmojiTile(emoji, ls.tint(color, 35), 72.dp, 24.dp, 34)
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(4.dp, 4.dp)
                            .size(26.dp)
                            .border(3.dp, ls.bg, CircleShape)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(ls.primary),
                        contentAlignment = Alignment.Center,
                    ) { LsIcon(Icons.Rounded.Edit, ls.onPrimary, 12.dp) }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Name")
                    NameField(name, { name = it }, ringColor = ls.pastel(color), placeholder = "Read 10 pages", height = 56.dp)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FieldLabel("Colour")
                Swatches(color) { color = it }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(ls.surface)
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Daily reminder", style = ts(16, 700, ls.ink))
                        Text("One gentle nudge a day", style = ts(13, 600, ls.ink2))
                    }
                    LsSwitch(reminderOn) { want -> if (want) askNotify() else reminderOn = false }
                }
                AnimatedVisibility(reminderOn) {
                    val h24 = is24h(ctx)
                    val hour = minutes / 60
                    val shownHour = if (h24) hour else (hour % 12).let { if (it == 0) 12 else it }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeBox("%02d".format(shownHour), selected = true) { pickTime = true }
                        Text(":", style = ts(36, 700, ls.ink))
                        TimeBox("%02d".format(minutes % 60), selected = false) { pickTime = true }
                        if (!h24) Text(if (hour < 12) "AM" else "PM", style = ts(15, 800, ls.ink2), modifier = Modifier.padding(start = 4.dp))
                        Spacer(Modifier.weight(1f))
                        LsIcon(Icons.Rounded.Schedule, ls.ink2, 24.dp, Modifier.padding(end = 4.dp))
                    }
                }
            }
            if (existing != null) {
                SectionCard {
                    ListRow(
                        "Archive habit",
                        leading = Icons.Rounded.Inventory2,
                        onClick = { Repo.setArchived(existing.id, true); onGone() },
                    ) { Text("Keeps history", style = ts(13, 600, ls.ink2)) }
                    CardDivider()
                    ListRow(
                        "Delete habit",
                        titleColor = ls.danger,
                        leading = Icons.Rounded.Delete,
                        onClick = { confirmDelete = true },
                    )
                }
            }
        }
        PillButton(
            if (existing == null) "Add habit" else "Save changes",
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            onClick = {
                val h = existing?.copy(
                    name = name.trim(), emoji = emoji, color = color,
                    reminderOn = reminderOn, reminderMinutes = minutes,
                ) ?: Habit(
                    id = Repo.newId(), name = name.trim(), emoji = emoji, color = color,
                    reminderOn = reminderOn, reminderMinutes = minutes, createdDay = todayEpoch(),
                )
                Repo.upsert(h)
                onClose()
            },
        )
    }

    if (pickEmoji) EmojiPickerDialog(emoji, onPick = { emoji = it; pickEmoji = false }, onDismiss = { pickEmoji = false })
    if (pickTime) TimeDialog(minutes, onDismiss = { pickTime = false }, onPick = { minutes = it; pickTime = false })
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "Delete “${existing.name}”?",
            body = "This removes the habit and all its squares. Archive it instead if you'd like to keep the history.",
            confirm = "Delete",
            danger = true,
            onConfirm = { confirmDelete = false; Repo.delete(existing.id); onGone() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun TimeBox(text: String, selected: Boolean, onClick: () -> Unit) {
    val ls = LocalLs.current
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .size(96.dp, 72.dp)
            .clip(shape)
            .background(if (selected) ls.sel else ls.surface2)
            .then(if (selected) Modifier.border(2.dp, ls.pastel(Pastel.LAVENDER), shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = ts(40, 700, ls.ink)) }
}
