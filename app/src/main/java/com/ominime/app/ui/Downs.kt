package com.ominime.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ominime.app.data.Ep
import com.ominime.app.data.Media
import com.ominime.app.data.Shelf
import com.ominime.app.data.langs
import com.ominime.app.data.repo
import com.ominime.app.data.store
import com.ominime.app.data.offline
import java.util.Locale

fun mb(n: Long): String {
    if (n <= 0) return "0 MB"
    val m = n / 1048576.0
    return when {
        m >= 1024 -> String.format(Locale.US, "%.2f GB", m / 1024)
        m >= 100 -> String.format(Locale.US, "%.0f MB", m)
        else -> String.format(Locale.US, "%.1f MB", m)
    }
}

@Composable
fun Downs(
    onPlay: (Int, Int) -> Unit,
    onBrowse: () -> Unit
) {
    val p = localpal.current
    val rows by offline.live.collectAsState()
    val list = rememberLazyListState()
    track(list)
    var sort by remember { mutableStateOf(0) }
    var only by remember { mutableStateOf(0) }
    val shelves = remember(rows, sort, only) {
        val base = offline.shelf(rows)
        val pick = when (only) {
            1 -> base.mapNotNull { s -> s.copy(rows = s.rows.filter { it.state != "done" }).takeIf { it.rows.isNotEmpty() } }
            2 -> base.mapNotNull { s -> s.copy(rows = s.rows.filter { it.state == "done" }).takeIf { it.rows.isNotEmpty() } }
            else -> base
        }
        when (sort) {
            1 -> pick.sortedByDescending { it.size }
            2 -> pick.sortedByDescending { it.done }
            else -> pick
        }
    }
    val used = rows.filter { it.state == "done" }.sumOf { it.size }
    val busy = rows.count { it.state == "run" || it.state == "wait" }

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 54.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "DOWNLOADS",
                    color = p.text,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    if (rows.isEmpty()) "Nothing saved yet" else rows.size.toString() + " episodes  •  " + mb(used) + " on device",
                    color = p.mute,
                    fontSize = 11.sp
                )
            }
            if (rows.isNotEmpty()) {
                Chip(if (busy > 0) "Pause all" else "Resume") {
                    if (busy > 0) offline.holdAll() else offline.resumAll()
                }
            }
        }

        if (rows.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                listOf("All", "Active", "Saved").forEachIndexed { i, name ->
                    Label(name, sel = only == i) { only = i }
                }
                Spacer(Modifier.weight(1f))
                Label(if (sort == 0) "Newest" else if (sort == 1) "Size" else "Progress") {
                    sort = (sort + 1) % 3
                }
            }
        }

        if (shelves.isEmpty()) {
            Empty(rows.isEmpty(), onBrowse)
            return@Column
        }

        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 132.dp)
        ) {
            items(shelves.size, key = { shelves[it].id }) { i ->
                Folder(shelves[i], onPlay)
            }
            item(key = "tail") {
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.Center) {
                    Chip("Delete everything") { offline.wipe() }
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, click: () -> Unit) {
    val p = localpal.current
    Box(
        Modifier
            .clip(cut(8.dp))
            .background(p.glass)
            .border(0.8.dp, p.line, cut(8.dp))
            .press { click() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text.uppercase(), color = p.text, fontSize = 9.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    }
}

@Composable
private fun Empty(none: Boolean, onBrowse: () -> Unit) {
    val p = localpal.current
    Column(
        Modifier.fillMaxWidth().padding(top = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(74.dp)
                .clip(cut(16.dp))
                .background(p.card)
                .border(0.9.dp, p.line, cut(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Download, null, tint = p.acc, modifier = Modifier.size(32.dp))
        }
        Text(
            if (none) "No downloads yet" else "Nothing matches this filter",
            color = p.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (none) "Pick a title, tap download, choose the episodes you want and they land here as offline folders." else "Try another filter.",
            color = p.mute,
            fontSize = 11.5.sp,
            modifier = Modifier.padding(horizontal = 26.dp)
        )
        if (none) Btn("Browse titles", main = true) { onBrowse() }
    }
}

@Composable
private fun Folder(s: Shelf, onPlay: (Int, Int) -> Unit) {
    val p = localpal.current
    var open by remember { mutableStateOf(true) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(cut(12.dp))
            .background(p.card)
            .border(0.9.dp, p.line, cut(12.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                Modifier
                    .width(46.dp)
                    .aspectRatio(0.7f)
                    .clip(cut(7.dp))
                    .background(p.lift)
            ) {
                AsyncImage(
                    model = s.cover,
                    contentDescription = s.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    s.title,
                    color = p.text,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Folder, null, tint = p.acc, modifier = Modifier.size(12.dp))
                    Text(
                        s.done.toString() + " of " + s.rows.size + " ep  •  " + mb(s.size),
                        color = p.mute,
                        fontSize = 10.5.sp
                    )
                    if (s.busy) Text("DOWNLOADING", color = p.acc, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                }
            }
            Circle(Icons.Rounded.DeleteOutline, "Delete folder", 34.dp) { offline.dropShelf(s.id) }
            Circle(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, "Toggle", 34.dp) { open = !open }
        }

        if (open) {
            s.rows.forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 11.dp)
                        .padding(bottom = 9.dp)
                        .clip(cut(8.dp))
                        .background(p.lift.copy(alpha = 0.55f))
                        .press(0.98f) {
                            if (row.state == "done") onPlay(s.id, row.ep) else offline.resume(row.key)
                        }
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (row.state == "done") p.acc.copy(alpha = 0.15f) else Color(0x14FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (row.state) {
                            "done" -> Icon(Icons.Rounded.Check, null, tint = p.acc, modifier = Modifier.size(15.dp))
                            "run" -> Icon(Icons.Rounded.Pause, null, tint = p.text, modifier = Modifier.size(14.dp))
                            "fail" -> Icon(Icons.Rounded.Download, null, tint = Color(0xFFFF6B63), modifier = Modifier.size(14.dp))
                            else -> Icon(Icons.Rounded.PlayArrow, null, tint = p.mute, modifier = Modifier.size(14.dp))
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("EP " + row.ep, color = p.text, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            if (row.subs.isNotEmpty()) Text(
                                "  " + row.subs.take(3).joinToString(" ") { it.uppercase() },
                                color = p.acc.copy(alpha = 0.85f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                when (row.state) {
                                    "done" -> mb(row.size)
                                    "fail" -> "Failed"
                                    "hold" -> "Paused"
                                    "wait" -> "Queued"
                                    else -> (row.frac * 100).toInt().toString() + "%"
                                },
                                color = if (row.state == "fail") Color(0xFFFF6B63) else p.mute,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(cut(2.dp))
                                .background(Color(0x1FFFFFFF))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(row.frac.coerceIn(0f, 1f))
                                    .height(3.dp)
                                    .clip(cut(2.dp))
                                    .background(if (row.state == "done") p.acc else p.acc2)
                            )
                        }
                    }
                    if (row.state == "run" || row.state == "wait") {
                        Circle(Icons.Rounded.Pause, "Pause", 30.dp) { offline.pause(row.key) }
                    }
                    Circle(Icons.Rounded.DeleteOutline, "Delete", 30.dp) { offline.drop(row.key) }
                }
            }
        }
    }
}

@Composable
fun Float(title: String, sub: String, onClose: () -> Unit, body: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val p = localpal.current
    val card = cut(18.dp, 0.18f)
    val pop = remember { androidx.compose.animation.core.Animatable(0.94f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.62f, stiffness = 420f))
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Box(
            Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 34.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .widthIn(max = 430.dp)
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = pop.value }
                    .clip(card)
                    .background(p.card)
                    .background(Brush.verticalGradient(listOf(p.lift, p.card, p.bg)))
                    .border(1.dp, p.acc.copy(alpha = 0.55f), card)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .size(5.dp, 34.dp)
                            .clip(cut(3.dp))
                            .background(Brush.verticalGradient(listOf(p.acc, p.acc2)))
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            title.uppercase(),
                            color = p.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.3.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            sub,
                            color = p.mute,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(cut(8.dp))
                            .background(p.glass)
                            .border(0.8.dp, p.line, cut(8.dp))
                            .press(0.94f) { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Close, "Close", tint = p.text.copy(alpha = 0.75f), modifier = Modifier.size(13.dp))
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Brush.horizontalGradient(listOf(p.acc.copy(alpha = 0.55f), Color.Transparent)))
                )
                body()
            }
        }
    }
}

@Composable
fun Batch(m: Media, eps: List<Ep>, onClose: () -> Unit) {
    val p = localpal.current
    var rows by remember(m.id) { mutableStateOf(eps.filter { it.num > 0 }) }
    var loading by remember(m.id) { mutableStateOf(rows.isEmpty()) }
    var pick by remember(m.id) { mutableStateOf<Set<Int>>(emptySet()) }
    var sel by remember(m.id) { mutableStateOf(offline.want()) }
    var found by remember(m.id) { mutableStateOf<List<String>?>(null) }
    val held = offline.all.filter { it.id == m.id }.associateBy { it.ep }

    LaunchedEffect(m.id) {
        if (rows.isEmpty()) {
            rows = runCatching { repo.eps(m).first }.getOrDefault(emptyList()).filter { it.num > 0 }
        }
        loading = false
    }

    LaunchedEffect(m.id, rows.size) {
        val first = rows.firstOrNull()?.num ?: return@LaunchedEffect
        found = runCatching {
            val one = repo.play(m, store.conf.audio, first)
            one.streams.flatMap { it.subs }.map { langs.norm(it.lang) }.filter { it.isNotBlank() }.distinct().sorted()
        }.getOrNull() ?: emptyList()
    }

    Float(
        "Batch download",
        m.title + "  •  " + rows.size + " aired episodes",
        onClose
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Label("All aired", sel = rows.isNotEmpty() && pick.size == rows.size) { pick = rows.map { it.num }.toSet() }
            Label("First 12", sel = false) { pick = rows.map { it.num }.take(12).toSet() }
            Label("Latest 12", sel = false) { pick = rows.map { it.num }.takeLast(12).toSet() }
            Label("None", sel = pick.isEmpty()) { pick = emptySet() }
        }

        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Subtitles, null, tint = p.acc, modifier = Modifier.size(13.dp))
                Text("SUBTITLES", color = p.mute, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    when {
                        sel.isEmpty() -> "NOT SAVED"
                        sel.size == 1 -> langs.name(sel.first()).uppercase()
                        else -> sel.size.toString() + " LANGUAGES"
                    },
                    color = if (sel.isEmpty()) p.mute else p.acc,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            val box = found
            when {
                box == null -> Text("Reading available subtitles…", color = p.mute, fontSize = 10.5.sp)
                box.isEmpty() -> Text("This provider ships no subtitles for the show.", color = p.mute, fontSize = 10.5.sp)
                else -> Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    box.take(5).forEach { code ->
                        val on = sel.contains(code)
                        Label(if (on) langs.name(code) + "  ✓" else langs.name(code), sel = on) {
                            sel = if (on) sel - code else sel + code
                            store.conf(store.conf.copy(dsubs = sel.joinToString(",")))
                        }
                    }
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(p.line)
        )

        when {
            loading -> Box(Modifier.fillMaxWidth().padding(vertical = 26.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Loader(size = 16.dp)
                    Text("Looking for episodes", color = p.mute, fontSize = 11.sp)
                }
            }
            rows.isEmpty() -> Column(
                Modifier.fillMaxWidth().padding(vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("No episodes yet", color = p.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "The providers did not return an episode list for this title.",
                    color = p.mute,
                    fontSize = 10.5.sp
                )
            }
            else -> LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items((rows.size + 5) / 6, key = { it }) { band ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        rows.drop(band * 6).take(6).forEach { e ->
                            val on = pick.contains(e.num)
                            val have = held[e.num]
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(cut(7.dp))
                                    .background(
                                        when {
                                            have?.state == "done" -> p.acc.copy(alpha = 0.22f)
                                            on -> p.acc.copy(alpha = 0.15f)
                                            else -> p.glass
                                        }
                                    )
                                    .border(
                                        0.8.dp,
                                        when {
                                            on || have?.state == "done" -> p.acc.copy(alpha = 0.6f)
                                            else -> p.line
                                        },
                                        cut(7.dp)
                                    )
                                    .press(0.94f) {
                                        pick = if (on) pick - e.num else pick + e.num
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    e.num.toString(),
                                    color = if (on || have?.state == "done") p.acc else p.text,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                                when (have?.state) {
                                    "done" -> Icon(
                                        Icons.Rounded.Check,
                                        null,
                                        tint = p.acc,
                                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 3.dp).size(9.dp)
                                    )
                                    "run", "wait" -> Icon(
                                        Icons.Rounded.Download,
                                        null,
                                        tint = p.acc,
                                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 3.dp).size(9.dp)
                                    )
                                    else -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        pick.isEmpty() -> "Nothing selected"
                        pick.size == 1 -> "1 episode"
                        else -> pick.size.toString() + " episodes"
                    },
                    color = if (pick.isEmpty()) p.mute else p.text,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (sel.isEmpty()) "subtitles off" else "subs: " + sel.joinToString(", "),
                    color = p.mute,
                    fontSize = 9.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Btn("Download", icon = Icons.Rounded.Download, main = pick.isNotEmpty()) {
                if (pick.isEmpty()) return@Btn
                offline.grabMany(m, pick.sorted(), sel)
                onClose()
            }
        }
    }
}
