package com.littlesquares.app.ui.components

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.littlesquares.app.data.Habit
import com.littlesquares.app.data.Repo
import com.littlesquares.app.data.isDone
import com.littlesquares.app.data.letter
import com.littlesquares.app.data.todayEpoch
import com.littlesquares.app.ui.theme.LocalLs
import com.littlesquares.app.ui.theme.ts
import kotlinx.coroutines.delay
import java.text.BreakIterator
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Today's epoch day, kept fresh across midnight. */
@Composable
fun rememberToday(): Long {
    val state: State<Long> = produceState(todayEpoch()) {
        while (true) {
            delay(30_000)
            value = todayEpoch()
        }
    }
    return state.value
}

fun formatTime(minutes: Int): String =
    LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

fun is24h(context: Context): Boolean = DateFormat.is24HourFormat(context)

/**
 * Last seven days for one habit, oldest first, today outlined.
 * The three days before today can be tapped to backfill.
 */
@Composable
fun HabitStrip(
    h: Habit,
    today: Long,
    height: Dp,
    radius: Dp,
    labels: Boolean = true,
    interactive: Boolean = true,
    gap: Dp = 6.dp,
) {
    val ls = LocalLs.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
        for (off in 6 downTo 0) {
            val day = today - off
            val d = h.isDone(day)
            val can = interactive && off in 1..3
            DayCell(
                bg = if (d) ls.pastel(h.color) else ls.tint(h.color),
                modifier = Modifier.weight(1f).height(height),
                radius = radius,
                ring = if (off == 0) ls.todayRing(h.color) else null,
                label = if (labels) day.letter() else null,
                labelColor = if (d) ls.onPastel else ls.ink2,
                onClick = if (can) ({ Repo.toggle(h.id, day) }) else null,
            )
        }
    }
}

@Composable
fun NameField(
    value: String,
    onValue: (String) -> Unit,
    ringColor: Color,
    placeholder: String,
    emoji: String? = null,
    onEmojiClick: (() -> Unit)? = null,
    height: Dp = 58.dp,
) {
    val ls = LocalLs.current
    var focused by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val shape = RoundedCornerShape(18.dp)
    BasicTextField(
        value = value,
        onValueChange = { onValue(it.take(60)) },
        singleLine = true,
        textStyle = ts(17, 600, ls.ink),
        cursorBrush = SolidColor(ls.ink),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(shape)
                    .background(ls.surface)
                    .then(if (focused) Modifier.border(2.dp, ringColor, shape) else Modifier)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (emoji != null) {
                    Text(
                        emoji,
                        fontSize = 22.sp,
                        modifier = if (onEmojiClick != null) Modifier.clickable(onClick = onEmojiClick) else Modifier,
                    )
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = ts(17, 600, ls.ink3))
                    inner()
                }
            }
        },
    )
}

val EMOJIS = listOf(
    "💧", "📖", "🌿", "✍️", "🧘", "🌙", "💊", "🏃", "🚶", "🚴", "🏋️", "🧠",
    "🎸", "🎨", "🍎", "🥗", "🥦", "☕", "🍵", "🚭", "📵", "😴", "🪥", "🧴",
    "🌞", "🌱", "🪴", "🐶", "🧹", "🧺", "💰", "📚", "📝", "💻", "🎧", "🙏",
    "❤️", "😊", "🗣️", "📞", "✉️", "🚿", "🦷", "🏊", "⚽", "🧗", "🎹", "🍳",
)

private fun firstGrapheme(s: String): String {
    val t = s.trim()
    if (t.isEmpty()) return ""
    val it = BreakIterator.getCharacterInstance()
    it.setText(t)
    val end = it.next()
    return if (end == BreakIterator.DONE) t else t.substring(0, end)
}

@Composable
fun EmojiPickerDialog(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val ls = LocalLs.current
    var custom by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(ls.surface)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Pick an emoji", style = ts(20, 800, ls.ink))
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.heightIn(max = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(EMOJIS) { e ->
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (e == current) ls.sel else Color.Transparent)
                            .clickable { onPick(e) },
                        contentAlignment = Alignment.Center,
                    ) { Text(e, fontSize = 24.sp) }
                }
            }
            FieldLabel("Or type your own")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    BasicTextField(
                        value = custom,
                        onValueChange = { custom = firstGrapheme(it.takeLast(16)) },
                        singleLine = true,
                        textStyle = ts(22, 400, ls.ink),
                        cursorBrush = SolidColor(ls.ink),
                        decorationBox = { inner ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ls.surface2)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) { inner() }
                        },
                    )
                }
                PillButton("Use", onClick = { if (custom.isNotBlank()) onPick(custom) }, height = 52.dp, enabled = custom.isNotBlank())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(initialMinutes: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val ls = LocalLs.current
    val ctx = LocalContext.current
    val st = rememberTimePickerState(initialMinutes / 60, initialMinutes % 60, is24h(ctx))
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ls.surface,
        confirmButton = {
            TextButton(onClick = { onPick(st.hour * 60 + st.minute) }) { Text("OK", style = ts(15, 800, ls.ink)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", style = ts(15, 800, ls.ink2)) }
        },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(st) }
        },
    )
}

/** Small confirm dialog in the app's style. */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
) {
    val ls = LocalLs.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ls.surface,
        title = { Text(title, style = ts(20, 800, ls.ink)) },
        text = { Text(body, style = ts(15, 400, ls.ink2, lh = 1.45f)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, style = ts(15, 800, if (danger) ls.danger else ls.ink)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", style = ts(15, 800, ls.ink2)) }
        },
    )
}
