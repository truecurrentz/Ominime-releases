package com.ominime.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Media(
    val id: Int = 0,
    val mal: Int? = null,
    val title: String = "",
    val romaji: String = "",
    val native: String? = null,
    val cover: String? = null,
    val banner: String? = null,
    val desc: String? = null,
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val score: Int? = null,
    val total: Int? = null,
    val rank: Int? = null,
    val alts: List<String> = emptyList(),
    val dur: Int? = null,
    val state: String? = null,
    val year: Int? = null,
    val season: String? = null,
    val fmt: String? = null,
    val studio: String? = null,
    val color: String? = null,
    val pop: Int? = null,
    val next: Next? = null,
    val trailer: String? = null,
    val links: List<Link> = emptyList(),
    val rel: List<Rel> = emptyList(),
    val recs: List<Rel> = emptyList(),
    val cast: List<Cast> = emptyList(),
    val shots: List<Shot> = emptyList()
)

@Serializable
data class Next(
    val ep: Int = 0,
    val at: Long = 0
)

@Serializable
data class Rel(
    val id: Int = 0,
    val title: String = "",
    val cover: String? = null,
    val fmt: String? = null,
    val kind: String? = null,
    val score: Int? = null
)

@Serializable
data class Cast(
    val name: String = "",
    val img: String? = null,
    val va: String? = null
)

@Serializable
data class Shot(
    val title: String = "",
    val img: String? = null
)

@Serializable
data class Link(
    val site: String = "",
    val url: String = ""
)

@Serializable
data class Ep(
    val num: Int = 0,
    val title: String? = null,
    val desc: String? = null,
    val img: String? = null,
    val air: String? = null,
    val filler: Boolean = false,
    val sub: Boolean = false,
    val dub: Boolean = false
)

@Serializable
data class Src(
    val name: String = "",
    val subs: List<Ep> = emptyList(),
    val dubs: List<Ep> = emptyList()
)

@Serializable
data class Subs(
    val url: String = "",
    val label: String = "",
    val lang: String = "",
    val fmt: String = "vtt",
    val sel: Boolean = false,
    val ref: String = ""
)

@Serializable
data class Stream(
    val url: String = "",
    val kind: String = "hls",
    val server: String = "",
    val subs: List<Subs> = emptyList(),
    val prio: Int = 0,
    val ref: String = ""
)

data class Slot(
    val at: Long = 0,
    val ep: Int = 0,
    val m: Media = Media()
)

data class Find(
    val gens: Set<String> = emptySet(),
    val fmts: Set<String> = emptySet(),
    val stats: Set<String> = emptySet(),
    val season: String = "",
    val year: Int = 0,
    val score: Int = 0,
    val sort: String = "POPULARITY_DESC"
)

fun Find.any(): Boolean = gens.isNotEmpty() || fmts.isNotEmpty() || stats.isNotEmpty() ||
    season.isNotBlank() || year > 0 || score > 0

@Serializable
data class Rec(
    val id: Int = 0,
    val title: String = "",
    val cover: String? = null,
    val ep: Int = 0,
    val pos: Long = 0,
    val dur: Long = 0,
    val at: Long = 0,
    val total: Int? = null,
    val mal: Int? = null,
    val stat: String = "",
    val aired: Int = 0,
    val seen: Long = 0
)

@Serializable
data class Note(
    val id: Int = 0,
    val title: String = "",
    val cover: String? = null,
    val ep: Int = 0,
    val at: Long = 0,
    val read: Boolean = false
)

@Serializable
data class Conf(
    val dark: Int = 1,
    val audio: String = "sub",
    val auto: Boolean = true,
    val skip: Boolean = true,
    val romaji: Boolean = false,
    val hd: Boolean = true,
    val intro: Boolean = true,
    val skipauto: Boolean = false,
    val alerts: Boolean = true,
    val rate: Float = 1f,
    val sub: String = "en",
    val subsize: Int = 100,
    val subbold: Boolean = true,
    val subcolor: String = "white",
    val subedge: Int = 1,
    val subpos: Int = 0,
    val subbox: Boolean = false,
    val subfont: String = "sans",
    val subital: Boolean = false,
    val subwide: Boolean = false,
    val acc: String = "orange"
)

@Serializable
data class Data(
    val conf: Conf = Conf(),
    val list: List<Rec> = emptyList(),
    val hist: List<Rec> = emptyList(),
    val notes: List<Note> = emptyList()
)

class Fail(val code: Int, val note: String) : Exception(note)
