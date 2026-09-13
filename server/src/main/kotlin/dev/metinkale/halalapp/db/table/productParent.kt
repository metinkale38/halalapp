package dev.metinkale.halalapp.db.table

import dev.metinkale.halalapp.db.entity.ProductParent
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction


object ProductParentTable : CrmTable<ProductParent>("product_parent", "child_id") {
    val parentId = text("parent_id")
    val childId = idRaw


    fun findByChildId(child: String) = transaction {
        select(parentId).where { childId eq child }.map { it[parentId] }.firstOrNull()
    }

    override fun map(row: ResultRow): ProductParent = ProductParent(
        row[childId],
        row[parentId]
    )
}

