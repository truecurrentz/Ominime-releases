package com.ominime.app.ui

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.util.TypedValue
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player as Exo
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.ominime.app.BuildConfig
import com.ominime.app.data.Cue
import com.ominime.app.data.Subs
import com.ominime.app.data.Stream
import com.ominime.app.data.aniskip
import com.ominime.app.data.caps
import com.ominime.app.data.langs
import com.ominime.app.data.net
import com.ominime.app.data.store
import kotlinx.coroutines.delay

data class Cfg(
    val title: String = "",
    val line: String = "",
    val ep: Int = 0,
    val mal: Int = 0,
    val auto: Boolean = true,
    val intro: Boolean = true,
    val start: Long = 0,
    val hasnext: Boolean = false,
    val nextep: Int = 0,
    val hasprev: Boolean = false
)

@Composable
fun Player(
    stream: Stream?,
    cfg: Cfg,
    pool: List<Stream> = emptyList(),
    onBack: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onServers: () -> Unit,
    onEps: () -> Unit,
    lock: Boolean = false,
    land: FocusRequester? = null,
    onProgress: (Long, Long) -> Unit
) {
    val pal = localpal.current
    val ctx = LocalContext.current
    val act = remember(ctx) { ctx.findact() }
    val am = remember(ctx) { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val bag by store.live.collectAsState()
    val conf = bag.conf
    val pref = conf.sub
    val ssize = conf.subsize

    val exo = remember { ExoPlayer.Builder(ctx).build() }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var buf by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var show by remember { mutableStateOf(true) }
    val pad = remember { FocusRequester() }
    val grip = remember { FocusRequester() }
    var seat by remember { mutableStateOf(false) }
    var hop by remember { mutableLongStateOf(0L) }
    var row by remember { mutableIntStateOf(2) }
    var sel by remember { mutableIntStateOf(2) }
    var top by remember { mutableIntStateOf(0) }
    val telly = BuildConfig.tv

    fun flip() {
        if (exo.isPlaying) exo.pause() else exo.play()
    }

    LaunchedEffect(telly) {
        if (telly) runCatching { pad.requestFocus() }
    }
    var speed by remember { mutableFloatStateOf(conf.rate) }
    var aspect by remember { mutableIntStateOf(0) }
    var qual by remember { mutableIntStateOf(0) }
    var seeked by remember { mutableStateOf(false) }
    var pending by remember { mutableLongStateOf(0L) }
    var resumeAt by remember { mutableLongStateOf(0L) }
    var cuts by remember { mutableStateOf<List<aniskip.Cut>>(emptyList()) }
    var cues by remember { mutableStateOf<List<Cue>>(emptyList()) }
    var cnote by remember { mutableStateOf("") }
    var plat by remember { mutableStateOf("") }
    var opts by remember { mutableStateOf(false) }
    var panel by remember { mutableStateOf(false) }
    val pane = remember { FocusRequester() }
    var side by remember { mutableStateOf<List<Subs>>(emptyList()) }
    var mine by remember { mutableStateOf<Subs?>(null) }
    var hint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var rip by remember { mutableStateOf<Int?>(null) }
    var prompt by remember { mutableIntStateOf(0) }
    var trackver by remember { mutableIntStateOf(0) }
    var bright by remember { mutableFloatStateOf(act?.window?.attributes?.screenBrightness?.takeIf { it > 0f } ?: 0.6f) }
    var vol by remember {
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        mutableFloatStateOf(if (max > 0) am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max else 0.5f)
    }

    fun tone() {
        val mx = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, (vol * mx).toInt(), 0) }
    }

    fun glow() {
        val w = act?.window ?: return
        runCatching {
            val a = w.attributes
            a.screenBrightness = bright.coerceIn(0.05f, 1f)
            w.attributes = a
        }
    }

    fun jump(ms: Long) {
        val to = (exo.currentPosition + ms).coerceIn(0L, if (dur > 0) dur else Long.MAX_VALUE)
        exo.seekTo(to)
        pos = to
        rip = if (ms < 0) 0 else 1
    }

    fun fire(i: Int) {
        when (i) {
            0 -> if (cfg.hasprev) onPrev()
            1 -> jump(-10000L)
            2 -> flip()
            3 -> jump(10000L)
            4 -> if (cfg.hasnext) onNext()
            5 -> panel = true
            6 -> opts = true
            7 -> onEps()
            8 -> onServers()
        }
    }

    fun move(dir: Int) {
        val step = if (dur > 0) (dur / 90L).coerceIn(4000L, 30000L) else 10000L
        val at = (exo.currentPosition + dir * step).coerceIn(0L, if (dur > 0) dur else Long.MAX_VALUE)
        exo.seekTo(at)
        pos = at
        rip = if (dir < 0) 0 else 1
    }

    val pv = remember {
        PlayerView(ctx).apply {
            useController = false
            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            keepScreenOn = true
        }
    }

    LaunchedEffect(ssize, show, conf.subbold, conf.subital, conf.subfont, conf.subcolor, conf.subedge, conf.subpos, conf.subbox) {
        pv.subtitleView?.apply {
            val tint = when (conf.subcolor) {
                "amber" -> android.graphics.Color.rgb(255, 192, 46)
                "orange" -> android.graphics.Color.rgb(255, 106, 0)
                "green" -> android.graphics.Color.rgb(50, 215, 75)
                "cyan" -> android.graphics.Color.rgb(80, 220, 255)
                "pink" -> android.graphics.Color.rgb(255, 61, 113)
                "soft" -> android.graphics.Color.rgb(226, 226, 232)
                else -> android.graphics.Color.WHITE
            }
            val family = when (conf.subfont) {
                "serif" -> "serif"
                "mono" -> "monospace"
                "cursive" -> "casual"
                else -> "sans-serif"
            }
            val style = when {
                conf.subbold && conf.subital -> android.graphics.Typeface.BOLD_ITALIC
                conf.subbold -> android.graphics.Typeface.BOLD
                conf.subital -> android.graphics.Typeface.ITALIC
                else -> android.graphics.Typeface.NORMAL
            }
            val edge = if (conf.subedge > 0) CaptionStyleCompat.EDGE_TYPE_OUTLINE else CaptionStyleCompat.EDGE_TYPE_NONE
            val edgeColor = if (conf.subedge > 0) android.graphics.Color.BLACK else android.graphics.Color.TRANSPARENT
            val back = if (conf.subbox) android.graphics.Color.argb(170, 8, 8, 11) else android.graphics.Color.TRANSPARENT
            setStyle(
                CaptionStyleCompat(
                    tint,
                    back,
                    android.graphics.Color.TRANSPARENT,
                    edge,
                    edgeColor,
                    android.graphics.Typeface.create(family, style)
                )
            )
            setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, (if (BuildConfig.tv) 23f else 15f) * (ssize.coerceIn(70, 170) / 100f))
            setApplyEmbeddedStyles(false)
            setApplyEmbeddedFontSizes(false)
            val rest = when (conf.subpos) {
                1 -> 0.16f
                2 -> 0.32f
                else -> 0.03f
            }
            setBottomPaddingFraction((if (show) rest + 0.18f else rest).coerceIn(0f, 0.6f))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { pv.player = null }
            runCatching { exo.release() }
        }
    }

    LaunchedEffect(pv, exo) { pv.player = exo }

    LaunchedEffect(stream?.url, stream?.ref) {
        val one = stream ?: return@LaunchedEffect
        err = null
        ready = false
        seeked = false
        pending = if (cfg.start > 2000) cfg.start else resumeAt
        pos = 0
        dur = 0
        buf = 0
        val ref = stream.ref.ifBlank { origin(one.url) }
        val head = buildMap {
            put("Referer", ref)
            put("User-Agent", net.ua)
            origin(ref).takeIf { it.isNotEmpty() }?.let { put("Origin", it) }
            putAll(net.share(one.url))
        }
        val http = OkHttpDataSource.Factory(net.cli)
            .setUserAgent(net.ua)
            .setDefaultRequestProperties(head)
        val ds = DefaultDataSource.Factory(ctx, http)
        val extra = stream.subs
            .filter { it.url.isNotBlank() && !it.url.contains(".m3u8", true) }
            .map { it.copy(url = caps.resolve(it.url, stream)) }
        val man = runCatching { caps.manifest(one.url, stream.ref) }.getOrDefault(emptyList())
        side = (extra + man).distinctBy { it.url }
        val pick = if (pref == "off") null else
            side.firstOrNull { langs.norm(it.lang) == pref } ?: side.firstOrNull()
        mine = pick
        val tracks = side
            .sortedBy { if (langs.norm(it.lang) == pref) 0 else 1 }
            .mapNotNull { t ->
                if (t.url.isBlank()) return@mapNotNull null
                val u = runCatching { Uri.parse(t.url) }.getOrNull() ?: return@mapNotNull null
                val local = t.url.startsWith("file:")
                val mime = when {
                    local -> "text/vtt"
                    t.fmt == "srt" -> "application/x-subrip"
                    t.fmt == "ttml" -> "application/ttml+xml"
                    else -> "text/vtt"
                }
                SubtitleConfiguration.Builder(u)
                    .setMimeType(mime)
                    .setId(langs.norm(t.lang) + "|" + t.url.hashCode())
                    .setLanguage(langs.norm(t.lang))
                    .setLabel(langs.name(t.lang))
                    .setSelectionFlags(if (pick != null && t.url == pick.url) C.SELECTION_FLAG_DEFAULT else 0)
                    .build()
            }
        val item = MediaItem.Builder()
            .setUri(one.url)
            .setSubtitleConfigurations(tracks)
            .build()
        val src = runCatching {
            if (one.url.isBlank()) return@runCatching null
            if (one.kind == "hls" || one.url.contains(".m3u8", true)) {
                HlsMediaSource.Factory(ds).setAllowChunklessPreparation(true).createMediaSource(item)
            } else {
                ProgressiveMediaSource.Factory(ds).createMediaSource(item)
            }
        }.getOrNull()
        if (src == null) {
            err = "This source could not be opened"
            return@LaunchedEffect
        }
        runCatching {
            val b = exo.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_TEXT)
            b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, pref == "off")
            if (pref != "off") {
                b.setPreferredTextLanguage(langs.norm(pick?.lang ?: pref))
                b.setSelectUndeterminedTextLanguage(true)
            }
            exo.trackSelectionParameters = b.build()
            exo.setMediaSource(src)
            exo.prepare()
            exo.playWhenReady = true
        }.onFailure { err = it.message }
    }

    LaunchedEffect(stream?.url) {
        val one = stream ?: return@LaunchedEffect
        val packed = runCatching { kotlinx.coroutines.withTimeoutOrNull(7000L) { caps.pack(one) } }
            .getOrNull().orEmpty()
        val others = if (packed.size >= 3) emptyList() else pool
            .filter { it.url != one.url && it.subs.isNotEmpty() }
            .take(3)
            .mapNotNull { alt ->
                runCatching { kotlinx.coroutines.withTimeoutOrNull(4000L) { caps.pack(alt) } }.getOrNull()
            }
            .flatten()
        val more = (packed + others).distinctBy { it.url }.filter { t -> side.none { it.url == t.url } }
        side = if (more.isEmpty()) side else more + side
    }

    DisposableEffect(exo) {
        val listen = object : Exo.Listener {
            override fun onIsPlayingChanged(v: Boolean) {
                playing = v
            }

            override fun onPlaybackStateChanged(st: Int) {
                ready = st == Exo.STATE_READY
                if (st == Exo.STATE_READY && !seeked) {
                    seeked = true
                    if (pending > 2000) exo.seekTo(pending)
                    exo.setPlaybackSpeed(speed)
                }
            }

            override fun onPlayerError(e: PlaybackException) {
                err = "This stream could not be played"
            }

            override fun onTracksChanged(tracks: Tracks) {
                trackver++
            }

            override fun onCues(cueGroup: androidx.media3.common.text.CueGroup) {
                plat = cueGroup.cues.mapNotNull { it.text?.toString() }.joinToString("\n").trim()
            }

            override fun onEvents(pl: Exo, events: Exo.Events) {
                dur = pl.duration.coerceAtLeast(0)
                buf = pl.bufferedPosition
            }
        }
        exo.addListener(listen)
        onDispose { exo.removeListener(listen) }
    }

    LaunchedEffect(exo) {
        while (true) {
            pos = exo.currentPosition.coerceAtLeast(0)
            if (exo.isPlaying) resumeAt = pos
            delay(220)
        }
    }

    LaunchedEffect(cfg.ep, stream?.url) {
        while (true) {
            delay(5000)
            if (dur > 0 && pos > 4000) onProgress(pos, dur)
        }
    }

    LaunchedEffect(speed) { runCatching { exo.setPlaybackSpeed(speed) } }

    LaunchedEffect(qual) {
        runCatching {
            val b = exo.trackSelectionParameters.buildUpon()
            if (qual <= 0) b.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
            else b.setMaxVideoSize(qual * 16 / 9, qual)
            exo.trackSelectionParameters = b.build()
        }
    }

    LaunchedEffect(pref) {
        runCatching {
            val b = exo.trackSelectionParameters.buildUpon()
            if (pref == "off") {
                b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            } else {
                b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                b.setPreferredTextLanguage(pref)
                b.setSelectUndeterminedTextLanguage(true)
            }
            exo.trackSelectionParameters = b.build()
        }
    }

    LaunchedEffect(aspect) {
        pv.resizeMode = when (aspect) {
            1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            2 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    LaunchedEffect(cfg.mal, cfg.ep, cfg.intro) {
        cuts = if (cfg.intro) runCatching { aniskip.cuts(cfg.mal, cfg.ep) }.getOrDefault(emptyList()) else emptyList()
    }

    LaunchedEffect(show, playing, panel, opts) {
        if (show && playing && !panel && !opts && !lock) {
            delay(3600)
            show = false
        }
    }

    LaunchedEffect(hint) {
        if (hint != null) {
            delay(900)
            hint = null
        }
    }

    LaunchedEffect(rip) {
        if (rip != null) {
            delay(520)
            rip = null
        }
    }

    val near = dur > 0 && dur - pos <= 27000 && cfg.hasnext && cfg.auto
    LaunchedEffect(near, cfg.ep) {
        if (near) {
            prompt = 10
            while (prompt > 1) {
                delay(1000)
                if (prompt <= 0) return@LaunchedEffect
                prompt -= 1
            }
            if (prompt == 1) {
                prompt = 0
                onNext()
            }
        } else {
            prompt = 0
        }
    }

    val cut = cuts.firstOrNull { c -> pos / 1000.0 >= c.start && pos / 1000.0 < c.end - 0.6 }
    var jumped by remember(cfg.ep, stream?.url) { mutableStateOf<String?>(null) }

    fun leap(one: aniskip.Cut) {
        val to = (one.end * 1000).toLong()
        exo.seekTo(to)
        pos = to
        jumped = one.kind + one.start
    }


    LaunchedEffect(cut?.start, cut?.end, conf.skipauto, cfg.intro) {
        val one = cut ?: return@LaunchedEffect
        if (!conf.skipauto || !cfg.intro) return@LaunchedEffect
        val mark = one.kind + one.start
        if (jumped == mark) return@LaunchedEffect
        jumped = mark
        val to = (one.end * 1000).toLong()
        runCatching {
            exo.seekTo(to)
            pos = to
        }
    }
    val tracks = remember(trackver) { exo.currentTracks }
    val subs = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
    val auds = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
    val subOn = subs.any { g -> g.isSelected }

    fun equip(t: Subs?) {
        mine = t
        runCatching {
            val b = exo.trackSelectionParameters.buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            if (t == null) {
                b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            } else {
                b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                b.setPreferredTextLanguage(langs.norm(t.lang))
                b.setSelectUndeterminedTextLanguage(true)
            }
            exo.trackSelectionParameters = b.build()
        }
        val want = if (t == null) "off" else langs.norm(t.lang)
        if (store.conf.sub != want) store.conf(store.conf.copy(sub = want))
        trackver++
        panel = false
    }

    LaunchedEffect(mine?.url, stream?.url) {
        val t = mine
        if (t == null) {
            cues = emptyList()
            cnote = ""
            return@LaunchedEffect
        }
        val refs = listOf(
            t.ref,
            stream?.ref.orEmpty(),
            stream?.url?.let { origin(it) }.orEmpty()
        )
        val got = runCatching { caps.cuesOf(t, refs) }.getOrDefault(emptyList())
        cues = got
        cnote = when {
            got.isNotEmpty() -> ""
            t.url.startsWith("file:") -> "converted file was empty"
            else -> "the source would not serve this file"
        }
        runCatching {
            val b = exo.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_TEXT)
            b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            b.setPreferredTextLanguage(langs.norm(t.lang))
            b.setSelectUndeterminedTextLanguage(true)
            exo.trackSelectionParameters = b.build()
        }
        trackver++
    }

    LaunchedEffect(side, pref, stream?.url) {
        if (side.isEmpty()) return@LaunchedEffect
        if (pref == "off") {
            if (mine != null) equip(null)
            return@LaunchedEffect
        }
        val hit = side.firstOrNull { langs.norm(it.lang) == pref } ?: side.first()
        if (mine?.url != hit.url) equip(hit)
    }

    fun picksub(g: Tracks.Group?, i: Int) = runCatching {
        if (g == null) {
            exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            store.conf(store.conf.copy(sub = "off"))
        } else {
            val f = g.getTrackFormat(i)
            val code = langs.norm(f.language ?: f.label)
            exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setPreferredTextLanguage(code)
                .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i))
                .build()
            store.conf(store.conf.copy(sub = code))
        }
        trackver++
        panel = false
    }

    var tried by remember(stream?.url) { mutableStateOf(false) }

    LaunchedEffect(trackver, pref, stream?.url) {
        if (tried || pref == "off" || subs.isEmpty() || subs.any { it.isSelected }) return@LaunchedEffect
        val g = subs.firstOrNull { it.length > 0 && langs.norm(it.getTrackFormat(0).language) == pref }
            ?: subs.firstOrNull { it.length > 0 }
            ?: return@LaunchedEffect
        tried = true
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setPreferredTextLanguage(pref)
            .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, 0))
            .build()
        trackver++
    }

    fun pickaud(g: Tracks.Group, i: Int) {
        val b = exo.trackSelectionParameters.buildUpon()
        b.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        b.setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i))
        exo.trackSelectionParameters = b.build()
        trackver++
    }

    BackHandler(enabled = !lock) {
        when {
            panel -> panel = false
            opts -> opts = false
            else -> onBack()
        }
    }

    LaunchedEffect(show, panel, opts, telly) {
        if (!telly || panel || opts || lock || !show) return@LaunchedEffect
        runCatching { pad.requestFocus() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(pad)
            .focusable()
            .onFocusChanged {
                seat = it.isFocused
                if (it.isFocused) {
                    if (panel || opts) runCatching { pane.requestFocus() }
                    else if (lock && land != null) runCatching { land.requestFocus() }
                }
            }
            .onKeyEvent { e ->
                if (!telly || e.type != KeyEventType.KeyDown) return@onKeyEvent false
                val code = e.nativeKeyEvent.keyCode
                if (code == android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || code == android.view.KeyEvent.KEYCODE_HEADSETHOOK) {
                    flip()
                    return@onKeyEvent true
                }
                if (panel || opts || lock) return@onKeyEvent false
                when (e.key) {
                    Key.DirectionCenter, Key.Enter -> {
                        if (!show) {
                            flip()
                        } else {
                            when (row) {
                                0 -> {
                                    val one = cut
                                    if (top == 1 && one != null && cfg.intro) leap(one) else onBack()
                                }
                                1 -> if (dur > 0) {
                                    val at = pos
                                    val step = (dur / 90L).coerceIn(4000L, 30000L)
                                    exo.seekTo((at + step).coerceIn(0L, dur))
                                }
                                else -> fire(sel)
                            }
                        }
                        true
                    }
                    Key.DirectionLeft -> {
                        when {
                            !show -> {
                                if (System.currentTimeMillis() - hop > 400L) {
                                    hop = System.currentTimeMillis()
                                    jump(-10000L)
                                }
                            }
                            row == 2 -> sel = (sel - 1).coerceAtLeast(0)
                            row == 0 -> top = (top - 1).coerceAtLeast(0)
                            else -> move(-1)
                        }
                        true
                    }
                    Key.DirectionRight -> {
                        when {
                            !show -> {
                                if (System.currentTimeMillis() - hop > 400L) {
                                    hop = System.currentTimeMillis()
                                    jump(10000L)
                                }
                            }
                            row == 2 -> sel = (sel + 1).coerceAtMost(8)
                            row == 0 -> top = (top + 1).coerceAtMost(if (cut != null && cfg.intro) 1 else 0)
                            else -> move(1)
                        }
                        true
                    }
                    Key.DirectionUp -> {
                        if (!show) {
                            show = true
                            row = 2
                        } else {
                            row = (row - 1).coerceAtLeast(0)
                        }
                        true
                    }
                    Key.DirectionDown -> {
                        if (!show) {
                            show = true
                            row = 2
                        } else {
                            row = (row + 1).coerceAtMost(2)
                        }
                        true
                    }
                    Key.Back -> false
                    else -> false
                }
            }
    ) {
        AndroidView(factory = { pv }, modifier = Modifier.fillMaxSize())

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(cfg.ep) {
                    detectTapGestures(
                        onTap = { show = !show },
                        onDoubleTap = { o ->
                            when {
                                o.x < size.width * 0.34f -> jump(-10000)
                                o.x > size.width * 0.66f -> jump(10000)
                                else -> show = !show
                            }
                        }
                    )
                }
                .pointerInput(cfg.ep) {
                    var at = 0f
                    detectVerticalDragGestures(
                        onDragStart = { at = it.x },
                        onVerticalDrag = { _, dy ->
                            show = true
                            if (at < size.width / 2f) {
                                bright = (bright - dy / size.height.toFloat() * 1.4f).coerceIn(0.05f, 1f)
                                glow()
                                hint = 0 to (bright * 100).toInt()
                            } else {
                                vol = (vol - dy / size.height.toFloat() * 1.4f).coerceIn(0f, 1f)
                                tone()
                                hint = 1 to (vol * 100).toInt()
                            }
                        }
                    )
                }
        )

        val line = remember(cues, pos, plat) {
            if (plat.isNotBlank()) null else cues.firstOrNull { pos >= it.a && pos < it.z }?.text
        }
        if (line != null) Captions(line, conf, ssize, show)

        AnimatedVisibility(
            visible = show || !playing,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(170)),
            exit = fadeOut(tween(220))
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color(0xB3000000),
                                0.24f to Color(0x40000000),
                                0.40f to Color.Transparent,
                                0.66f to Color.Transparent,
                                1f to Color(0xE6000000)
                            )
                        )
                )
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayCircle(Icons.Rounded.ArrowBack, "Back", 40.dp) { onBack() }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            cfg.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            cfg.line,
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        Modifier
                            .clip(cut(6.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "EP ${cfg.ep}",
                            color = pal.acc,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(fmt.clock(pos), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(11.dp))
                        Seek(
                            pos = pos,
                            dur = dur,
                            buf = buf,
                            marks = if (cfg.intro) cuts else emptyList(),
                            pick = show && row == 1,
                            mod = Modifier.weight(1f),
                            onScrub = { pos = it },
                            onDone = { exo.seekTo(it); pos = it }
                        )
                        Spacer(Modifier.width(11.dp))
                        Text(
                            fmt.clock(dur),
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        val bar = row == 2
                        Tap(Icons.Rounded.SkipPrevious, "Previous", 40.dp, cfg.hasprev, pick = bar && sel == 0) { fire(0) }
                        Tap(Icons.Rounded.Replay10, "Back 10s", 42.dp, pick = bar && sel == 1) { fire(1) }
                        Spacer(Modifier.width(8.dp))
                        PlayMain(
                            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            grip,
                            bar && sel == 2
                        ) { fire(2) }
                        Spacer(Modifier.width(8.dp))
                        Tap(Icons.Rounded.Forward10, "Forward 10s", 42.dp, pick = bar && sel == 3) { fire(3) }
                        Tap(Icons.Rounded.SkipNext, "Next", 40.dp, cfg.hasnext, pick = bar && sel == 4) { fire(4) }
                        Spacer(Modifier.weight(1f))
                        Tap(
                            Icons.Rounded.Subtitles,
                            "Subtitles",
                            40.dp,
                            pick = bar && sel == 5,
                            tint = if (mine != null || subs.any { g -> g.isSelected }) pal.acc else Color.White
                        ) { fire(5) }
                        Tap(Icons.Rounded.Speed, "Speed", 40.dp, pick = bar && sel == 6) { fire(6) }
                        Tap(Icons.Rounded.List, "Episodes", 40.dp, pick = bar && sel == 7) { fire(7) }
                        Tap(Icons.Rounded.Settings, "Servers", 40.dp, pick = bar && sel == 8) { fire(8) }
                    }
                    if (telly) {
                        val at = when (row) {
                            0 -> if (cut != null && cfg.intro && top == 1) {
                                if (cut.kind.contains("ed")) "skip ending" else "skip opening"
                            } else {
                                "back"
                            }
                            1 -> "scrub with left and right"
                            else -> listOf(
                                "previous", "back 10s", "play", "forward 10s", "next",
                                "subtitles", "speed", "episodes", "servers"
                            )[sel]
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                at.uppercase(),
                                color = pal.acc,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                "arrows move  ·  ok selects  ·  up or down changes row",
                                color = Color.White.copy(alpha = 0.34f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        if (err != null) {
            Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(err ?: "", color = Color.White, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DarkBtn("Retry", true) {
                        err = null
                        runCatching { exo.prepare(); exo.playWhenReady = true }
                    }
                    DarkBtn("Servers") { onServers() }
                }
            }
        } else if (!ready && stream != null) {
            Box(Modifier.align(Alignment.Center)) { Loader(size = 30.dp) }
        } else if (!playing) {
            PlayCircle(Icons.Rounded.PlayArrow, "Play", 66.dp, Modifier.align(Alignment.Center)) { exo.play() }
        }

        hint?.let { h ->
            Row(
                Modifier
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color(0xD9000000))
                    .padding(horizontal = 18.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (h.first == 0) Icons.Rounded.Brightness6 else Icons.Rounded.VolumeUp,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text("${h.second}%", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        rip?.let { dir ->
            Row(
                Modifier
                    .align(if (dir == 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 26.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .padding(horizontal = 15.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    if (dir == 0) Icons.Rounded.Replay10 else Icons.Rounded.Forward10,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text("10s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 116.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (cut != null && cfg.intro) {
                val skipShape = cut(9.dp)
                val left = ((cut.end * 1000).toLong() - pos) / 1000L
                val on = show && row == 0 && top == 1
                Row(
                    Modifier
                        .clip(skipShape)
                        .background(if (on) Color(0xF2FF6A00) else Color(0xE60A0A0C))
                        .border(if (on) 2.dp else 0.9.dp, if (on) Color.White else pal.acc2.copy(alpha = 0.55f), skipShape)
                        .press(0.97f) { leap(cut) }
                        .padding(start = 11.dp, end = 13.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(cut(5.dp))
                            .background(pal.acc2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            null,
                            tint = Color(0xFF0A0A0C),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            if (cut.kind.contains("ed")) "SKIP ENDING" else "SKIP OPENING",
                            color = if (on) Color(0xFF0A0A0C) else pal.acc2,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            if (left > 0) "$left" + "s left" else "tap to jump",
                            color = if (on) Color(0xFF0A0A0C).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.55f),
                            fontSize = 9.sp
                        )
                    }
                }
            }
            if (prompt > 0) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xF2101014))
                        .border(0.8.dp, Color(0x26FFFFFF), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "EP ${cfg.nextep} in ${prompt}s",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DarkBtn("Play now", true) {
                            prompt = 0
                            onNext()
                        }
                        DarkBtn("Cancel") { prompt = 0 }
                    }
                }
            }
        }

        if (panel) {
            SubPanel(
                list = subs,
                extra = side,
                mine = mine,
                onMine = { t -> equip(t) },
                lines = cues.size,
                note = cnote,
                size = ssize,
                onPick = { g, i -> picksub(g, i) },
                onSize = { store.conf(store.conf.copy(subsize = it)) },
                land = pane,
                onClose = { panel = false }
            )
        }
        if (opts) {
            BackHandler { opts = false }
            LaunchedEffect(Unit) {
                if (BuildConfig.tv) runCatching { pane.requestFocus() }
            }
            Side(
                "Playback",
                "${cfg.title}  •  EP ${cfg.ep}",
                onClose = { opts = false }
            ) {
                Column(
                    Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Group("Speed", listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).map {
                        (if (it == 1f) "1x" else "${it}x") to (speed == it)
                    }, land = pane) { i ->
                        val s = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)[i]
                        speed = s
                        store.conf(store.conf.copy(rate = s))
                    }
                    Group("Aspect", listOf("Fit", "Zoom", "Stretch").mapIndexed { i, s -> s to (aspect == i) }) { i ->
                        aspect = i
                    }
                    Group("Quality", listOf(0, 1080, 720, 480).map {
                        (if (it == 0) "Auto" else "${it}p") to (qual == it)
                    }) { i ->
                        qual = listOf(0, 1080, 720, 480)[i]
                    }
                    val tracks = auds.mapNotNull { g -> if (g.length > 0) g.getTrackFormat(0) to g else null }
                    if (tracks.size > 1) {
                        Group("Audio", tracks.map { (f, g) ->
                            (f.label ?: f.language ?: "Track") to g.isSelected
                        }) { i ->
                            pickaud(tracks[i].second, 0)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Screen("Subtitles", "the pick sticks for the next episode")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pick(if (subOn) langs.name(pref) else "Off", subOn) {
                                opts = false
                                panel = true
                            }
                            Pick("All tracks  ${subs.size}", false) { panel = true; opts = false }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Screen("Source", "every source was asked at once")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pick("Switch server", false) { onServers() }
                            Pick("Episodes", false) { onEps() }
                        }
                    }
                    Keys("arrows move  ·  ok picks  ·  back returns to the player")
                }
            }
        }
    }
}

@Composable
private fun Screen(name: String, note: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            name.uppercase(),
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.3.sp
        )
        Box(Modifier.width(18.dp).height(1.5.dp).background(Color(0x66FF6A00)))
        Text(note, color = Color.White.copy(alpha = 0.32f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Group(
    name: String,
    items: List<Pair<String, Boolean>>,
    land: androidx.compose.ui.focus.FocusRequester? = null,
    onPick: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Screen(name, "")
        items.chunked(3).forEachIndexed { r, line ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                line.forEachIndexed { c, one ->
                    val at = r * 3 + c
                    val mod = if (at == 0 && land != null) Modifier.weight(1f).focusRequester(land) else Modifier.weight(1f)
                    Pick(one.first, one.second, mod = mod) { onPick(at) }
                }
                repeat(3 - line.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PlayCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    note: String,
    size: androidx.compose.ui.unit.Dp,
    mod: Modifier = Modifier,
    click: () -> Unit
) {
    Box(
        mod
            .size(size)
            .clip(cut(8.dp))
            .background(Color(0x4D000000))
            .border(0.8.dp, Color(0x33FFFFFF), cut(8.dp))
            .press(0.96f, click),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, note, tint = Color.White, modifier = Modifier.size(size * 0.46f))
    }
}

@Composable
private fun DarkBtn(text: String, main: Boolean = false, click: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (main) Color.White else Color(0x33000000))
            .border(0.7.dp, if (main) Color.Transparent else Color(0x33FFFFFF), CircleShape)
            .press(0.96f, click)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            text,
            color = if (main) Color.Black else Color.White,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun Tap(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    note: String,
    size: androidx.compose.ui.unit.Dp = 42.dp,
    on: Boolean = true,
    pick: Boolean = false,
    tint: Color = Color.White,
    click: () -> Unit
) {
    val pal = localpal.current
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (pick) pal.acc.copy(alpha = 0.22f) else Color.Transparent)
            .then(if (pick) Modifier.border(2.dp, pal.acc, CircleShape) else Modifier)
            .then(if (on) Modifier.press(0.97f, click) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            note,
            tint = tint.copy(alpha = if (on) 1f else 0.3f),
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
private fun PlayMain(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    grip: FocusRequester? = null,
    pick: Boolean = false,
    click: () -> Unit
) {
    val pal = localpal.current
    var mod = Modifier.size(52.dp)
    if (grip != null) mod = mod.focusRequester(grip)
    Box(
        mod
            .clip(cut(10.dp))
            .background(pal.acc)
            .then(if (pick) Modifier.border(3.dp, Color.White, cut(10.dp)) else Modifier)
            .press(0.96f, click),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, "Play or pause", tint = Color(0xFF08080B), modifier = Modifier.size(27.dp))
    }
}

@Composable
private fun Seek(
    pos: Long,
    dur: Long,
    buf: Long,
    marks: List<aniskip.Cut> = emptyList(),
    pick: Boolean = false,
    mod: Modifier = Modifier,
    onScrub: (Long) -> Unit,
    onDone: (Long) -> Unit
) {
    var drag by remember { mutableStateOf(false) }
    var temp by remember { mutableLongStateOf(0L) }
    var wide by remember { mutableIntStateOf(1) }
    val pal = localpal.current
    val tint = pal.acc
    val at = if (drag) temp else pos
    val frac = if (dur > 0) (at.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val bfrac = if (dur > 0) (buf.toFloat() / dur).coerceIn(0f, 1f) else 0f
    var foc by remember { mutableStateOf(false) }
    Box(
        mod
            .height(30.dp)
            .onFocusChanged { foc = it.isFocused }
            .focusable()
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                val step = if (dur > 0) (dur / 90L).coerceIn(4000L, 30000L) else 10000L
                when (e.key) {
                    Key.DirectionLeft -> {
                        onDone((at - step).coerceIn(0L, if (dur > 0) dur else at))
                        true
                    }
                    Key.DirectionRight -> {
                        onDone((at + step).coerceIn(0L, if (dur > 0) dur else at + step))
                        true
                    }
                    Key.DirectionCenter, Key.Enter -> true
                    else -> false
                }
            }
            .background(if (foc || pick) pal.acc.copy(alpha = 0.10f) else Color.Transparent)
            .border(if (foc || pick) 2.dp else 0.dp, if (foc || pick) pal.acc else Color.Transparent, cut(6.dp))
            .onSizeChanged { wide = it.width }
            .pointerInput(dur) {
                detectTapGestures { o ->
                    val t = (o.x / size.width * dur).toLong().coerceIn(0L, dur)
                    onDone(t)
                }
            }
            .pointerInput(dur) {
                detectHorizontalDragGestures(
                    onDragStart = { o ->
                        drag = true
                        temp = (o.x / wide * dur).toLong().coerceIn(0L, dur)
                    },
                    onHorizontalDrag = { _, dx ->
                        temp = (temp + (dx / wide * dur).toLong()).coerceIn(0L, dur)
                        onScrub(temp)
                    },
                    onDragEnd = {
                        drag = false
                        onDone(temp)
                    },
                    onDragCancel = { drag = false }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(Modifier.fillMaxWidth().height(30.dp)) {
            val cy = size.height / 2f
            val r = (size.height * 0.06f).coerceAtLeast(2.dp.toPx())
            drawRoundRect(
                color = Color(0x40FFFFFF),
                topLeft = Offset(0f, cy - r / 2f),
                size = Size(size.width, r),
                cornerRadius = CornerRadius(r / 2f)
            )
            if (dur > 0) {
                for (m in marks) {
                    val a = (m.start * 1000.0).toLong()
                    val z = (m.end * 1000.0).toLong()
                    if (z <= 0 || a >= dur) continue
                    val x0 = (size.width * (a.toFloat() / dur)).coerceIn(0f, size.width)
                    val x1 = (size.width * (z.toFloat() / dur)).coerceIn(0f, size.width)
                    val w = (x1 - x0).coerceAtLeast(2.dp.toPx()).coerceAtMost(size.width - x0)
                    drawRoundRect(
                        color = pal.acc2,
                        topLeft = Offset(x0, cy - r / 2f),
                        size = Size(w, r),
                        cornerRadius = CornerRadius(r / 2f)
                    )
                }
            }
            drawRoundRect(
                color = Color(0x8CFFFFFF),
                topLeft = Offset(0f, cy - r / 2f),
                size = Size(size.width * bfrac, r),
                cornerRadius = CornerRadius(r / 2f)
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(0f, cy - r / 2f),
                size = Size(size.width * frac, r),
                cornerRadius = CornerRadius(r / 2f)
            )
            drawCircle(
                color = pal.acc2,
                radius = (if (drag) 8.5.dp else 6.5.dp).toPx(),
                center = Offset(size.width * frac, cy)
            )
            drawCircle(
                color = Color(0xFF0A0A0C),
                radius = (if (drag) 5.5.dp else 4.dp).toPx(),
                center = Offset(size.width * frac, cy)
            )
            drawCircle(
                color = Color.White,
                radius = (if (drag) 3.dp else 2.dp).toPx(),
                center = Offset(size.width * frac, cy)
            )
        }
        if (drag) {
            Box(
                Modifier
                    .graphicsLayer { translationX = (wide * frac) - 28f }
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xE6000000))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(fmt.clock(temp), color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SubPanel(
    list: List<Tracks.Group>,
    extra: List<Subs>,
    mine: Subs?,
    onMine: (Subs?) -> Unit,
    lines: Int,
    note: String,
    size: Int,
    onPick: (Tracks.Group?, Int) -> Unit,
    onSize: (Int) -> Unit,
    land: FocusRequester,
    onClose: () -> Unit
) {
    val pal = localpal.current
    val embedded = list.mapNotNull { g -> if (g.length > 0) g.getTrackFormat(0) to g else null }
        .sortedBy { langs.name(it.first.language).lowercase() }
    val external = extra.sortedBy { langs.name(it.lang).lowercase() }
    val total = embedded.size + external.size
    val off = mine == null && embedded.none { it.second.isSelected }
    LaunchedEffect(Unit) {
        if (BuildConfig.tv) runCatching { land.requestFocus() }
    }
    Side(
        "Subtitles",
        when {
            total == 0 -> "this source exposed no tracks"
            note.isNotEmpty() -> "${total} track${if (total == 1) "" else "s"}  •  $note"
            else -> "${total} track${if (total == 1) "" else "s"}  •  $lines lines ready"
        },
        onClose = onClose
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .aim(cut(8.dp), 1.02f)
                .focusRequester(land)
                .clip(cut(8.dp))
                .background(if (off) Color(0x1FFFFFFF) else Color(0x0DFFFFFF))
                .border(0.7.dp, if (off) Color(0x66FFFFFF) else Color(0x1AFFFFFF), cut(8.dp))
                .press(0.96f) {
                    onMine(null)
                    onPick(null, 0)
                }
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "OFF",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.9.sp,
                modifier = Modifier.weight(1f)
            )
            if (off) Icon(Icons.Rounded.Check, null, tint = pal.acc, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (external.isNotEmpty()) {
                item { Screen("From the source", "converted to WebVTT on device") }
                items(external.size) { i ->
                    val t = external[i]
                    val code = langs.norm(t.lang)
                    val on = mine?.url == t.url
                    Track(code, t.label, on) { onMine(t) }
                }
            }
            if (embedded.isNotEmpty()) {
                item { Screen("In the stream", "burned into the manifest") }
                items(embedded.size) { i ->
                    val f = embedded[i].first
                    val code = langs.norm(f.language ?: f.label)
                    Track(code, f.label, embedded[i].second.isSelected) {
                        onMine(null)
                        onPick(embedded[i].second, 0)
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
            Screen("Text size", "captions sit above the controls")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(80 to "S", 100 to "M", 125 to "L", 150 to "XL").forEach { one ->
                    Pick(one.second, size == one.first, mod = Modifier.weight(1f)) { onSize(one.first) }
                }
            }
            Keys("arrows move  ·  ok picks  ·  back returns to the player")
        }
    }
}

@Composable
private fun Keys(line: String) {
    if (!BuildConfig.tv) return
    Text(
        line.uppercase(),
        color = Color.White.copy(alpha = 0.34f),
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        modifier = Modifier.padding(top = 10.dp)
    )
}

@Composable
private fun Track(code: String, label: String?, on: Boolean, click: () -> Unit) {
    val pal = localpal.current
    Row(
        Modifier
            .fillMaxWidth()
            .aim(cut(8.dp), 1.02f)
            .clip(cut(8.dp))
            .background(if (on) pal.acc.copy(alpha = 0.16f) else Color(0x0DFFFFFF))
            .border(0.7.dp, if (on) pal.acc.copy(alpha = 0.5f) else Color(0x1AFFFFFF), cut(8.dp))
            .press(0.96f, click)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(30.dp, 24.dp)
                .clip(cut(5.dp))
                .background(if (on) pal.acc else Color(0x1FFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                langs.flag(code),
                color = if (on) Color(0xFF0A0A0C) else Color.White.copy(alpha = 0.8f),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.4.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                langs.name(code),
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val note = label?.takeIf { it.isNotBlank() && !it.equals(langs.name(code), true) } ?: code
            Text(
                note,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 9.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (on) Icon(Icons.Rounded.Check, null, tint = pal.acc, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun Captions(text: String, conf: com.ominime.app.data.Conf, ssize: Int, show: Boolean) {
    val tone = when (conf.subcolor) {
        "amber" -> Color(0xFFFFC02E)
        "orange" -> Color(0xFFFF6A00)
        "green" -> Color(0xFF32D74B)
        "cyan" -> Color(0xFF50DCFF)
        "pink" -> Color(0xFFFF3D71)
        "soft" -> Color(0xFFE2E2E8)
        else -> Color.White
    }
    val face = when (conf.subfont) {
        "serif" -> FontFamily.Serif
        "mono" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }
    val weight = when {
        conf.subbold && conf.subwide -> FontWeight.Black
        conf.subbold -> FontWeight.Bold
        conf.subwide -> FontWeight.SemiBold
        else -> FontWeight.Normal
    }
    val base = (if (BuildConfig.tv) 23f else 15f) * (ssize.coerceIn(70, 170) / 100f)
    val step = when (conf.subedge) {
        3 -> 2.4.dp
        2 -> 1.8.dp
        1 -> 1.1.dp
        else -> 0.dp
    }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        val wide = this.maxWidth
        val room = when (conf.subpos) {
            1 -> 0.26f
            2 -> 0.44f
            else -> 0.05f
        }
        val lift = maxHeight * (
            if (BuildConfig.tv) {
                room * 0.9f + (if (show) 0.26f else 0.04f)
            } else {
                room + (if (show) 0.2f else 0f)
            }
            )
        Box(
            Modifier
                .fillMaxWidth()
                .padding(bottom = lift)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                Modifier
                    .widthIn(max = wide * 0.86f)
                    .then(
                        if (conf.subbox) Modifier
                            .clip(cut(4.dp))
                            .background(Color(0xB307070A))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (step > 0.dp) {
                    val push = listOf(-1f to 0f, 1f to 0f, 0f to -1f, 0f to 1f, -1f to -1f, 1f to 1f)
                    push.forEach { (x, y) ->
                        Text(
                            text,
                            color = Color.Black,
                            fontFamily = face,
                            fontSize = base.sp,
                            lineHeight = (base * 1.2f).sp,
                            fontWeight = weight,
                            fontStyle = if (conf.subital) FontStyle.Italic else FontStyle.Normal,
                            letterSpacing = (if (conf.subwide) 0.6f else 0f).sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.offset(x = (x * step.value).dp, y = (y * step.value).dp)
                        )
                    }
                }
                Text(
                    text,
                    color = tone,
                    fontFamily = face,
                    fontSize = base.sp,
                    lineHeight = (base * 1.2f).sp,
                    fontWeight = weight,
                    fontStyle = if (conf.subital) FontStyle.Italic else FontStyle.Normal,
                    letterSpacing = (if (conf.subwide) 0.6f else 0f).sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
