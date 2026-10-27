package com.ominime.app.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@Serializable
data class Down(
    val id: Int = 0,
    val ep: Int = 0,
    val title: String = "",
    val cover: String? = null,
    val total: Int = 0,
    val file: String = "",
    val size: Long = 0,
    val part: Long = 0,
    val segs: Int = 0,
    val done: Int = 0,
    val subs: List<String> = emptyList(),
    val state: String = "wait",
    val at: Long = 0
) {
    val key: String get() = id.toString() + "_" + ep

    val frac: Float get() = when {
        state == "done" -> 1f
        segs > 0 -> (done.toFloat() / segs).coerceIn(0f, 1f)
        size > 0 && part > 0 -> (part.toFloat() / size).coerceIn(0f, 1f)
        else -> 0f
    }
}

data class Shelf(
    val id: Int = 0,
    val title: String = "",
    val cover: String? = null,
    val total: Int = 0,
    val rows: List<Down> = emptyList()
) {
    val done: Int get() = rows.count { it.state == "done" }
    val size: Long get() = rows.sumOf { it.size }
    val busy: Boolean get() = rows.any { it.state == "run" }
}

object offline {
    private val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val flow = MutableStateFlow<List<Down>>(emptyList())
    private val jobs = HashMap<String, Job>()
    private var box: File? = null

    val live = flow
    val all: List<Down> get() = flow.value

    fun init(c: Context) {
        val dir = File(c.filesDir, "down")
        dir.mkdirs()
        box = dir
        val idx = File(dir, "index.json")
        if (idx.exists()) {
            runCatching {
                val text = idx.readText()
                val rows = net.jso.decodeFromString(kotlinx.serialization.builtins.ListSerializer(Down.serializer()), text)
                flow.value = rows.mapNotNull { row ->
                    val f = File(row.file)
                    when {
                        row.state == "done" && !f.exists() -> null
                        else -> row.copy(size = if (row.state == "done" && f.exists()) f.length() else row.size)
                    }
                }
            }
        }
        flow.value.forEach { row -> if (row.state == "run" || row.state == "wait") queue(row) }
    }

    fun shelf(list: List<Down> = flow.value): List<Shelf> = list
        .groupBy { it.id }
        .map { (id, rows) ->
            val one = rows.first()
            Shelf(
                id = id,
                title = one.title,
                cover = one.cover,
                total = one.total,
                rows = rows.sortedBy { it.ep }
            )
        }
        .sortedByDescending { s -> s.rows.maxOf { it.at } }

    fun hit(id: Int, ep: Int): File? {
        val row = flow.value.firstOrNull { it.id == id && it.ep == ep && it.state == "done" } ?: return null
        val f = File(row.file)
        return if (f.exists() && f.length() > 0) f else null
    }

    fun find(id: Int, ep: Int): Down? = flow.value.firstOrNull { it.id == id && it.ep == ep }

    fun has(id: Int, ep: Int): Boolean = hit(id, ep) != null

    fun want(raw: String = store.conf.dsubs): List<String> =
        raw.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()

    fun grab(m: Media, ep: Int, langs: List<String> = want()) {
        val dir = box ?: return
        if (find(m.id, ep) != null) return
        val at = System.currentTimeMillis()
        val name = "ep" + ep.toString().padStart(2, '0') + ".mp4"
        val folder = File(dir, m.id.toString())
        folder.mkdirs()
        val row = Down(
            id = m.id,
            ep = ep,
            title = m.title,
            cover = m.cover,
            total = m.total ?: 0,
            file = File(folder, name).absolutePath,
            subs = langs,
            state = "wait",
            at = at
        )
        flow.value = flow.value + row
        save()
        queue(row)
    }

    fun grabMany(m: Media, eps: List<Int>, langs: List<String> = want()) {
        eps.forEach { grab(m, it, langs) }
    }

    fun pause(key: String) {
        jobs.remove(key)?.cancel()
        tweak(key) { it.copy(state = "hold") }
    }

    fun resume(key: String) {
        val row = flow.value.firstOrNull { it.key == key } ?: return
        if (row.state == "done") return
        val next = row.copy(state = "wait")
        flow.value = flow.value.map { if (it.key == key) next else it }
        save()
        queue(next)
    }

    fun drop(key: String) {
        jobs.remove(key)?.cancel()
        val row = flow.value.firstOrNull { it.key == key }
        flow.value = flow.value.filterNot { it.key == key }
        save()
        if (row != null) runCatching {
            val f = File(row.file)
            val stem = f.nameWithoutExtension
            f.parentFile?.listFiles()?.filter { it.name.startsWith(stem + ".") }?.forEach { it.delete() }
            f.delete()
            f.parentFile?.delete()
        }
    }

    fun dropShelf(id: Int) {
        flow.value.filter { it.id == id }.forEach { drop(it.key) }
    }

    fun holdAll() {
        flow.value.filter { it.state == "run" || it.state == "wait" }.forEach { pause(it.key) }
    }

    fun resumAll() {
        flow.value.filter { it.state == "hold" || it.state == "fail" }.forEach { resume(it.key) }
    }

    fun wipe() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        flow.value.forEach { runCatching { File(it.file).delete() } }
        flow.value = emptyList()
        save()
        box?.listFiles()?.forEach { runCatching { it.delete() } }
    }

    fun used(): Long = flow.value.filter { it.state == "done" }.sumOf { it.size }

    private fun tweak(key: String, fn: (Down) -> Down) {
        flow.value = flow.value.map { if (it.key == key) fn(it) else it }
    }

    private fun edit(row: Down): Down? = flow.value.firstOrNull { it.key == row.key }

    private fun save() {
        val dir = box ?: return
        runCatching {
            dir.mkdirs()
            File(dir, "index.json").writeText(
                net.jso.encodeToString(kotlinx.serialization.builtins.ListSerializer(Down.serializer()), flow.value)
            )
        }
    }

    private fun queue(row: Down) {
        if (jobs.containsKey(row.key)) return
        if (jobs.size >= 2) {
            tweak(row.key) { it.copy(state = "wait") }
            return
        }
        tweak(row.key) { it.copy(state = "run") }
        val job = sc.launch {
            val ok = runCatching { pull(row) }
            val boom = ok.exceptionOrNull()
            when {
                boom is CancellationException -> tweak(row.key) { it.copy(state = "hold") }
                boom != null -> tweak(row.key) { it.copy(state = "fail", part = 0, done = 0) }
                else -> tweak(row.key) {
                    it.copy(
                        state = "done",
                        size = File(it.file).length(),
                        part = File(it.file).length()
                    )
                }
            }
            jobs.remove(row.key)
            save()
            if (boom == null) {
                flow.value.firstOrNull { it.state == "wait" }?.let { next -> if (jobs.size < 2) queue(next) }
            }
        }
        jobs[row.key] = job
    }

    private suspend fun pull(row: Down) {
        val media = repo.quick(row.id) ?: throw IllegalStateException("Unknown title")
        val play = repo.play(media, store.conf.audio, row.ep)
        val pick = play.streams.maxByOrNull { it.prio } ?: play.streams.firstOrNull()
            ?: throw IllegalStateException(play.note ?: "No source")
        val url = pick.url
        if (url.isBlank()) throw IllegalStateException("Empty stream")
        val file = File(row.file)
        file.parentFile?.mkdirs()
        val saved = sidecar(row, play, pick)
        if (saved.isNotEmpty()) {
            tweak(row.key) { it.copy(subs = saved) }
            save()
        }
        if (url.contains(".m3u8", true) || pick.kind == "hls") {
            val parts = list(url, pick.ref)
            if (parts.isEmpty()) throw IllegalStateException("Playlist was empty")
            val at = edit(row)?.done ?: 0
            if (at == 0) file.delete()
            file.createNewFile()
            FileOutputStream(file, at > 0).use { out ->
                var i = at
                while (i < parts.size) {
                    val batch = parts.subList(i, minOf(i + 3, parts.size))
                    val got = coroutineScope {
                        batch.map { href -> async { fetch(href, pick.ref) } }.awaitAll()
                    }
                    got.forEach { out.write(it) }
                    i += batch.size
                    out.flush()
                    val cur = i
                    tweak(row.key) {
                        it.copy(
                            done = cur,
                            segs = parts.size,
                            part = file.length(),
                            state = "run"
                        )
                    }
                    save()
                }
            }
        } else {
            val at = if (File(row.file).exists()) File(row.file).length() else 0L
            val req = Request.Builder().url(url)
                .header("User-Agent", net.ua)
                .apply {
                    net.share(url).forEach { (n, v) -> header(n, v) }
                    if (at > 0) header("Range", "bytes=" + at + "-")
                }
                .build()
            net.cli.newCall(req).execute().use { res ->
                if (!res.isSuccessful) throw IllegalStateException("HTTP " + res.code)
                val body = res.body ?: throw IllegalStateException("No body")
                val total = body.contentLength().let { if (it > 0) at + it else 0L }
                FileOutputStream(file, at > 0).use { out ->
                    val buf = ByteArray(64 * 1024)
                    val src = body.byteStream()
                    var got = at
                    while (true) {
                        val n = src.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        got += n
                        if (total > 0) {
                            tweak(row.key) {
                                it.copy(size = total, part = got, state = "run")
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun list(url: String, ref: String): List<String> {
        var text = play(url, ref)
        if (!text.contains("#EXT-X-STREAM-INF")) return parts(url, text, ref)
        val rows = mutableListOf<Triple<Int, Int, String>>()
        var band = 0
        var wide = 0
        text.lines().forEach { raw ->
            val line = raw.trim()
            when {
                line.startsWith("#EXT-X-STREAM-INF") -> {
                    band = kotlin.math.max(band, pick(line, "BANDWIDTH") ?: 0)
                    wide = kotlin.math.max(wide, pick(line, "RESOLUTION") ?: 0)
                }
                line.isNotEmpty() && !line.startsWith("#") -> rows.add(Triple(band, wide, full(line, url)))
            }
        }
        val best = rows.maxByOrNull { it.first.toLong() * 1000L + it.second }?.third
            ?: throw IllegalStateException("No variant")
        text = play(best, ref)
        return parts(best, text, ref)
    }

    private fun pick(line: String, name: String): Int? = runCatching {
        val at = line.indexOf(name + "=")
        if (at < 0) return null
        val raw = line.substring(at + name.length + 1).substringBefore(",")
        raw.filter { it.isDigit() }.toIntOrNull()
    }.getOrNull()


    private suspend fun sidecar(row: Down, play: Play, pick: Stream): List<String> {
        val box = File(row.file).parentFile ?: return emptyList()
        val stem = File(row.file).nameWithoutExtension
        val ask = row.subs.ifEmpty { want() }
        val pool = (pick.subs + play.streams.flatMap { it.subs })
            .filter { it.url.isNotBlank() }
            .distinctBy { langs.norm(it.lang) + "|" + it.url }
        if (pool.isEmpty()) return emptyList()
        val out = mutableListOf<String>()
        for (one in pool) {
            val code = langs.norm(one.lang)
            if (ask.isNotEmpty() && code !in ask) continue
            val raw = runCatching { load(one, pick) }.getOrNull() ?: continue
            val text = toVtt(String(raw, Charsets.UTF_8))
            if (text.isBlank()) continue
            val name = stem + "." + code.ifBlank { "und" } + ".vtt"
            runCatching { File(box, name).writeText(text) }.onSuccess { out.add(code.ifBlank { "und" }) }
        }
        return out.distinct()
    }

    private suspend fun load(one: Subs, pick: Stream): ByteArray {
        val link = runCatching { caps.resolve(one.url, pick) }.getOrDefault(one.url)
        val head = HashMap<String, String>()
        net.share(link).forEach { (n, v) -> head[n] = v }
        if (pick.ref.isNotBlank()) head["Referer"] = pick.ref
        return net.bytes(link, head)
    }

    private fun toVtt(raw: String): String {
        if (raw.startsWith("\uFEFF")) return raw.drop(1)
        val text = raw.replace("\r\n", "\n").replace('\r', '\n')
        if (text.trimStart().startsWith("WEBVTT")) return text
        val body = text.lines().joinToString("\n") { line ->
            val at = line.indexOf("-->")
            if (at > 0) line.substring(0, at).replace(',', '.') + line.substring(at).replace(',', '.') else line
        }
        return "WEBVTT\n\n" + body.trim()
    }

    fun local(row: Down): List<Subs> {
        val f = File(row.file)
        val box = f.parentFile ?: return emptyList()
        val stem = f.nameWithoutExtension
        return runCatching {
            box.listFiles()
                ?.filter { it.name.startsWith(stem + ".") && it.extension.equals("vtt", true) }
                ?.map { one ->
                    val code = one.nameWithoutExtension.removePrefix(stem).trim('.')
                    Subs(
                        url = "file://" + one.absolutePath,
                        label = langs.name(code),
                        lang = code,
                        fmt = "vtt"
                    )
                }
                ?.sortedBy { it.lang }
                ?: emptyList()
        }.getOrDefault(emptyList())
    }

    private fun parts(base: String, text: String, ref: String): List<String> {
        val out = mutableListOf<String>()
        text.lines().forEach { raw ->
            val line = raw.trim()
            when {
                line.startsWith("#EXT-X-MAP:") -> {
                    val uri = line.substringAfter("URI=\"", "").substringBefore("\"")
                    if (uri.isNotBlank()) out.add(full(uri, base))
                }
                line.startsWith("#EXT-X-KEY:") -> {
                    if (line.contains("METHOD=AES-128")) throw IllegalStateException("Encrypted stream")
                }
                line.isNotEmpty() && !line.startsWith("#") -> out.add(full(line, base))
            }
        }
        return out
    }

    private fun full(link: String, base: String): String {
        if (link.startsWith("http")) return link
        return runCatching {
            val b = java.net.URI(base)
            val origin = b.scheme + "://" + b.authority
            if (link.startsWith("/")) origin + link else {
                val dir = b.path.substringBeforeLast('/', "")
                origin + dir + "/" + link
            }
        }.getOrDefault(link)
    }

    private suspend fun play(url: String, ref: String): String {
        val head = HashMap<String, String>()
        net.share(url).forEach { (n, v) -> head[n] = v }
        if (ref.isNotBlank()) head["Referer"] = ref
        return net.text(url, head)
    }

    private suspend fun fetch(url: String, ref: String): ByteArray {
        val head = HashMap<String, String>()
        net.share(url).forEach { (n, v) -> head[n] = v }
        if (ref.isNotBlank()) head["Referer"] = ref
        return net.bytes(url, head)
    }
}
