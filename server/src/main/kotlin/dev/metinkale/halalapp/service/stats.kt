package dev.metinkale.halalapp.service

import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.fixedRateTimer

object StatsService {
    private val map = ConcurrentHashMap<String, Int>()

    fun increment(path: String) {
        map.merge(path, 1) { oldVal, newVal -> oldVal + newVal }

        if (map.size > 1000) flushToDatabase()
    }

    init {
        fixedRateTimer("stats-flusher", daemon = true, period = 5 * 60 * 1000) {
            if (map.isNotEmpty()) {
                flushToDatabase()
            }
        }
    }

    @Synchronized
    private fun flushToDatabase() {
        val snapshot = HashMap(map)
        if (snapshot.isEmpty()) return
        map.clear()

        val periods = LocalDate.now().let {
            listOf(
                "total",
                it.year.toString(),
                it.year.toString().padStart(2, '0') + "-" + it.monthValue.toString().padStart(2, '0'),
                it.year.toString().padStart(2, '0') + "-" + it.monthValue.toString()
                    .padStart(2, '0') + "-" + it.dayOfMonth.toString().padStart(2, '0')
            )
        }
        transaction {
            snapshot.forEach { (path, count) ->
                periods.forEach { period ->
                    exec(
                        """
                              INSERT INTO stats (path, period, counter) VALUES (?, ?, ?) ON CONFLICT (path, period) 
                              DO UPDATE SET counter = stats.counter + EXCLUDED.counter """,
                        listOf(VarCharColumnType() to path, VarCharColumnType() to period, IntegerColumnType() to count)
                    )
                }
            }
        }
    }
}