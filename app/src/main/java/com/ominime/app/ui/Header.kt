package com.ominime.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ominime.app.R

object chrome {
    val hidden = mutableStateOf(false)
    val search = mutableStateOf(false)
}

@Composable
fun track(l: LazyListState) {
    var last by remember { mutableIntStateOf(0) }
    LaunchedEffect(l) {
        snapshotFlow { l.firstVisibleItemIndex * 100000 + l.firstVisibleItemScrollOffset }
            .collect { v ->
                if (v <= 160) chrome.hidden.value = false
                else if (v > last + 18) chrome.hidden.value = true
                else if (v < last - 12) chrome.hidden.value = false
                last = v
            }
    }
    DisposableEffect(l) {
        onDispose { chrome.hidden.value = false }
    }
}

@Composable
fun Header(onSearch: () -> Unit, onSettings: () -> Unit, onNotes: () -> Unit = {}, unread: Int = 0) {
    val hide = chrome.hidden.value
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = !hide,
            enter = slideInVertically(animationSpec = tween(260)) { -it } + fadeIn(tween(200)),
            exit = slideOutVertically(animationSpec = tween(220)) { -it } + fadeOut(tween(150))
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 14.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    Modifier
                        .height(42.dp)
                        .aspectRatio(4.06f)
                        .clip(cut(2.dp))
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo),
                        contentDescription = "Ominime",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.weight(1f))
                Bell(unread) { onNotes() }
                Spacer(Modifier.width(9.dp))
                Knob(Icons.Rounded.Search, "Search") { onSearch() }
                Spacer(Modifier.width(9.dp))
                Knob(Icons.Rounded.Settings, "Settings") { onSettings() }
            }
        }
    }
}

@Composable
private fun Bell(unread: Int, click: () -> Unit) {
    val p = localpal.current
    val shape = cut(10.dp)
    val hot by animateFloatAsState(if (unread > 0) 1f else 0f, tween(240), label = "hot")
    Box(
        Modifier
            .size(42.dp)
            .clip(shape)
            .background(p.glass)
            .border(0.8.dp, if (unread > 0) p.acc.copy(alpha = 0.5f) else p.line, shape)
            .press { click() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.NotificationsActive, "Activity", tint = if (unread > 0) p.acc else p.text, modifier = Modifier.size(18.dp))
        if (unread > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 7.dp, end = 7.dp)
                    .size(7.dp + 1.dp * hot)
                    .clip(CircleShape)
                    .background(p.acc)
            )
        }
    }
}

@Composable
private fun Knob(icon: ImageVector, note: String, click: () -> Unit) {
    val p = localpal.current
    val shape = cut(10.dp)
    Box(
        Modifier
            .size(42.dp)
            .clip(shape)
            .background(p.glass)
            .border(0.8.dp, p.line, shape)
            .clickable(onClick = click),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, note, tint = p.text, modifier = Modifier.size(18.dp))
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 5.dp)
                .size(11.dp, 1.5.dp)
                .clip(cut(1.dp))
                .background(p.acc)
        )
    }
}
