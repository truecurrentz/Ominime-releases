package com.ominime.app.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class Home(
    val hero: List<Media> = emptyList(),
    val air: List<Media> = emptyList(),
    val hot: List<Media> = emptyList(),
    val top: List<Media> = emptyList(),
    val soon: List<Media> = emptyList(),
    val pick: List<Media> = emptyList()
)

data class Play(
    val prov: String = "",
    val audio: String = "sub",
    val streams: List<Stream> = emptyList(),
    val note: String? = null
)

object repo {
    val providers: List<String> get() = source.names

    val genres = listOf(
        "Action", "Adventure", "Comedy", "Drama", "Ecchi", "Fantasy", "Horror",
        "Mahou Shoujo", "Mecha", "Music", "Mystery", "Psychological", "Romance",
        "Sci-Fi", "Slice of Life", "Sports", "Supernatural", "Thriller"
    )

    val formats = listOf("TV", "MOVIE", "OVA", "ONA", "SPECIAL")

    val states = listOf("RELEASING", "FINISHED", "NOT_YET_RELEASED")

    val seasons = listOf("WINTER", "SPRING", "SUMMER", "FALL")

    val sorts = listOf(
        "Popular" to "POPULARITY_DESC",
        "Top rated" to "SCORE_DESC",
        "Trending" to "TRENDING_DESC",
        "Newest" to "START_DATE_DESC",
        "A to Z" to "TITLE_ROMAJI"
    )

    private val hold = HashMap<String, Any>()
    private var homeAt = 0L
    private var airAt = 0L

    suspend fun home(force: Boolean = false): Home = coroutineScope {
        if (!force && System.currentTimeMillis() - homeAt < 10 * 60_000L) {
            (hold["home"] as? Home)?.let { return@coroutineScope it }
        }
        val (sn, sy) = anilist.seasonnow()
        val h = async { runCatching { anilist.page(1, "TRENDING_DESC") }.getOrDefault(emptyList()) }
        val a = async { runCatching { anilist.page(1, "[POPULARITY_DESC]", state = "RELEASING") }.getOrDefault(emptyList()) }
        val p = async { runCatching { anilist.page(1, "[POPULARITY_DESC]", season = sn, year = sy) }.getOrDefault(emptyList()) }
        val t = async { runCatching { anilist.page(1, "SCORE_DESC") }.getOrDefault(emptyList()) }
        val s = async { runCatching { anilist.page(1, "[POPULARITY_DESC]", state = "NOT_YET_RELEASED") }.getOrDefault(emptyList()) }
        val g = async { runCatching { anilist.page(1, "[POPULARITY_DESC]", genre = "Romance") }.getOrDefault(emptyList()) }
        val hot = p.await()
        val out = Home(
            hero = (h.await().ifEmpty { hot }).take(6),
            air = a.await(),
            hot = hot,
            top = t.await(),
            soon = s.await(),
            pick = g.await()
        )
        hold["home"] = out
        homeAt = System.currentTimeMillis()
        out
    }

    suspend fun airing(force: Boolean = false): List<Slot> {
        if (!force && System.currentTimeMillis() - airAt < 20 * 60_000L) {
            (hold["air"] as? List<*>)?.let { return it.filterIsInstance<Slot>() }
        }
        val now = System.currentTimeMillis()
        val list = runCatching {
            anilist.airing((now - 14 * 3600_000L) / 1000L, (now + 7 * 24 * 3600_000L) / 1000L)
        }.getOrDefault(emptyList())
        hold["air"] = list
        airAt = System.currentTimeMillis()
        return list
    }

    suspend fun detail(id: Int, force: Boolean = false): Media {
        val k = "m$id"
        if (!force) (hold[k] as? Media)?.let { return it }
        val m = anilist.full(id)
        hold[k] = m
        return m
    }

    fun quick(id: Int): Media? = hold["m$id"] as? Media

    fun rec(m: Media): Rec = Rec(
        id = m.id,
        title = m.title.ifBlank { m.romaji },
        cover = m.banner ?: m.cover,
        total = m.total,
        mal = m.mal,
        aired = aired(m)
    )

    fun aired(m: Media): Int {
        val next = m.next?.ep ?: 0
        val all = m.total ?: 0
        val done = m.state == "FINISHED" || m.state == "CANCELLED"
        return when {
            next > 0 -> next - 1
            done -> all
            else -> 0
        }
    }

    suspend fun eps(m: Media, prov: String? = null): Pair<List<Ep>, List<Src>> = coroutineScope {
        val sets = runCatching { source.eps(m) }.getOrDefault(emptyList())
        val pick = sets.firstOrNull { it.name == prov } ?: sets.firstOrNull()
        val have = aired(m)
        val meta = runCatching { kitsu.eps(m, have) }.getOrDefault(emptyMap())
        val shots = m.shots.associate { one -> num(one.title) to one }
        val rows = ((pick?.subs ?: emptyList()) + (pick?.dubs ?: emptyList()))
            .filter { have <= 0 || it.num <= have }
        val map = rows.groupBy { it.num }.mapValues { it.value.first() }
        val nums = rows.map { it.num }.distinct().sorted().ifEmpty {
            if (have > 0) (1..have).toList() else emptyList()
        }
        val out = nums.map { n ->
            val row = map[n]
            val shot = shots[n]
            val info = meta[n]
            Ep(
                num = n,
                title = info?.title?.takeIf { it.isNotBlank() }
                    ?: row?.title?.takeIf { it.isNotBlank() && it != "Episode $n" }
                    ?: shot?.title
                    ?: "Episode $n",
                desc = info?.desc ?: row?.desc,
                img = info?.img ?: row?.img ?: shot?.img,
                air = info?.air ?: row?.air,
                filler = row?.filler == true,
                sub = true,
                dub = pick?.dubs?.any { it.num == n } == true
            )
        }
        out to sets
    }

    private fun num(s: String): Int? = Regex("(?i)episode\\s*(\\d+)").find(s)?.groupValues?.getOrNull(1)?.toIntOrNull()

    data class Scan(
        val notes: List<Note> = emptyList(),
        val checked: Int = 0,
        val watched: Int = 0,
        val note: String = ""
    )

    suspend fun scan(): Scan {
        val rows = store.list.filter { it.stat != "COMPLETED" && it.stat != "DROPPED" }
        if (rows.isEmpty()) return Scan(note = "Your list is empty")
        val ids = rows.map { one -> one.id }
        val air: Map<Int, Air> = runCatching { airingNow(ids) }.getOrElse { emptyMap() }
        if (air.isEmpty()) return Scan(checked = rows.size, note = "AniList did not answer")
        val fresh = mutableListOf<Note>()
        var known = 0
        for (r in rows) {
            val a = air[r.id] ?: continue
            if (a.aired <= 0) continue
            known++
            val base = r.seen > 0
            if (base && a.aired > r.aired) {
                fresh.add(
                    Note(
                        id = r.id,
                        title = r.title,
                        cover = r.cover,
                        ep = a.aired,
                        at = System.currentTimeMillis()
                    )
                )
            }
            store.track(r.id, a.aired)
        }
        fresh.forEach { store.note(it) }
        val note = when {
            fresh.isNotEmpty() -> "${fresh.size} new episode${if (fresh.size == 1) "" else "s"}"
            known == 0 -> "No schedule for these titles yet"
            else -> "Nothing new from $known title${if (known == 1) "" else "s"}"
        }
        return Scan(fresh, rows.size, known, note)
    }

    suspend fun play(m: Media, audio: String, ep: Int): Play {
        val want = if (audio == "dub") "dub" else "sub"
        val all = runCatching { source.gather(m, want, ep) }.getOrNull()
            ?: return Play("", want, emptyList(), "No source answered")
        if (all.streams.isEmpty()) {
            return Play("", want, emptyList(), "No stream for episode $ep")
        }
        val note = when {
            all.audio == "sub" && want == "dub" -> "Dub missing, playing sub"
            else -> null
        }
        return Play("", all.audio, all.streams, note)
    }

    suspend fun arts(): Map<String, String?> {
        val now = System.currentTimeMillis()
        (hold["arts"] as? Pair<*, *>)?.let { row ->
            val at = row.first as? Long ?: 0L
            if (now - at < 6 * 3600_000L) return (row.second as? Map<*, *>)?.entries?.associate { it.key as String to it.value as String? } ?: emptyMap()
        }
        val picks: List<Pair<String, String?>> = coroutineScope {
            genres.take(8).map { name ->
                async {
                    val list: List<Media> = runCatching { anilist.page(1, "[POPULARITY_DESC]", genre = name) }
                        .getOrDefault(emptyList())
                    name to list.filter { it.cover != null }.randomOrNull()?.cover
                }
            }.awaitAll()
        }
        val map = picks.toMap()
        hold["arts"] = now to map
        return map
    }

    suspend fun explore(q: String, f: Find, page: Int): List<Media> {
        val season = f.season.ifBlank { null }
        val year = if (f.year > 0) f.year else null
        return anilist.page(
            page = page,
            sort = f.sort,
            search = q.ifBlank { null },
            season = season,
            year = year,
            gens = f.gens,
            fmts = f.fmts,
            stats = f.stats,
            score = f.score
        )
    }
}
