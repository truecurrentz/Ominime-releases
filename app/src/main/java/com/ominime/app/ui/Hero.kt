package com.ominime.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ominime.app.data.Media
import kotlinx.coroutines.delay

@Composable
fun Hero(list: List<Media>, onOpen: (Int) -> Unit, onPlay: (Int, Int) -> Unit) {
    if (list.isEmpty()) return
    val pager = rememberPagerState(pageCount = { list.size })
    LaunchedEffect(list.size) {
        if (list.size < 2) return@LaunchedEffect
        while (true) {
            delay(7200)
            if (!pager.isScrollInProgress) {
                runCatching { pager.animateScrollToPage((pager.currentPage + 1) % list.size) }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(492.dp)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { i ->
            val m = list.getOrNull(i) ?: return@HorizontalPager
            Slide(m, onOpen, onPlay)
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)) {
            Dots(list.size, pager.currentPage)
        }
    }
}

@Composable
private fun chip(text: String, tone: Color) {
    val p = localpal.current
    Box(
        Modifier
            .clip(cut(5.dp))
            .background(Color(0xCC0A0A0C))
            .border(0.8.dp, p.line, cut(5.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            color = tone,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun Slide(m: Media, onOpen: (Int) -> Unit, onPlay: (Int, Int) -> Unit) {
    val p = localpal.current
    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(m.cover ?: m.banner).crossfade(320).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.30f to Color.Transparent,
                    0.58f to p.bg.copy(alpha = 0.55f),
                    0.80f to p.bg.copy(alpha = 0.94f),
                    1f to p.bg
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(p.bg.copy(alpha = 0.55f), Color.Transparent))
            )
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(6.dp).clip(cut(2.dp)).background(p.acc))
                Text(
                    "TRENDING NOW",
                    color = p.acc,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
            Text(
                m.title.ifBlank { m.romaji },
                color = p.text,
                fontSize = 30.sp,
                lineHeight = 33.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.6).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                if ((m.score ?: 0) > 0) chip(
                    String.format(java.util.Locale.US, "%.1f", (m.score ?: 0) / 10f),
                    p.acc2
                )
                if (!m.fmt.isNullOrBlank()) chip(fmt.upper(m.fmt), p.text)
                if ((m.year ?: 0) > 0) chip("${m.year}", p.text)
            }
            if (!m.desc.isNullOrBlank()) Text(
                m.desc,
                color = p.mute.copy(alpha = 0.92f),
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Btn("Watch now", Icons.Rounded.PlayArrow, main = true) { onPlay(m.id, 1) }
                Btn("Details", Icons.Rounded.Info) { onOpen(m.id) }
            }
        }
    }
}
