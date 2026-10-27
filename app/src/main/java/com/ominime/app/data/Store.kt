package com.ominime.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

object store {
    private val slot = stringPreferencesKey("data")
    private var ds: DataStore<Preferences>? = null
    private val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val flow = MutableStateFlow(Data())
    val ready = MutableStateFlow(false)

    val live = flow
    val conf: Conf get() = flow.value.conf
    val list: List<Rec> get() = flow.value.list
    val hist: List<Rec> get() = flow.value.hist

    fun init(c: Context) {
        val d = PreferenceDataStoreFactory.create(produceFile = { File(c.filesDir, "store.preferences_pb") })
        ds = d
        sc.launch {
            d.data.collect { p ->
                val raw = p[slot]
                if (raw != null) {
                    runCatching { net.jso.decodeFromString(Data.serializer(), raw) }
                        .onSuccess { if (it != flow.value) flow.value = it }
                }
                ready.value = true
            }
        }
    }

    private fun put(d: Data) {
        flow.value = d
        val dd = ds ?: return
        sc.launch { runCatching { dd.edit { it[slot] = net.jso.encodeToString(Data.serializer(), d) } } }
    }

    fun conf(c: Conf) = put(flow.value.copy(conf = c))

    fun save(r: Rec) {
        val d = flow.value
        val rest = d.hist.filterNot { it.id == r.id }
        val hist = (listOf(r) + rest).take(48)
        put(d.copy(hist = hist))
    }

    fun drop(id: Int) {
        val d = flow.value
        put(d.copy(hist = d.hist.filterNot { it.id == id }))
    }

    fun add(r: Rec) {
        val d = flow.value
        if (d.list.any { it.id == r.id }) return
        put(d.copy(list = listOf(r.copy(seen = System.currentTimeMillis())) + d.list))
    }

    fun remove(id: Int) {
        val d = flow.value
        put(d.copy(list = d.list.filterNot { it.id == id }))
    }

    fun status(id: Int): String = flow.value.list.firstOrNull { it.id == id }?.stat.orEmpty()

    fun mark(id: Int, stat: String) {
        val d = flow.value
        val hit = d.list.firstOrNull { it.id == id }
        if (hit == null) return
        put(d.copy(list = d.list.map { if (it.id == id) it.copy(stat = stat) else it }))
    }

    fun bulk(rows: List<Rec>) {
        val d = flow.value
        val have = d.list.map { it.id }.toSet()
        put(d.copy(list = d.list + rows.filter { it.id !in have }))
    }

    fun notes(): List<Note> = flow.value.notes

    fun unread(): Int = flow.value.notes.count { !it.read }

    fun read() {
        val d = flow.value
        if (d.notes.none { !it.read }) return
        put(d.copy(notes = d.notes.map { if (it.read) it else it.copy(read = true) }))
    }

    fun note(n: Note) {
        val d = flow.value
        if (d.notes.any { it.id == n.id && it.ep == n.ep }) return
        put(d.copy(notes = (listOf(n) + d.notes).take(40)))
    }

    fun wipeNotes() {
        val d = flow.value
        put(d.copy(notes = emptyList()))
    }

    fun track(id: Int, aired: Int) {
        val d = flow.value
        val hit = d.list.firstOrNull { it.id == id } ?: return
        if (hit.aired == aired && hit.seen > 0) return
        val now = System.currentTimeMillis()
        put(
            d.copy(
                list = d.list.map {
                    if (it.id == id) it.copy(aired = aired, seen = if (it.seen > 0) it.seen else now) else it
                }
            )
        )
    }

    fun has(id: Int): Boolean = flow.value.list.any { it.id == id }

    fun find(id: Int): Rec? = flow.value.hist.firstOrNull { it.id == id }

    fun tag(id: Int): Rec? = flow.value.list.firstOrNull { it.id == id }

    fun wipe() = put(Data(conf = flow.value.conf))

    fun export(): String {
        val body = list.joinToString(",") { r ->
            val pairs = listOf(
                "id" to r.id.toString(),
                "title" to r.title,
                "cover" to (r.cover ?: ""),
                "status" to r.stat.ifBlank { "PLANNING" },
                "episode" to r.ep.toString(),
                "total" to (r.total ?: 0).toString()
            )
            pairs.joinToString(",", "{", "}") { pair ->
                "\"" + pair.first + "\":\"" + pair.second.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
            }
        }
        return "{\"app\":\"Ominime\",\"version\":1,\"list\":[" + body + "]}"
    }

    fun decode(raw: String): List<Rec> {
        val root = runCatching { net.jso.parseToJsonElement(raw.trim()) }.getOrNull() ?: return emptyList()
        val arr = when {
            root is JsonArray -> root
            root is JsonObject -> (root["list"] as? JsonArray ?: (root["entries"] as? JsonArray ?: return emptyList()))
            else -> return emptyList()
        }
        return arr.mapNotNull { one ->
            val o = one as? JsonObject ?: return@mapNotNull null
            fun txt(vararg keys: String): String = keys.firstNotNullOfOrNull { o[it]?.jsonPrimitive?.contentOrNull } ?: ""
            val id = txt("id", "mediaId").toIntOrNull() ?: 0
            val title = txt("title", "name")
            if (id == 0 && title.isBlank()) return@mapNotNull null
            Rec(
                id = id,
                title = title,
                cover = txt("cover", "image").ifBlank { null },
                ep = txt("episode", "progress").toIntOrNull() ?: 0,
                total = txt("total", "episodes").toIntOrNull()?.takeIf { it > 0 },
                stat = txt("status").ifBlank { "PLANNING" }
            )
        }.filter { it.id > 0 }
    }
}
