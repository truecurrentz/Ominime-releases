package com.ominime.app.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

object scrape {
    val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    private val stop = setOf(
        "a", "an", "and", "of", "the", "to", "in", "on", "for", "no", "wa",
        "season", "part", "stage", "cour", "chapter", "chapters", "episode",
        "episodes", "movie", "film", "tv", "ova", "ona", "special", "specials",
        "final", "finale", "arc"
    )

    private val parts = setOf("season", "part", "stage", "cour")

    fun ent(s: String?): String {
        if (s.isNullOrEmpty()) return ""
        var t = s
        t = Regex("&#(\\d+);").replace(t) { m -> runCatching { Character.toChars(m.groupValues[1].toInt()).concatToString() }.getOrDefault("") }
        t = Regex("&#x([0-9a-fA-F]+);").replace(t) { m -> runCatching { Character.toChars(m.groupValues[1].toInt(16)).concatToString() }.getOrDefault("") }
        return t
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }

    fun strip(h: String?): String {
        if (h.isNullOrEmpty()) return ""
        return ent(Regex("<[^>]*>").replace(h, " ").replace(Regex("\\s+"), " "))
    }

    fun attr(tag: String, name: String): String {
        val m = Regex(Regex.escape(name) + "=[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE).find(tag) ?: return ""
        return ent(m.groupValues[1])
    }

    fun norm(s: String?): String = (s ?: "").lowercase().replace(Regex("[^a-z0-9]"), "")

    fun text(s: String?): String {
        if (s.isNullOrEmpty()) return ""
        val flat = runCatching { Normalizer.normalize(s, Normalizer.Form.NFKD) }.getOrDefault(s)
        return flat
            .lowercase()
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
    }

    fun dice(a: String?, b: String?): Double {
        val x = norm(a)
        val y = norm(b)
        if (x == y) return if (x.isEmpty()) 0.0 else 1.0
        if (x.length < 2 || y.length < 2) return 0.0
        val bag = HashMap<String, Int>()
        for (i in 0 until x.length - 1) {
            val g = x.substring(i, i + 2)
            bag[g] = (bag[g] ?: 0) + 1
        }
        var hits = 0
        for (i in 0 until y.length - 1) {
            val g = y.substring(i, i + 2)
            val c = bag[g] ?: 0
            if (c > 0) {
                hits++
                bag[g] = c - 1
            }
        }
        return (2.0 * hits) / (x.length + y.length - 2).toDouble()
    }

    private fun uni(a: String, b: String): Double {
        val x = text(a).replace(" ", "")
        val y = text(b).replace(" ", "")
        if (x == y) return if (x.isEmpty()) 0.0 else 1.0
        if (x.length < 2 || y.length < 2) return 0.0
        val bag = HashMap<String, Int>()
        for (i in 0 until x.length - 1) {
            val g = x.substring(i, i + 2)
            bag[g] = (bag[g] ?: 0) + 1
        }
        var hits = 0
        for (i in 0 until y.length - 1) {
            val g = y.substring(i, i + 2)
            val c = bag[g] ?: 0
            if (c > 0) {
                hits++
                bag[g] = c - 1
            }
        }
        return (2.0 * hits) / (x.length + y.length - 2).toDouble()
    }

    fun words(s: String?): Set<String> {
        val out = LinkedHashSet<String>()
        for (w in text(s).split(" ")) {
            if (w.length <= 1) continue
            if (w in stop) continue
            if (Regex("^\\d+(st|nd|rd|th)?$").matches(w)) continue
            out.add(w)
        }
        return out
    }

    private val count = mapOf(
        "one" to "1", "two" to "2", "three" to "3", "four" to "4", "five" to "5",
        "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9", "ten" to "10",
        "first" to "1", "second" to "2", "third" to "3", "fourth" to "4", "fifth" to "5"
    )

    fun marks(raw: String?): Map<String, MutableSet<String>> {
        val t = text(raw)
        val c = t.replace(" ", "")
        val out = LinkedHashMap<String, MutableSet<String>>()
        for (k in parts) {
            val v = LinkedHashSet<String>()
            Regex(k + "(\\d+)").findAll(c).forEach { v.add(it.groupValues[1]) }
            Regex("(\\d+)(st|nd|rd|th)" + k).findAll(c).forEach { v.add(it.groupValues[1]) }
            for ((w, n) in count) if (c.contains(k + w) || c.contains(w + k)) v.add(n)
            if (v.isNotEmpty()) out[k] = v
        }
        Regex("final(season|chapters?|part|arc)").findAll(c).forEach { m ->
            val g = m.groupValues[1]
            val set = out.getOrPut("final") { LinkedHashSet() }
            set.add(if (g.startsWith("chapter")) "chapters" else g)
        }
        if (Regex("movie|film|gekijouban").containsMatchIn(c)) out["format"] = linkedSetOf("movie")
        Regex("\\b(ova|ona|special)\\b").find(t)?.let { out["format"] = linkedSetOf(it.groupValues[1]) }
        val end = t.split(" ").lastOrNull() ?: ""
        val num = Regex("^(\\d{1,2})(st|nd|rd|th)?$").find(end)?.groupValues?.get(1)
            ?: Regex("([a-z])(\\d{1,2})$").find(c)?.groupValues?.get(2)
        val roman = mapOf("ii" to "2", "iii" to "3", "iv" to "4", "v" to "5", "vi" to "6", "vii" to "7", "viii" to "8", "ix" to "9", "x" to "10")[end]
        val ev = num ?: roman
        if (ev != null) {
            val done = parts.any { out[it]?.contains(ev) == true }
            if (!done) out["numbered"] = linkedSetOf(ev)
        }
        return out
    }

    fun identity(titles: List<String>, cands: List<String>): Double {
        val ts = titles.filter { it.isNotBlank() }
        val cs = cands.filter { it.isNotBlank() }
        if (ts.isEmpty() || cs.isEmpty()) return 0.0
        val tm = LinkedHashMap<String, MutableSet<String>>()
        for (t in ts.take(3)) {
            for ((k, v) in marks(t)) tm.getOrPut(k) { LinkedHashSet() }.addAll(v)
        }
        var best = 0.0
        for (title in ts) {
            val qw = words(title)
            for (cand in cs) {
                val cw = words(cand)
                val coreT = if (qw.isEmpty()) title else qw.joinToString(" ")
                val coreC = if (cw.isEmpty()) cand else cw.joinToString(" ")
                val lex = uni(coreT, coreC)
                if (qw.isEmpty() || cw.isEmpty()) {
                    if (lex < 0.86) continue
                }
                val cm = marks(cand)
                var clash = false
                var pen = 0.0
                for ((k, vs) in tm) {
                    val cv = cm[k]
                    if (cv != null && vs.none { cv.contains(it) }) {
                        clash = true
                        break
                    }
                    if (cv == null && !(k == "season" && cm.containsKey("final")))
                        pen += if (k in parts) 0.22 else 0.16
                }
                if (clash) continue
                for (k in cm.keys) if (!tm.containsKey(k)) pen += if (k in parts) 0.18 else 0.12
                pen = min(0.48, pen)
                var shared = 0
                for (w in qw) if (cw.contains(w)) shared++
                val cov = if (qw.isNotEmpty() && cw.isNotEmpty()) min(shared.toDouble() / qw.size, shared.toDouble() / cw.size) else lex
                if (min(qw.size, cw.size) < 3) {
                    if (lex < 0.76) continue
                } else if (cov < 0.5) continue
                best = max(best, max(0.0, lex * 0.7 + cov * 0.3 - pen))
            }
        }
        return best
    }

    fun queries(titles: List<String>, limit: Int = 14): List<String> {
        val out = LinkedHashMap<String, String>()
        fun add(v: String?) {
            val q = v?.replace(Regex("\\s+"), " ")?.trim() ?: return
            if (q.length >= 3) out.putIfAbsent(q.lowercase(), q)
        }
        val u = titles.mapNotNull { it.trim().ifBlank { null } }.distinct().take(12)
        for (t in u.take(8)) add(t)
        for (t in u.take(4)) {
            add(Regex("[^\\p{L}\\p{N}]+").replace(t, " "))
            add(
                t.replace(Regex("(?i)\\bseason\\s*\\d+\\b"), " ")
                    .replace(Regex("(?i)\\bpart\\s*\\d+\\b"), " ")
                    .replace(Regex("(?i)\\b\\d+(rd|th|st|nd)\\b"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
            )
            for (seg in t.split(Regex("[:\\u2013\\u2014|]"))) if (seg.trim().length >= 4) add(seg)
            add(
                t.replace(Regex("(?i)\\b(season|part|cour|stage|chapter|episode)\\s*(\\d+|one|two|three|four|five|final)?\\b"), " ")
                    .replace(Regex("(?i)\\bthe\\s+final\\s+chapters?\\b"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
            )
        }
        for (t in u.drop(8)) add(t)
        return out.values.take(limit)
    }

    fun block(html: String, key: String): String? {
        val m = Regex("\\\\?\"" + Regex.escape(key) + "\\\\?\"\\s*:\\s*([\\[{])").find(html) ?: return null
        val start = m.range.first + m.value.length - 1
        val open = html[start]
        val close = if (open == '[') ']' else '}'
        var depth = 0
        var i = start
        while (i < html.length) {
            val c = html[i]
            if (c == '\\') {
                i += 2
                continue
            }
            if (c == open) depth++
            else if (c == close) {
                depth--
                if (depth == 0) return html.substring(start, i + 1)
            }
            i++
        }
        return null
    }

    fun loose(s: String): String = s
        .replace("\\u0026", "&")
        .replace("\\u003d", "=")
        .replace("\\/", "/")
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")

    fun arr(raw: String?): JsonArray? = runCatching { net.jso.parseToJsonElement(raw ?: "") as JsonArray }.getOrNull()

    fun obj(raw: String?): JsonObject? = runCatching { net.jso.parseToJsonElement(raw ?: "") as JsonObject }.getOrNull()

    fun hls(url: String): Boolean = url.contains(".m3u8", true)

    fun enc(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")

    fun origin(url: String): String = runCatching {
        val u = java.net.URI(url)
        (u.scheme ?: "https") + "://" + (u.host ?: "")
    }.getOrDefault("https://www.google.com")

    suspend fun html(url: String, ref: String? = null, lang: Boolean = true): String = net.text(
        url,
        buildMap {
            put("User-Agent", ua)
            put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            if (lang) put("Accept-Language", "en-US,en;q=0.9")
            if (ref != null) put("Referer", ref)
        }
    )

    suspend fun ajax(url: String, ref: String? = null): String = net.text(
        url,
        buildMap {
            put("User-Agent", ua)
            put("Accept", "application/json, text/javascript, */*; q=0.01")
            put("X-Requested-With", "XMLHttpRequest")
            if (ref != null) put("Referer", ref)
        }
    )

    suspend fun json(url: String, ref: String? = null, origin: String? = null): JsonObject = obj(
        net.text(
            url,
            buildMap {
                put("User-Agent", ua)
                put("Accept", "application/json,*/*")
                if (ref != null) put("Referer", ref)
                if (origin != null) put("Origin", origin)
            }
        )
    ) ?: JsonObject(emptyMap())

    fun str(o: JsonObject?, k: String): String? = o?.st(k)

    fun int(o: JsonObject?, k: String): Int? = o?.num(k)

    fun sub(o: JsonObject?, k: String): JsonObject? = o?.obj(k)
}

interface Prov {
    val name: String
    suspend fun eps(m: Media): Src?
    suspend fun play(m: Media, audio: String, ep: Int): List<Stream>
}

fun String.slug(): String = replace(Regex("[^a-zA-Z0-9]+"), "-").trim('-').lowercase()

fun String.secure(): String = if (startsWith("//")) "https:$this" else if (startsWith("http://")) "https://" + substring(7) else this

fun JsonObject.txt(k: String): String? = runCatching { this[k]?.jsonPrimitive?.content }.getOrNull()?.takeIf { it.isNotBlank() }
