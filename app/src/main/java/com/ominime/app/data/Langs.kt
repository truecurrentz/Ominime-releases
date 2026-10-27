package com.ominime.app.data

object langs {
    val all: List<Pair<String, String>> = listOf(
        "en" to "English",
        "es" to "Español",
        "pt" to "Português",
        "fr" to "Français",
        "de" to "Deutsch",
        "it" to "Italiano",
        "ru" to "Русский",
        "ar" to "العربية",
        "id" to "Indonesia",
        "hi" to "हिन्दी",
        "ja" to "日本語",
        "ko" to "한국어",
        "zh" to "中文",
        "tr" to "Türkçe",
        "pl" to "Polski",
        "vi" to "Tiếng Việt",
        "th" to "ไทย",
        "nl" to "Nederlands",
        "ms" to "Melayu",
        "tl" to "Filipino"
    )

    private val names = mapOf(
        "english" to "en", "eng" to "en", "en" to "en",
        "spanish" to "es", "español" to "es", "espanol" to "es", "spa" to "es", "es" to "es",
        "portuguese" to "pt", "português" to "pt", "portugues" to "pt", "por" to "pt", "pt" to "pt",
        "french" to "fr", "français" to "fr", "fra" to "fr", "fre" to "fr", "fr" to "fr",
        "german" to "de", "deutsch" to "de", "ger" to "de", "deu" to "de", "de" to "de",
        "italian" to "it", "italiano" to "it", "ita" to "it", "it" to "it",
        "russian" to "ru", "русский" to "ru", "rus" to "ru", "ru" to "ru",
        "arabic" to "ar", "العربية" to "ar", "ara" to "ar", "ar" to "ar",
        "indonesian" to "id", "indonesia" to "id", "ind" to "id", "id" to "id",
        "hindi" to "hi", "हिन्दी" to "hi", "hin" to "hi", "hi" to "hi",
        "japanese" to "ja", "日本語" to "ja", "jpn" to "ja", "ja" to "ja",
        "korean" to "ko", "한국어" to "ko", "kor" to "ko", "ko" to "ko",
        "chinese" to "zh", "中文" to "zh", "chi" to "zh", "zho" to "zh", "zh" to "zh",
        "turkish" to "tr", "türkçe" to "tr", "tur" to "tr", "tr" to "tr",
        "polish" to "pl", "polski" to "pl", "pol" to "pl", "pl" to "pl",
        "vietnamese" to "vi", "tiếng việt" to "vi", "vie" to "vi", "vi" to "vi",
        "thai" to "th", "ไทย" to "th", "tha" to "th", "th" to "th",
        "dutch" to "nl", "nederlands" to "nl", "nld" to "nl", "nl" to "nl",
        "malay" to "ms", "melayu" to "ms", "msa" to "ms", "ms" to "ms",
        "filipino" to "tl", "tagalog" to "tl", "tgl" to "tl", "tl" to "tl",
        "in" to "id", "iw" to "he", "ji" to "yi", "nb" to "no", "nn" to "no",
        "hebrew" to "he", "he" to "he", "greek" to "el", "el" to "el",
        "ukrainian" to "uk", "uk" to "uk", "czech" to "cs", "cs" to "cs",
        "hungarian" to "hu", "hu" to "hu", "romanian" to "ro", "ro" to "ro",
        "swedish" to "sv", "sv" to "sv", "danish" to "da", "da" to "da",
        "finnish" to "fi", "fi" to "fi", "norwegian" to "no", "und" to "en"
    )

    private val spot = mapOf(
        "brazil" to "pt", "brasil" to "pt", "latin" to "es", "latino" to "es", "castellano" to "es",
        "simplified" to "zh", "traditional" to "zh", "mandarin" to "zh", "cantonese" to "zh",
        "hardsub" to "", "softsub" to "", "signs" to "", "songs" to "", "full" to "", "cc" to "",
        "sdh" to "", "forced" to "", "default" to "", "eng" to "en", "por" to "pt", "spa" to "es",
        "fre" to "fr", "fra" to "fr", "ger" to "de", "deu" to "de", "ita" to "it", "rus" to "ru",
        "ara" to "ar", "ind" to "id", "hin" to "hi", "jpn" to "ja", "kor" to "ko", "chi" to "zh",
        "zho" to "zh", "tur" to "tr", "pol" to "pl", "vie" to "vi", "tha" to "th", "nld" to "nl",
        "msa" to "ms", "tgl" to "tl", "fil" to "tl"
    )

    fun norm(raw: String?): String {
        val s = raw?.trim()?.lowercase().orEmpty()
        if (s.isBlank()) return "en"
        val clean = s.replace(Regex("[._\\-()\\[\\]{}:;,/]"), " ").replace(Regex("\\s+"), " ").trim()
        if (clean.isBlank()) return "en"
        names[clean]?.let { return it }
        names[clean.replace(" ", "")]?.let { return it }
        val bits = clean.split(' ')
        for (b in bits) {
            names[b]?.let { return it }
        }
        for (b in bits) {
            val hit = spot[b]
            if (hit != null && hit.isNotEmpty()) return hit
        }
        val head = bits.firstOrNull().orEmpty()
        if (head.length in 2..3) return head
        return "en"
    }

    fun name(code: String?): String {
        val c = norm(code)
        return all.firstOrNull { it.first == c }?.second ?: c.uppercase()
    }

    fun label(code: String?, raw: String?): String {
        val one = name(code)
        val two = raw?.trim().orEmpty()
        if (two.isBlank()) return one
        if (two.equals(one, true)) return one
        if (norm(two) == norm(code)) return one
        return one + "  •  " + two
    }

    fun flag(code: String?): String = norm(code).uppercase()
}
