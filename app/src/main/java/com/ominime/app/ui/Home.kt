package com.ominime.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ominime.app.data.Home as Feed
import com.ominime.app.data.Rec
import com.ominime.app.data.repo
import com.ominime.app.data.store
import kotlinx.coroutines.launch

@Composable
fun Home(
    onOpen: (Int) -> Unit,
    onPlay: (Int, Int) -> Unit,
    onGenre: (String) -> Unit,
    onList: () -> Unit,
    onDiscord: () -> Unit
) {
    val p = localpal.current
    val d by store.live.collectAsState()
    var feed by remember { mutableStateOf<Feed?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(true) }
    var arts by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun pull() {
        scope.launch {
            busy = true
            note = null
            runCatching { repo.home(force = true) }
                .onSuccess { feed = it }
                .onFailure { note = it.message ?: "Could not reach AniList" }
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        runCatching { repo.arts() }.onSuccess { arts = it }
    }

    LaunchedEffect(Unit) {
        if (feed != null) return@LaunchedEffect
        runCatching { repo.home() }
            .onSuccess { feed = it }
            .onFailure { note = it.message ?: "Could not reach AniList" }
        busy = false
    }

    track(list)

    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize().background(p.bg),
        contentPadding = PaddingValues(bottom = 126.dp)
    ) {
        item(key = "hero") {
            val f = feed
            if (f != null && f.hero.isNotEmpty()) {
                Hero(f.hero, onOpen, onPlay)
            } else {
                Box(Modifier.fillMaxWidth().height(470.dp)) {
                    Shine(Modifier.fillMaxSize(), rad = 0.dp)
                    if (!busy && note != null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Oops(note ?: "Something went wrong") { pull() }
                        }
                    }
                }
            }
        }

        if (note != null && feed != null) {
            item(key = "note") {
                Banner("Showing saved copy", note ?: "", "Refresh") { pull() }
            }
        }

        if (d.hist.isNotEmpty()) {
            item(key = "resume") {
                ResumeShelf(
                    "Continue watching",
                    d.hist.take(12),
                    onOpen = { r: Rec -> onPlay(r.id, if (r.ep > 0) r.ep else 1) },
                    action = "History",
                    onAction = onList
                )
            }
        }

        val f = feed
        if (f != null) {
            if (f.air.isNotEmpty()) item(key = "air") {
                Reveal { WideShelf("Airing now", f.air.take(12), onOpen, onAll = { onGenre("Airing") }) }
            }
            if (f.hero.isNotEmpty()) item(key = "hot") {
                Reveal { Shelf("Trending this season", f.hot, onOpen) }
            }
            if (f.top.isNotEmpty()) item(key = "top") {
                Reveal { RankShelf("Top 10 all time", f.top, onOpen) }
            }
            if (f.soon.isNotEmpty()) item(key = "soon") {
                Reveal { GridShelf("Upcoming", f.soon, onOpen) }
            }
            item(key = "genres") {
                Spacer(Modifier.height(6.dp))
                Head("Browse by genre", action = "See all") { onGenre("All") }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repo.genres.take(8).chunked(2).forEachIndexed { row, line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            line.forEachIndexed { col, name ->
                                GenresTile(
                                    name,
                                    when (name) {
                                        "Action" -> "Fights and flames"
                                        "Adventure" -> "Worlds to roam"
                                        "Comedy" -> "Laugh out loud"
                                        "Drama" -> "Heavy hearts"
                                        "Fantasy" -> "Magic and myth"
                                        "Horror" -> "Lights out"
                                        "Romance" -> "Butterflies"
                                        "Sci-Fi" -> "Far futures"
                                        else -> "Explore"
                                    },
                                    row * 2 + col,
                                    arts[name],
                                    Modifier.weight(1f)
                                ) { onGenre(name) }
                            }
                            if (line.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            if (f.pick.isNotEmpty()) item(key = "pick") {
                Spacer(Modifier.height(8.dp))
                Reveal { Shelf("Romance picks", f.pick, onOpen, onAll = { onGenre("Romance") }) }
            }
            item(key = "foot") {
                Column(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "OMINIME",
                        color = p.mute.copy(alpha = 0.45f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.2.sp
                    )
                    Discord { onDiscord() }
                }
            }
        } else if (busy) {
            item(key = "load") {
                Row(Modifier.fillMaxWidth().padding(40.dp), horizontalArrangement = Arrangement.Center) { Loader() }
            }
        }
    }
}
