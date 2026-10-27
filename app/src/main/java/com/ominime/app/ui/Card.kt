package com.ominime.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ominime.app.data.Media
import com.ominime.app.data.Rec
import java.util.Locale

private val cover: Float = 0.7f
private val wide: Float = 1.78f
private val reel: Float = 1.72f
private val notch: Dp = 13.dp
private val cardw: Dp = 146.dp
private val widew: Dp = 254.dp
private val resumew: Dp = 240.dp

fun sub(m: Media): String = listOfNotNull(
    m.fmt?.let { fmt.upper(it) },
    m.year?.toString(),
    m.total?.takeIf { it > 0 }?.let { "$it ep" }
).joinToString("  •  ")

@Composable
fun Badge(text: String, tone: Color? = null, fg: Color? = null, mod: Modifier = Modifier) {
    val p = localpal.current
    val bg = tone ?: Color(0xD908080B)
    val ink = fg ?: Color.White
    val shape = cut(4.dp)
    Box(
        mod
            .clip(shape)
            .background(bg)
            .border(0.6.dp, Color(0x26FFFFFF), shape)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text.uppercase(),
            color = ink,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}

@Composable
fun Pill2(text: String, mod: Modifier = Modifier, tone: Color = Color(0xCC000000), fg: Color = Color.White) {
    Badge(text, tone = tone, fg = fg, mod = mod)
}

@Composable
fun Rate(v: Int?, mod: Modifier = Modifier) {
    if (v == null || v <= 0) return
    val p = localpal.current
    Row(
        mod
            .clip(cut(4.dp))
            .background(Color(0xD908080B))
            .border(0.6.dp, Color(0x26FFFFFF), cut(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(Icons.Rounded.Star, null, tint = p.acc2, modifier = Modifier.size(9.dp))
        Text(
            String.format(Locale.US, "%.1f", v / 10f),
            color = Color.White,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.2.sp
        )
    }
}

@Composable
private fun Ring(shape: Shape) {
    val p = localpal.current
    Box(Modifier.fillMaxSize().border(0.7.dp, p.line, shape))
}

@Composable
private fun Veil(mod: Modifier = Modifier, shape: Shape) {
    Box(
        mod
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0.55f to Color.Transparent,
                    1f to Color(0xB3000000)
                )
            )
    )
}

@Composable
fun Poster(m: Media, width: Dp? = cardw, click: () -> Unit) {
    val p = localpal.current
    val shape = cut(notch)
    val box = if (width == null) Modifier.fillMaxWidth() else Modifier.width(width)
    Column(box.press(0.975f, click)) {
        Box {
            Pic(m.cover ?: m.banner, Modifier.fillMaxWidth().aspectRatio(cover), shape = shape)
            Veil(Modifier.matchParentSize(), shape)
            Ring(shape)
            Box(Modifier.align(Alignment.TopStart).padding(6.dp)) { Rate(m.score) }
            if (m.state == "RELEASING") {
                Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                    Box(Modifier.size(6.dp).clip(cut(1.5.dp)).background(p.acc))
                }
            }
            if ((m.total ?: 0) > 0) {
                Box(Modifier.align(Alignment.BottomEnd).padding(6.dp)) {
                    Badge("${m.total} EP", tone = Color(0xE60A0A0C))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            m.title.ifBlank { m.romaji },
            color = p.text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.15).sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            sub(m),
            color = p.mute.copy(alpha = 0.86f),
            fontSize = 10.sp,
            letterSpacing = 0.1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun Ranked(m: Media, at: Int, width: Dp = 132.dp, click: () -> Unit) {
    val p = localpal.current
    val shape = cut(notch)
    Row(Modifier.width(width + 46.dp).press(0.975f, click), verticalAlignment = Alignment.Bottom) {
        Text(
            "$at",
            color = if (at <= 3) p.acc.copy(alpha = 0.55f) else p.text.copy(alpha = 0.2f),
            fontSize = if (at <= 3) 78.sp else 68.sp,
            lineHeight = 64.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-6).sp,
            modifier = Modifier.padding(end = 1.dp, bottom = 16.dp)
        )
        Column(Modifier.width(width)) {
            Box {
                Pic(m.cover, Modifier.fillMaxWidth().aspectRatio(cover), shape = shape)
                Ring(shape)
                Box(Modifier.align(Alignment.TopStart).padding(6.dp)) { Rate(m.score) }
            }
            Spacer(Modifier.height(7.dp))
            Text(
                m.title.ifBlank { m.romaji },
                color = p.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun Resume(r: Rec, click: () -> Unit) {
    val p = localpal.current
    val shape = cut(notch)
    val frac = if (r.dur > 0) (r.pos.toFloat() / r.dur.toFloat()).coerceIn(0f, 1f) else 0f
    Column(Modifier.width(resumew).press(0.97f, click)) {
        Box {
            Pic(r.cover, Modifier.fillMaxWidth().aspectRatio(reel), shape = shape)
            Veil(Modifier.matchParentSize(), shape)
            Box(
                Modifier.matchParentSize().background(
                    Brush.horizontalGradient(listOf(Color(0x66000000), Color.Transparent))
                )
            )
            Ring(shape)
            Box(Modifier.align(Alignment.TopStart).padding(7.dp)) {
                Badge(fmt.ep(r.ep), tone = p.acc, fg = Color(0xFF0A0A0C))
            }
            Box(
                Modifier.align(Alignment.Center).size(38.dp).clip(cut(8.dp)).background(Color(0xD9FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF0A0A0C), modifier = Modifier.size(22.dp))
            }
            if (r.dur > 0) {
                Box(Modifier.align(Alignment.BottomEnd).padding(end = 7.dp, bottom = 9.dp)) {
                    Badge(fmt.clock(r.pos) + " / " + fmt.clock(r.dur), tone = Color(0xCC08080B))
                }
            }
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp)
                    .background(Color(0x4DFFFFFF))
            ) {
                if (frac > 0f) Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(p.acc))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            r.title,
            color = p.text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (r.dur > 0) String.format(Locale.US, "%d%% WATCHED", (frac * 100).toInt()) else "IN PROGRESS",
                color = p.acc,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
            Text(
                listOfNotNull(
                    (r.total ?: 0).takeIf { it > 0 }?.let { "of $it" },
                    if (r.dur > 0) fmt.left(r.dur - r.pos) + " left" else null
                ).joinToString("  •  "),
                color = p.mute,
                fontSize = 9.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun Person(name: String, role: String, img: String?, width: Dp = 92.dp) {
    val p = localpal.current
    Column(Modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Pic(img, Modifier.fillMaxWidth().aspectRatio(1f), shape = CircleShape)
            Box(Modifier.fillMaxSize().border(0.8.dp, p.line, CircleShape))
        }
        Spacer(Modifier.height(7.dp))
        Text(name, color = p.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(role, color = p.mute, fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun Wide(m: Media, click: () -> Unit) {
    val p = localpal.current
    val shape = cut(notch)
    Column(Modifier.width(widew).press(0.97f, click)) {
        Box {
            Pic(m.banner ?: m.cover, Modifier.fillMaxWidth().aspectRatio(wide), shape = shape)
            Veil(Modifier.matchParentSize(), shape)
            Ring(shape)
            Box(Modifier.align(Alignment.TopStart).padding(7.dp)) { Rate(m.score) }
            Row(
                Modifier.align(Alignment.BottomStart).padding(start = 9.dp, bottom = 9.dp, end = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(5.dp).clip(cut(1.5.dp)).background(fmt.tone(m.state)))
                Text(
                    if (m.state == "RELEASING") "Airing" else fmt.upper(m.state),
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    maxLines = 1
                )
            }
            if (m.next != null && m.next.ep > 0) {
                Box(Modifier.align(Alignment.TopEnd).padding(7.dp)) {
                    Badge(fmt.ep(m.next.ep), tone = p.acc, fg = Color(0xFF0A0A0C))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            m.title.ifBlank { m.romaji },
            color = p.text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(sub(m), color = p.mute.copy(alpha = 0.86f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun Tile(m: Media, click: () -> Unit) {
    val p = localpal.current
    val shape = cut(notch)
    Column(Modifier.press(0.97f, click)) {
        Box {
            Pic(m.cover, Modifier.fillMaxWidth().aspectRatio(cover), shape = shape)
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0.42f to Color.Transparent,
                        0.74f to Color(0x99000000),
                        1f to Color(0xF208080B)
                    )
                )
            )
            Ring(shape)
            Box(Modifier.align(Alignment.TopStart).padding(6.dp)) { Rate(m.score) }
            Box(Modifier.align(Alignment.BottomStart).padding(horizontal = 9.dp, vertical = 8.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (m.next != null && m.next.ep > 0) {
                        Text(
                            fmt.ep(m.next.ep).uppercase() + "  •  " + fmt.whens(m.next.at).uppercase(),
                            color = p.acc,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.7.sp,
                            maxLines = 1
                        )
                    }
                    Text(
                        m.title.ifBlank { m.romaji },
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
