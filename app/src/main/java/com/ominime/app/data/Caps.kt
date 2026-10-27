package com.ominime.app.data

import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.Locale
import kotlin.math.abs

data class Cue(val a: Long, val z: Long, val text: String)

object caps {
    private var dir: File? = null
    private val made = HashMap<String, Subs>()

    fun init(c: Context) {
        val d = File(c.cacheDir, "caps")
        if (!d.exists()) d.mkdirs()
        dir = d
        made.clear()
    }

    fun size(): Long = dir?.listFiles()?.sumOf { it.length() } ?: 0L

    fun wipe() {
        made.clear()
        dir?.listFiles()?.forEach { runCatching { it.delete() } }
    }

    suspend fun pack(s: Stream): List<Subs> = coroutineScope {
        s.subs.filter { it.url.isNotBlank() }
            .distinctBy { it.url }
            .map { t -> async { one(t.copy(url = full(t.url, s), label = t.label.ifBlank { t.lang }), s) } }
            .awaitAll()
            .filterNotNull()
            .distinctBy { it.lang + "|" + it.url }
    }

    private fun full(url: String, s: Stream): String {
        val u = url.trim()
        if (u.startsWith("http://") || u.startsWith("https://") || u.startsWith("file:")) return u
        val base = s.ref.ifBlank { s.url.ifBlank { host(u) } }
        return when {
            u.startsWith("//") -> "https:" + u
            u.startsWith("/") -> base.trimEnd('/') + u
            else -> base.trimEnd('/') + "/" + u
        }
    }

    private suspend fun one(t: Subs, s: Stream): Subs? {
        if (t.url.isBlank()) return null
        if (t.url.contains(".m3u8", true)) return null
        made[t.url]?.let { return it }
        if (t.url.startsWith("file:")) return t
        val local = File(dir ?: return null, "c" + abs(t.url.hashCode()).toString(16) + ".vtt")
        if (local.exists() && local.length() > 12L) {
            val done = t.copy(url = "file://" + local.absolutePath, fmt = "vtt")
            made[t.url] = done
            return done
        }
        val raw = grab(t.url, s.ref.ifBlank { s.url })
        if (raw != null && raw.size >= 4) {
            val vtt = sub(read(raw))
            if (vtt.length > 12 && runCatching { local.writeText(vtt) }.isSuccess) {
                val done = t.copy(url = "file://" + local.absolutePath, fmt = "vtt")
                made[t.url] = done
                return done
            }
        }
        made[t.url] = t
        return t
    }

    private suspend fun grab(url: String, ref: String): ByteArray? {
        val base = if (ref.isBlank()) host(url) else ref
        val head = mapOf(
            "Referer" to base,
            "Origin" to base.trimEnd('/'),
            "Accept" to "text/vtt,application/x-subrip,text/plain,*/*",
            "Accept-Language" to "en-US,en;q=0.9"
        )
        runCatching { net.bytes(url, head) }.getOrNull()?.takeIf { it.size > 4 }?.let { return it }
        runCatching { net.bytes(url, mapOf("Referer" to base, "User-Agent" to scrape.ua)) }
            .getOrNull()?.takeIf { it.size > 4 }?.let { return it }
        return runCatching { net.bytes(url) }.getOrNull()
    }

    private fun host(url: String): String = runCatching {
        val u = java.net.URI(url)
        (u.scheme ?: "https") + "://" + (u.host ?: "")
    }.getOrDefault("https://www.google.com")

    fun read(b: ByteArray): String {
        if (b.size >= 3 && b[0] == 0xEF.toByte() && b[1] == 0xBB.toByte() && b[2] == 0xBF.toByte()) {
            return String(b, 3, b.size - 3, Charsets.UTF_8)
        }
        if (b.size >= 2 && b[0] == 0xFF.toByte() && b[1] == 0xFE.toByte()) {
            return String(b, 2, b.size - 2, Charsets.UTF_16LE)
        }
        if (b.size >= 2 && b[0] == 0xFE.toByte() && b[1] == 0xFF.toByte()) {
            return String(b, 2, b.size - 2, Charsets.UTF_16BE)
        }
        val strict = runCatching {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(b))
                .toString()
        }
        if (strict.isSuccess) return strict.getOrThrow()
        val win = runCatching { String(b, Charset.forName("windows-1252")) }.getOrNull()
        if (win != null && win.isNotBlank()) return win
        return String(b, Charsets.ISO_8859_1)
    }

    fun sub(raw: String): String {
        val t = raw.replace("\r\n", "\n").replace('\r', '\n').removePrefix("\uFEFF").trim()
        if (t.isBlank()) return ""
        val head = t.take(6000).lowercase()
        return when {
            head.contains("webvtt") -> vtt(t)
            head.contains("[script info]") || head.contains("dialogue:") -> ass(t)
            tile(t) -> xml(t)
            head.contains("<tt") || head.contains("<?xml") || head.contains("<transcript") -> xml(t)
            frame(t) -> micro(t)
            t.contains("-->") -> srt(t)
            head.contains("[00:") || Regex("\\[\\d{1,2}:\\d{2}[.:]\\d{2}\\]").containsMatchIn(t) -> lrc(t)
            else -> loose(t)
        }
    }

    private fun loose(t: String): String {
        val low = t.take(400).lowercase()
        if (low.contains("<html") || low.contains("<body") || low.contains("404") || low.contains("not found")) return ""
        val lines = t.lines().map { body(it) }.filter { it.isNotBlank() }
        if (lines.size < 3) return ""
        if (lines.any { it.length > 300 }) return ""
        if (lines.sumOf { it.length } / lines.size > 90) return ""
        val junk = t.count { it == '{' || it == '}' || it == '=' || it == '"' }
        if (junk > t.length / 20) return ""
        val out = StringBuilder("WEBVTT\n\n")
        var at = 0L
        for (line in lines.take(600)) {
            val take = (600L + line.length * 34L).coerceAtMost(5200L)
            out.append(clock(at)).append(" --> ").append(clock(at + take)).append('\n').append(line).append("\n\n")
            at += take + 120L
        }
        return out.toString()
    }

    private fun tile(t: String): Boolean =
        Regex("<(p|text|span|div|s)\\b[^>]*(begin|start|t)=\"[^\"]+\"", RegexOption.IGNORE_CASE).containsMatchIn(t)

    private fun frame(t: String): Boolean = Regex("^\\s*\\{\\d+\\}\\{\\d+\\}", RegexOption.MULTILINE).containsMatchIn(t)

    private fun vtt(t: String): String {
        val body = if (t.startsWith("WEBVTT", true)) t else "WEBVTT\n\n" + t
        val fix = body.replace(Regex("(\\d{1,3}:\\d{2}:\\d{2}),(\\d{3})"), "$1.$2")
            .replace(Regex("(\\d{1,3}:\\d{2}),(\\d{3})"), "$1.$2")
        return if (fix.contains("-->")) fix.trimEnd() + "\n" else ""
    }

    private fun srt(t: String): String {
        val out = StringBuilder("WEBVTT\n\n")
        for (block in t.split(Regex("\n[ \t]*\n"))) {
            val lines = block.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
            if (lines.isEmpty()) continue
            val at = lines.indexOfFirst { it.contains("-->") }
            if (at < 0) continue
            val mark = lines[at]
            val side = mark.split("-->")
            if (side.size < 2) continue
            val a = ms(side[0]) ?: continue
            val z = ms(side[1].trim().split(Regex("\\s+")).firstOrNull() ?: "") ?: continue
            if (z <= a) continue
            val body = body(lines.drop(at + 1).joinToString("\n"))
            if (body.isBlank()) continue
            out.append(clock(a)).append(" --> ").append(clock(z)).append('\n').append(body).append("\n\n")
        }
        return if (out.length > 9) out.toString() else ""
    }

    private fun ass(t: String): String {
        val out = StringBuilder("WEBVTT\n\n")
        var live = false
        var keys = listOf("layer", "start", "end", "style", "name", "marginl", "marginr", "marginv", "effect", "text")
        for (line in t.lines()) {
            val s = line.trim()
            if (s.startsWith("[")) {
                live = s.equals("[Events]", true)
                continue
            }
            if (!live) continue
            if (s.startsWith("Format:", true)) {
                val next = s.substringAfter(':').split(',').map { it.trim().lowercase() }
                if (next.size >= 10) keys = next
                continue
            }
            if (!s.startsWith("Dialogue:", true)) continue
            val parts = s.substringAfter(':').split(",", limit = keys.size)
            if (parts.size < keys.size) continue
            val ai = keys.indexOf("start")
            val bi = keys.indexOf("end")
            val ti = keys.indexOf("text")
            if (ai < 0 || bi < 0 || ti < 0) continue
            val a = ms(parts[ai]) ?: continue
            val z = ms(parts[bi]) ?: continue
            if (z <= a) continue
            val body = body(parts[ti])
            if (body.isBlank()) continue
            out.append(clock(a)).append(" --> ").append(clock(z)).append('\n').append(body).append("\n\n")
        }
        return if (out.length > 9) out.toString() else ""
    }

    private fun xml(t: String): String {
        val out = StringBuilder("WEBVTT\n\n")
        val pat = Regex("<(p|text|s|span|div|cue)\\b([^>]*)>(.*?)</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        for (m in pat.findAll(t)) {
            val attr = m.groupValues[2]
            val body = body(m.groupValues[3])
            if (body.isBlank()) continue
            val a = pick(attr, "begin", "start", "t") ?: continue
            val end = pick(attr, "end")
            val dur = pick(attr, "dur", "d")
            val z = when {
                end != null -> end
                dur != null -> a + dur
                else -> a + 2500
            }
            if (z <= a) continue
            out.append(clock(a)).append(" --> ").append(clock(z)).append('\n').append(body).append("\n\n")
        }
        return if (out.length > 9) out.toString() else ""
    }

    private fun micro(t: String): String {
        val out = StringBuilder("WEBVTT\n\n")
        var fps = 23.976
        val lines = t.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val pat = Regex("^\\{(\\d+)\\}\\{(\\d+)\\}(.*)\$")
        for (line in lines) {
            val m = pat.find(line) ?: continue
            val a = m.groupValues[1].toLongOrNull() ?: continue
            val b = m.groupValues[2].toLongOrNull() ?: continue
            val text = m.groupValues[3]
            if (a <= 1L && b <= 1L) {
                text.trim().toDoubleOrNull()?.let { if (it in 10.0..60.0) fps = it }
                continue
            }
            val body = body(text.replace("|", "\n"))
            if (body.isBlank()) continue
            val x = (a / fps * 1000.0).toLong()
            val y = (b / fps * 1000.0).toLong()
            out.append(clock(x)).append(" --> ").append(clock(y)).append('\n').append(body).append("\n\n")
        }
        return if (out.length > 9) out.toString() else ""
    }

    private fun lrc(t: String): String {
        val out = StringBuilder("WEBVTT\n\n")
        val pat = Regex("^\\[(\\d{1,2}):(\\d{2})[.:](\\d{2,3})\\]\\s*(.+)\$")
        val rows = t.lines().mapNotNull { line ->
            val m = pat.find(line.trim()) ?: return@mapNotNull null
            val mnt = m.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val sec = m.groupValues[2].toLongOrNull() ?: return@mapNotNull null
            val frac = m.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
            ((mnt * 60L + sec) * 1000L + frac) to m.groupValues[4]
        }.sortedBy { it.first }
        for (i in rows.indices) {
            val a = rows[i].first
            val z = rows.getOrNull(i + 1)?.first ?: (a + 3000)
            val body = body(rows[i].second)
            if (body.isBlank()) continue
            out.append(clock(a)).append(" --> ").append(clock((z - 60).coerceAtLeast(a + 400))).append('\n').append(body).append("\n\n")
        }
        return if (out.length > 9) out.toString() else ""
    }

    private fun pick(attr: String, vararg names: String): Long? {
        for (n in names) {
            val m = Regex("\\b" + n + "\\s*=\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(attr) ?: continue
            ms(m.groupValues[1])?.let { return it }
        }
        return null
    }

    fun ms(v: String): Long? {
        var s = v.trim().lowercase()
        if (s.isBlank()) return null
        var scale = 1.0
        if (s.endsWith("ms")) {
            s = s.dropLast(2)
            scale = 1.0
            s.toDoubleOrNull()?.let { return it.toLong() }
            return null
        }
        if (s.endsWith("s")) {
            s = s.dropLast(1)
            s.toDoubleOrNull()?.let { return (it * 1000).toLong() }
            return null
        }
        s = s.substringBefore(' ')
        s = s.replace(',', '.')
        val bits = s.split(':').map { it.trim() }
        val nums = bits.map { it.toDoubleOrNull() ?: return null }
        val total = when (nums.size) {
            1 -> nums[0] * 1000.0
            2 -> (nums[0] * 60.0 + nums[1]) * 1000.0
            3 -> (nums[0] * 3600.0 + nums[1] * 60.0 + nums[2]) * 1000.0
            4 -> (nums[0] * 3600.0 + nums[1] * 60.0 + nums[2] + nums[3] / 25.0) * 1000.0
            else -> return null
        }
        return (total * scale).toLong().coerceAtLeast(0L)
    }

    fun clock(v: Long): String {
        val h = v / 3600000L
        val m = (v % 3600000L) / 60000L
        val s = (v % 60000L) / 1000L
        val f = v % 1000L
        return String.format(Locale.US, "%02d:%02d:%02d.%03d", h, m, s, f)
    }

    private val tags by lazy { Regex("\\{[^}]*\\}") }
    private val marks by lazy { Regex("<[^>]+>") }

    private fun body(raw: String): String {
        var t = raw
        t = t.replace(Regex("\\\\[Nn]"), "\n").replace("\\h", " ")
        t = tags.replace(t, "")
        t = marks.replace(t, "")
        t = t.replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
        t = t.replace(Regex("\\{\\\\[^}]*\\}"), "")
        t = t.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
        return t.trim()
    }


    private val seen = HashMap<String, List<Cue>>()

    fun clear() = seen.clear()

    private fun origin(url: String): String = runCatching {
        val u = java.net.URI(url)
        (u.scheme ?: "https") + "://" + (u.host ?: "")
    }.getOrDefault("")

    private fun attr(line: String, key: String): String? =
        Regex(key + "=\"([^\"]*)\"").find(line)?.groupValues?.get(1)

    suspend fun manifest(url: String, ref: String): List<Subs> {
        if (url.isBlank() || !url.contains(".m3u8", true)) return emptyList()
        val raw = runCatching { scrape.html(url, ref.ifBlank { origin(url) }) }.getOrNull() ?: return emptyList()
        if (!raw.contains("#EXT-X-MEDIA", true)) return emptyList()
        val out = mutableListOf<Subs>()
        for (m in Regex("#EXT-X-MEDIA:([^\\n\\r]*)").findAll(raw)) {
            val line = m.groupValues[1]
            if (!line.contains("TYPE=SUBTITLES", true)) continue
            val uri = attr(line, "URI") ?: continue
            val name = attr(line, "NAME") ?: attr(line, "LANGUAGE") ?: "Subtitles"
            val lang = attr(line, "LANGUAGE") ?: name
            val link = when {
                uri.startsWith("http", true) -> uri
                uri.startsWith("//") -> "https:" + uri
                else -> url.substringBeforeLast('/') + "/" + uri.trimStart('/')
            }
            out.add(Subs(url = link, label = name, lang = langs.norm(lang), fmt = "vtt", sel = line.contains("DEFAULT=YES", true)))
        }
        return out.distinctBy { it.url }
    }

    private fun looks(t: String): Boolean = t.contains("-->") ||
        t.contains("WEBVTT", true) ||
        t.contains("[Script Info]", true) ||
        Regex("\\{\\d+\\}\\{\\d+\\}").containsMatchIn(t) ||
        Regex("<[^>]*(begin|start)=", RegexOption.IGNORE_CASE).containsMatchIn(t)

    private suspend fun fetch(url: String, refs: List<String>): String? {
        val tries = (refs + origin(url) + url + "").map { it.trim() }.distinct()
        for (r in tries) {
            val head = buildMap {
                put("User-Agent", scrape.ua)
                put("Accept", "text/vtt,application/x-subrip,text/plain,*/*")
                put("Accept-Language", "en-US,en;q=0.9")
                if (r.isNotEmpty()) {
                    put("Referer", r)
                    origin(r).takeIf { it.isNotEmpty() }?.let { put("Origin", it) }
                }
                putAll(net.share(url))
            }
            val b = runCatching { net.bytes(url, head) }.getOrNull()
            if (b != null && b.size > 8) {
                val text = read(b)
                if (looks(text)) return text
            }
        }
        return null
    }

    suspend fun cuesOf(t: Subs, refs: List<String>): List<Cue> {
        if (t.url.isBlank()) return emptyList()
        seen[t.url]?.let { return it }
        val raw = when {
            t.url.startsWith("file:") -> runCatching { File(java.net.URI(t.url)).readText() }.getOrNull()
            else -> fetch(t.url, refs)
        } ?: return emptyList()
        val list = cues(raw)
        if (list.isNotEmpty()) seen[t.url] = list
        return list
    }

    fun cues(raw: String): List<Cue> {
        val norm = runCatching { sub(raw) }.getOrDefault("")
        val body = if (norm.contains("-->")) norm else raw
        val out = ArrayList<Cue>(512)
        for (block in body.replace("\r\n", "\n").split(Regex("\n[ \t]*\n"))) {
            val lines = block.lines().filter { it.isNotBlank() }
            val at = lines.indexOfFirst { it.contains("-->") }
            if (at < 0) continue
            val bits = lines[at].split("-->")
            if (bits.size < 2) continue
            val a = ms(bits[0]) ?: continue
            val z = ms(bits[1].trim().split(Regex("\\s+")).firstOrNull() ?: "") ?: continue
            if (z <= a) continue
            val text = plain(lines.drop(at + 1).joinToString("\n"))
            if (text.isBlank()) continue
            out.add(Cue(a, z, text))
        }
        out.sortBy { it.a }
        return out
    }

    private val wrap by lazy { Regex("<[^>]*>") }
    private val brace by lazy { Regex("\\{[^}]*\\}") }

    private fun plain(raw: String): String {
        var t = wrap.replace(raw, "")
        t = brace.replace(t, "")
        t = t.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'").replace("&amp;", "&")
        return t.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n").trim()
    }

    fun resolve(u: String, s: Stream): String = runCatching { full(u, s) }.getOrDefault(u)


}
