package dev.metinkale.halalapp.db.table

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object StatsTable : IntIdTable("stats") {
    val path = text("path")
    val period = text("period")
    val counter = integer("counter")
}