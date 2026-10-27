package com.ominime.app.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ominime.app.data.Ep
import com.ominime.app.data.Media
import com.ominime.app.data.repo
import com.ominime.app.data.store
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun Detail(
    id: Int,
    onBack: () -> Unit,
    onWatch: (Int, Int) -> Unit,
    onOpen: (Int) -> Unit
) {
    val p = localpal.current
    val ctx = LocalContext.current
    var m by remember { mutableStateOf(repo.quick(id)) }
    var note by remember { mutableStateOf<String?>(null) }
    var eps by remember { mutableStateOf<List<Ep>>(emptyList()) }
    var open by remember { mutableStateOf(false) }
    var grid by remember { mutableStateOf(true) }
    var all by remember { mutableStateOf(false) }
    var first by remember { mutableStateOf(true) }
    val d by store.live.collectAsState()
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val saved = d.list.any { it.id == id }
    val mark = d.hist.firstOrNull { it.id == id }

    LaunchedEffect(id) {
        runCatching { repo.detail(id) }
            .onSuccess { m = it }
            .onFailure { note = it.message ?: "Could not load this title" }
        first = false
    }

    LaunchedEffect(id, m?.id) {
        val md = m ?: return@LaunchedEffect
        runCatching { repo.eps(md) }.onSuccess { pair -> eps = pair.first }
    }

    fun share() {
        runCatching {
            ctx.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "https://anilist.co/anime/$id")
                    },
                    "Share anime"
                )
            )
        }
    }

    var stat by remember { mutableStateOf("") }
    var track by remember { mutableStateOf(false) }

    LaunchedEffect(saved, d.list) {
        stat = d.list.firstOrNull { it.id == id }?.stat.orEmpty()
    }

    fun toggle() {
        val mm = m ?: return
        if (saved) {
            track = true
        } else {
            store.add(repo.rec(mm))
            track = true
        }
    }

    Box(Modifier.fillMaxSize().background(p.bg)) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item(key = "stage") {
                Stage(
                    m = m,
                    saved = saved,
                    onBack = onBack,
                    onShare = { share() },
                    onSave = { toggle() }
                )
            }

            item(key = "act") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(cut(10.dp))
                            .background(p.acc)
                            .clickable {
                                val at = if (mark != null && mark.ep > 0) mark.ep else 1
                                onWatch(id, at)
                            }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF0A0A0C), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (mark != null && mark.ep > 0) "RESUME  •  EP ${mark.ep}" else "WATCH NOW",
                            color = Color(0xFF0A0A0C),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.1.sp
                        )
                    }
                    Circle(
                        if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        "Save",
                        50.dp
                    ) { toggle() }
                    Circle(Icons.Rounded.Share, "Share", 50.dp) { share() }
                }
            }

            item(key = "about") {
                Column(Modifier.padding(horizontal = 20.dp).padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val mm = m
                    if (mm != null && mm.genres.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(mm.genres.size, key = { mm.genres[it] }) { i ->
                                Label(mm.genres[i]) { }
                            }
                        }
                    }
                    if (mm != null && !mm.desc.isNullOrBlank()) {
                        Column {
                            Text(
                                mm.desc,
                                color = p.mute,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                maxLines = if (open) 60 else 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (open) "Show less" else "Read more",
                                color = p.text,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 7.dp).clickable { open = !open }
                            )
                        }
                    }
                }
            }

            item(key = "info") {
                val mm = m ?: return@item
                Column(Modifier.padding(horizontal = 20.dp).padding(top = 14.dp)) { Facts(mm) }
            }

            item(key = "ephead") {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 22.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(3.dp, 17.dp).clip(CircleShape).background(p.acc))
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "Episodes",
                        color = p.text,
                        fontSize = 19.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (eps.isEmpty()) "loading" else "${eps.size}",
                        color = p.mute,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Circle(
                        if (grid) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                        "Layout",
                        34.dp
                    ) { grid = !grid }
                }
            }

            if (eps.isEmpty() && !first) {
                item(key = "blank") {
                    Blank(
                        "No episodes found",
                        "The sources did not return an episode list for this title.",
                        icon = Icons.Rounded.PlayArrow
                    )
                }
            }

            val avail = eps
            val shown = (if (all) avail else avail.take(30))
            if (grid) {
                item(key = "grid") {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        shown.chunked(5).forEach { line ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                line.forEach { e ->
                                    val on = mark != null && mark.ep == e.num
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .clip(cut(7.dp))
                                            .background(if (on) p.acc else p.glass)
                                            .border(0.8.dp, if (on) Color.Transparent else p.line, cut(7.dp))
                                            .press(0.93f) { onWatch(id, e.num) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${e.num}",
                                            color = if (on) Color(0xFF0A0A0C) else p.text,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        if (e.filler) Box(
                                            Modifier.align(Alignment.TopEnd).padding(3.dp)
                                                .size(4.dp).clip(CircleShape).background(p.acc2)
                                        )
                                    }
                                }
                                repeat(5 - line.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            } else {
                items(shown.size, key = { shown[it].num }) { i ->
                    val e = shown[i]
                    Reveal(delay = (i % 6) * 30) {
                        EpRow(e, mark, { onWatch(id, e.num) })
                    }
                }
            }

            if (!all && avail.size > 30) {
                item(key = "more") {
                    Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.Center) {
                        Btn("Show all ${avail.size} episodes") { all = true }
                    }
                }
            }

            val mm = m
            if (mm != null) {
                if (mm.cast.isNotEmpty()) item(key = "cast") {
                    Spacer(Modifier.height(8.dp))
                    CastShelf(mm.cast)
                }
                if (mm.rel.isNotEmpty()) item(key = "rel") {
                    Shelf("Related", mm.rel.map { it.toMedia() }, onOpen)
                }
                if (mm.recs.isNotEmpty()) item(key = "recs") {
                    Shelf("More like this", mm.recs.map { it.toMedia() }, onOpen)
                }
            }

            if (note != null && m == null) {
                item(key = "err") {
                    Oops(note ?: "") { scope.launch { runCatching { repo.detail(id, force = true) }.onSuccess { m = it } } }
                }
            }
        }

        val lift = list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 260
        AnimatedVisibility(visible = lift, enter = fadeIn(tween(200)), exit = fadeOut(tween(160))) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(p.bg.copy(alpha = 0.96f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Circle(Icons.Rounded.ArrowBack, "Back", 38.dp) { onBack() }
                Spacer(Modifier.width(12.dp))
                Text(
                    m?.title ?: "",
                    color = p.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Circle(if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, "Save", 38.dp) { toggle() }
            }
        }

        if (track) {
            Stat(
                current = stat,
                saved = saved,
                title = m?.title ?: m?.romaji.orEmpty(),
                cover = m?.cover,
                episode = mark?.ep ?: 0,
                onPick = { key ->
                    runCatching {
                        val mm = m
                        if (mm != null && !store.has(id)) store.add(repo.rec(mm))
                        store.mark(id, key)
                    }
                    stat = key
                    track = false
                },
                onClose = { track = false },
                onRemove = {
                    store.remove(id)
                    stat = ""
                    track = false
                }
            )
        }
    }
}

@Composable
private fun Stage(
    m: Media?,
    saved: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    val p = localpal.current
    Box(Modifier.fillMaxWidth().height(468.dp)) {
        if (m == null) {
            Shine(Modifier.fillMaxSize(), rad = 0.dp)
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(m.banner ?: m.cover).crossfade(300).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp, bottom = 132.dp)
                    .width(120.dp)
                    .aspectRatio(0.68f)
                    .clip(cut(14.dp))
                    .border(0.8.dp, p.line, cut(14.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(m.cover).crossfade(300).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to p.bg.copy(alpha = 0.72f),
                    0.18f to p.bg.copy(alpha = 0.28f),
                    0.42f to p.bg.copy(alpha = 0.55f),
                    0.74f to p.bg.copy(alpha = 0.94f),
                    1f to p.bg
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(p.bg.copy(alpha = 0.86f), Color.Transparent))
            )
        )
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Circle(Icons.Rounded.ArrowBack, "Back", 40.dp) { onBack() }
            Spacer(Modifier.weight(1f))
            Circle(Icons.Rounded.Share, "Share", 40.dp) { onShare() }
            Spacer(Modifier.width(9.dp))
            Circle(if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, "Save", 40.dp) { onSave() }
        }
        Column(
            Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp).padding(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (m != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(fmt.tone(m.state)))
                        Text(fmt.upper(m.state), color = p.text.copy(alpha = 0.8f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    if ((m.score ?: 0) > 0) Text(
                        String.format(Locale.US, "%.1f", (m.score ?: 0) / 10f),
                        color = p.acc2,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if ((m.rank ?: 0) > 0) Text("#${m.rank}", color = p.mute, fontSize = 11.sp)
                }
                Text(
                    m.title.ifBlank { m.romaji },
                    color = p.text,
                    fontSize = 29.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (m.romaji.isNotBlank() && m.romaji != m.title) {
                    Text(m.romaji, color = p.mute, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Mini(fmt.upper(m.fmt))
                    Mini("${m.year ?: "-"}")
                    Mini(
                        when {
                            (m.total ?: 0) > 0 && (m.state == "RELEASING") -> "${repo.aired(m)}/${m.total} EP"
                            (m.total ?: 0) > 0 -> "${m.total} EP"
                            else -> "EP ?"
                        }
                    )
                    Mini(if ((m.dur ?: 0) > 0) "${m.dur}m" else "TV")
                }
            }
        }
    }
}

@Composable
private fun Mini(text: String) {
    val p = localpal.current
    Text(
        text.uppercase(),
        color = p.text.copy(alpha = 0.9f),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        modifier = Modifier
            .clip(cut(5.dp))
            .background(p.glass)
            .border(0.7.dp, p.line, cut(5.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}

@Composable
private fun Facts(m: Media) {
    val p = localpal.current
    val pairs = listOfNotNull(
        "Studio" to (m.studio ?: "-"),
        "Season" to (fmt.upper(m.season) + " " + (m.year ?: "")).trim().ifBlank { "-" },
        "Status" to fmt.upper(m.state),
        "Popularity" to ((m.pop ?: 0).takeIf { it > 0 }?.let { n -> String.format(Locale.US, "%,d", n) } ?: "-")
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(cut(16.dp))
            .background(p.glass)
            .border(0.8.dp, p.line, cut(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        pairs.chunked(2).forEach { line ->
            Row(Modifier.fillMaxWidth()) {
                line.forEach { pair ->
                    Column(Modifier.weight(1f)) {
                        Text(
                            pair.first.uppercase(),
                            color = p.mute.copy(alpha = 0.75f),
                            fontSize = 9.sp,
                            letterSpacing = 0.9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            pair.second.ifBlank { "-" },
                            color = p.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (line.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (m.tags.isNotEmpty()) {
            Divider()
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                m.tags.take(3).forEach { Mini(it) }
            }
        }
    }
}

@Composable
private fun EpRow(e: Ep, mark: com.ominime.app.data.Rec?, onClick: () -> Unit) {
    val p = localpal.current
    val prog = if (mark != null && mark.ep == e.num && mark.dur > 0) (mark.pos.toFloat() / mark.dur.toFloat()).coerceIn(0f, 1f) else 0f
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .clip(cut(12.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.width(132.dp).height(74.dp)) {
            Pic(e.img, Modifier.fillMaxSize(), shape = cut(10.dp))
            if (e.filler) Text(
                "FILLER",
                color = Color(0xFF0A0A0C),
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.4.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .clip(cut(3.dp))
                    .background(p.acc2)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )
            if (prog > 0f) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp).background(Color(0x59FFFFFF))) {
                    Box(Modifier.fillMaxWidth(prog).fillMaxHeight().background(p.acc))
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "EP ${e.num}",
                    color = if (prog > 0f) p.acc else p.mute,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            Text(
                e.title ?: "Episode ${e.num}",
                color = p.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!e.desc.isNullOrBlank()) Text(
                e.desc,
                color = p.mute,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Rounded.PlayArrow, null, tint = p.mute, modifier = Modifier.size(18.dp))
    }
}

private fun com.ominime.app.data.Rel.toMedia(): Media = Media(
    id = id,
    title = title,
    cover = cover,
    fmt = fmt,
    score = score
)
