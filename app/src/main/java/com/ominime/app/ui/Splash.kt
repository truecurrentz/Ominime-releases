package com.ominime.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ominime.app.R
import kotlinx.coroutines.delay

@Composable
fun Splash() {
    val p = localpal.current
    var on by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(700)
        on = false
    }
    AnimatedVisibility(
        visible = on,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(420))
    ) {
        Box(Modifier.fillMaxSize().background(p.bg), contentAlignment = Alignment.Center) {
            AnimatedVisibility(visible = true, enter = scaleIn(tween(500), initialScale = 0.8f) + fadeIn(tween(400))) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        Modifier
                            .width(268.dp)
                            .aspectRatio(4.06f)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.logo),
                            contentDescription = "Ominime",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        "anime, quietly",
                        color = p.mute,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp
                    )
                }
            }
        }
    }
}
