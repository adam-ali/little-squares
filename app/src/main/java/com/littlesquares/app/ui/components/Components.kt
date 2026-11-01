package com.littlesquares.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlesquares.app.data.Pastel
import com.littlesquares.app.ui.theme.LocalLs
import com.littlesquares.app.ui.theme.ts

/** Full-screen page: app background, insets handled. */
@Composable
fun Screen(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(LocalLs.current.bg)
            .safeDrawingPadding(),
        content = content,
    )
}

@Composable
fun LsIcon(v: ImageVector, tint: Color = LocalLs.current.ink, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Icon(v, contentDescription = null, tint = tint, modifier = modifier.size(size))
}

@Composable
fun IconBtn(
    v: ImageVector,
    onClick: () -> Unit,
    tint: Color = LocalLs.current.ink,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    contentDescription: String? = null,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClickLabel = contentDescription, onClick = onClick)
            .alpha(if (enabled) 1f else .35f),
        contentAlignment = Alignment.Center,
    ) { LsIcon(v, tint) }
}

@Composable
fun TopBar(
    title: String?,
    nav: ImageVector,
    onNav: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconBtn(nav, onNav)
        if (title != null) Text(title, style = ts(20, 800))
        Spacer(Modifier.weight(1f))
        actions()
    }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    bg: Color = LocalLs.current.primary,
    fg: Color = LocalLs.current.onPrimary,
    height: Dp = 56.dp,
    enabled: Boolean = true,
    padding: PaddingValues = PaddingValues(horizontal = 24.dp),
) {
    val ls = LocalLs.current
    // Disabled buttons sit back as a quiet surface instead of a faded primary pill.
    val animBg by animateColorAsState(if (enabled) bg else ls.surface2, tween(250), label = "pillBg")
    val animFg by animateColorAsState(if (enabled) fg else ls.ink3, tween(250), label = "pillFg")
    Row(
        modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(animBg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) LsIcon(icon, animFg, 22.dp)
        Text(text, style = ts(16, 800, animFg))
        if (trailingIcon != null) LsIcon(trailingIcon, animFg, 22.dp)
    }
}

/**
 * The round check-off button: press 0.92 → fill + pop 1.14 → settle (≈320 ms spring).
 */
@Composable
fun CheckButton(
    done: Boolean,
    pastel: Pastel,
    size: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null,
) {
    val ls = LocalLs.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pop = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(done) {
        if (first) { first = false; return@LaunchedEffect }
        if (done) {
            pop.animateTo(1.14f, tween(110))
            pop.animateTo(1f, spring(dampingRatio = .42f, stiffness = 500f))
        } else {
            pop.animateTo(.95f, tween(80))
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    val press by animateFloatAsState(if (pressed) .92f else 1f, label = "press")
    val bg by animateColorAsState(if (done) ls.pastel(pastel) else ls.tint(pastel, 30), tween(250), label = "checkBg")
    val ic by animateColorAsState(if (done) ls.onPastel else ls.checkIdle(pastel), tween(250), label = "checkIc")
    val haptic = LocalHapticFeedback.current
    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                scaleX = pop.value * press
                scaleY = pop.value * press
            }
            .clip(CircleShape)
            .background(bg)
            .clickable(interaction, indication = null, role = Role.Checkbox) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) content() else LsIcon(Icons.Rounded.Check, ic, iconSize)
    }
}

/** One day square. */
@Composable
fun DayCell(
    bg: Color,
    modifier: Modifier,
    radius: Dp,
    ring: Color? = null,
    ringWidth: Dp = 2.dp,
    label: String? = null,
    labelColor: Color = Color.Unspecified,
    labelSize: Int = 11,
    labelWeight: Int = 800,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(radius)
    val animBg by animateColorAsState(bg, tween(250), label = "cell")
    var m = modifier.clip(shape).background(animBg)
    if (ring != null) m = m.border(BorderStroke(ringWidth, ring), shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m, contentAlignment = Alignment.Center) {
        if (label != null) Text(label, style = ts(labelSize, labelWeight, labelColor), maxLines = 1)
    }
}

@Composable
fun EmojiTile(emoji: String, bg: Color, size: Dp, radius: Dp, fontSize: Int, modifier: Modifier = Modifier) {
    val animBg by animateColorAsState(bg, tween(350), label = "tile")
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(animBg),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = fontSize.sp) }
}

@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    selectedBg: Color,
    icon: ((T) -> ImageVector)? = null,
    shadow: Boolean = false,
) {
    val ls = LocalLs.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(ls.surface2)
            .padding(4.dp),
    ) {
        options.forEach { o ->
            val sel = o == selected
            val bg by animateColorAsState(if (sel) selectedBg else Color.Transparent, tween(200), label = "seg")
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (sel && shadow) Modifier.border(BorderStroke(1.dp, ls.line), RoundedCornerShape(18.dp)) else Modifier)
                    .clip(RoundedCornerShape(18.dp))
                    .background(bg)
                    .clickable { onSelect(o) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                if (icon != null) LsIcon(icon(o), if (sel) ls.ink else ls.ink2, 18.dp)
                Text(label(o), style = ts(14, 800, if (sel) ls.ink else ls.ink2))
            }
        }
    }
}

/** Material-style switch, drawn to match the design (check in the thumb when on). */
@Composable
fun LsSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val ls = LocalLs.current
    val track by animateColorAsState(if (checked) ls.primary else ls.surface2, label = "track")
    val thumb by animateDpAsState(if (checked) 24.dp else 16.dp, label = "thumbSize")
    val x by animateDpAsState(if (checked) 24.dp else 8.dp, label = "thumbX")
    val thumbColor by animateColorAsState(if (checked) ls.onPrimary else ls.ink3, label = "thumbColor")
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .size(52.dp, 32.dp)
            .clip(shape)
            .background(track)
            .then(if (!checked) Modifier.border(2.dp, ls.ink3, shape) else Modifier)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = x)
                .size(thumb)
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center,
        ) { if (checked) LsIcon(Icons.Rounded.Check, ls.primary, 16.dp) }
    }
}

@Composable
fun Swatches(selected: Pastel, onSelect: (Pastel) -> Unit) {
    val ls = LocalLs.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Pastel.entries.forEach { p ->
            val sel = p == selected
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .then(if (sel) Modifier.border(2.dp, ls.ink, CircleShape) else Modifier)
                    .clickable { onSelect(p) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(ls.pastel(p)),
                    contentAlignment = Alignment.Center,
                ) { if (sel) LsIcon(Icons.Rounded.Check, ls.onPastel, 18.dp) }
            }
        }
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(text, style = ts(13, 700, LocalLs.current.ink2), modifier = Modifier.padding(start = 6.dp))
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(LocalLs.current.surface),
        content = content,
    )
}

@Composable
fun CardDivider() {
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(1.5.dp)
            .background(LocalLs.current.bg),
    )
}

/** A tappable settings-style row: title (+ optional subtitle) on the left, trailing content on the right. */
@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    titleColor: Color = LocalLs.current.ink,
    leading: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val ls = LocalLs.current
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .height(if (subtitle != null) 68.dp else 56.dp)
            .padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (leading != null) {
            LsIcon(leading, if (titleColor == ls.ink) ls.ink2 else titleColor, 22.dp)
            Spacer(Modifier.width(2.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = ts(16, 700, titleColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = ts(13, 600, ls.ink2), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}
