package com.ominime.app.data

object kitsu {
    private val base = "https://kitsu.io/api/edge"
    private val hold = HashMap<Int, Pair<Long, String?>>()

    private suspend fun show(m: Media): String? {
        val now = System.currentTimeMillis()
        hold[m.id]?.let { if (now - it.first < 12 * 3600_000L) return it.second }
        val id = runCatching {
            val q = if ((m.mal ?: 0) > 0)
                "$base/anime?filter%5BmalId%5D=${m.mal}&page%5Blimit%5D=1"
            else
                "$base/anime?filter%5Btext%5D=" + scrape.enc(m.romaji.ifBlank { m.title }) + "&page%5Blimit%5D=1"
            val raw = net.text(q, mapOf("Accept" to "application/vnd.api+json"), ttl = 12 * 3600_000L, tag = "ks:$q")
            scrape.obj(raw)?.arr("data")?.firstOrNull()?.node()?.txt("id")
        }.getOrNull()
        hold[m.id] = now to id
        return id
    }

    suspend fun eps(m: Media, total: Int): Map<Int, Ep> {
        val id = show(m) ?: return emptyMap()
        val out = HashMap<Int, Ep>()
        var url: String? = "$base/anime/$id/episodes?page%5Blimit%5D=20&page%5Boffset%5D=0&sort=number"
        var guard = 0
        while (url != null && guard < 10) {
            guard++
            val raw = runCatching { net.text(url!!, mapOf("Accept" to "application/vnd.api+json"), ttl = 12 * 3600_000L) }.getOrNull() ?: break
            val o = scrape.obj(raw) ?: break
            val arr = o.arr("data") ?: break
            for (e in arr) {
                val one = e.node() ?: continue
                val a = one.obj("attributes") ?: continue
                val num = (a.txt("number")?.toIntOrNull()) ?: (a.txt("relativeNumber")?.toIntOrNull()) ?: continue
                if (num < 1) continue
                val img = a.obj("thumbnail")?.txt("original")
                out[num] = Ep(
                    num = num,
                    title = a.txt("canonicalTitle"),
                    desc = a.txt("synopsis")?.let { anilist.strip(it) },
                    img = img,
                    air = a.txt("airdate"),
                    sub = true
                )
            }
            url = o.obj("links")?.txt("next")
            if (total > 0 && out.size >= total) break
        }
        return out
    }
}
