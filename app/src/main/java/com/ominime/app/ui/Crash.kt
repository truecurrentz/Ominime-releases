package com.ominime.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Crash(report: String, onClose: () -> Unit) {
    val p = localpal.current
    val ctx = LocalContext.current
    var done by remember { mutableStateOf(false) }

    fun copy() {
        runCatching {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Ominime crash", report))
        }
        done = true
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .statusBarsPadding()
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(cut(16.dp, 0.2f))
                .background(p.card)
                .background(Brush.verticalGradient(listOf(p.lift, p.card, p.bg)))
                .border(0.9.dp, p.acc.copy(alpha = 0.55f), cut(16.dp, 0.2f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Text(
                "SOMETHING BROKE",
                color = p.acc,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                "The app closed on the last run. Send this report so it can be fixed.",
                color = p.text,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(cut(9.dp))
                    .background(p.bg)
                    .border(0.8.dp, p.line, cut(9.dp))
                    .padding(10.dp)
            ) {
                Text(
                    report,
                    color = p.text.copy(alpha = 0.85f),
                    fontSize = 9.5.sp,
                    lineHeight = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Btn(if (done) "Copied" else "Copy report", main = true) { copy() }
                Btn("Close") { onClose() }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
fun CrashBar(report: String, onOpen: () -> Unit, onDrop: () -> Unit) {
    val p = localpal.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(cut(10.dp))
            .background(p.lift)
            .border(0.9.dp, p.acc.copy(alpha = 0.55f), cut(10.dp))
            .press(0.97f, onOpen)
            .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "LAST RUN CLOSED UNEXPECTEDLY",
            color = p.text,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            "REPORT",
            color = p.acc,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Box(
            Modifier
                .size(24.dp)
                .clip(cut(6.dp))
                .background(p.glass)
                .press(0.97f, onDrop),
            contentAlignment = Alignment.Center
        ) {
            Text("✕", color = p.text.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}
