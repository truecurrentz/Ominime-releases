package com.ominime.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.ominime.app.BuildConfig
import com.ominime.app.R
import com.ominime.app.data.cache
import com.ominime.app.data.caps
import com.ominime.app.data.langs
import com.ominime.app.data.net
import com.ominime.app.data.alerts
import com.ominime.app.data.store
import coil.Coil

private val tones = listOf(
    "white" to Color(0xFFFFFFFF),
    "soft" to Color(0xFFE2E2E8),
    "amber" to Color(0xFFFFC02E),
    "orange" to Color(0xFFFF6A00),
    "green" to Color(0xFF32D74B),
    "cyan" to Color(0xFF50DCFF),
    "pink" to Color(0xFFFF3D71)
)

@Composable
fun Settings() {
    val p = localpal.current
    val ctx = LocalContext.current
    val d by store.live.collectAsState()
    val c = d.conf
    var freed by remember { mutableStateOf(cache.size() + caps.size()) }
    val list = rememberLazyListState()
    track(list)

    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize().background(p.bg).statusBarsPadding(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 150.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("SETTINGS", color = p.text, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("Tune the app, and make captions yours.", color = p.mute, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
            }
        }

        item {
            Block("Appearance") {
                Text("Theme", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Seg(listOf("System", "Dark", "Light", "Black"), c.dark) { i ->
                    store.conf(c.copy(dark = i))
                }
                Toggle("Romaji titles", "Show original titles instead of English", c.romaji) { v ->
                    store.conf(c.copy(romaji = v))
                }
                Text("Accent", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    com.ominime.app.ui.ink.tones.forEach { one ->
                        val on = c.acc == one.first
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(cut(7.dp))
                                .background(one.second)
                                .border(if (on) 2.dp else 0.8.dp, if (on) p.text else p.line, cut(7.dp))
                                .clickable { store.conf(c.copy(acc = one.first)) }
                        )
                    }
                }
                Text(
                    "The accent drives buttons, chips, the dock indicator and the player bar.",
                    color = p.mute,
                    fontSize = 10.5.sp
                )
            }
        }

        item {
            Block("Subtitles", "Drawn on top of the video, saved for every episode") {
                Text("Preview", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Preview(c)

                Text("Default language", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Label("Off", c.sub == "off") { store.conf(c.copy(sub = "off")) }
                    listOf("en", "es", "pt", "fr", "de", "it", "ar", "id", "ru", "ja", "ko", "zh").forEach { code ->
                        Label(langs.name(code), c.sub == code) { store.conf(c.copy(sub = code)) }
                    }
                }

                Text("Text size", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Seg(
                    listOf("Small", "Normal", "Large", "Huge"),
                    when {
                        c.subsize <= 90 -> 0
                        c.subsize <= 110 -> 1
                        c.subsize <= 135 -> 2
                        else -> 3
                    }
                ) { i ->
                    store.conf(c.copy(subsize = listOf(80, 100, 125, 150)[i]))
                }

                Toggle("Bold text", "Heavier strokes, easier to read fast", c.subbold) { v ->
                    store.conf(c.copy(subbold = v))
                }

                Text("Colour", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    tones.forEach { (key, tone) ->
                        val on = c.subcolor == key
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(cut(7.dp))
                                .background(tone)
                                .border(
                                    if (on) 2.dp else 0.8.dp,
                                    if (on) p.acc else p.line,
                                    cut(7.dp)
                                )
                                .clickable { store.conf(c.copy(subcolor = key)) }
                        )
                    }
                }

                Text("Font", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Seg(listOf("Sans", "Serif", "Mono", "Round"), when (c.subfont) {
                    "serif" -> 1
                    "mono" -> 2
                    "cursive" -> 3
                    else -> 0
                }) { i ->
                    store.conf(c.copy(subfont = listOf("sans", "serif", "mono", "cursive")[i]))
                }
                Toggle("Italic", "Slanted text", c.subital) { v -> store.conf(c.copy(subital = v)) }
                Toggle("Wide letter spacing", "Extra room between letters", c.subwide) { v ->
                    store.conf(c.copy(subwide = v))
                }

                Text("Outline", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Seg(
                    listOf("None", "Thin", "Medium", "Heavy"),
                    c.subedge.coerceIn(0, 3)
                ) { i ->
                    store.conf(c.copy(subedge = i))
                }
                Toggle("Dark plate behind text", "A solid strip for very bright scenes", c.subbox) { v ->
                    store.conf(c.copy(subbox = v))
                }

                Text("Placement", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Seg(listOf("Low", "Middle", "High"), c.subpos) { i ->
                    store.conf(c.copy(subpos = i))
                }
                Note("Low keeps captions at the very bottom when the controls hide, and lifts them clear when the bar appears.")
            }
        }

        item {
            Block("New episodes") {
                Toggle(
                    "Episode alerts",
                    "Automatic background check, phone notification and an Activity entry whenever a show on your list airs",
                    c.alerts
                ) { v ->
                    store.conf(c.copy(alerts = v))
                    if (v) alerts.start(ctx) else alerts.stop(ctx)
                }
                Note("Runs in the background even when Ominime is closed. Titles marked Completed or Dropped stay quiet.")
            }
        }

        item {
            Block("Playback") {
                Toggle("Autoplay next episode", "Jump straight into the next one", c.auto) { v ->
                    store.conf(c.copy(auto = v))
                }
                Toggle("Skip opening button", "Offer a jump button when AniSkip knows the timing", c.intro) { v ->
                    store.conf(c.copy(intro = v))
                }
                Toggle(
                    "Auto skip OP and ED",
                    "Jump past the opening and ending on its own when the timing is known",
                    c.skipauto
                ) { v ->
                    store.conf(c.copy(skipauto = v))
                }
                Text("Preferred audio", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Seg(listOf("Sub", "Dub"), if (c.audio == "dub") 1 else 0) { i ->
                    store.conf(c.copy(audio = if (i == 1) "dub" else "sub"))
                }
            }
        }

        item {
            Block("Storage") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Cached data", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(fmt.size(freed) + " of artwork, captions and metadata", color = p.mute, fontSize = 11.5.sp)
                    }
                    Circle(Icons.Rounded.Delete, "Clear", 36.dp) {
                        net.flush()
                        caps.wipe()
                        runCatching {
                            Coil.imageLoader(ctx).memoryCache?.clear()
                            Coil.imageLoader(ctx).diskCache?.clear()
                        }
                        freed = 0
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clip(CircleShape).clickable { store.wipe() }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Clear watch history", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        item {
            Block("About") {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(Modifier.fillMaxWidth(0.72f).aspectRatio(4.06f)) {
                        Image(
                            painter = painterResource(R.drawable.logo),
                            contentDescription = "Ominime",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        "version v" + BuildConfig.VERSION_NAME,
                        color = p.mute,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        "Ominime is a free app for everyone. No account, no subscription, no ads, and nothing to configure: open a title, pick an episode, and every source is asked at once.",
                        color = p.mute,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(2.dp))
                Discord {
                    runCatching {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/ZmkNZUEFqV")))
                    }
                }
                Text(
                    "Ominime does not host any media. Everything is streamed from third party sites, for personal use only.",
                    color = p.mute.copy(alpha = 0.55f),
                    fontSize = 10.5.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun Preview(c: com.ominime.app.data.Conf) {
    val p = localpal.current
    val tone = tones.firstOrNull { it.first == c.subcolor }?.second ?: Color.White
    val face = when (c.subfont) {
        "serif" -> androidx.compose.ui.text.font.FontFamily.Serif
        "mono" -> androidx.compose.ui.text.font.FontFamily.Monospace
        "cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
        else -> androidx.compose.ui.text.font.FontFamily.SansSerif
    }
    val weight = when {
        c.subbold && c.subwide -> FontWeight.Black
        c.subbold -> FontWeight.Bold
        c.subwide -> FontWeight.SemiBold
        else -> FontWeight.Normal
    }
    val size = 14f * (c.subsize.coerceIn(70, 170) / 100f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(126.dp)
            .clip(cut(12.dp))
            .background(Color(0xFF05050A))
            .border(0.8.dp, p.line, cut(12.dp))
    ) {
        Box(
            Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color(0xFF1B2233), Color(0xFF05050A))
                )
            )
        )
        Box(
            Modifier.fillMaxSize().padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .then(
                        if (c.subbox) Modifier
                            .clip(cut(4.dp))
                            .background(Color(0xB307070A))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (c.subedge > 0) {
                    val step = when (c.subedge) {
                        2 -> 1.9.dp
                        3 -> 2.6.dp
                        else -> 1.2.dp
                    }
                    listOf(-1f to 0f, 1f to 0f, 0f to -1f, 0f to 1f, -1f to -1f, 1f to 1f).forEach { (x, y) ->
                        Text(
                            "This is how captions will look",
                            color = Color.Black,
                            fontFamily = face,
                            fontSize = size.sp,
                            lineHeight = (size * 1.22f).sp,
                            fontWeight = weight,
                            fontStyle = if (c.subital) androidx.compose.ui.text.font.FontStyle.Italic
                            else androidx.compose.ui.text.font.FontStyle.Normal,
                            letterSpacing = (if (c.subwide) 0.6f else 0f).sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.offset(x = (x * step.value).dp, y = (y * step.value).dp)
                        )
                    }
                }
                Text(
                    "This is how captions will look",
                    color = tone,
                    fontFamily = face,
                    fontSize = size.sp,
                    lineHeight = (size * 1.22f).sp,
                    fontWeight = weight,
                    fontStyle = if (c.subital) androidx.compose.ui.text.font.FontStyle.Italic
                    else androidx.compose.ui.text.font.FontStyle.Normal,
                    letterSpacing = (if (c.subwide) 0.6f else 0f).sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        Text(
            "PREVIEW",
            color = p.mute.copy(alpha = 0.5f),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.4.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(9.dp)
        )
    }
}

@Composable
private fun Note(text: String) {
    Text(text, color = localpal.current.mute.copy(alpha = 0.8f), fontSize = 10.5.sp, lineHeight = 15.sp)
}

@Composable
private fun Link(text: String, click: () -> Unit) {
    val p = localpal.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(cut(8.dp))
            .background(p.glass)
            .border(0.7.dp, p.line, cut(8.dp))
            .clickable(onClick = click)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(3.dp, 14.dp).clip(cut(1.5.dp)).background(p.acc))
        Spacer(Modifier.width(9.dp))
        Text(text, color = p.text.copy(alpha = 0.86f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Block(title: String, note: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val p = localpal.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(3.5.dp, 16.dp).clip(cut(2.dp)).background(p.acc))
            Text(
                title.uppercase(),
                color = p.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.4.sp
            )
        }
        if (note != null) Text(
            note,
            color = p.mute.copy(alpha = 0.8f),
            fontSize = 10.5.sp,
            modifier = Modifier.padding(start = 12.dp, top = 3.dp, bottom = 2.dp)
        )
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(cut(16.dp))
                .background(p.card.copy(alpha = 0.7f))
                .border(0.8.dp, p.line, cut(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun Toggle(name: String, note: String, on: Boolean, set: (Boolean) -> Unit) {
    val p = localpal.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Text(name, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (note.isNotBlank()) Text(note, color = p.mute, fontSize = 11.sp)
        }
        Switch(
            checked = on,
            onCheckedChange = set,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0A0A0C),
                checkedTrackColor = p.acc,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = p.mute,
                uncheckedTrackColor = p.lift,
                uncheckedBorderColor = p.line
            )
        )
    }
}
