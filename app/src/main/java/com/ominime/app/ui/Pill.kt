package com.ominime.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Pill(at: String, onTab: (String) -> Unit) {
    val p = localpal.current
    var open by remember(at) { mutableStateOf(at) }
    val tabs = listOf(
        Triple("home", "Home", Icons.Rounded.Home),
        Triple("schedule", "Schedule", Icons.Rounded.CalendarMonth),
        Triple("list", "My List", Icons.Rounded.Bookmark),
        Triple("downs", "Downloads", Icons.Rounded.Download)
    )
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 14.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            Modifier
                .clip(cut(14.dp, 0.12f))
                .background(p.dock.copy(alpha = 0.97f))
                .border(0.8.dp, p.line, cut(14.dp, 0.12f))
                .padding(horizontal = 5.dp, vertical = 4.dp)
                .animateContentSize(tween(230)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (key, label, icon) ->
                val on = at == key
                val wide = open == key
                Box(contentAlignment = Alignment.TopCenter) {
                    Row(
                        Modifier
                            .clip(cut(9.dp))
                            .background(if (on) p.acc.copy(alpha = 0.13f) else Color.Transparent)
                            .press(0.94f) {
                                if (open == key) {
                                    if (on) open = "" else onTab(key)
                                } else {
                                    open = key
                                    if (!on) onTab(key)
                                }
                            }
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            icon,
                            label,
                            tint = if (on) p.acc else p.mute,
                            modifier = Modifier.size(19.dp)
                        )
                        AnimatedVisibility(
                            visible = wide,
                            enter = fadeIn(tween(150)) + expandHorizontally(tween(200)),
                            exit = fadeOut(tween(90)) + shrinkHorizontally(tween(150))
                        ) {
                            Text(
                                label.uppercase(),
                                color = if (on) p.acc else p.mute,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.9.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(end = 1.dp)
                            )
                        }
                    }
                    if (on) Box(
                        Modifier
                            .width(20.dp)
                            .height(2.5.dp)
                            .clip(cut(2.dp))
                            .background(p.acc)
                    )
                }
            }
        }
    }
}
