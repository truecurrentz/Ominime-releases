package com.ominime.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ominime.app.data.Slot
import com.ominime.app.data.repo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private fun day(at: Long): Int {
    val c = Calendar.getInstance()
    c.timeInMillis = at
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return (c.timeInMillis / 86400000L).toInt()
}

private fun label(at: Long): String {
    val now = day(System.currentTimeMillis())
    return when (day(at) - now) {
        0 -> "Today"
        1 -> "Tomorrow"
        else -> SimpleDateFormat("EEE", Locale.US).format(Date(at))
    }
}

@Composable
fun Schedule(onOpen: (Int) -> Unit) {
    val p = localpal.current
    var all by remember { mutableStateOf<List<Slot>>(emptyList()) }
    var note by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(true) }
    var at by remember { mutableIntStateOf(0) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun pull(force: Boolean) {
        scope.launch {
            busy = true
            note = null
            runCatching { repo.airing(force) }
                .onSuccess { all = it.sortedBy { s -> s.at } }
                .onFailure { note = it.message ?: "Could not load the schedule" }
            busy = false
        }
    }

    LaunchedEffect(Unit) { pull(false) }
    track(list)

    val days = remember(all) {
        val base = day(System.currentTimeMillis())
        (0..6).map { base + it }
    }
    val pick = days.getOrNull(at) ?: days.first()
    val shown = all.filter { it.at > System.currentTimeMillis() - 3600_000L && day(it.at) == pick }

    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize().background(p.bg).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 126.dp)
    ) {
        item(key = "days") {
            Column(Modifier.padding(top = 54.dp)) {
                Text(
                    "SCHEDULE",
                    color = p.text,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Text(
                    "New episodes airing this week",
                    color = p.mute,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    items(7) { i ->
                        val d = days[i]
                        val on = i == at
                        Column(
                            Modifier
                                .width(58.dp)
                                .clip(cut(8.dp))
                                .background(if (on) p.acc else p.glass)
                                .press(0.97f) { at = i }
                                .padding(vertical = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                SimpleDateFormat("EEE", Locale.US).format(Date(d * 86400000L)),
                                color = if (on) Color(0xFF0A0A0C).copy(alpha = 0.75f) else p.mute,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                SimpleDateFormat("d", Locale.US).format(Date(d * 86400000L)),
                                color = if (on) Color(0xFF0A0A0C) else p.text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        if (busy && all.isEmpty()) {
            item(key = "load") {
                Row(Modifier.fillMaxWidth().padding(36.dp), horizontalArrangement = Arrangement.Center) { Loader() }
            }
        }

        if (note != null) {
            item(key = "note") { Oops(note ?: "") { pull(true) } }
        }

        if (!busy && shown.isEmpty() && note == null) {
            item(key = "blank") {
                Blank(
                    "Nothing else today",
                    "Pick another day above to see what is coming up.",
                    icon = Icons.Rounded.CalendarMonth
                )
            }
        }

        items(shown.size, key = { shown[it].m.id * 1000 + shown[it].ep }) { i ->
            val s = shown[i]
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 3.dp)
                    .clip(cut(12.dp))
                    .press(0.97f) { onOpen(s.m.id) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.size(58.dp, 80.dp)) {
                    Pic(s.m.cover, Modifier.fillMaxSize(), shape = cut(9.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        s.m.title.ifBlank { s.m.romaji },
                        color = p.text,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "EP ${s.ep}  •  " + fmt.upper(s.m.fmt) + "  •  " + (s.m.dur ?: 0) + " min",
                        color = p.mute,
                        fontSize = 11.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(p.acc))
                        Text(
                            SimpleDateFormat("HH:mm", Locale.US).format(Date(s.at)) + "  •  " + fmt.whens(s.at),
                            color = p.acc2,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    label(s.at),
                    color = p.mute.copy(alpha = 0.7f),
                    fontSize = 10.5.sp
                )
            }
        }

        item(key = "pad") { Spacer(Modifier.height(20.dp)) }
    }
}
