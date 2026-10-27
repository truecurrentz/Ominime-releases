package com.ominime.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ominime.app.data.Note
import com.ominime.app.data.repo
import kotlinx.coroutines.launch
import com.ominime.app.data.store

fun ago(v: Long): String {
    if (v <= 0) return "now"
    val d = System.currentTimeMillis() - v
    val m = d / 60000L
    val h = d / 3600000L
    val day = d / 86400000L
    return when {
        m < 1 -> "now"
        m < 60 -> "${m}m"
        h < 24 -> "${h}h"
        day < 7 -> "${day}d"
        else -> "${day / 7}w"
    }
}

@Composable
fun Notes(onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val p = localpal.current
    val d by store.live.collectAsState()
    val rows = d.notes
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var line by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(p.bg).statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp)
    ) {
        item(key = "head") {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(cut(10.dp))
                        .background(p.glass)
                        .border(0.8.dp, p.line, cut(10.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ArrowBack, "Back", tint = p.text, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "ACTIVITY",
                        color = p.text,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        if (rows.isEmpty()) "Nothing has dropped yet" else "${rows.size} updates from your list",
                        color = p.mute,
                        fontSize = 12.sp
                    )
                }
                Box(
                    Modifier
                        .clip(cut(8.dp))
                        .background(p.glass)
                        .border(0.8.dp, p.line, cut(8.dp))
                        .press { store.wipeNotes() }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text("CLEAR", color = p.mute, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }

        item(key = "check") {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Btn(if (busy) "Checking" else "Check now", main = true) {
                    if (!busy) {
                        busy = true
                    line = ""
                        scope.launch {
                            val got = runCatching { repo.scan() }.getOrNull()
                            line = got?.note ?: "The check could not run"
                            busy = false
                        }
                    }
                }
                Text(
                    line,
                    color = p.mute,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (rows.isEmpty()) {
            item(key = "blank") {
                Blank(
                    "No new episodes",
                    "Titles on your list get a notification here the moment a fresh episode airs.",
                    icon = Icons.Rounded.NotificationsActive
                )
            }
        }

        items(rows.size, key = { "${rows[it].id}-${rows[it].ep}" }) { i ->
            Reveal(delay = (i % 8) * 40) {
                NoteRow(rows[i], { onOpen(rows[i].id) })
            }
        }
    }
}

@Composable
private fun NoteRow(n: Note, click: () -> Unit) {
    val p = localpal.current
    val shape = cut(13.dp, 0.2f)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 9.dp)
            .clip(shape)
            .background(if (n.read) p.lift else p.card)
            .background(
                if (n.read) Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                else Brush.horizontalGradient(listOf(p.acc.copy(alpha = 0.16f), Color.Transparent))
            )
            .border(0.8.dp, if (n.read) p.line else p.acc.copy(alpha = 0.45f), shape)
            .press { click() }
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(cut(10.dp, 0.3f))
                    .background(p.glass)
            ) {
                if (n.cover != null) AsyncImage(
                    model = n.cover,
                    contentDescription = n.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (!n.read) Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(p.acc)
                    .border(1.5.dp, p.card, CircleShape)
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                n.title,
                color = p.text,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .clip(cut(4.dp))
                        .background(p.acc.copy(alpha = 0.16f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("EP ${n.ep}", color = p.acc, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
                }
                Text("is out now", color = p.mute, fontSize = 11.5.sp)
            }
        }
        Text(ago(n.at), color = p.mute.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun NoteToast(note: Note?, onOpen: (Int) -> Unit, onClose: () -> Unit) {
    val p = localpal.current
    val on = note != null
    val slide by animateFloatAsState(if (on) 0f else -1f, tween(260), label = "toast")
    AnimatedVisibility(
        visible = on,
        enter = slideInVertically(tween(300)) { -it } + fadeIn(tween(200)) + scaleIn(tween(300), initialScale = 0.94f),
        exit = slideOutVertically(tween(220)) { -it } + fadeOut(tween(160))
    ) {
        val shape = cut(14.dp, 0.2f)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .statusBarsPadding()
                .padding(top = 58.dp)
                .clip(shape)
                .background(p.card)
                .background(Brush.horizontalGradient(listOf(p.acc.copy(alpha = 0.24f), p.card)))
                .border(0.9.dp, p.acc.copy(alpha = 0.5f), shape)
                .press { note?.let { onOpen(it.id) } }
                .padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(cut(9.dp, 0.3f))
                    .background(p.glass)
            ) {
                if (note?.cover != null) AsyncImage(
                    model = note.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    note?.title.orEmpty(),
                    color = p.text,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Episode ${note?.ep ?: 0} just aired",
                    color = p.acc2,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                Modifier
                    .size(26.dp)
                    .clip(cut(6.dp))
                    .background(p.glass)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = p.text.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}
