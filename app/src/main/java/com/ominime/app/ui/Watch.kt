package com.ominime.app.ui

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ominime.app.data.Ep
import com.ominime.app.data.Stream
import com.ominime.app.data.offline
import com.ominime.app.data.Play
import com.ominime.app.data.Rec
import com.ominime.app.data.Src
import com.ominime.app.data.repo
import com.ominime.app.data.store
import kotlin.math.abs

@Composable
fun Watch(
    id: Int,
    ep: Int,
    onBack: () -> Unit,
    onOpen: (Int) -> Unit
) {
    val ctx = LocalContext.current
    val act = remember(ctx) { ctx.findact() }
    val conf = LocalConfiguration.current
    val land = conf.orientation == Configuration.ORIENTATION_LANDSCAPE
    val d by store.live.collectAsState()
    val c = d.conf
    var m by remember { mutableStateOf(repo.quick(id)) }
    var eps by remember { mutableStateOf<List<Ep>>(emptyList()) }
    var srcs by remember { mutableStateOf<List<Src>>(emptyList()) }
    var play by remember { mutableStateOf<Play?>(null) }
    var at by remember { mutableIntStateOf(0) }
    var cur by remember { mutableIntStateOf(ep) }
    LaunchedEffect(id, cur) { com.ominime.app.data.sync.now(id, cur) }
    var note by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(true) }
    var panel by remember { mutableIntStateOf(0) }
    var start by remember { mutableLongStateOf(0L) }
    var kept by remember { mutableLongStateOf(0L) }
    var tryn by remember { mutableIntStateOf(0) }
    var aud by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        runCatching { repo.detail(id) }.onSuccess { m = it }
    }

    LaunchedEffect(id, m?.id) {
        val md = m ?: return@LaunchedEffect
        runCatching { repo.eps(md) }.onSuccess { pair ->
            eps = pair.first
            srcs = pair.second
        }
    }

    LaunchedEffect(id, cur, tryn) {
        busy = true
        note = null
        runCatching {
            val md = m ?: runCatching { repo.detail(id) }.getOrNull()
            if (md == null) {
                note = "Could not load this title"
                return@runCatching
            }
            m = md
            if (eps.isEmpty()) runCatching { repo.eps(md) }.onSuccess { eps = it.first }
            val mark = store.find(id)
            start = if (mark != null && mark.ep == cur) mark.pos else 0L
            val off = offline.hit(id, cur)
            if (off != null) {
                val mark = offline.find(id, cur)
                val tracks = if (mark != null) offline.local(mark) else emptyList()
                play = Play(
                    audio = c.audio,
                    streams = listOf(
                        Stream(
                            url = "file://" + off.absolutePath,
                            kind = "file",
                            server = "Downloaded",
                            prio = 99,
                            subs = tracks
                        )
                    )
                )
                at = 0
                return@runCatching
            }
            val one = repo.play(md, aud ?: c.audio, cur)
            play = one
            at = 0
            if (one.streams.isEmpty()) note = one.note ?: "No stream available"
        }.onFailure { note = it.message ?: "Playback could not start" }
        busy = false
    }

    LaunchedEffect(Unit) {
        val w = act?.window ?: return@LaunchedEffect
        runCatching { act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE }
        val ctl = WindowCompat.getInsetsController(w, w.decorView)
        ctl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ctl.hide(WindowInsetsCompat.Type.systemBars())
    }

    DisposableEffect(Unit) {
        val w = act?.window
        w?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            w?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            runCatching { act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
            if (w != null) WindowCompat.getInsetsController(w, w.decorView).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val idx = eps.indexOfFirst { it.num == cur }
    val nextep = when {
        idx >= 0 -> eps.getOrNull(idx + 1)?.num
        (m?.total ?: 0) > cur -> cur + 1
        else -> null
    }
    val prevep = when {
        idx > 0 -> eps.getOrNull(idx - 1)?.num
        cur > 1 -> cur - 1
        else -> null
    }
    val meta = eps.firstOrNull { it.num == cur }
    val stream = play?.streams?.getOrNull(at)

    fun next() {
        val n = nextep ?: return
        cur = n
        start = 0L
    }

    fun prev() {
        val n = prevep ?: return
        cur = n
        start = 0L
    }

    BackHandler {
        if (panel != 0) panel = 0 else onBack()
    }

    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
        if (land) {
            Player(
                stream = stream,
                pool = play?.streams ?: emptyList(),
                cfg = Cfg(
                    title = m?.title ?: "Ominime",
                    line = "EP $cur" + (if (meta?.title != null) "  •  " + meta.title else ""),
                    ep = cur,
                    mal = m?.mal ?: 0,
                    auto = c.auto,
                    intro = c.intro,
                    start = start,
                    hasnext = nextep != null,
                    nextep = nextep ?: 0,
                    hasprev = prevep != null
                ),
                onBack = onBack,
                onNext = { next() },
                onPrev = { prev() },
                onServers = { panel = 2 },
                onEps = { panel = 1 },
                onProgress = { pos, dur ->
                    val md = m
                    if (md != null && dur > 0 && abs(pos - kept) > 9000) {
                        kept = pos
                        store.save(
                            Rec(
                                id = id,
                                title = md.title.ifBlank { md.romaji },
                                cover = md.banner ?: md.cover,
                                ep = cur,
                                pos = pos,
                                dur = dur,
                                at = System.currentTimeMillis(),
                                total = md.total,
                                mal = md.mal
                            )
                        )
                    }
                }
            )

            if (busy) {
                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0x66000000)), contentAlignment = Alignment.Center) {
                    Loader(size = 30.dp)
                }
            }

            if (!busy && stream == null) {
                Box(
                    Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xE6000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 26.dp)
                    ) {
                        Text(
                            "No stream for episode $cur",
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            note ?: "Every source came back empty.",
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.65f),
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Btn("Servers", main = true) { panel = 2 }
                            Btn("Retry") { tryn += 1 }
                            Btn("Episodes") { panel = 1 }
                        }
                    }
                }
            }

            if (panel == 1) {
                EpPanel(m, eps, cur, onPick = { n ->
                    cur = n
                    start = 0L
                    panel = 0
                }) { panel = 0 }
            }
            if (panel == 2) {
                SrcPanel(
                    list = play?.streams ?: emptyList(),
                    at = at,
                    prov = "",
                    audio = play?.audio ?: aud ?: c.audio,
                    onPick = { i ->
                        at = i
                        panel = 0
                    },
                    onAudio = { kind ->
                        aud = kind
                        tryn += 1
                    },
                    onClose = { panel = 0 }
                )
            }
        } else {
            Rotate { onBack() }
            if (!busy && stream == null) {
                Text(
                    note ?: "No stream for this episode",
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp, start = 30.dp, end = 30.dp)
                )
            }
        }
    }
}

@Composable
private fun Rotate(onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xE608080B)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp).align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color(0x33FFFFFF))
                    .border(0.8.dp, androidx.compose.ui.graphics.Color(0x26FFFFFF), CircleShape)
                    .press(0.97f, onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    "Back",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Box(
                Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color(0x1AFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.ScreenRotation,
                    null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
            Text(
                "Rotate your phone",
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
            )
            Text(
                "Turn your device sideways to start watching. Servers and subtitles appear once the player opens.",
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Go back") { onBack() }
            }
        }
    }
}
