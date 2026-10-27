package com.ominime.app.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.ceil

object waves : Prov {
    override val name = "aniwaves"
    private const val base = "https://aniwaves.ru"
    private val shows = HashMap<Int, Triple<Long, Pick?, Boolean>>()
    private val lists = HashMap<Int, Pair<Long, List<Raw>>>()

    data class Hit(val slug: String, val id: Int, val title: String, val jp: String)

    data class Pick(
        val slug: String,
        val id: Int,
        val title: String,
        val score: Double,
        val total: Int,
        val fmt: String,
        val year: Int
    )

    data class Raw(
        val num: Int,
        val src: String,
        val ids: String,
        val title: String,
        val sub: Boolean,
        val dub: Boolean,
        val filler: Boolean,
        val air: String,
        val shot: String
    )

    private fun ref() = "$base/"

    override suspend fun eps(m: Media): Src? {
        val show = pick(m) ?: return null
        val rows = list(m, show) ?: return null
        if (rows.isEmpty()) return null
        val az = runCatching { anizip.get(m.id) }.getOrNull()
        val subs = rows.filter { it.sub }.map { ep(it, az, true) }
        val dubs = rows.filter { it.dub }.map { ep(it, az, false) }
        if (subs.isEmpty() && dubs.isEmpty()) return null
        return Src(name, subs, dubs)
    }

    private fun ep(r: Raw, az: kotlinx.serialization.json.JsonObject?, sub: Boolean): Ep = Ep(
        num = r.num,
        title = anizip.title(az, r.num) ?: r.title.ifBlank { "Episode ${r.num}" },
        desc = anizip.note(az, r.num),
        img = anizip.shot(az, r.num) ?: r.shot,
        air = r.air.ifBlank { anizip.air(az, r.num) ?: "" },
        filler = r.filler || anizip.filler(az, r.num),
        sub = sub,
        dub = !sub
    )

    override suspend fun play(m: Media, audio: String, ep: Int): List<Stream> {
        val show = pick(m) ?: return emptyList()
        val rows = list(m, show) ?: return emptyList()
        val row = rows.firstOrNull { it.num == ep } ?: return emptyList()
        if (audio == "dub" && !row.dub) return emptyList()
        if (audio == "sub" && !row.sub) return emptyList()
        val referer = "$base/watch/${show.slug}/ep-${row.src}"
        val raw = runCatching { scrape.ajax("$base/ajax/server/list?servers=${show.id}&eps=${scrape.enc(row.src)}", referer) }.getOrNull()
            ?: return emptyList()
        val html = scrape.obj(raw)?.txt("result") ?: return emptyList()
        val links = servers(html, audio).take(4)
        if (links.isEmpty()) return emptyList()
        val got = coroutineScope {
            links.map { one ->
                async {
                    runCatching {
                        val src = scrape.ajax("$base/ajax/sources?id=${scrape.enc(one.second)}&asi=0&autoPlay=0", referer)
                        val url = scrape.obj(src)?.obj("result")?.txt("url") ?: return@runCatching emptyList()
                        val host = scrape.origin(url)
                        val found: List<Stream> = extract.grab(url, host)
                        if (found.isEmpty()) emptyList<Stream>() else found.map { s ->
                            Stream(
                                url = s.url,
                                kind = s.kind,
                                server = one.first,
                                subs = s.subs,
                                prio = if (s.kind == "hls") 7 else 5,
                                ref = s.ref.ifBlank { host }
                            )
                        }
                    }.getOrDefault(emptyList<Stream>())
                }
            }.awaitAll()
        }
        return got.flatten().distinctBy { it.url }
    }

    private fun servers(html: String, audio: String): List<Pair<String, String>> {
        val marks = Regex("<div\\b([^>]*)>", RegexOption.IGNORE_CASE).findAll(html).mapNotNull { m ->
            val type = scrape.attr(m.groupValues[1], "data-type")
            if (type == "sub" || type == "dub") m.range.first to type else null
        }.toList()
        val out = mutableListOf<Pair<String, String>>()
        for (i in marks.indices) {
            val at = marks[i].first
            val kind = marks[i].second
            if (kind != audio) continue
            val end = if (i + 1 < marks.size) marks[i + 1].first else html.length
            val part = html.substring(at, end)
            for (li in Regex("<li\\b([^>]*)>([\\s\\S]*?)</li>", RegexOption.IGNORE_CASE).findAll(part)) {
                val id = scrape.attr(li.groupValues[1], "data-link-id")
                if (id.isBlank()) continue
                out.add(scrape.strip(li.groupValues[2]).ifBlank { "AniWaves" } to id)
            }
        }
        return out
    }

    private suspend fun list(m: Media, show: Pick): List<Raw>? {
        val now = System.currentTimeMillis()
        lists[m.id]?.let { if (now - it.first < 3 * 3600_000L) return it.second }
        val raw = runCatching { scrape.ajax("$base/ajax/episode/list/${show.id}?vrf=", "$base/watch/${show.slug}") }.getOrNull() ?: return null
        val html = scrape.obj(raw)?.txt("result") ?: return null
        val out = LinkedHashMap<Int, Raw>()
        for (a in Regex("<a\\b([^>]*)>([\\s\\S]*?)</a>", RegexOption.IGNORE_CASE).findAll(html)) {
            val tag = a.groupValues[1]
            val num = scrape.attr(tag, "data-num").toIntOrNull() ?: continue
            if (num < 1 || out.containsKey(num)) continue
            val ids = scrape.attr(tag, "data-ids")
            if (ids.isBlank()) continue
            val shot = Regex("<img[^>]+src=[\"']([^\"']+)[\"']").find(a.groupValues[2])?.groupValues?.get(1) ?: ""
            out[num] = Raw(
                num = num,
                src = scrape.attr(tag, "data-slug").ifBlank { num.toString() },
                ids = ids,
                title = scrape.strip(a.groupValues[2]).replace(Regex("^\\d+\\s*"), ""),
                sub = scrape.attr(tag, "data-sub") == "1",
                dub = scrape.attr(tag, "data-dub") == "1",
                filler = scrape.attr(tag, "data-filler") == "1",
                air = scrape.attr(tag, "data-aired"),
                shot = shot
            )
        }
        val done = out.values.sortedBy { it.num }
        lists[m.id] = now to done
        return done
    }

    private suspend fun pick(m: Media): Pick? {
        val now = System.currentTimeMillis()
        shows[m.id]?.let { row -> if (now - row.first < 6 * 3600_000L) return row.second }
        val az = runCatching { anizip.get(m.id) }.getOrNull()
        val titles = buildList {
            if (m.title.isNotBlank()) add(m.title)
            if (m.romaji.isNotBlank()) add(m.romaji)
            m.native?.let { if (it.isNotBlank()) add(it) }
            addAll(m.alts)
            addAll(anizip.titles(az))
        }.distinct()
        val found = LinkedHashMap<String, Hit>()
        val qs = scrape.queries(titles, 6)
        val hits = coroutineScope {
            qs.map { q -> async { runCatching { search(q) }.getOrDefault(emptyList()) } }.awaitAll()
        }
        for (list in hits) for (one in list) found.putIfAbsent(one.slug, one)
        if (found.isEmpty()) {
            shows[m.id] = Triple(now, null, true)
            return null
        }
        val short = found.values
            .map { it to scrape.identity(titles, listOf(it.title, it.jp, it.slug.replace("-", " "))) }
            .filter { it.second >= 0.5 }
            .sortedByDescending { it.second }
            .take(8)
        val details = coroutineScope {
            short.map { one -> async { runCatching { detail(one.first) }.getOrNull() } }.awaitAll()
        }.filterNotNull()
        val expect = (m.total ?: 0).takeIf { it > 0 } ?: anizip.count(az) ?: 0
        val scored = details.mapNotNull { one -> valid(one, m, titles, expect) }.sortedByDescending { it.score }
        val first = scored.firstOrNull()
        if (first == null) {
            shows[m.id] = Triple(now, null, true)
            return null
        }
        val next = scored.getOrNull(1)
        if (first.score < 0.8 || (next != null && first.score - next.score < 0.08)) {
            shows[m.id] = Triple(now, null, true)
            return null
        }
        shows[m.id] = Triple(now, first, false)
        return first
    }

    private suspend fun search(q: String): List<Hit> {
        val html = scrape.html("$base/filter?keyword=${scrape.enc(q)}", ref())
        return cards(html)
    }

    private fun cards(html: String): List<Hit> {
        val out = LinkedHashMap<String, Hit>()
        for (a in Regex("<a\\b([^>]*)>([\\s\\S]*?)</a>", RegexOption.IGNORE_CASE).findAll(html)) {
            val tag = a.groupValues[1]
            if (!Regex("\\bclass=[\"'][^\"']*\\bname\\b[^\"']*\\bd-title\\b", RegexOption.IGNORE_CASE).containsMatchIn(tag)) continue
            val href = scrape.attr(tag, "href")
            val slug = Regex("^/watch/([a-z0-9-]+)$", RegexOption.IGNORE_CASE).find(href)?.groupValues?.get(1) ?: continue
            val id = Regex("-(\\d+)$").find(slug)?.groupValues?.get(1)?.toIntOrNull() ?: continue
            val title = scrape.strip(a.groupValues[2])
            if (title.isBlank()) continue
            out.putIfAbsent(slug, Hit(slug, id, title, scrape.attr(tag, "data-jp")))
        }
        return out.values.toList()
    }

    private suspend fun detail(hit: Hit): Pick {
        val html = scrape.html("$base/watch/${hit.slug}", ref())
        val type = fmt(field(html, "Type"))
        val aired = field(html, "Date aired")
        val prem = field(html, "Premiered")
        val year = Regex("\\d{4}").find(aired)?.value?.toIntOrNull()
            ?: Regex("\\d{4}").find(prem)?.value?.toIntOrNull() ?: 0
        val count = Regex("\\d+").find(field(html, "Episodes"))?.value?.toIntOrNull() ?: 0
        return Pick(hit.slug, hit.id, hit.title, 0.0, count, type, year)
    }

    private fun field(html: String, label: String): String {
        val m = Regex("<div>\\s*" + Regex.escape(label) + ":\\s*<span[^>]*>([\\s\\S]*?)</span>", RegexOption.IGNORE_CASE).find(html)
        return m?.let { scrape.strip(it.groupValues[1]) } ?: ""
    }

    private fun fmt(name: String): String {
        val t = name.uppercase()
        return when {
            t.contains("SPECIAL") -> "special"
            t == "TV" || t == "TV_SHORT" -> "tv"
            t == "MOVIE" -> "movie"
            t == "OVA" -> "ova"
            t == "ONA" -> "ona"
            else -> ""
        }
    }

    private fun valid(one: Pick, m: Media, titles: List<String>, expect: Int): Pick? {
        val id = scrape.identity(titles, listOf(one.title, one.slug.replace("-", " ")))
        if (id < 0.68) return null
        val want = fmt(m.fmt ?: "")
        if (want.isNotBlank() && one.fmt.isNotBlank() && want != one.fmt) return null
        val year = m.year ?: 0
        if (year > 0 && one.year > 0 && year != one.year) return null
        var cover = 1.0
        if (expect >= 1) cover = if (one.total < 1) 0.0 else 1.0
        if (m.state == "FINISHED" && expect >= 6) {
            val need = ceil(expect * 0.8).toInt()
            cover = minOf(1.0, one.total.toDouble() / need.toDouble())
            if (cover < 0.8) return null
        }
        val bonus = if (want.isNotBlank() && want == one.fmt) 0.14 else 0.07
        val ybonus = if (year > 0 && year == one.year) 0.1 else 0.04
        val score = id * 0.72 + bonus + ybonus + cover * 0.04
        return one.copy(score = score)
    }
}
