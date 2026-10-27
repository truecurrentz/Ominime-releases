package com.ominime.app.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

object source {
    val all: List<Prov> = listOf(dunya, koto, senshi, waves)

    val names: List<String> get() = all.map { it.name }

    fun label(name: String): String {
        val at = names.indexOf(name)
        return if (at < 0) "Source 1" else "Source " + (at + 1)
    }

    fun short(name: String): String = label(name)

    fun ready(): Boolean = true

    suspend fun ping(): Boolean = true

    suspend fun eps(m: Media): List<Src> = coroutineScope {
        all.map { one -> async { runCatching { one.eps(m) }.getOrNull() } }.awaitAll()
            .filterNotNull()
            .filter { it.subs.isNotEmpty() || it.dubs.isNotEmpty() }
    }

    suspend fun watch(m: Media, prov: String, audio: String, ep: Int): List<Stream> {
        val one = all.firstOrNull { it.name == prov } ?: return emptyList()
        return runCatching { one.play(m, audio, ep) }
            .getOrDefault(emptyList())
            .sortedByDescending { it.prio }
    }

    data class All(val streams: List<Stream>, val audio: String, val hit: List<String>)

    suspend fun gather(m: Media, audio: String, ep: Int): All = coroutineScope {
        val order = all.map { it.name }
        val rows = order.map { id ->
            async {
                val one = all.firstOrNull { it.name == id } ?: return@async Triple(id, emptyList<Stream>(), audio)
                val main = runCatching { one.play(m, audio, ep) }.getOrDefault(emptyList())
                if (main.isNotEmpty()) return@async Triple(id, main, audio)
                if (audio == "dub") {
                    val next = runCatching { one.play(m, "sub", ep) }.getOrDefault(emptyList())
                    if (next.isNotEmpty()) return@async Triple(id, next, "sub")
                }
                Triple(id, emptyList<Stream>(), audio)
            }
        }.awaitAll()
        val seen = LinkedHashSet<String>()
        val out = mutableListOf<Stream>()
        val hits = mutableListOf<String>()
        var used = audio
        for ((id, list, kind) in rows) {
            if (list.isEmpty()) continue
            hits.add(id)
            if (kind == "sub") used = "sub"
            for (one in list) {
                if (one.url.isBlank() || !seen.add(one.url)) continue
                out.add(one.copy(server = "Source " + (out.size + 1)))
            }
        }
        All(out, used, hits)
    }
}
