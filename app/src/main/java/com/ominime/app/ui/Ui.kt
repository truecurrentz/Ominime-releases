package com.ominime.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.composed
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.ominime.app.BuildConfig
import com.ominime.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest

fun cut(rad: Dp = 10.dp, corner: Float = 0.34f): Shape = GenericShape { size, _ ->
    val r = rad.value * 2.6f
    val c = r.coerceAtMost(size.minDimension * corner)
    moveTo(c, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - c)
    lineTo(size.width - c, size.height)
    lineTo(0f, size.height)
    lineTo(0f, c)
    close()
}

@Composable
fun Glass(
    mod: Modifier = Modifier,
    rad: Dp = 24.dp,
    tone: Color? = null,
    line: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val p = localpal.current
    val base = tone ?: p.glass
    val shape = RoundedCornerShape(rad)
    Box(
        mod
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(base.copy(alpha = (base.alpha * 1.7f).coerceAtMost(1f)), base)
                )
            )
            .then(if (line) Modifier.border(0.7.dp, p.line, shape) else Modifier),
        content = content
    )
}

@Composable
fun Shine(mod: Modifier = Modifier, rad: Dp = 18.dp) {
    val p = localpal.current
    val t = rememberInfiniteTransition(label = "shine")
    val x by t.animateFloat(
        initialValue = -260f,
        targetValue = 720f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "x"
    )
    Box(
        mod.clip(RoundedCornerShape(rad))
            .background(
                Brush.linearGradient(
                    listOf(p.lift, p.card.copy(alpha = 0.7f), p.lift),
                    start = Offset(x, 0f),
                    end = Offset(x + 320f, 260f)
                )
            )
    )
}

@Composable
fun Pic(url: String?, mod: Modifier = Modifier, rad: Dp = 18.dp, ratio: Float? = null, shape: Shape? = null) {
    val p = localpal.current
    val form = shape ?: RoundedCornerShape(rad)
    Box(
        mod
            .then(if (ratio != null) Modifier.aspectRatio(ratio) else Modifier)
            .clip(form)
            .background(p.lift)
    ) {
        if (url.isNullOrBlank()) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Movie, null, tint = p.mute.copy(alpha = 0.45f), modifier = Modifier.size(26.dp))
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(180).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
fun Circle(
    icon: ImageVector,
    note: String,
    size: Dp = 40.dp,
    tint: Color? = null,
    bg: Color? = null,
    click: () -> Unit
) {
    val p = localpal.current
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg ?: p.glass)
            .border(0.7.dp, p.line, CircleShape)
            .press(0.96f, click),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, note, tint = tint ?: p.text, modifier = Modifier.size(size * 0.46f))
    }
}

@Composable
fun Btn(
    text: String,
    icon: ImageVector? = null,
    main: Boolean = false,
    mod: Modifier = Modifier,
    click: () -> Unit
) {
    val p = localpal.current
    val shape = cut(9.dp)
    val fg = if (main) Color(0xFF0A0A0C) else p.text
    Row(
        mod
            .clip(shape)
            .background(if (main) p.acc else p.glass)
            .then(if (main) Modifier else Modifier.border(0.8.dp, p.line, shape))
            .press(0.96f, click)
            .padding(horizontal = 19.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(17.dp))
        Text(
            text.uppercase(),
            color = fg,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.1.sp
        )
    }
}

@Composable
fun Label(text: String, sel: Boolean = false, mod: Modifier = Modifier, click: () -> Unit) {
    val p = localpal.current
    val shape = cut(7.dp)
    Box(
        mod
            .clip(shape)
            .background(if (sel) p.acc else p.glass)
            .then(if (sel) Modifier else Modifier.border(0.7.dp, p.line, shape))
            .press(0.96f, click)
            .padding(horizontal = 13.dp, vertical = 8.dp)
    ) {
        Text(
            text.uppercase(),
            color = if (sel) Color(0xFF0A0A0C) else p.text.copy(alpha = 0.9f),
            fontSize = 11.5.sp,
            fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
fun Head(title: String, action: String? = null, onAction: () -> Unit = {}) {
    val p = localpal.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 22.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(3.5.dp, 20.dp).clip(cut(2.dp)).background(p.acc))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title.uppercase(),
                color = p.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.4.sp
            )
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.2f)
                    .height(1.5.dp)
                    .background(Brush.horizontalGradient(listOf(p.acc.copy(alpha = 0.85f), Color.Transparent)))
            )
        }
        if (action != null) {
            Text(
                action.uppercase(),
                color = p.mute,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
fun Reveal(delay: Int = 0, mod: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delay > 0) kotlinx.coroutines.delay(delay.toLong())
        on = true
    }
    val a by animateFloatAsState(if (on) 1f else 0f, tween(if (on) 420 else 0), label = "a")
    val y by animateDpAsState(if (on) 0.dp else 26.dp, tween(460), label = "y")
    Box(mod.graphicsLayer { alpha = a; translationY = y.toPx() }, content = content)
}

@Composable
fun Sheet(
    title: String,
    note: String? = null,
    height: Float = 0.82f,
    onClose: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val p = localpal.current
    val slide = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(Unit) { slide.animateTo(0f, tween(300)) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f * (1f - slide.value)))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() }
        )
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(height)
                .offset(y = (slide.value * 620f).dp)
                .clip(cut(24.dp, 0.16f))
                .background(p.card)
                .background(
                    Brush.verticalGradient(
                        listOf(p.lift, p.card, p.bg)
                    )
                )
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
        ) {
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .align(Alignment.CenterHorizontally)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(p.line)
            )
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    if (note != null) Text(note, color = p.mute, fontSize = 12.sp)
                }
                Circle(Icons.Rounded.Close, "Close", 34.dp) { onClose() }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(p.acc, p.acc2.copy(alpha = 0.25f), Color.Transparent)))
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun Divider(mod: Modifier = Modifier) {
    val p = localpal.current
    Box(mod.fillMaxWidth().height(0.7.dp).background(p.line))
}

@Composable
fun Loader(mod: Modifier = Modifier, size: Dp = 22.dp) {
    val p = localpal.current
    CircularProgressIndicator(
        modifier = mod.size(size),
        color = p.text,
        strokeWidth = 2.dp
    )
}

@Composable
fun Score(v: Int?) {
    if (v == null || v <= 0) return
    val p = localpal.current
    Row(
        Modifier
            .clip(CircleShape)
            .background(Color(0xCC000000))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(Icons.Rounded.Star, null, tint = p.acc2, modifier = Modifier.size(11.dp))
        Text(String.format(java.util.Locale.US, "%.1f", v / 10f), color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Meta(text: String, mod: Modifier = Modifier) {
    val p = localpal.current
    Text(text, color = p.mute, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = mod)
}

@Composable
fun Dots(n: Int, at: Int, mod: Modifier = Modifier) {
    val p = localpal.current
    Row(mod, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(n) { i ->
            val w by animateDpAsState(if (i == at) 20.dp else 6.dp, label = "dot")
            Box(
                Modifier
                    .height(5.dp)
                    .width(w)
                    .clip(CircleShape)
                    .background(if (i == at) p.text else p.text.copy(alpha = 0.28f))
            )
        }
    }
}

@Composable
fun Oops(note: String, mod: Modifier = Modifier, retry: () -> Unit) {
    val p = localpal.current
    Column(
        mod
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.CloudOff, null, tint = p.mute, modifier = Modifier.size(34.dp))
        Text(note, color = p.mute, fontSize = 13.sp, textAlign = TextAlign.Center)
        Btn("Try again", icon = null) { retry() }
    }
}

@Composable
fun Blank(
    title: String,
    note: String = "",
    icon: ImageVector = Icons.Rounded.CloudOff,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    val p = localpal.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = p.mute.copy(alpha = 0.7f), modifier = Modifier.size(34.dp))
        Text(title, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (note.isNotBlank()) Text(note, color = p.mute, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 19.sp)
        if (action != null) {
            Spacer(Modifier.height(4.dp))
            Btn(action, main = true) { onAction() }
        }
    }
}

@Composable
fun Seg(options: List<String>, at: Int, mod: Modifier = Modifier, onPick: (Int) -> Unit) {
    val p = localpal.current
    Row(
        mod
            .clip(cut(8.dp))
            .background(p.glass)
            .border(0.7.dp, p.line, cut(8.dp))
            .padding(4.dp)
    ) {
        options.forEachIndexed { i, s ->
            val on = i == at
            Box(
                Modifier
                    .weight(1f)
                    .clip(cut(6.dp))
                    .background(if (on) p.acc else Color.Transparent)
                    .clickable { onPick(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    s.uppercase(),
                    color = if (on) Color(0xFF0A0A0C) else p.mute,
                    fontSize = 11.5.sp,
                    fontWeight = if (on) FontWeight.Black else FontWeight.Medium,
                    letterSpacing = 0.7.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun Side(
    title: String,
    note: String? = null,
    wide: Dp = 412.dp,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val p = localpal.current
    val slide = remember { Animatable(1f) }
    LaunchedEffect(Unit) { slide.animateTo(0f, tween(260)) }
    val shape = cut(16.dp, 0.18f)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.58f * (1f - slide.value * 0.4f)))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() }
        )
        Column(
            Modifier
                .width(wide)
                .fillMaxHeight()
                .graphicsLayer { translationX = slide.value * 470f }
                .clip(shape)
                .background(p.card.copy(alpha = 0.97f))
                .border(0.8.dp, p.line, shape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(3.5.dp, 18.dp).clip(cut(2.dp)).background(p.acc))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title.uppercase(),
                        color = p.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.4.sp
                    )
                    if (note != null) Text(
                        note,
                        color = p.mute,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(cut(7.dp))
                        .background(p.glass)
                        .border(0.7.dp, p.line, cut(7.dp))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, "Close", tint = p.text, modifier = Modifier.size(15.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun Pick(text: String, on: Boolean, note: String? = null, mod: Modifier = Modifier, click: () -> Unit) {
    val p = localpal.current
    val shape = cut(7.dp)
    Row(
        mod
            .clip(shape)
            .background(if (on) p.acc else p.glass)
            .border(0.7.dp, if (on) Color.Transparent else p.line, shape)
            .press(0.96f, click)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text.uppercase(),
            color = if (on) Color(0xFF0A0A0C) else p.text.copy(alpha = 0.92f),
            fontSize = 11.sp,
            fontWeight = if (on) FontWeight.Black else FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            maxLines = 1
        )
        if (note != null) Text(
            note,
            color = if (on) Color(0x990A0A0C) else p.mute,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            maxLines = 1
        )
    }
}

@Composable
fun Discord(onClick: () -> Unit) {
    val p = localpal.current
    val mark = Color(0xFF5865F2)
    val deep = Color(0xFF3B45D6)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(cut(14.dp, 0.22f))
            .background(p.lift)
            .background(
                Brush.horizontalGradient(
                    listOf(mark.copy(alpha = 0.34f), mark.copy(alpha = 0.12f), Color.Transparent)
                )
            )
            .border(0.9.dp, mark.copy(alpha = 0.5f), cut(14.dp, 0.22f))
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.horizontalGradient(listOf(p.acc.copy(alpha = 0.10f), Color.Transparent)))
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(cut(12.dp, 0.3f))
                    .background(mark)
                    .border(0.9.dp, Color.White.copy(alpha = 0.2f), cut(12.dp, 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.discord),
                    contentDescription = "Discord",
                    modifier = Modifier.size(34.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "JOIN THE DISCORD",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp
                )
                Text(
                    "Requests, updates and people to watch with",
                    color = p.text.copy(alpha = 0.62f),
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                Modifier
                    .clip(cut(8.dp, 0.3f))
                    .background(mark)
                    .border(0.8.dp, Color.White.copy(alpha = 0.22f), cut(8.dp, 0.3f))
                    .padding(start = 11.dp, end = 9.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "JOIN",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Icon(
                    Icons.Rounded.KeyboardArrowRight,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
fun Stat(
    current: String,
    saved: Boolean = true,
    title: String = "",
    cover: String? = null,
    episode: Int = 0,
    onPick: (String) -> Unit,
    onClose: () -> Unit,
    onRemove: () -> Unit
) {
    val p = localpal.current
    val rows = listOf(
        Triple("Watching", "CURRENT", Icons.Rounded.PlayArrow),
        Triple("Plan to watch", "PLANNING", Icons.Rounded.Bookmark),
        Triple("Completed", "COMPLETED", Icons.Rounded.Check),
        Triple("On hold", "PAUSED", Icons.Rounded.Pause),
        Triple("Dropped", "DROPPED", Icons.Rounded.Close)
    )
    val card = cut(18.dp, 0.18f)
    val pop = remember { androidx.compose.animation.core.Animatable(0.94f) }
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.62f, stiffness = 420f))
        step = 1
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Box(
            Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .widthIn(max = 366.dp)
                    .fillMaxWidth()
                    .graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = pop.value }
                    .clip(card)
                    .background(p.card)
                    .background(Brush.verticalGradient(listOf(p.lift, p.card, p.bg)))
                    .border(1.dp, p.acc.copy(alpha = 0.55f), card)
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    Box(
                        Modifier
                            .size(54.dp, 40.dp)
                            .clip(cut(9.dp, 0.3f))
                            .background(p.glass)
                            .border(0.8.dp, p.acc.copy(alpha = 0.35f), cut(9.dp, 0.3f))
                    ) {
                        if (cover != null) androidx.compose.foundation.Image(
                            painter = coil.compose.rememberAsyncImagePainter(cover),
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            title.ifBlank { "TRACK THIS" },
                            color = p.text,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            when {
                                !saved -> "Not on your list yet"
                                episode > 0 -> "On your list  •  EP $episode"
                                else -> "On your list"
                            },
                            color = p.mute,
                            fontSize = 10.5.sp
                        )
                    }
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(cut(8.dp))
                            .background(p.glass)
                            .border(0.8.dp, p.line, cut(8.dp))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Close, "Close", tint = p.text.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Brush.horizontalGradient(listOf(p.acc.copy(alpha = 0.55f), Color.Transparent)))
                )
                rows.forEachIndexed { at, (name, key, icon) ->
                    val on = current == key
                    val shape = cut(10.dp)
                    val lit by animateFloatAsState(
                        targetValue = if (step == 1) 1f else 0f,
                        animationSpec = tween(260, delayMillis = at * 35),
                        label = "row"
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = if (step == 1) lit else 0f; translationX = (1f - lit) * 26f }
                            .clip(shape)
                            .background(
                                if (on) Brush.horizontalGradient(listOf(p.acc.copy(alpha = 0.3f), p.acc.copy(alpha = 0.04f)))
                                else Brush.horizontalGradient(listOf(p.lift, p.lift.copy(alpha = 0.45f)))
                            )
                            .border(0.9.dp, if (on) p.acc.copy(alpha = 0.85f) else p.line, shape)
                            .press(0.98f) { onPick(key) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(cut(7.dp, 0.35f))
                                .background(if (on) Brush.linearGradient(listOf(p.acc, p.acc2)) else Brush.linearGradient(listOf(p.glass, p.glass))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                null,
                                tint = if (on) Color(0xFF0A0A0C) else p.text.copy(alpha = 0.85f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            name,
                            color = if (on) p.text else p.text.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .border(1.4.dp, if (on) p.acc else p.line, CircleShape)
                                .background(if (on) p.acc else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (on) Box(Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF0A0A0C)))
                        }
                    }
                }
                if (saved) {
                    Spacer(Modifier.height(1.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(cut(9.dp))
                            .background(Color(0x1FFF3B30))
                            .border(0.8.dp, Color(0x44FF3B30), cut(9.dp))
                            .press { onRemove() }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Delete, null, tint = Color(0xFFFF6B63), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "REMOVE FROM LIST",
                            color = Color(0xFFFF6B63),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.1.sp
                        )
                    }
                }
            }
        }
    }
}

fun Modifier.press(scale: Float = 0.965f, onClick: () -> Unit): Modifier = this.composed {
    val src = remember { MutableInteractionSource() }
    val held by src.collectIsPressedAsState()
    val aim by src.collectIsFocusedAsState()
    val pal = localpal.current
    val up = if (BuildConfig.tv && aim) 1.05f else 1f
    val s by animateFloatAsState(
        targetValue = if (held) scale else up,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "press"
    )
    this.graphicsLayer { scaleX = s; scaleY = s }
        .then(if (BuildConfig.tv && aim) Modifier.border(2.dp, pal.acc, cut(9.dp)) else Modifier)
        .clickable(interactionSource = src, indication = null, onClick = onClick)
}
