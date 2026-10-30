package com.ominime.app.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ominime.app.BuildConfig
import com.ominime.app.data.Ep
import com.ominime.app.data.Media
import com.ominime.app.data.Stream
import com.ominime.app.data.langs

@Composable
fun EpPanel(
    m: Media?,
    list: List<Ep>,
    cur: Int,
    land: FocusRequester? = null,
    onPick: (Int) -> Unit,
    onClose: () -> Unit
) {
    val p = localpal.current
    val l = rememberLazyListState()
    LaunchedEffect(land) {
        if (land != null && BuildConfig.tv) runCatching { land.requestFocus() }
    }
    LaunchedEffect(cur) {
        val at = list.indexOfFirst { it.num == cur }
        if (at >= 0) runCatching { l.scrollToItem(at) }
    }
    Side(
        "Episodes",
        if (list.isEmpty()) "nothing loaded" else "${list.size}  •  ${m?.title ?: ""}",
        onClose = onClose
    ) {
        LazyColumn(Modifier.weight(1f), state = l, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            items(list.size, key = { list[it].num }) { i ->
                val e = list[i]
                val on = e.num == cur
                val shape = cut(9.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .then(if (i == 0 && land != null) Modifier.focusRequester(land) else Modifier)
                        .clip(shape)
                        .background(if (on) p.acc.copy(alpha = 0.14f) else p.glass)
                        .border(0.7.dp, if (on) p.acc.copy(alpha = 0.55f) else Color.Transparent, shape)
                        .press(0.97f) { onPick(e.num) }
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(Modifier.width(74.dp).height(44.dp)) {
                        Pic(e.img, Modifier.fillMaxSize(), shape = cut(6.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            e.title ?: "Episode ${e.num}",
                            color = if (on) p.text else p.text.copy(alpha = 0.88f),
                            fontSize = 12.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "EP ${e.num}",
                                color = if (on) p.acc else p.mute,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                            if (e.filler) Text("FILLER", color = p.acc2, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
                            if (e.dub) Text("DUB", color = p.acc, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
                        }
                    }
                    Icon(
                        if (on) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                        null,
                        tint = if (on) p.acc else p.mute,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SrcPanel(
    list: List<Stream>,
    at: Int,
    prov: String,
    audio: String,
    land: FocusRequester? = null,
    onPick: (Int) -> Unit,
    onAudio: (String) -> Unit,
    onClose: () -> Unit
) {
    val p = localpal.current
    val langsOn = remember(list) { list.getOrNull(at)?.subs?.map { langs.flag(it.lang) }?.distinct() ?: emptyList() }
    Side(
        "Servers",
        if (list.isEmpty()) "nothing answered" else "${list.size} source${if (list.size == 1) "" else "s"}  •  ${audio.uppercase()}",
        onClose = onClose
    ) {
        if (list.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Seg(listOf("Sub", "Dub"), if (audio == "dub") 1 else 0, Modifier.weight(1f)) { i ->
                    onAudio(if (i == 1) "dub" else "sub")
                }
            }
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(list.size, key = { list[it].url }) { i ->
                val s = list[i]
                val on = i == at
                val shape = cut(10.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .then(if (i == 0 && land != null) Modifier.focusRequester(land) else Modifier)
                        .clip(shape)
                        .background(if (on) p.acc.copy(alpha = 0.12f) else p.glass)
                        .border(0.8.dp, if (on) p.acc.copy(alpha = 0.55f) else p.line, shape)
                        .press(0.97f) { onPick(i) }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(cut(7.dp))
                            .background(if (on) p.acc else p.lift)
                            .border(0.7.dp, if (on) Color.Transparent else p.line, cut(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${i + 1}",
                            color = if (on) Color(0xFF0A0A0C) else p.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                if (s.server.isBlank()) "Source ${i + 1}" else s.server,
                                color = p.text,
                                fontSize = 12.5.sp,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (on) Text(
                                "PLAYING",
                                color = p.acc,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.9.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                s.kind.uppercase(),
                                color = p.mute,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.7.sp
                            )
                            if (s.subs.isEmpty()) Text(
                                "NO SUBS",
                                color = p.mute.copy(alpha = 0.6f),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.7.sp
                            )
                            s.subs.map { langs.flag(it.lang) }.distinct().take(5).forEach { tag ->
                                Text(
                                    tag,
                                    color = p.acc2.copy(alpha = 0.85f),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                    Icon(
                        if (on) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                        null,
                        tint = if (on) p.acc else p.mute,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
        if (langsOn.isNotEmpty()) {
            Text(
                "Subtitles equipped for this source: " + langsOn.joinToString("  "),
                color = p.mute.copy(alpha = 0.7f),
                fontSize = 9.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
