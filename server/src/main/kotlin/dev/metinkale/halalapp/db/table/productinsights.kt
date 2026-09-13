package dev.metinkale.halalapp.db.table

import dev.metinkale.halalapp.db.entity.Classification
import dev.metinkale.halalapp.db.entity.ProductInsights
import dev.metinkale.halalapp.domain.Confidence
import kotlinx.datetime.toKotlinLocalDate
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object ProductInsightsTable : CrmTable<ProductInsights>("product_insights") {
    val product_id = text("product_id")
    val content = text("content")
    val date = date("date")
    val animal = array("animal", TextColumnType())
    val alcohol = array("alcohol", TextColumnType())


    override fun map(row: ResultRow): ProductInsights {
        return ProductInsights(
            id = row[id].value,
            product_id = row[product_id],
            content = row[content],
            date = row[date].toKotlinLocalDate(),
            animal = row[animal].mapNotNull {
                ClassificationsTable.findById(it)?.copy(confidence = Confidence.MANUFACTURER)
            },
            alcohol = row[alcohol].mapNotNull {
                ClassificationsTable.findById(it)?.copy(confidence = Confidence.MANUFACTURER)
            },
        )
    }

    fun findByBarcode(barcode: String): List<ProductInsights> = transaction {
        selectAll().where { product_id eq barcode }.map { map(it) }
    }

    fun findAllByBarcode(barcodes: List<String>) = transaction {
        selectAll().where { product_id inList barcodes }.map { map(it) }
    }
}

