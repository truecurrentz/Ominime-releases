package com.ominime.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ominime.app.data.Find
import com.ominime.app.data.Media
import com.ominime.app.data.any
import com.ominime.app.data.repo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Seek(start: String?, onOpen: (Int) -> Unit) {
    val p = localpal.current
    var q by remember { mutableStateOf("") }
    var f by remember {
        mutableStateOf(
            Find(
                gens = if (start != null && start != "All" && start != "Airing") setOf(start) else emptySet(),
                stats = if (start == "Airing") setOf("RELEASING") else emptySet()
            )
        )
    }
    var page by remember { mutableIntStateOf(1) }
    var hits by remember { mutableStateOf<List<Media>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var sheet by remember { mutableStateOf(false) }
    var more by remember { mutableStateOf(true) }
    val list = rememberLazyListState()
    val focus = remember { FocusRequester() }
    var hot by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load(first: Boolean) {
        busy = true
        note = null
        val want = if (first) 1 else page
        runCatching { repo.explore(q, f, want) }
            .onSuccess { r ->
                if (first) {
                    hits = r
                    page = 1
                    more = r.isNotEmpty()
                } else {
                    hits = (hits + r).distinctBy { it.id }
                    more = r.isNotEmpty() && page < 20
                }
            }
            .onFailure { note = it.message ?: "Search failed" }
        busy = false
    }

    LaunchedEffect(q, f) {
        delay(340)
        load(true)
    }

    LaunchedEffect(chrome.search.value) {
        if (chrome.search.value) {
            chrome.search.value = false
            runCatching { focus.requestFocus() }
        }
    }

    LaunchedEffect(list, hits.size, busy) {
        snapshotFlow { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { at ->
                if (!busy && more && hits.isNotEmpty() && page < 20 && at >= list.layoutInfo.totalItemsCount - 4) {
                    page += 1
                    load(false)
                }
            }
    }

    track(list)

    Box(Modifier.fillMaxSize().background(p.bg)) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 126.dp)
        ) {
            item(key = "bar") {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            Modifier
                                .weight(1f)
                                .clip(RoundedChip)
                                .background(p.glass)
                                .border(if (hot) 2.dp else 0.8.dp, if (hot) p.acc else p.line, RoundedChip)
                                .padding(start = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Search, null, tint = p.mute, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(9.dp))
                            BasicTextField(
                                value = q,
                                onValueChange = { q = it },
                                singleLine = true,
                                textStyle = TextStyle(color = p.text, fontSize = 14.sp, lineHeight = 18.sp),
                                cursorBrush = SolidColor(p.acc),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focus)
                                    .onFocusChanged { hot = it.isFocused },
                                decorationBox = { inner ->
                                    Box(
                                        Modifier.fillMaxWidth().height(48.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (q.isEmpty()) Text("Search anime", color = p.mute, fontSize = 14.sp)
                                        Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) { inner() }
                                        }
                                    }
                                }
                            )
                            if (q.isNotEmpty()) {
                                Icon(
                                    Icons.Rounded.Close,
                                    "Clear",
                                    tint = p.mute,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(16.dp)
                                        .press(0.97f) { q = "" }
                                )
                            }
                            Box(
                                Modifier
                                    .width(0.8.dp)
                                    .height(26.dp)
                                    .background(p.line)
                            )
                            Box(
                                Modifier
                                    .press(0.97f) { sheet = !sheet }
                                    .padding(horizontal = 14.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Tune,
                                        "Filters",
                                        tint = if (f.any()) p.text else p.mute,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    if (f.any()) Box(Modifier.size(5.dp).clip(CircleShape).background(p.acc))
                                }
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = sheet,
                        enter = expandVertically(androidx.compose.animation.core.tween(240)) + fadeIn(androidx.compose.animation.core.tween(160)),
                        exit = shrinkVertically(androidx.compose.animation.core.tween(190)) + fadeOut(androidx.compose.animation.core.tween(120))
                    ) {
                        Column {
                            Spacer(Modifier.height(6.dp))
                            Filters(f, { f = it }, { sheet = false })
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        repo.sorts.take(4).forEach { one ->
                            Label(one.first, f.sort == one.second) { f = f.copy(sort = one.second) }
                        }
                    }
                    if (f.any()) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            f.gens.take(2).forEach { name ->
                                Label(name, true) { f = f.copy(gens = f.gens - name) }
                            }
                            if (f.fmts.isNotEmpty()) Label(f.fmts.first(), true) { f = f.copy(fmts = emptySet()) }
                            if (f.stats.isNotEmpty()) Label("Airing", true) { f = f.copy(stats = emptySet()) }
                            Text(
                                "Reset",
                                color = p.mute,
                                fontSize = 12.sp,
                                modifier = Modifier.press(0.97f) { f = Find(sort = f.sort) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            items((hits.size + 2) / 3, key = { "row$it" }) { row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    for (c in 0..2) {
                        val m = hits.getOrNull(row * 3 + c)
                        Box(Modifier.weight(1f)) {
                            if (m != null) Reveal(delay = c * 50) { Poster(m, width = null) { onOpen(m.id) } }
                        }
                    }
                }
            }

            if (busy && hits.isEmpty()) {
                item(key = "load") {
                    Row(Modifier.fillMaxWidth().padding(30.dp), horizontalArrangement = Arrangement.Center) { Loader() }
                }
            }

            if (hits.isEmpty() && !busy) {
                item(key = "blank") {
                    if (note != null) Oops(note ?: "") { scope.launch { load(true) } } else Blank(
                        if (q.isBlank()) "Nothing here yet" else "No results",
                        if (q.isBlank()) "Pick a genre or open the filters." else "Try another title or loosen the filters.",
                        icon = Icons.Rounded.Search
                    )
                }
            }

            if (hits.isNotEmpty() && busy) {
                item(key = "more") {
                    Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) { Loader() }
                }
            }
        }

        if (sheet) BackHandler { sheet = false }
    }
}

private val RoundedChip = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
