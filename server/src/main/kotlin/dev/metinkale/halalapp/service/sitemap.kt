package dev.metinkale.halalapp.service

import dev.metinkale.halalapp.db.table.ProcessedProductsTable
import dev.metinkale.halalapp.db.table.ProductParentTable
import dev.metinkale.halalapp.domain.Confidence
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.notInSubQuery
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object SitemapService {
    private val manufacturerConfidences = Confidence.entries.filter { it >= Confidence.MANUFACTURER }

    fun indexableProducts(): List<String> = transaction {
        val duplicates = ProductParentTable.select(ProductParentTable.childId)
        ProcessedProductsTable
            .select(ProcessedProductsTable.id)
            .where {
                (ProcessedProductsTable.confidence inList manufacturerConfidences) and
                        ProcessedProductsTable.filter and
                        ProcessedProductsTable.id.notInSubQuery(duplicates)
            }
            .map { it[ProcessedProductsTable.id].value }
    }
}
