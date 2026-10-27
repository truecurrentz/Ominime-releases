package com.ominime.app.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object anilist {
    private val url = "https://graphql.anilist.co"
    private val card =
        "id idMal title { romaji english native } synonyms coverImage { extraLarge large color } bannerImage description genres averageScore episodes duration status season seasonYear format popularity studios(isMain: true) { nodes { name } } nextAiringEpisode { episode airingAt }"
    private val hour = 3600_000L

    fun strip(s: String): String = s
        .replace("<br>", "\n")
        .replace(Regex("<[^>]*>"), "")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&amp;", "&")
        .replace("&mdash;", "-")
        .replace("&ndash;", "-")
        .trim()

    private fun body(q: String) = JsonObject(mapOf("query" to JsonPrimitive(q))).toString()

    private fun clean(s: String) = s.replace("\\", " ").replace("\"", " ").replace("\n", " ")

    fun read(o: JsonObject): Media {
        val t = o.obj("title")
        val c = o.obj("coverImage")
        val s = o.obj("studios")?.arr("nodes")?.firstOrNull().node()
        val n = o.obj("nextAiringEpisode")
        val gs = o.arr("genres")?.mapNotNull { e -> runCatching { e.jsonPrimitive.contentOrNull }.getOrNull() } ?: emptyList()
        val tag = o.arr("tags")?.mapNotNull { e ->
            val n2 = e.node() ?: return@mapNotNull null
            if (n2.boo("isMediaSpoiler") == true) null else n2.st("name")
        } ?: emptyList()
        return Media(
            id = o.num("id") ?: 0,
            mal = o.num("idMal"),
            romaji = t?.st("romaji") ?: "",
            title = t?.st("english") ?: t?.st("romaji") ?: "",
            native = t?.st("native"),
            cover = c?.st("extraLarge") ?: c?.st("large"),
            banner = o.st("bannerImage"),
            desc = o.st("description")?.let { strip(it) },
            genres = gs,
            tags = tag,
            score = o.num("averageScore"),
            total = o.num("episodes"),
            alts = o.arr("synonyms")?.mapNotNull { e -> runCatching { e.jsonPrimitive.contentOrNull }.getOrNull() } ?: emptyList(),
            dur = o.num("duration"),
            state = o.st("status"),
            year = o.num("seasonYear"),
            season = o.st("season"),
            fmt = o.st("format"),
            studio = s?.st("name"),
            color = c?.st("color"),
            pop = o.num("popularity"),
            next = n?.let { Next(it.num("episode") ?: 0, (it.lng("airingAt") ?: 0L) * 1000L) }
        )
    }

    suspend fun page(
        page: Int = 1,
        sort: String? = "TRENDING_DESC",
        genre: String? = null,
        search: String? = null,
        season: String? = null,
        year: Int? = null,
        state: String? = null,
        gens: Set<String> = emptySet(),
        fmts: Set<String> = emptySet(),
        stats: Set<String> = emptySet(),
        score: Int = 0
    ): List<Media> {
        val a = StringBuilder("type: ANIME, isAdult: false")
        if (sort != null) a.append(", sort: ").append(sort)
        if (genre != null) a.append(", genre: \"").append(clean(genre)).append("\"")
        if (gens.isNotEmpty()) a.append(", genre_in: [").append(gens.joinToString(", ") { "\"" + clean(it) + "\"" }).append("]")
        if (fmts.isNotEmpty()) a.append(", format_in: [").append(fmts.joinToString(", ")).append("]")
        if (stats.isNotEmpty()) a.append(", status_in: [").append(stats.joinToString(", ")).append("]")
        if (search != null) a.append(", search: \"").append(clean(search)).append("\"")
        if (season != null) a.append(", season: ").append(season)
        if (year != null) a.append(", seasonYear: ").append(year)
        if (state != null) a.append(", status: ").append(state)
        if (score > 0) a.append(", averageScore_greater: ").append(score)
        val q = "query { Page(page: $page, perPage: 30) { pageInfo { hasNextPage } media($a) { $card } } }"
        val raw = net.post(url, body(q), ttl = 2 * hour)
        val arr = net.obj(raw).obj("data")?.obj("Page")?.arr("media") ?: return emptyList()
        return arr.mapNotNull { it.node()?.let { n -> read(n) } }
    }

    suspend fun airing(from: Long, to: Long): List<Slot> {
        val q = "query { Page(page: 1, perPage: 90) { airingSchedules(airingAt_greater: $from, airingAt_lesser: $to, sort: TIME) { airingAt episode media { $card } } } }"
        val raw = runCatching { net.post(url, body(q), ttl = 20 * 60_000L) }.getOrNull() ?: return emptyList()
        val arr = net.obj(raw).obj("data")?.obj("Page")?.arr("airingSchedules") ?: return emptyList()
        return arr.mapNotNull { e ->
            val n = e.node() ?: return@mapNotNull null
            val m = n.obj("media")?.let { read(it) } ?: return@mapNotNull null
            Slot(
                at = (n.lng("airingAt") ?: 0L) * 1000L,
                ep = n.num("episode") ?: 0,
                m = m
            )
        }
    }

    suspend fun full(id: Int): Media {
        val q = "query { Media(id: $id, type: ANIME) { $card trailer { id site } externalLinks { site url } relations { edges { relationType(version: 2) node { id type format title { romaji english } coverImage { large } averageScore } } } recommendations(perPage: 12, sort: RATING_DESC) { nodes { mediaRecommendation { id format title { romaji english } coverImage { large } averageScore } } } characters(perPage: 10, sort: [ROLE, RELEVANCE]) { edges { role node { name { full } image { large } } voiceActors(language: JAPANESE) { name { full } } } } streamingEpisodes { title thumbnail } tags { name rank isMediaSpoiler } } }"
        val raw = net.post(url, body(q), ttl = 12 * hour)
        val o = net.obj(raw).obj("data")?.obj("Media") ?: throw Fail(0, "Not found")
        val m = read(o)
        val tr = o.obj("trailer")
        val links = o.arr("externalLinks")?.mapNotNull { e ->
            e.node()?.let { l -> Link(l.st("site") ?: "", l.st("url") ?: "") }
        } ?: emptyList()
        val rel = o.obj("relations")?.arr("edges")?.mapNotNull { e ->
            val n = e.node()?.obj("node") ?: return@mapNotNull null
            val t = n.obj("title")
            if (n.st("type") != "ANIME") return@mapNotNull null
            Rel(
                id = n.num("id") ?: 0,
                title = t?.st("english") ?: t?.st("romaji") ?: "",
                cover = n.obj("coverImage")?.st("large"),
                fmt = n.st("format"),
                kind = e.node()?.st("relationType")
            )
        } ?: emptyList()
        val recs = o.obj("recommendations")?.arr("nodes")?.mapNotNull { e ->
            val n = e.node()?.obj("mediaRecommendation") ?: return@mapNotNull null
            val t = n.obj("title")
            Rel(
                id = n.num("id") ?: 0,
                title = t?.st("english") ?: t?.st("romaji") ?: "",
                cover = n.obj("coverImage")?.st("large"),
                fmt = n.st("format"),
                score = n.num("averageScore")
            )
        } ?: emptyList()
        val cast = o.obj("characters")?.arr("edges")?.mapNotNull { e ->
            val n = e.node() ?: return@mapNotNull null
            val p = n.obj("node")
            val va = n.arr("voiceActors")?.firstOrNull()?.node()?.obj("name")?.st("full")
            Cast(
                name = p?.obj("name")?.st("full") ?: "",
                img = p?.obj("image")?.st("large"),
                va = va
            )
        } ?: emptyList()
        val shots = o.arr("streamingEpisodes")?.mapNotNull { e ->
            e.node()?.let { Shot(it.st("title") ?: "", it.st("thumbnail")) }
        } ?: emptyList()
        return m.copy(
            trailer = if (tr?.st("site") == "youtube") tr.st("id") else null,
            links = links,
            rel = rel.filter { it.id != 0 },
            recs = recs.filter { it.id != 0 },
            cast = cast,
            shots = shots
        )
    }

    fun seasonnow(): Pair<String, Int> {
        val cal = java.util.Calendar.getInstance()
        val m = cal.get(java.util.Calendar.MONTH)
        val y = cal.get(java.util.Calendar.YEAR)
        val s = when (m) {
            in 0..2 -> "WINTER"
            in 3..5 -> "SPRING"
            in 6..8 -> "SUMMER"
            else -> "FALL"
        }
        return s to y
    }
}

suspend fun animelist(name: String): List<Rec> {
    val safe = name.replace("\\", "").replace("\"", "")
    val q = "query { User(name: \"" + safe + "\") { mediaListCollection(type: ANIME) { lists { entries { status progress media { id title { romaji english } coverImage { large } episodes } } } } } }"
    val raw = net.post("https://graphql.anilist.co", JsonObject(mapOf("query" to JsonPrimitive(q))).toString())
    val root = runCatching { net.jso.parseToJsonElement(raw) }.getOrNull() as? JsonObject ?: return emptyList()
    val lists = root["data"]?.jsonObject?.get("User")?.jsonObject
        ?.get("mediaListCollection")?.jsonObject
        ?.get("lists")?.jsonArray ?: return emptyList()
    val out = mutableListOf<Rec>()
    for (one in lists) {
        val rows = (one as? JsonObject)?.get("entries")?.jsonArray ?: continue
        for (e in rows) {
            val o = e as? JsonObject ?: continue
            val m = o["media"]?.jsonObject ?: continue
            val id = m["id"]?.jsonPrimitive?.intOrNull ?: continue
            val t = m["title"]?.jsonObject
            val title = t?.get("english")?.jsonPrimitive?.contentOrNull
                ?: t?.get("romaji")?.jsonPrimitive?.contentOrNull ?: ""
            out.add(
                Rec(
                    id = id,
                    title = title,
                    cover = m["coverImage"]?.jsonObject?.get("large")?.jsonPrimitive?.contentOrNull,
                    ep = o["progress"]?.jsonPrimitive?.intOrNull ?: 0,
                    total = m["episodes"]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 },
                    stat = o["status"]?.jsonPrimitive?.contentOrNull ?: "PLANNING"
                )
            )
        }
    }
    return out.distinctBy { it.id }
}

data class Air(
    val id: Int = 0,
    val aired: Int = 0,
    val total: Int? = null,
    val next: Int = 0,
    val at: Long = 0,
    val state: String = ""
)

suspend fun airingNow(ids: List<Int>): Map<Int, Air> {
    if (ids.isEmpty()) return emptyMap()
    val out = HashMap<Int, Air>()
    for (chunk in ids.distinct().chunked(40)) {
        val list = chunk.joinToString(",")
        val q = "query { Page(perPage: 50) { media(id_in: [$list], type: ANIME) { id episodes status nextAiringEpisode { episode airingAt } } } }"
        val raw = runCatching {
            net.post("https://graphql.anilist.co", JsonObject(mapOf("query" to JsonPrimitive(q))).toString())
        }.getOrNull() ?: continue
        val rows = runCatching {
            (net.jso.parseToJsonElement(raw) as? JsonObject)
                ?.get("data")?.jsonObject
                ?.get("Page")?.jsonObject
                ?.get("media")?.jsonArray
        }.getOrNull() ?: continue
        for (row in rows) {
            val o = row as? JsonObject ?: continue
            val id = o["id"]?.jsonPrimitive?.intOrNull ?: continue
            val total = o["episodes"]?.jsonPrimitive?.intOrNull
            val state = o["status"]?.jsonPrimitive?.contentOrNull ?: ""
            val next = o["nextAiringEpisode"]?.jsonObject
            val ep = next?.get("episode")?.jsonPrimitive?.intOrNull ?: 0
            val at = next?.get("airingAt")?.jsonPrimitive?.longOrNull ?: 0L
            val done = state == "FINISHED" || state == "CANCELLED"
            val aired = when {
                ep > 0 -> ep - 1
                done -> total ?: 0
                else -> 0
            }
            out[id] = Air(id, aired, total, ep, at * 1000L, state)
        }
    }
    return out
}
