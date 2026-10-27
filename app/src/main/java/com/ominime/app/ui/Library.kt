package com.ominime.app.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ominime.app.data.Rec
import com.ominime.app.data.store
import kotlinx.coroutines.launch

@Composable
fun Library(onOpen: (Int) -> Unit, onPlay: (Int, Int) -> Unit, onExplore: () -> Unit) {
    val p = localpal.current
    val ctx = LocalContext.current
    val d by store.live.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var move by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    track(list)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize().background(p.bg).statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 126.dp)
        ) {
            item(key = "head") {
                Column(Modifier.padding(top = 54.dp, start = 20.dp, end = 20.dp)) {
                    Text(
                        "MY LIST",
                        color = p.text,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        when {
                            d.list.isEmpty() && d.hist.isEmpty() -> "Everything you save shows up here."
                            else -> "${d.list.size} saved  •  ${d.hist.size} in progress"
                        },
                        color = p.mute,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Seg(listOf("Watching", "Saved"), tab, Modifier.weight(1f)) { tab = it }
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(cut(8.dp))
                                .background(p.glass)
                                .border(0.8.dp, p.line, cut(8.dp))
                                .clickable { move = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.ImportExport,
                                "Import or export",
                                tint = p.text,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (tab == 0) {
                if (d.hist.isEmpty()) {
                    item(key = "blank") {
                        Reveal {
                            Blank(
                                "Nothing in progress",
                                "Start an episode and Ominime keeps your place right here.",
                                icon = Icons.Rounded.History,
                                action = "Find something",
                                onAction = onExplore
                            )
                        }
                    }
                } else {
                    items(d.hist.size, key = { d.hist[it].id }) { i ->
                        val r = d.hist[i]
                        Reveal {
                            Progress(
                                r = r,
                                onOpen = { onPlay(r.id, if (r.ep > 0) r.ep else 1) },
                                onDrop = { store.drop(r.id) }
                            )
                        }
                    }
                    item(key = "clear") {
                        Row(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Btn("Clear history", Icons.Rounded.Delete) { store.wipe() }
                        }
                    }
                }
            } else {
                if (d.list.isEmpty()) {
                    item(key = "blank") {
                        Reveal {
                            Blank(
                                "No saved titles yet",
                                "Tap the bookmark on any show to keep it for later.",
                                icon = Icons.Rounded.Bookmark,
                                action = "Browse anime",
                                onAction = onExplore
                            )
                        }
                    }
                } else {
                    items(d.list.size, key = { d.list[it].id }) { i ->
                        val r = d.list[i]
                        Reveal {
                            Saved(r, { onOpen(r.id) }, { store.remove(r.id) })
                        }
                    }
                }
            }
        }

        if (move) Move(onClose = { move = false })
    }
}

@Composable
private fun Progress(r: Rec, onOpen: () -> Unit, onDrop: () -> Unit) {
    val p = localpal.current
    val frac = if (r.dur > 0) (r.pos.toFloat() / r.dur.toFloat()).coerceIn(0f, 1f) else 0f
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .clip(cut(12.dp))
            .clickable(onClick = onOpen)
            .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.width(128.dp).height(74.dp).background(p.lift)) {
            Pic(r.cover, Modifier.fillMaxSize(), shape = cut(10.dp))
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)))
                )
            )
            Box(
                Modifier.align(Alignment.Center).size(32.dp).clip(cut(7.dp)).background(Color(0xD908080B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(19.dp))
            }
            if (frac > 0f) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp).background(Color(0x59FFFFFF))) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(p.acc))
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                r.title,
                color = p.text,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${fmt.ep(r.ep)}  •  ${fmt.clock(r.pos)} of ${if (r.dur > 0) fmt.clock(r.dur) else "--:--"}",
                color = p.mute,
                fontSize = 11.sp
            )
            Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(p.line)) {
                Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(CircleShape).background(p.acc))
            }
        }
        Circle(Icons.Rounded.Close, "Remove", 30.dp) { onDrop() }
    }
}

@Composable
private fun Saved(r: Rec, onOpen: () -> Unit, onDrop: () -> Unit) {
    val p = localpal.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .clip(cut(12.dp))
            .clickable(onClick = onOpen)
            .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(58.dp, 82.dp)) {
            Pic(r.cover, Modifier.fillMaxSize(), shape = cut(9.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                r.title,
                color = p.text,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (r.stat.isNotBlank()) Text(
                    r.stat.uppercase(),
                    color = p.acc,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.9.sp
                )
                Text(
                    listOfNotNull(
                        (r.total ?: 0).takeIf { it > 0 }?.let { "$it episodes" },
                        if (r.ep > 0) "Stopped at ${r.ep}" else null
                    ).joinToString("  •  ").ifBlank { "Saved" },
                    color = p.mute,
                    fontSize = 11.sp
                )
            }
        }
        Circle(Icons.Rounded.Close, "Remove", 30.dp) { onDrop() }
    }
}

@Composable
private fun Move(onClose: () -> Unit) {
    val p = localpal.current
    val ctx = LocalContext.current
    var user by remember { mutableStateOf("") }
    var paste by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun push(text: String) {
        runCatching {
            ctx.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Ominime list")
                        putExtra(Intent.EXTRA_TEXT, text)
                    },
                    "Export your list"
                )
            )
        }
    }

    Sheet("Your list", "Move it in or out", 0.84f, onClose) {
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Export", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                "Share every saved title and its status as JSON, so you can keep a copy anywhere.",
                color = p.mute,
                fontSize = 11.sp
            )
            Btn("Export my list", main = true) {
                push(store.export())
            }

            Spacer(Modifier.height(4.dp))
            Text("Import from AniList", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("Your AniList profile name, and the list must be public.", color = p.mute, fontSize = 11.sp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(cut(9.dp))
                    .background(p.glass)
                    .border(0.8.dp, p.line, cut(9.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                if (user.isEmpty()) Text("AniList username", color = p.mute, fontSize = 12.5.sp)
                androidx.compose.foundation.text.BasicTextField(
                    value = user,
                    onValueChange = { user = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = p.text, fontSize = 12.5.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(p.acc),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(if (busy) "Working" else "Import AniList") {
                    if (busy || user.isBlank()) return@Btn
                    busy = true
                    note = null
                    scope.launch {
                        val got = runCatching { com.ominime.app.data.animelist(user.trim()) }
                        got.onSuccess { rows ->
                            store.bulk(rows)
                            note = if (rows.isEmpty()) "Nothing public found" else "${rows.size} titles added"
                        }.onFailure { note = it.message ?: "Import failed" }
                        busy = false
                    }
                }
                Btn("Paste JSON") {
                    val rows = runCatching { store.decode(paste) }.getOrDefault(emptyList())
                    if (rows.isEmpty()) {
                        note = "Nothing to read"
                    } else {
                        store.bulk(rows)
                        note = "${rows.size} titles added"
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(cut(9.dp))
                    .background(p.glass)
                    .border(0.8.dp, p.line, cut(9.dp))
                    .padding(10.dp)
            ) {
                if (paste.isEmpty()) Text("Paste an Ominime JSON export here", color = p.mute, fontSize = 11.sp)
                androidx.compose.foundation.text.BasicTextField(
                    value = paste,
                    onValueChange = { paste = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = p.text, fontSize = 11.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(p.acc),
                    modifier = Modifier.fillMaxSize()
                )
            }
            note?.let {
                Text(it, color = p.acc2, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
