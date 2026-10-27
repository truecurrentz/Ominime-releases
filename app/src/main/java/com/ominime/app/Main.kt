package com.ominime.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ominime.app.data.Note
import com.ominime.app.data.alerts
import com.ominime.app.data.repo
import com.ominime.app.data.store
import com.ominime.app.lastcrash
import com.ominime.app.wipecrash
import com.ominime.app.ui.Crash
import com.ominime.app.ui.CrashBar
import com.ominime.app.ui.Detail
import com.ominime.app.ui.Downs
import com.ominime.app.ui.Header
import com.ominime.app.ui.Home
import com.ominime.app.ui.Library
import com.ominime.app.ui.Notes
import com.ominime.app.ui.NoteToast
import com.ominime.app.ui.Pill
import com.ominime.app.ui.Rail
import com.ominime.app.ui.railpeek
import com.ominime.app.ui.railw
import com.ominime.app.ui.Schedule
import com.ominime.app.ui.Seek
import com.ominime.app.ui.Settings
import com.ominime.app.ui.Skin
import com.ominime.app.ui.Splash
import kotlinx.coroutines.delay
import com.ominime.app.ui.Watch
import com.ominime.app.ui.chrome
import com.ominime.app.ui.localpal

class Main : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val d by store.live.collectAsState()
            Skin(d.conf.dark, d.conf.acc) { Root() }
        }
    }
}

fun NavHostController.tab(key: String) {
    navigate(key) {
        popUpTo(graph.findStartDestination().id) { saveState = false; inclusive = false }
        launchSingleTop = true
        restoreState = false
    }
}

@Composable
fun Root() {
    val p = localpal.current
    val ctx = LocalContext.current
    fun link(url: String) {
        runCatching {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val tabs = setOf("home", "schedule", "list", "downs")
    val root = route in tabs
    var start by remember { mutableStateOf("") }
    var scan by remember { mutableIntStateOf(1) }
    var bad by remember { mutableStateOf(lastcrash(ctx)?.also { wipecrash(ctx) }) }
    var fresh by remember { mutableStateOf<Note?>(null) }
    val led by store.live.collectAsState()
    val ok by store.ready.collectAsState()
    val hot = led.notes.count { !it.read }
    val life = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val ask = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    var asked by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(led.conf.alerts, ok) {
        if (!ok) return@LaunchedEffect
        if (led.conf.alerts) {
            alerts.start(ctx)
            if (!asked && android.os.Build.VERSION.SDK_INT >= 33) {
                asked = true
                val fine = ctx.checkSelfPermission("android.permission.POST_NOTIFICATIONS") ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!fine) ask.launch("android.permission.POST_NOTIFICATIONS")
            }
        } else {
            alerts.stop(ctx)
        }
    }

    DisposableEffect(life) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_START) scan = scan + 1
        }
        life.lifecycle.addObserver(obs)
        onDispose { life.lifecycle.removeObserver(obs) }
    }

    LaunchedEffect(scan, ok, led.conf.alerts) {
        if (!led.conf.alerts || !ok) return@LaunchedEffect
        val got = runCatching { repo.scan() }.getOrNull() ?: return@LaunchedEffect
        if (got.notes.isNotEmpty()) {
            got.notes.take(4).forEach { alerts.push(ctx, it) }
            fresh = got.notes.first()
        }
    }

    LaunchedEffect(led.conf.alerts) {
        if (!led.conf.alerts) return@LaunchedEffect
        while (true) {
            delay(300_000L)
            scan += 1
        }
    }

    LaunchedEffect(fresh) {
        if (fresh != null) {
            delay(5200)
            fresh = null
        }
    }
    var show by remember { mutableStateOf(false) }

    LaunchedEffect(route) { chrome.hidden.value = false }

    fun seek(name: String) {
        start = name
        nav.navigate("seek?start=" + name.replace(" ", "%20")) {
            launchSingleTop = true
        }
    }

    val telly = BuildConfig.tv
    val rail = telly && route != "watch/{id}/{ep}"
    var railOpen by remember { mutableStateOf(true) }
    val inset by animateDpAsState(if (railOpen) railw else railpeek, tween(240), label = "inset")

    Box(Modifier.fillMaxSize().background(p.bg)) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = if (rail) inset else 0.dp)
        ) {
        NavHost(
            nav,
            startDestination = "home",
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                when {
                    targetState.destination.route == "watch/{id}/{ep}" -> fadeIn(tween(320))
                    targetState.destination.route?.startsWith("detail") == true ->
                        slideInVertically(tween(380)) { it / 12 } + fadeIn(tween(280)) + scaleIn(tween(420), initialScale = 0.97f)
                    initialState.destination.route in tabs && targetState.destination.route in tabs ->
                        fadeIn(tween(240)) + scaleIn(tween(320), initialScale = 0.985f)
                    else -> slideInHorizontally(animationSpec = tween(340)) { it / 5 } + fadeIn(tween(260))
                }
            },
            exitTransition = {
                when {
                    initialState.destination.route in tabs && targetState.destination.route in tabs ->
                        fadeOut(tween(150))
                    targetState.destination.route?.startsWith("detail") == true -> fadeOut(tween(220))
                    else -> fadeOut(tween(170)) + scaleOut(tween(240), targetScale = 0.985f)
                }
            },
            popEnterTransition = {
                fadeIn(tween(230)) + scaleIn(tween(300), initialScale = 0.985f)
            },
            popExitTransition = {
                when {
                    initialState.destination.route == "watch/{id}/{ep}" -> fadeOut(tween(280))
                    else -> slideOutHorizontally(animationSpec = tween(300)) { it / 7 } + fadeOut(tween(220))
                }
            }
        ) {
            composable("home") {
                Home(
                    onOpen = { id -> nav.navigate("detail/$id") },
                    onPlay = { id, ep -> nav.navigate("watch/$id/$ep") },
                    onGenre = { g -> seek(g) },
                    onList = { nav.tab("list") },
                    onDiscord = { link("https://discord.gg/ZmkNZUEFqV") }
                )
            }
            composable("schedule") {
                Schedule { id -> nav.navigate("detail/$id") }
            }
            composable("list") {
                Library(
                    onOpen = { id -> nav.navigate("detail/$id") },
                    onPlay = { id, ep -> nav.navigate("watch/$id/$ep") },
                    onExplore = { seek("All") }
                )
            }
            composable(
                "seek?start={start}",
                arguments = listOf(navArgument("start") { type = NavType.StringType; defaultValue = "" })
            ) { e ->
                val s = e.arguments?.getString("start")?.ifBlank { null } ?: start.ifBlank { null }
                Seek(s) { id -> nav.navigate("detail/$id") }
            }
            composable("downs") {
                Downs(
                    onPlay = { id, ep -> nav.navigate("watch/$id/$ep") },
                    onBrowse = { nav.tab("home") }
                )
            }
            composable("settings") { Settings() }
            composable("notes") {
                Notes(
                    onBack = { nav.popBackStack() },
                    onOpen = { id -> nav.navigate("detail/$id") }
                )
            }
            composable(
                "detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { e ->
                val id = e.arguments?.getInt("id") ?: 0
                Detail(
                    id = id,
                    onBack = { nav.popBackStack() },
                    onWatch = { mid, ep -> nav.navigate("watch/$mid/$ep") },
                    onOpen = { nid -> nav.navigate("detail/$nid") }
                )
            }
            composable(
                "watch/{id}/{ep}",
                arguments = listOf(
                    navArgument("id") { type = NavType.IntType },
                    navArgument("ep") { type = NavType.IntType }
                )
            ) { e ->
                Watch(
                    id = e.arguments?.getInt("id") ?: 0,
                    ep = e.arguments?.getInt("ep") ?: 1,
                    onBack = { nav.popBackStack() },
                    onOpen = { nid -> nav.navigate("detail/$nid") }
                )
            }
        }

        if (root) {
            Header(
                onSearch = { seek("All") },
                onSettings = { nav.navigate("settings") },
                onNotes = {
                    store.read()
                    nav.navigate("notes")
                },
                unread = hot
            )
        }
        if (!telly && (root || route.startsWith("seek"))) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.BottomCenter) {
                Pill(if (root) route else "") { key -> nav.tab(key) }
            }
        }
        }

        if (rail) {
            Box(Modifier.align(androidx.compose.ui.Alignment.CenterStart)) {
                Rail(
                    at = route,
                    hot = hot,
                    open = railOpen,
                    onOpen = { railOpen = it },
                    onTab = { key -> nav.tab(key) },
                    onNotes = {
                        store.read()
                        nav.navigate("notes")
                    },
                    onSettings = { nav.navigate("settings") }
                )
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
            NoteToast(
                note = fresh,
                onOpen = { id ->
                    fresh = null
                    nav.navigate("detail/$id")
                },
                onClose = { fresh = null }
            )
        }

        val report = bad
        if (report != null && !show) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 96.dp),
                contentAlignment = androidx.compose.ui.Alignment.BottomCenter
            ) {
                CrashBar(
                    report = report,
                    onOpen = { show = true },
                    onDrop = { bad = null }
                )
            }
        }
        if (report != null && show) {
            Crash(report) {
                show = false
                bad = null
            }
        }
        Splash()
    }
}
