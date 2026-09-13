package dev.metinkale.halalapp.worker.productprocessor

import dev.metinkale.halalapp.db.entity.Product
import dev.metinkale.halalapp.db.table.ProcessedProductsTable
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import java.time.Instant

fun ProductProcessorWorker.persist(products: List<Product>) {
    val now = Instant.now()

    ProcessedProductsTable.deleteWhere { ProcessedProductsTable.id inList products.map { it.id } }

    ProcessedProductsTable.batchInsert(products) {
        this[ProcessedProductsTable.id] = it.id
        this[ProcessedProductsTable.brand] = it.brand
        this[ProcessedProductsTable.name] = it.name
        this[ProcessedProductsTable.ingredientsText] = it.ingredientsText
        this[ProcessedProductsTable.traces] = it.traces.map { it.id }
        this[ProcessedProductsTable.allergens] = it.allergens.map { it.id }
        this[ProcessedProductsTable.animalIngredients] = it.animalIngredients
        this[ProcessedProductsTable.alcoholIngredients] = it.alcoholIngredients
        this[ProcessedProductsTable.unknownIngredients] = it.unknownIngredients
        this[ProcessedProductsTable.images] = it.images
        this[ProcessedProductsTable.tags] = it.tags
        this[ProcessedProductsTable.status] = it.status.map { it.name }
        this[ProcessedProductsTable.confidence] = it.confidence
        this[ProcessedProductsTable.lastUpdated] = now

    }

}
