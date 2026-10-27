package com.ominime.app.data

object aniskip {
    data class Cut(val kind: String, val start: Double, val end: Double)

    suspend fun cuts(mal: Int, ep: Int): List<Cut> {
        if (mal <= 0) return emptyList()
        val raw = runCatching {
            net.text(
                "https://api.aniskip.com/v2/skip-times/$mal/$ep?types%5B%5D=op&types%5B%5D=ed&episodeLength=0",
                ttl = 12 * 3600_000L
            )
        }.getOrNull() ?: return emptyList()
        val o = net.obj(raw)
        if (o.boo("found") != true) return emptyList()
        return o.arr("results")?.mapNotNull { it.node()?.let { n ->
            val iv = n.obj("interval") ?: return@let null
            Cut(
                kind = n.st("skipType") ?: "",
                start = (iv.flt("startTime") ?: 0f).toDouble(),
                end = (iv.flt("endTime") ?: 0f).toDouble()
            )
        } } ?: emptyList()
    }
}
