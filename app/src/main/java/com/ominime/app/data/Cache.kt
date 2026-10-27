package com.ominime.app.data

import java.io.File
import kotlin.math.abs

object cache {
    private val mem = LinkedHashMap<String, Pair<Long, String>>(24, 0.75f, true)
    private var dir: File? = null

    fun init(f: File) {
        if (!f.exists()) f.mkdirs()
        dir = f
    }

    private fun name(k: String) = abs(k.hashCode()).toString(16) + "-" + k.length + ".json"

    fun put(k: String, v: String, ttl: Long) {
        val exp = if (ttl <= 0L) Long.MAX_VALUE else System.currentTimeMillis() + ttl
        synchronized(mem) {
            mem[k] = exp to v
            if (mem.size > 48) mem.keys.firstOrNull()?.let { mem.remove(it) }
        }
        val d = dir ?: return
        runCatching { File(d, name(k)).writeText(exp.toString() + "\n" + v) }
    }

    fun get(k: String): String? {
        synchronized(mem) {
            val hit = mem[k]
            if (hit != null) {
                if (hit.first > System.currentTimeMillis()) return hit.second
                mem.remove(k)
            }
        }
        val d = dir ?: return null
        val f = File(d, name(k))
        if (!f.exists()) return null
        val raw = runCatching { f.readText() }.getOrNull() ?: return null
        val cut = raw.indexOf('\n')
        if (cut <= 0) return null
        val exp = raw.substring(0, cut).toLongOrNull() ?: return null
        val v = raw.substring(cut + 1)
        if (exp <= System.currentTimeMillis()) {
            runCatching { f.delete() }
            return null
        }
        synchronized(mem) { mem[k] = exp to v }
        return v
    }

    fun size(): Long {
        val d = dir ?: return 0L
        return runCatching { d.listFiles()?.sumOf { it.length() } ?: 0L }.getOrDefault(0L)
    }

    fun clear() {
        synchronized(mem) { mem.clear() }
        val d = dir ?: return
        runCatching { d.listFiles()?.forEach { it.delete() } }
    }
}
