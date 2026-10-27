package com.ominime.app.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

object dunya : Prov {
    override val name = "dunya"
    private const val base = "https://anime-dunya.com"
    private val hold = HashMap<Int, Pair<Long, List<Ep>>>()

    private fun ref() = "$base/"

    override suspend fun eps(m: Media): Src? {
        val mal = m.mal ?: return null
        val list = guard(m, mal) ?: return null
        val subs = list.filter { it.sub }
        if (subs.isEmpty()) return null
        return Src(name, subs, emptyList())
    }

    private suspend fun guard(m: Media, mal: Int): List<Ep>? {
        val now = System.currentTimeMillis()
        hold[mal]?.let { if (now - it.first < 3 * 3600_000L) return it.second }
        val html = runCatching { scrape.html("$base/en/anime/$mal", ref()) }.getOrNull() ?: return null
        val raw = scrape.block(html, "episodes") ?: return null
        val arr = scrape.arr(scrape.loose(raw)) ?: return null
        val az = runCatching { anizip.get(m.id) }.getOrNull()
        val thumb = Regex("""(https?://[^\s"'`<>]+?/thumbnail/)([a-zA-Z0-9]+?)/((?:small|large)\.jpg)""").find(html)
        val cdn = thumb?.groupValues?.get(1) ?: "https://cdn.anime-dunya.com/thumbnail/"
        val ext = thumb?.groupValues?.get(3) ?: "small.jpg"
        val out = mutableListOf<Ep>()
        for (e in arr) {
            val o = e.node() ?: continue
            val num = o.num("episodeNumber") ?: continue
            if (o.st("streamId") == null) continue
            val name = o.arr("translations")?.firstOrNull()?.node()?.st("title")
            out.add(
                Ep(
                    num = num,
                    title = name ?: anizip.title(az, num) ?: "Episode $num",
                    desc = anizip.note(az, num),
                    img = o.st("streamId")?.let { cdn + it + "/" + ext } ?: anizip.shot(az, num),
                    air = anizip.air(az, num),
                    filler = o.boo("filler") == true || anizip.filler(az, num),
                    sub = true,
                    dub = false
                )
            )
        }
        val done = out.sortedBy { it.num }
        hold[mal] = now to done
        return done
    }

    override suspend fun play(m: Media, audio: String, ep: Int): List<Stream> {
        val mal = m.mal ?: return emptyList()
        val html = runCatching { scrape.html("$base/en/play/$mal/$ep", ref()) }.getOrNull() ?: return emptyList()
        val raw = scrape.block(html, "stream")
        val o = raw?.let { scrape.obj(scrape.loose(it)) }
        val link = o?.txt("source")
            ?: Regex("\"source\"\\s*:\\s*\"([^\"]+)\"").find(html)?.groupValues?.get(1)?.replace("\\", "")
            ?: return emptyList()
        val subs = o?.arr("subtitles")?.mapNotNull { one -> live(one.node()) } ?: emptyList()
        val kind = if (scrape.hls(link)) "hls" else "mp4"
        return listOf(Stream(url = link.secure(), kind = kind, server = "Dunya", subs = subs, prio = 9, ref = ref()))
    }

    private fun live(o: JsonObject?): Subs? {
        if (o == null) return null
        val url = o.txt("src") ?: o.txt("url") ?: return null
        return Subs(
            url = url.secure(),
            label = o.txt("label") ?: "English",
            lang = langs.norm(o.txt("srclang") ?: o.txt("label")),
            fmt = if (url.contains(".vtt", true)) "vtt" else "srt",
            sel = o.boo("default") == true,
            ref = ref()
        )
    }

    fun split(o: JsonArray): List<JsonObject> = o.mapNotNull { it.node() }
}
