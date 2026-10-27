package com.ominime.app.data

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

object senshi : Prov {
    override val name = "senshi"
    private const val base = "https://senshi.to"
    private const val cloud = "https://s.vidcloud.se"
    private val hold = HashMap<Int, Pair<Long, List<Ep>>>()
    private val dubs = HashMap<Int, Boolean>()

    private fun ref() = "$base/"

    override suspend fun eps(m: Media): Src? {
        val mal = m.mal ?: return null
        val list = guard(m, mal) ?: return null
        if (list.isEmpty()) return null
        val dub = dubs[mal] ?: probe(mal)
        val subs = list.map { it.copy(sub = true, dub = false) }
        val dubsList = if (dub) list.map { it.copy(sub = false, dub = true) } else emptyList()
        return Src(name, subs, dubsList)
    }

    private suspend fun guard(m: Media, mal: Int): List<Ep>? {
        val now = System.currentTimeMillis()
        hold[mal]?.let { if (now - it.first < 3 * 3600_000L) return it.second }
        val raw = runCatching { scrape.html("$base/episodes/$mal", ref()) }.getOrNull() ?: return null
        val arr = flex(raw) ?: return null
        val az = runCatching { anizip.get(m.id) }.getOrNull()
        val out = mutableListOf<Ep>()
        for (e in arr) {
            val o = e.node() ?: continue
            val num = o.num("ep_id") ?: continue
            out.add(
                Ep(
                    num = num,
                    title = o.st("ep_title") ?: anizip.title(az, num) ?: "Episode $num",
                    desc = anizip.note(az, num),
                    img = o.st("ep_thumbnail") ?: anizip.shot(az, num),
                    air = anizip.air(az, num),
                    filler = o.boo("ep_filler") == true || anizip.filler(az, num)
                )
            )
        }
        val done = out.sortedBy { it.num }
        hold[mal] = now to done
        return done
    }

    private suspend fun probe(mal: Int): Boolean {
        dubs[mal]?.let { return it }
        val ok = runCatching {
            val arr = flex(scrape.html("$base/episode-embeds/$mal/1", ref())) ?: return@runCatching false
            arr.any { it.node()?.st("status")?.lowercase() == "dub" }
        }.getOrDefault(false)
        dubs[mal] = ok
        return ok
    }

    private fun flex(raw: String): kotlinx.serialization.json.JsonArray? = scrape.arr(raw)
        ?: scrape.obj(raw)?.let { o -> o.arr("episodes") ?: o.arr("data") ?: o.arr("results") ?: o.arr("embeds") }

    override suspend fun play(m: Media, audio: String, ep: Int): List<Stream> {
        val mal = m.mal ?: return emptyList()
        val arr = runCatching { flex(scrape.html("$base/episode-embeds/$mal/$ep", ref())) }.getOrNull() ?: return emptyList()
        val want = audio == "dub"
        val pick = arr.mapNotNull { it.node() }.firstOrNull {
            val st = it.st("status")?.lowercase()
            if (want) st == "dub" else st != "dub"
        } ?: return emptyList()
        val out = mutableListOf<Stream>()
        val remote = pick.txt("remote_source_id")
        if (remote != null) {
            val raw = runCatching {
                scrape.json("$cloud/_v1/sources?id=${scrape.enc(remote)}", ref(), base)
            }.getOrNull()
            out.addAll(parse(raw, audio))
        }
        if (out.isEmpty()) {
            val link = pick.txt("url") ?: pick.txt("server2")
            if (link != null) out.addAll(runCatching { extract.grab(link.secure(), ref()) }.getOrDefault(emptyList()))
        }
        return out.distinctBy { it.url }
    }

    private fun parse(raw: JsonObject?, audio: String): List<Stream> {
        if (raw == null) return emptyList()
        val one = raw.txt("src")
        val items: List<JsonObject> = if (one != null) listOf(raw) else raw.arr("sources")?.mapNotNull { it.node() } ?: listOf(raw)
        val out = mutableListOf<Stream>()
        for (o in items) {
            val src = o.obj("source") ?: o
            val link = src.txt("src") ?: src.txt("file") ?: continue
            val aid = src.txt("audio")?.lowercase()
            if (aid != null && aid != "both" && aid != audio) continue
            val subs = (o.arr("tracks") ?: o.arr("subtitles"))?.mapNotNull { t -> track(t) } ?: emptyList()
            out.add(
                Stream(
                    url = link.secure(),
                    kind = if (scrape.hls(link)) "hls" else "mp4",
                    server = "Senshi",
                    subs = subs,
                    prio = if (out.isEmpty()) 8 else 6,
                    ref = ref()
                )
            )
        }
        return out
    }

    private fun track(e: JsonElement): Subs? {
        val o = e.node() ?: return null
        val link = o.txt("vtt_url") ?: o.txt("url") ?: o.txt("src") ?: return null
        val label = o.txt("label") ?: "English"
        if (label.equals("chapter", true)) return null
        val lang = langs.norm(o.txt("language") ?: o.txt("srclang") ?: label)
        return Subs(url = link.secure(), label = label, lang = lang, fmt = "vtt", sel = o.boo("default") == true, ref = ref())
    }
}
