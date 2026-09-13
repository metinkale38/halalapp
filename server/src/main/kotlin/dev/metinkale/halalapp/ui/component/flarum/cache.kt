package dev.metinkale.halalapp.ui.component.flarum

import java.util.concurrent.ConcurrentHashMap

// Cache-Container
object FlarumCache {
    private val cache = ConcurrentHashMap<String, Pair<Long, FlarumResponse>>()
    private const val TTL = 600_000L // 10 Minuten in ms

    fun get(key: String): FlarumResponse? {
        val entry = cache[key] ?: return null
        return if (System.currentTimeMillis() - entry.first < TTL) entry.second else null
    }

    fun put(key: String, value: FlarumResponse) {
        cache[key] = Pair(System.currentTimeMillis(), value)
    }
}