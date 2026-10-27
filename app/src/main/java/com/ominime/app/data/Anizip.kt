package com.ominime.app.data

import kotlinx.serialization.json.JsonObject

object anizip {
    private val hold = HashMap<Int, Pair<Long, JsonObject?>>()

    suspend fun get(id: Int): JsonObject? {
        val now = System.currentTimeMillis()
        hold[id]?.let { if (now - it.first < 6 * 3600_000L) return it.second }
        val o = runCatching {
            net.obj(net.text("https://api.ani.zip/mappings?anilist_id=$id", ttl = 6 * 3600_000L, tag = "az:$id"))
        }.getOrNull()
        hold[id] = now to o
        return o
    }

    fun titles(o: JsonObject?): List<String> {
        val t = o?.obj("titles") ?: return emptyList()
        return listOfNotNull(t.st("en"), t.st("x-jat"), t.st("ja"), t.st("romaji")).distinct()
    }

    fun count(o: JsonObject?): Int? {
        val e = o?.obj("episodes") ?: return null
        return e.keys.mapNotNull { it.toIntOrNull() }.maxOrNull()
    }

    fun ep(o: JsonObject?, n: Int): JsonObject? = o?.obj("episodes")?.obj(n.toString())

    fun title(o: JsonObject?, n: Int): String? {
        val t = ep(o, n)?.obj("title") ?: return null
        return t.st("en") ?: t.st("x-jat") ?: t.st("ja")
    }

    fun note(o: JsonObject?, n: Int): String? {
        val e = ep(o, n) ?: return null
        return e.st("overview") ?: e.st("summary")
    }

    fun shot(o: JsonObject?, n: Int): String? = ep(o, n)?.st("image")

    fun air(o: JsonObject?, n: Int): String? {
        val e = ep(o, n) ?: return null
        return e.st("airdate") ?: e.st("airDate") ?: e.st("aired")
    }

    fun filler(o: JsonObject?, n: Int): Boolean = ep(o, n)?.boo("filler") == true
}
