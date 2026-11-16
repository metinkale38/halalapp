package dev.metinkale.halalapp.db.table

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object ProductsOffTable : ProductsTable("products_off") {
    fun exists(id: String): Boolean = transaction {
        selectAll().where { ProductsOffTable.id eq id }.limit(1).any()
    }
}
