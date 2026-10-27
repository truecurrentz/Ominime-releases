package com.ominime.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

object net {
    val ua = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
    val jso = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
    private val body = "application/json".toMediaType()
    lateinit var cli: OkHttpClient
    private val jar = HashMap<String, MutableList<Cookie>>()

    private val store = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            runCatching {
                val box = jar.getOrPut(url.host) { mutableListOf() }
                synchronized(box) {
                    for (c in cookies) {
                        box.removeAll { it.name == c.name }
                        box.add(c)
                    }
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> = runCatching {
            val box = jar[url.host] ?: return@runCatching emptyList<Cookie>()
            val now = System.currentTimeMillis()
            synchronized(box) {
                box.removeAll { it.expiresAt < now }
                box.toList()
            }
        }.getOrDefault(emptyList())
    }

    fun share(url: String): Map<String, String> = runCatching {
        val host = java.net.URI(url).host ?: ""
        val box = jar[host] ?: return@runCatching emptyMap<String, String>()
        val now = System.currentTimeMillis()
        val line = synchronized(box) {
            box.removeAll { it.expiresAt < now }
            box.joinToString("; ") { it.name + "=" + it.value }
        }
        if (line.isBlank()) emptyMap() else mapOf("Cookie" to line)
    }.getOrDefault(emptyMap())

    fun init(c: Context) {
        cli = OkHttpClient.Builder()
            .cache(Cache(File(c.cacheDir, "http"), 96L * 1024L * 1024L))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .cookieJar(store)
            .followRedirects(true)
            .build()
        cache.init(File(c.cacheDir, "json"))
    }

    suspend fun text(url: String, head: Map<String, String> = emptyMap(), ttl: Long = 0L, tag: String? = null): String =
        withContext(Dispatchers.IO) {
            val k = tag ?: url
            if (ttl > 0L) cache.get(k)?.let { return@withContext it }
            val b = Request.Builder().url(url)
                .header("User-Agent", ua)
                .header("Accept-Language", "en-US,en;q=0.9")
            head.forEach { (n, v) -> b.header(n, v) }
            cli.newCall(b.build()).execute().use { r ->
                val t = r.body?.string() ?: ""
                if (!r.isSuccessful) throw Fail(r.code, t.take(240))
                if (ttl > 0L) cache.put(k, t, ttl)
                t
            }
        }

    suspend fun bytes(url: String, head: Map<String, String> = emptyMap()): ByteArray =
        withContext(Dispatchers.IO) {
            val b = Request.Builder().url(url)
                .header("User-Agent", ua)
                .header("Accept-Language", "en-US,en;q=0.9")
            head.forEach { (n, v) -> b.header(n, v) }
            cli.newCall(b.build()).execute().use { r ->
                if (!r.isSuccessful) throw Fail(r.code, url)
                r.body?.bytes() ?: ByteArray(0)
            }
        }

    suspend fun post(url: String, payload: String, head: Map<String, String> = emptyMap(), ttl: Long = 0L, tag: String? = null): String =
        withContext(Dispatchers.IO) {
            val k = tag ?: (url + payload)
            if (ttl > 0L) cache.get(k)?.let { return@withContext it }
            val b = Request.Builder().url(url)
                .header("User-Agent", ua)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody(body))
            head.forEach { (n, v) -> b.header(n, v) }
            cli.newCall(b.build()).execute().use { r ->
                val t = r.body?.string() ?: ""
                if (!r.isSuccessful) throw Fail(r.code, t.take(240))
                if (ttl > 0L) cache.put(k, t, ttl)
                t
            }
        }

    fun flush() {
        runCatching { cli.cache?.evictAll() }
        cache.clear()
    }

    fun obj(raw: String): JsonObject = jso.parseToJsonElement(raw) as? JsonObject ?: JsonObject(emptyMap())
}

fun JsonElement?.node(): JsonObject? = if (this is JsonObject) this else null

fun JsonObject.st(k: String): String? {
    val e = this[k] ?: return null
    if (e is JsonNull) return null
    return runCatching { e.jsonPrimitive.contentOrNull }.getOrNull()?.takeIf { it.isNotBlank() }
}

fun JsonObject.num(k: String): Int? {
    val e = this[k] ?: return null
    if (e is JsonNull) return null
    return runCatching { e.jsonPrimitive.intOrNull }.getOrNull()
}

fun JsonObject.lng(k: String): Long? {
    val e = this[k] ?: return null
    if (e is JsonNull) return null
    return runCatching { e.jsonPrimitive.longOrNull }.getOrNull()
}

fun JsonObject.flt(k: String): Float? {
    val e = this[k] ?: return null
    if (e is JsonNull) return null
    return runCatching { e.jsonPrimitive.floatOrNull }.getOrNull()
}

fun JsonObject.boo(k: String): Boolean? {
    val e = this[k] ?: return null
    if (e is JsonNull) return null
    return runCatching { e.jsonPrimitive.booleanOrNull }.getOrNull()
}

fun JsonObject.obj(k: String): JsonObject? = this[k].node()

fun JsonObject.arr(k: String): JsonArray? = this[k] as? JsonArray
