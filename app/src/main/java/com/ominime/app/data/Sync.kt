package com.ominime.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object sync {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var last = ""

    fun now(id: Int, ep: Int, state: String? = null) {
        val tok = store.conf.token
        if (tok.isBlank() || id <= 0) return
        if (ep < 1 && state.isNullOrBlank()) return
        val key = id.toString() + ":" + ep + ":" + state
        if (key == last) return
        last = key
        scope.launch {
            runCatching { push(tok, id, ep, state) }
        }
    }
}
