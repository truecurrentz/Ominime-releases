package com.ominime.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ominime.app.data.Find
import com.ominime.app.data.repo

private enum class Kind { Genres, Format, Year, Status, Season, Score, Sort }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Filters(f: Find, onChange: (Find) -> Unit, onClose: () -> Unit) {
    val p = localpal.current
    var open by remember { mutableStateOf<Kind?>(null) }

    val rows = listOf(
        Kind.Genres to Kind.Format,
        Kind.Year to Kind.Status,
        Kind.Season to Kind.Score,
        Kind.Sort to null
    )

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(3.5.dp, 16.dp).clip(cut(2.dp)).background(p.acc))
            Spacer(Modifier.width(9.dp))
            Text(
                "FILTERS",
                color = p.text,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.4.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                "COLLAPSE",
                color = p.mute,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.9.sp,
                modifier = Modifier
                    .clip(cut(5.dp))
                    .press(0.97f, onClose)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
        Spacer(Modifier.height(9.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(cut(16.dp))
                .background(p.card.copy(alpha = 0.72f))
                .border(0.8.dp, p.line, cut(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows.forEach { (left, right) ->
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.weight(1f)) {
                        Cell(left, f, open == left) { open = if (open == left) null else left }
                    }
                    Box(Modifier.weight(1f)) {
                        if (right != null) Cell(right, f, open == right) {
                            open = if (open == right) null else right
                        } else Box(Modifier.fillMaxWidth())
                    }
                }
                val hit = listOfNotNull(left, right).firstOrNull { it == open }
                AnimatedVisibility(
                    visible = hit != null,
                    enter = expandVertically(androidx.compose.animation.core.tween(180)) + fadeIn(androidx.compose.animation.core.tween(140)),
                    exit = shrinkVertically(androidx.compose.animation.core.tween(150)) + fadeOut(androidx.compose.animation.core.tween(90))
                ) {
                    if (hit != null) Bar(hit, f, onChange, open)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Btn("Reset", mod = Modifier.weight(1f)) { onChange(Find(sort = "POPULARITY_DESC")) }
                Btn("Apply", main = true, mod = Modifier.weight(1f)) { onClose() }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun value(k: Kind, f: Find): String = when (k) {
    Kind.Genres -> if (f.gens.isEmpty()) "Any genre" else f.gens.joinToString(", ")
    Kind.Format -> if (f.fmts.isEmpty()) "Any format" else f.fmts.joinToString(", ") { fmt.upper(it) }
    Kind.Year -> if (f.year == 0) "Any year" else f.year.toString()
    Kind.Status -> if (f.stats.isEmpty()) "Any status" else f.stats.joinToString(", ") { fmt.upper(it) }
    Kind.Season -> if (f.season.isBlank()) "Any season" else fmt.upper(f.season)
    Kind.Score -> if (f.score == 0) "Any score" else "${f.score}+"
    Kind.Sort -> repo.sorts.firstOrNull { it.second == f.sort }?.first ?: "Popular"
}

@Composable
private fun name(k: Kind): String = when (k) {
    Kind.Genres -> "Genres"
    Kind.Format -> "Format"
    Kind.Year -> "Year"
    Kind.Status -> "Status"
    Kind.Season -> "Season"
    Kind.Score -> "Score"
    Kind.Sort -> "Sort by"
}

@Composable
private fun Cell(k: Kind, f: Find, expanded: Boolean, toggle: () -> Unit) {
    val p = localpal.current
    val on = expanded
    val shape = cut(9.dp)
    Column {
        Text(
            name(k).uppercase(),
            color = p.mute.copy(alpha = 0.8f),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.1.sp,
            modifier = Modifier.padding(start = 3.dp, bottom = 5.dp)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (on) p.acc.copy(alpha = 0.12f) else p.glass)
                .border(0.8.dp, if (on) p.acc.copy(alpha = 0.6f) else p.line, shape)
                .press(0.97f, toggle)
                .padding(start = 11.dp, end = 9.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value(k, f),
                color = if (on) p.text else p.text.copy(alpha = 0.9f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (on) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                null,
                tint = if (on) p.acc else p.mute,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Bar(k: Kind, f: Find, onChange: (Find) -> Unit, open: Kind?) {
    val p = localpal.current
    val shape = cut(10.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(p.lift.copy(alpha = 0.9f))
            .border(0.8.dp, p.line, shape)
            .padding(11.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .width(26.dp)
                .height(1.5.dp)
                .background(p.acc)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            when (k) {
                Kind.Genres -> {
                    Pick("Any", f.gens.isEmpty()) { onChange(f.copy(gens = emptySet())) }
                    repo.genres.forEach { one ->
                        Pick(one, one in f.gens) {
                            onChange(f.copy(gens = if (one in f.gens) f.gens - one else f.gens + one))
                        }
                    }
                }
                Kind.Format -> {
                    Pick("Any", f.fmts.isEmpty()) { onChange(f.copy(fmts = emptySet())) }
                    repo.formats.forEach { one ->
                        Pick(fmt.upper(one), one in f.fmts) {
                            onChange(f.copy(fmts = if (one in f.fmts) f.fmts - one else f.fmts + one))
                        }
                    }
                }
                Kind.Status -> {
                    Pick("Any", f.stats.isEmpty()) { onChange(f.copy(stats = emptySet())) }
                    repo.states.forEach { one ->
                        Pick(fmt.upper(one), one in f.stats) {
                            onChange(f.copy(stats = if (one in f.stats) f.stats - one else f.stats + one))
                        }
                    }
                }
                Kind.Season -> {
                    Pick("Any", f.season.isBlank()) { onChange(f.copy(season = "")) }
                    repo.seasons.forEach { one ->
                        Pick(fmt.upper(one), f.season == one) { onChange(f.copy(season = one)) }
                    }
                }
                Kind.Year -> {
                    Pick("Any", f.year == 0) { onChange(f.copy(year = 0)) }
                    (2026 downTo 1990).forEach { y ->
                        Pick("$y", f.year == y) { onChange(f.copy(year = y)) }
                    }
                }
                Kind.Score -> {
                    Pick("Any", f.score == 0) { onChange(f.copy(score = 0)) }
                    listOf(60, 70, 75, 80, 85, 90).forEach { v ->
                        Pick("$v+", f.score == v) { onChange(f.copy(score = v)) }
                    }
                }
                Kind.Sort -> repo.sorts.forEach { one ->
                    Pick(one.first, f.sort == one.second) { onChange(f.copy(sort = one.second)) }
                }
            }
        }
    }
}
