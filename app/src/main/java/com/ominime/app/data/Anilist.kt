package com.ominime.app.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
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

class Api(val note: String) : Exception(note)

private fun why(raw: String): String? {
    val hit = Regex("\"message\"\\s*:\\s*\"([^\"]{3,160})").find(raw)?.groupValues?.get(1)
    return hit?.replace("\\", "")?.trim()?.take(160)
}

private suspend fun query(q: String, vars: Map<String, JsonElement> = emptyMap(), token: String = ""): JsonObject {
    val body = buildString {
        append("{\"query\":\"").append(q.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " "))
        append("\",\"variables\":{")
        append(vars.entries.joinToString(",") { (k, v) -> "\"" + k + "\":" + v.toString() })
        append("}}")
    }
    val head = if (token.isBlank()) emptyMap() else mapOf("Authorization" to "Bearer " + token)
    val raw = runCatching { net.post("https://graphql.anilist.co", body, head) }.getOrElse { one ->
        val txt = (one as? Fail)?.note ?: ""
        throw Api(why(txt) ?: "AniList did not answer, try again")
    }
    val root = net.obj(raw)
    if (root.isEmpty()) throw Api("AniList did not answer")
    val bad = (root["errors"] as? JsonArray)
        ?.firstOrNull()?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
    if (!bad.isNullOrBlank()) throw Api(bad)
    return root["data"]?.jsonObject ?: throw Api("AniList returned no data")
}

private fun rec(o: JsonObject): Rec? {
    val m = o["media"]?.jsonObject ?: return null
    val id = m["id"]?.jsonPrimitive?.intOrNull ?: return null
    val t = m["title"]?.jsonObject
    val title = t?.get("english")?.jsonPrimitive?.contentOrNull
        ?: t?.get("romaji")?.jsonPrimitive?.contentOrNull ?: ""
    val next = m["nextAiringEpisode"]?.jsonObject?.get("episode")?.jsonPrimitive?.intOrNull
    val all = m["episodes"]?.jsonPrimitive?.intOrNull
    val state = m["status"]?.jsonPrimitive?.contentOrNull ?: ""
    val aired = when {
        next != null && next > 0 -> next - 1
        state == "FINISHED" || state == "CANCELLED" -> all ?: 0
        else -> 0
    }
    return Rec(
        id = id,
        title = title,
        cover = m["coverImage"]?.jsonObject?.get("large")?.jsonPrimitive?.contentOrNull,
        ep = o["progress"]?.jsonPrimitive?.intOrNull ?: 0,
        total = all?.takeIf { it > 0 },
        stat = o["status"]?.jsonPrimitive?.contentOrNull ?: "PLANNING",
        aired = aired
    )
}

private fun rows(data: JsonObject): List<Rec> {
    val lists = data["MediaListCollection"]?.jsonObject?.get("lists")?.jsonArray ?: return emptyList()
    val out = mutableListOf<Rec>()
    for (one in lists) {
        val entries = (one as? JsonObject)?.get("entries")?.jsonArray ?: continue
        for (e in entries) {
            val o = e as? JsonObject ?: continue
            val r = rec(o) ?: continue
            out.add(r)
        }
    }
    return out.distinctBy { it.id }
}

private suspend fun walk(who: String): List<Rec> {
    val out = mutableListOf<Rec>()
    var page = 1
    while (page <= 24) {
        val data = query(
            "query(\$name:String,\$page:Int){Page(page:\$page,perPage:50){pageInfo{hasNextPage}mediaList(userName:\$name,type:ANIME){status progress media{$pick}}}}",
            mapOf("name" to JsonPrimitive(who), "page" to JsonPrimitive(page))
        )
        val box = data["Page"]?.jsonObject ?: break
        val one = box["mediaList"]?.jsonArray ?: break
        if (one.isEmpty()) break
        for (e in one) {
            val o = e as? JsonObject ?: continue
            val r = rec(o) ?: continue
            out.add(r)
        }
        val more = box["pageInfo"]?.jsonObject?.get("hasNextPage")?.jsonPrimitive?.contentOrNull
        if (more != "true") break
        page++
    }
    return out.distinctBy { it.id }
}

private const val pick = "id title { romaji english } coverImage { large } episodes status nextAiringEpisode { episode }"

suspend fun mine(token: String): List<Rec> {
    if (token.isBlank()) throw Api("Connect your AniList account first")
    val me = runCatching { viewer(token) }.getOrNull()
    if (me == null || me.name.isBlank()) throw Api("That connection expired, connect again")
    val one = runCatching {
        rows(
            query(
                "query(\$name:String){MediaListCollection(userName:\$name,type:ANIME){lists{entries{status progress media{$pick}}}}}}",
                mapOf("name" to JsonPrimitive(me.name)),
                token
            )
        )
    }.getOrNull().orEmpty()
    if (one.isNotEmpty()) return one
    val two = runCatching { walk(me.name) }.getOrNull().orEmpty()
    if (two.isNotEmpty()) return two
    throw Api("Nothing on your AniList list yet")
}

data class Me(val id: Int = 0, val name: String = "", val avatar: String? = null)

suspend fun viewer(token: String): Me {
    if (token.isBlank()) return Me()
    val data = query("query{Viewer{id name avatar{large}}}", emptyMap(), token)
    val v = data["Viewer"]?.jsonObject ?: return Me()
    return Me(
        id = v["id"]?.jsonPrimitive?.intOrNull ?: 0,
        name = v["name"]?.jsonPrimitive?.contentOrNull ?: "",
        avatar = v["avatar"]?.jsonObject?.get("large")?.jsonPrimitive?.contentOrNull
    )
}

suspend fun push(token: String, id: Int, ep: Int, state: String?): Boolean {
    if (token.isBlank()) return false
    val vars = buildString {
        append("{")
        append("\"id\":").append(id)
        append(",\"progress\":").append(ep)
        if (!state.isNullOrBlank()) append(",\"status\":\"").append(state).append("\"")
        append("}")
    }
    val body = "{\"query\":\"mutation(\$id:Int,\$progress:Int,\$status:MediaListStatus){SaveMediaListEntry(mediaId:\$id,progress:\$progress,status:\$status){id progress}}\",\"variables\":" + vars + "}"
    return runCatching {
        val raw = net.post("https://graphql.anilist.co", body, mapOf("Authorization" to "Bearer " + token))
        val root = net.obj(raw)
        val bad = (root["errors"] as? JsonArray)?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
        if (!bad.isNullOrBlank()) throw Api(bad.take(120))
        true
    }.getOrElse { one ->
        if (one is Api) throw one
        throw Api(why((one as? Fail)?.note ?: "") ?: "AniList refused an update")
    }
}

private val states = setOf("CURRENT", "PLANNING", "COMPLETED", "DROPPED", "PAUSED", "REPEATING")

private fun mood(r: Rec): String {
    val one = r.stat.uppercase().trim()
    if (one in states) return one
    val all = r.total ?: 0
    return when {
        all > 0 && r.ep >= all -> "COMPLETED"
        r.ep > 0 -> "CURRENT"
        else -> "PLANNING"
    }
}

suspend fun syncall(token: String, rows: List<Rec>): Int {
    if (token.isBlank()) throw Api("Connect your AniList account first")
    val me = runCatching { viewer(token) }.getOrNull()
    if (me == null || me.name.isBlank()) throw Api("That connection expired, connect again")
    var done = 0
    var last = ""
    for (r in rows) {
        if (r.id <= 0) continue
        runCatching { push(token, r.id, r.ep.coerceAtLeast(0), mood(r)) }
            .onSuccess {
                done++
                kotlinx.coroutines.delay(420)
            }
            .onFailure { last = (it as? Api)?.note ?: it.message.orEmpty() }
        if (done >= 200) break
    }
    if (done == 0 && last.isNotBlank()) throw Api(last.take(120))
    return done
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
