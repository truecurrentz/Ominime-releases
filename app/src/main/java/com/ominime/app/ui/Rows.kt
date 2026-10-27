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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ominime.app.data.Cast
import com.ominime.app.data.Media
import com.ominime.app.data.Rec

@Composable
fun Shelf(title: String, list: List<Media>, onOpen: (Int) -> Unit, onAll: (() -> Unit)? = null) {
    if (list.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Head(title, action = if (onAll != null) "See all" else null, onAction = { onAll?.invoke() })
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(list.size, key = { list[it].id }) { i ->
                val m = list[i]
                Reveal(delay = (i % 6) * 40) { Poster(m) { onOpen(m.id) } }
            }
        }
    }
}

@Composable
fun GridShelf(title: String, list: List<Media>, onOpen: (Int) -> Unit) {
    if (list.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Head(title)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(list.size, key = { list[it].id }) { i ->
                val m = list[i]
                Reveal(delay = (i % 6) * 40) {
                    Box(Modifier.size(150.dp, 214.dp)) { Tile(m) { onOpen(m.id) } }
                }
            }
        }
    }
}

@Composable
fun WideShelf(title: String, list: List<Media>, onOpen: (Int) -> Unit, onAll: (() -> Unit)? = null) {
    if (list.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Head(title, action = if (onAll != null) "See all" else null, onAction = { onAll?.invoke() })
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(list.size, key = { list[it].id }) { i ->
                val m = list[i]
                Reveal(delay = (i % 5) * 40) { Wide(m) { onOpen(m.id) } }
            }
        }
    }
}

@Composable
fun RankShelf(title: String, list: List<Media>, onOpen: (Int) -> Unit) {
    if (list.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Head(title)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(minOf(list.size, 10)) { i ->
                val m = list[i]
                Reveal(delay = (i % 5) * 40) { Ranked(m, i + 1) { onOpen(m.id) } }
            }
        }
    }
}

@Composable
fun ResumeShelf(title: String, list: List<Rec>, onOpen: (Rec) -> Unit, action: String? = null, onAction: () -> Unit = {}) {
    if (list.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Head(title, action = action, onAction = onAction)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(list.size, key = { list[it].id }) { i ->
                Reveal(delay = (i % 5) * 40) { Resume(list[i]) { onOpen(list[i]) } }
            }
        }
    }
}

@Composable
fun CastShelf(list: List<Cast>) {
    if (list.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Head("Characters")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(list.size) { i ->
                val c = list[i]
                Person(c.name, c.va ?: "", c.img)
            }
        }
    }
}

@Composable
fun Chips(list: List<String>, pick: String?, onPick: (String) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        items(list.size, key = { list[it] }) { i ->
            val one = list[i]
            Label(one, sel = pick == one) { onPick(one) }
        }
    }
}

@Composable
fun Genres(pick: String?, onPick: (String) -> Unit) {
    Chips(listOf("All") + com.ominime.app.data.repo.genres, pick ?: "All", onPick)
}

@Composable
fun Banner(title: String, note: String, action: String, onAction: () -> Unit) {
    val p = localpal.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(cut(16.dp))
            .background(
                Brush.linearGradient(listOf(p.acc.copy(alpha = 0.16f), p.glass))
            )
            .border(0.7.dp, p.line, cut(16.dp))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(note, color = p.mute, fontSize = 12.sp, lineHeight = 17.sp)
            Text(
                action,
                color = p.text,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable(onClick = onAction)
            )
        }
    }
}

private val tones = listOf(
    Color(0xFF6D5BFF) to Color(0xFF3A2BD6),
    Color(0xFF21C08B) to Color(0xFF0D8A63),
    Color(0xFFFF7A45) to Color(0xFFD9412B),
    Color(0xFF3E9CFF) to Color(0xFF1F5FD0),
    Color(0xFFE85D9C) to Color(0xFFB32B6B),
    Color(0xFF00C2C7) to Color(0xFF0A8B94),
    Color(0xFFF5B93F) to Color(0xFFCE7E14),
    Color(0xFF9B6BFF) to Color(0xFF6D3AD6)
)

@Composable
fun GenresTile(name: String, count: String, tone: Int, art: String?, mod: Modifier = Modifier, click: () -> Unit) {
    val pair = tones[((tone % tones.size) + tones.size) % tones.size]
    Box(
        mod
            .height(116.dp)
            .clip(cut(16.dp))
            .background(Brush.linearGradient(listOf(pair.first, pair.second)))
            .clickable(onClick = click)
    ) {
        if (art != null) {
            Pic(art, Modifier.fillMaxSize(), shape = cut(16.dp))
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        listOf(pair.second.copy(alpha = 0.96f), pair.first.copy(alpha = 0.62f), Color.Transparent)
                    )
                )
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, pair.second.copy(alpha = 0.55f))
                    )
                )
            )
        } else {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, top = 34.dp)
                    .size(42.dp)
                    .clip(cut(10.dp))
                    .background(Color(0x33000000))
            )
        }
        Box(Modifier.fillMaxSize().border(0.7.dp, Color(0x1FFFFFFF), cut(16.dp)))
        Column(Modifier.padding(14.dp)) {
            Text(
                name.uppercase(),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.1.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                count.uppercase(),
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp
            )
        }
    }
}

@Composable
fun Band(text: String, mod: Modifier = Modifier) {
    val p = localpal.current
    Row(mod.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(p.acc2))
        Text(text.uppercase(), color = p.mute, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
    }
}
