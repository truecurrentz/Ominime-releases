package com.ominime.app.data

import kotlinx.serialization.json.JsonObject

object koto : Prov {
    override val name = "koto"
    private const val base = "https://anikototv.to"
    private val shows = HashMap<Int, Pair<Long, Pick?>>()
    private val lists = HashMap<String, Pair<Long, List<Raw>>>()

    data class Pick(val slug: String, val id: String, val title: String, val score: Double)

    data class Raw(
        val num: Int,
        val ids: String,
        val title: String,
        val sub: Boolean,
        val dub: Boolean,
        val filler: Boolean
    )

    private fun ref() = "$base/"

    private fun head() = mapOf(
        "Referer" to ref(),
        "X-Requested-With" to "XMLHttpRequest",
        "Accept" to "application/json, text/javascript, */*; q=0.01"
    )

    override suspend fun eps(m: Media): Src? {
        val show = pick(m) ?: return null
        val rows = rows(m, show) ?: return null
        if (rows.isEmpty()) return null
        val az = runCatching { anizip.get(m.id) }.getOrNull()
        val subs = rows.filter { it.sub }.map { one -> ep(one, az, true) }
        val dubs = rows.filter { it.dub }.map { one -> ep(one, az, false) }
        if (subs.isEmpty() && dubs.isEmpty()) return null
        return Src(name, subs, dubs)
    }

    private fun ep(r: Raw, az: JsonObject?, sub: Boolean) = Ep(
        num = r.num,
        title = r.title.ifBlank { anizip.title(az, r.num) ?: "Episode ${r.num}" },
        desc = anizip.note(az, r.num),
        img = anizip.shot(az, r.num),
        air = anizip.air(az, r.num),
        filler = r.filler || anizip.filler(az, r.num),
        sub = sub,
        dub = !sub
    )

    override suspend fun play(m: Media, audio: String, ep: Int): List<Stream> {
        val show = pick(m) ?: return emptyList()
        val rows = rows(m, show) ?: return emptyList()
        val row = rows.firstOrNull { it.num == ep } ?: return emptyList()
        if (audio == "dub" && !row.dub) return emptyList()
        if (audio == "sub" && !row.sub) return emptyList()
        val raw = runCatching {
            net.text(
                "$base/ajax/server/list?servers=${scrape.enc(row.ids)}",
                head()
            )
        }.getOrNull() ?: return emptyList()
        val html = scrape.obj(raw)?.txt("result") ?: return emptyList()
        val found = links(html, audio).take(6)
        if (found.isEmpty()) return emptyList()
        val out = mutableListOf<Stream>()
        for (one in found) {
            val url = runCatching {
                val body = net.text("$base/ajax/server?get=${scrape.enc(one)}", head())
                scrape.obj(body)?.obj("result")?.txt("url")
            }.getOrNull() ?: continue
            val host = scrape.origin(url)
            val got = runCatching { extract.grab(url, host) }.getOrDefault(emptyList())
            got.forEach { s ->
                out.add(
                    Stream(
                        url = s.url,
                        kind = s.kind,
                        server = "",
                        subs = s.subs,
                        prio = if (s.kind == "hls") 8 else 6,
                        ref = s.ref.ifBlank { host }
                    )
                )
            }
        }
        return out.distinctBy { it.url }
    }

    private fun links(html: String, audio: String): List<String> {
        val marks = Regex("<div\\b([^>]*)>", RegexOption.IGNORE_CASE).findAll(html).mapNotNull { m ->
            val kind = scrape.attr(m.groupValues[1], "data-type").lowercase()
            if (kind == "sub" || kind == "dub" || kind == "hsub") m.range.first to kind else null
        }.toList()
        val out = mutableListOf<String>()
        for (i in marks.indices) {
            val kind = marks[i].second
            if (kind != audio && !(audio == "sub" && kind == "hsub")) continue
            val from = marks[i].first
            val to = if (i + 1 < marks.size) marks[i + 1].first else html.length
            for (li in Regex("<li\\b([^>]*)>", RegexOption.IGNORE_CASE).findAll(html.substring(from, to))) {
                val id = scrape.attr(li.groupValues[1], "data-link-id")
                if (id.isNotBlank()) out.add(id)
            }
        }
        if (out.isEmpty()) {
            for (li in Regex("<li\\b([^>]*)>", RegexOption.IGNORE_CASE).findAll(html)) {
                val id = scrape.attr(li.groupValues[1], "data-link-id")
                if (id.isNotBlank()) out.add(id)
            }
        }
        return out
    }

    private suspend fun rows(m: Media, show: Pick): List<Raw>? {
        val now = System.currentTimeMillis()
        lists[show.id]?.let { if (now - it.first < 3 * 3600_000L) return it.second }
        val raw = runCatching {
            net.text("$base/ajax/episode/list/${scrape.enc(show.id)}?vrf=", head())
        }.getOrNull() ?: return null
        val html = scrape.obj(raw)?.txt("result") ?: return null
        val out = LinkedHashMap<Int, Raw>()
        for (a in Regex("<a\\b([^>]*)>([\\s\\S]*?)</a>", RegexOption.IGNORE_CASE).findAll(html)) {
            val tag = a.groupValues[1]
            val num = scrape.attr(tag, "data-num").toIntOrNull() ?: continue
            if (num < 1 || out.containsKey(num)) continue
            val ids = scrape.attr(tag, "data-ids")
            if (ids.isBlank()) continue
            out[num] = Raw(
                num = num,
                ids = ids,
                title = scrape.strip(a.groupValues[2]).replace(Regex("^\\d+\\s*"), ""),
                sub = scrape.attr(tag, "data-sub") != "0",
                dub = scrape.attr(tag, "data-dub") == "1",
                filler = Regex("filler", RegexOption.IGNORE_CASE).containsMatchIn(scrape.attr(tag, "class"))
            )
        }
        val done = out.values.sortedBy { it.num }
        lists[show.id] = now to done
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
        val qs = scrape.queries(titles, 5)
        val all = LinkedHashMap<String, Pair<String, String>>()
        for (q in qs) {
            val raw = runCatching { net.text("$base/ajax/anime/search?keyword=${scrape.enc(q)}", head()) }.getOrNull() ?: continue
            val html = scrape.obj(raw)?.obj("result")?.txt("html") ?: continue
            for (a in Regex("<a\\b([^>]*)>([\\s\\S]*?)</a>", RegexOption.IGNORE_CASE).findAll(html)) {
                val tag = a.groupValues[1]
                val link = scrape.attr(tag, "href")
                if (!link.contains("/watch/")) continue
                val name = scrape.strip(a.groupValues[2])
                if (link.isBlank() || name.isBlank()) continue
                all.putIfAbsent(link, link to name)
            }
        }
        val short = all.values
            .map { one -> Triple(one.first, one.second, scrape.identity(titles, listOf(one.second, one.first.substringAfterLast('/').replace("-", " ")))) }
            .filter { it.third >= 0.55 }
            .sortedByDescending { it.third }
            .take(3)
        for (one in short) {
            val html = runCatching { scrape.html(one.first, ref()) }.getOrNull() ?: continue
            val id = Regex("id=[\"']watch-main[\"'][^>]*data-id=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)
                ?: Regex("data-id=[\"'](\\d+)[\"']").find(html)?.groupValues?.get(1)
                ?: continue
            val pick = Pick(one.first, id, one.second, one.third)
            val probe = runCatching { rows(m, pick) }.getOrNull()
            val ok = probe != null && probe.isNotEmpty()
            if (ok && one.third >= 0.6) {
                shows[m.id] = now to pick
                return pick
            }
        }
        shows[m.id] = now to null
        return null
    }
}
