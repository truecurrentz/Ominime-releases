package com.ominime.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.focusable
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val railw = 128.dp

val railpeek = 26.dp

@Composable
fun Rail(
    at: String,
    hot: Int,
    open: Boolean,
    onOpen: (Boolean) -> Unit,
    onTab: (String) -> Unit,
    onNotes: () -> Unit,
    onSettings: () -> Unit
) {
    val p = localpal.current
    val first = remember { FocusRequester() }
    val handle = remember { FocusRequester() }
    var near by remember { mutableStateOf(false) }

    val w by animateDpAsState(if (open) railw else railpeek, tween(240), label = "railw")

    LaunchedEffect(Unit) {
        runCatching { first.requestFocus() }
    }

    LaunchedEffect(near, at) {
        if (near) {
            onOpen(true)
        } else {
            delay(4200)
            onOpen(false)
        }
    }

    if (!open) {
        Column(
            Modifier
                .width(w)
                .fillMaxHeight()
                .background(p.dock.copy(alpha = 0.92f))
                .border(0.7.dp, p.line)
                .focusRequester(handle)
                .focusable()
                .onFocusChanged { near = it.isFocused }
                .onKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionRight || e.key == Key.DirectionCenter || e.key == Key.Enter)) {
                        onOpen(true)
                        runCatching { first.requestFocus() }
                        true
                    } else {
                        false
                    }
                }
                .press(0.96f) {
                    onOpen(true)
                    runCatching { first.requestFocus() }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(46.dp)
                    .clip(cut(2.dp))
                    .background(Brush.verticalGradient(listOf(p.acc, p.acc2)))
            )
            Spacer(Modifier.height(9.dp))
            Icon(Icons.Rounded.ChevronRight, "Open menu", tint = p.acc, modifier = Modifier.size(18.dp))
        }
        return
    }

    Column(
        Modifier
            .width(w)
            .fillMaxHeight()
            .background(p.dock.copy(alpha = 0.99f))
            .border(0.7.dp, p.line)
            .padding(horizontal = 10.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(cut(9.dp))
                .background(p.acc.copy(alpha = 0.16f))
                .border(0.9.dp, p.acc.copy(alpha = 0.55f), cut(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PlayArrow, "Ominime", tint = p.acc, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(14.dp))

        Slot("home", "Home", Icons.Rounded.Home, at == "home", 0, first, { near = it }, onTab)
        Slot("schedule", "Schedule", Icons.Rounded.CalendarMonth, at == "schedule", 0, null, { near = it }, onTab)
        Slot("list", "My List", Icons.Rounded.Bookmark, at == "list", 0, null, { near = it }, onTab)
        Slot("downs", "Downloads", Icons.Rounded.Download, at == "downs", 0, null, { near = it }, onTab)

        Spacer(Modifier.weight(1f))

        Slot("notes", "Activity", Icons.Rounded.Notifications, at == "notes", hot, null, {}, { onNotes() })
        Slot("settings", "Settings", Icons.Rounded.Settings, at == "settings", 0, null, {}, { onSettings() })
    }
}

@Composable
private fun Slot(
    key: String,
    label: String,
    icon: ImageVector,
    on: Boolean,
    hot: Int,
    grip: FocusRequester?,
    watch: (Boolean) -> Unit = {},
    click: (String) -> Unit
) {
    val p = localpal.current
    var foc by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (foc) 1.07f else 1f, tween(150), label = "rail")
    var mod = Modifier
        .fillMaxWidth()
        .graphicsLayer { scaleX = s; scaleY = s }
        .clip(cut(10.dp))
        .background(
            when {
                foc -> p.acc.copy(alpha = 0.2f)
                on -> p.acc.copy(alpha = 0.12f)
                else -> Color.Transparent
            }
        )
        .border(
            if (foc) 2.dp else 0.7.dp,
            when {
                foc -> p.acc
                on -> p.acc.copy(alpha = 0.42f)
                else -> Color.Transparent
            },
            cut(10.dp)
        )
        .onFocusChanged { st -> foc = st.isFocused; watch(st.isFocused) }
    if (grip != null) mod = mod.focusRequester(grip)

    Column(
        mod
            .press(0.97f) { click(key) }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(icon, label, tint = if (on || foc) p.acc else p.mute, modifier = Modifier.size(27.dp))
            if (hot > 0) Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(p.acc)
            )
        }
        Text(
            label.uppercase(),
            color = if (on || foc) p.acc else p.mute,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
            maxLines = 1
        )
    }

    if (on) Box(
        Modifier
            .padding(top = 5.dp)
            .width(26.dp)
            .height(2.5.dp)
            .clip(cut(2.dp))
            .background(p.acc)
    )
}
