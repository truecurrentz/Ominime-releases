package com.ominime.app.data

import kotlinx.serialization.json.JsonObject
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object extract {
    private val mix = Regex("""https?:\S+?\.m3u8\S*""")

    suspend fun grab(url: String, ref: String): List<Stream> {
        val link = url.secure()
        val out = runCatching { route(link, ref) }.getOrDefault(emptyList())
        if (out.isNotEmpty()) return out.distinctBy { it.url }
        return runCatching { scan(link, ref) }.getOrDefault(emptyList()).distinctBy { it.url }
    }

    private suspend fun route(url: String, ref: String): List<Stream> {
        return when {
            Regex("megaplay\\.[^/]+/stream/", RegexOption.IGNORE_CASE).containsMatchIn(url) -> mega(url, ref, "getSources")
            Regex("vidtube\\.", RegexOption.IGNORE_CASE).containsMatchIn(url) -> mega(url, ref, "getSourcesNew")
            Regex("vidmoly\\.(net|biz|to|ru)", RegexOption.IGNORE_CASE).containsMatchIn(url) -> vidmoly(url, ref)
            Regex("echovideo\\.ru/embed-[01]", RegexOption.IGNORE_CASE).containsMatchIn(url) -> echo(url, ref)
            Regex("echovideo\\.ru/embed-20", RegexOption.IGNORE_CASE).containsMatchIn(url) -> echo(url, ref)
            Regex("upn\\.one", RegexOption.IGNORE_CASE).containsMatchIn(url) -> nova(url)
            else -> emptyList()
        }
    }

    private suspend fun vidmoly(url: String, ref: String): List<Stream> {
        val html = scrape.html(url, ref)
        val m = Regex("""sources:\s*\[\s*\{\s*file:\s*['"]([^'"]+?\.m3u8[^'"]*)['"]""").find(html) ?: return emptyList()
        return listOf(Stream(url = m.groupValues[1].secure(), kind = "hls", server = "Vidmoly", ref = scrape.origin(url)))
    }

    private suspend fun echo(url: String, ref: String): List<Stream> {
        val u = runCatching { java.net.URI(url) }.getOrNull() ?: return emptyList()
        val path = u.path ?: return emptyList()
        val seg = path.trim('/').split("/")
        if (seg.size < 2) return emptyList()
        val kind = seg[0]
        val id = seg[1]
        val host = (u.scheme ?: "https") + "://" + (u.host ?: "")
        val raw = net.text(
            "$host/$kind/getSources?id=$id",
            mapOf(
                "User-Agent" to scrape.ua,
                "Referer" to url,
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json,*/*"
            )
        )
        val o = scrape.obj(raw) ?: return emptyList()
        val list = mutableListOf<Stream>()
        val arr = o.arr("sources")
        if (arr != null) {
            for (e in arr) {
                val one = e.node()
                val link = one?.txt("file") ?: one?.txt("url") ?: runCatching { e.toString().trim('"') }.getOrNull()
                if (link == null || !link.startsWith("http")) continue
                list.add(
                    Stream(
                        url = link.secure(),
                        kind = if (scrape.hls(link)) "hls" else "mp4",
                        server = "Echovideo",
                        ref = "$host/"
                    )
                )
            }
        } else {
            val one = o.txt("sources") ?: o.txt("source")
            if (one != null && one.startsWith("http")) list.add(
                Stream(url = one.secure(), kind = if (scrape.hls(one)) "hls" else "mp4", server = "Echovideo", ref = "$host/")
            )
        }
        return list
    }

    private suspend fun nova(url: String): List<Stream> {
        val id = Regex("upn\\.one/#([A-Za-z0-9]+)", RegexOption.IGNORE_CASE).find(url)?.groupValues?.get(1) ?: return emptyList()
        val raw = net.text(
            "https://nova.upn.one/api/v1/video?id=$id&w=1920&h=1080&r=",
            mapOf("User-Agent" to scrape.ua, "Referer" to "https://nova.upn.one/", "Accept" to "application/json,*/*")
        ).trim()
        val key = hex("6b69656d7469656e6d75613931316361")
        val iv = hex("313233343536373839306f6975797472")
        val flat = runCatching {
            val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
            c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            String(c.doFinal(hex(raw)), Charsets.UTF_8)
        }.getOrNull() ?: return emptyList()
        val o = scrape.obj(flat) ?: return emptyList()
        val link = o.txt("cf") ?: o.txt("source") ?: return emptyList()
        return listOf(Stream(url = link.secure(), kind = "hls", server = "Nova", ref = "https://nova.upn.one/"))
    }

    private suspend fun mega(url: String, ref: String, path: String): List<Stream> {
        val u = runCatching { java.net.URI(url) }.getOrNull() ?: return emptyList()
        val host = (u.scheme ?: "https") + "://" + (u.host ?: "")
        val page = runCatching { scrape.html(url, "$host/") }.getOrNull() ?: return emptyList()
        val id = Regex("id=[\"']megaplay-player[\"'][^>]*data-id=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            .find(page)?.groupValues?.get(1)
            ?: Regex("data-id=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(page)?.groupValues?.get(1)
            ?: return emptyList()
        val type = u.path?.trim('/')?.split("/")?.lastOrNull() ?: "sub"
        val base = if (path == "getSourcesNew") "https://vidtube.site" else host
        val raw = listOf(
            "$base/stream/$path?id=$id",
            "$base/stream/$path?id=$id&type=$type"
        ).firstNotNullOfOrNull { link ->
            runCatching {
                net.text(
                    link,
                    mapOf(
                        "User-Agent" to scrape.ua,
                        "Accept" to "application/json,*/*",
                        "X-Requested-With" to "XMLHttpRequest",
                        "Referer" to url
                    )
                )
            }.getOrNull()?.takeIf { it.contains("file") || it.contains("enc") }
        } ?: return emptyList()
        val o = scrape.obj(raw) ?: return emptyList()
        val file = source(o) ?: return emptyList()
        val subs = (o.arr("tracks") ?: o.arr("subtitles"))?.mapNotNull { one -> live(one.node(), "$base/") } ?: emptyList()
        val link = sign(file, "$base/")
        return listOf(Stream(url = link, kind = "hls", server = "", subs = subs, prio = 7, ref = "$base/"))
    }

    private fun source(o: JsonObject): String? {
        val enc = o.txt("enc")
        if (!enc.isNullOrBlank()) {
            val key = ByteArray(32).also { b -> "i?LMTAx0Q6,:}50U".toByteArray().copyInto(b) }
            val iv = "W0;27ToaUpl_P%'c".toByteArray()
            val out = runCatching {
                val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
                c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
                String(c.doFinal(Base64.getUrlDecoder().decode(enc.trim().replace("=", ""))), Charsets.UTF_8)
            }.getOrNull() ?: return null
            return scrape.obj(out)?.txt("file")
        }
        val arr = o.arr("sources")
        val one = o.obj("sources")
        return one?.txt("file")
            ?: arr?.firstOrNull()?.node()?.txt("file")
            ?: o.txt("file")
    }

    private fun sign(file: String, ref: String): String {
        val clean = tidy(file)
        val m = Regex("/([a-f0-9]{32})/([a-f0-9]{32})/", RegexOption.IGNORE_CASE).find(clean) ?: return clean
        if (clean.contains("token=")) return clean
        val payload = ((System.currentTimeMillis() / 1000L) + 90).toString() + "|" +
            m.groupValues[1].lowercase() + "/" + m.groupValues[2].lowercase()
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec("MpCdnT0k3n!9f2K#xQ7vL5mR8wN1pY4s".toByteArray(), "HmacSHA256"))
        val sig = Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.toByteArray()))
        val body = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val mark = if (clean.contains("?")) "&" else "?"
        return clean + mark + "token=" + body + "." + sig
    }

    private fun live(o: JsonObject?, ref: String = ""): Subs? {
        if (o == null) return null
        if (o.txt("kind") == "thumbnails") return null
        val link = o.txt("file") ?: o.txt("src") ?: o.txt("url") ?: return null
        val label = o.txt("label") ?: o.txt("lang") ?: "English"
        val lang = langs.norm(o.txt("lang") ?: o.txt("srclang") ?: label)
        return Subs(
            url = tidy(link),
            label = label,
            lang = lang,
            fmt = if (link.contains(".srt", true)) "srt" else "vtt",
            sel = o.boo("default") == true || lang == "en",
            ref = ref
        )
    }

    private fun hex(s: String): ByteArray {
        val out = ByteArray(s.length / 2)
        for (i in out.indices) out[i] = ((Character.digit(s[i * 2], 16) shl 4) + Character.digit(s[i * 2 + 1], 16)).toByte()
        return out
    }

    private suspend fun scan(url: String, ref: String): List<Stream> {
        val direct = tail(url)
        if (direct != null) return listOf(Stream(url = direct, kind = "hls", server = "Direct", ref = scrape.origin(url)))
        val html = runCatching { scrape.html(url, ref) }.getOrNull() ?: return emptyList()
        val hits = LinkedHashSet<String>()
        for (m in mix.findAll(html)) hits.add(tidy(m.value))
        for (key in listOf("file", "src", "source", "url", "hls")) {
            val r = Regex("\"?" + key + "\"?\\s*[:=]\\s*['\"]([^'\"]+?\\.m3u8[^'\"]*)['\"]")
            for (m in r.findAll(html)) hits.add(tidy(m.groupValues[1]))
        }
        val base = scrape.origin(url)
        return hits.map { Stream(url = it, kind = "hls", server = "Web", ref = base) }
    }

    private fun tidy(s: String): String = s
        .replace("\\/", "/")
        .trim()
        .trimEnd('\\', '"', '\'', ',', ')', ';')
        .secure()

    private fun tail(url: String): String? {
        if (!url.contains("#aHR0")) return null
        val part = url.substringAfter("#")
        val raw = try {
            Base64.getUrlDecoder().decode(part)
        } catch (e: Exception) {
            runCatching { Base64.getDecoder().decode(part) }.getOrNull()
        } ?: return null
        val text = String(raw, Charsets.UTF_8)
        return if (scrape.hls(text)) text else null
    }

    fun decode(embed: String): JsonObject? = scrape.obj(embed)
}
