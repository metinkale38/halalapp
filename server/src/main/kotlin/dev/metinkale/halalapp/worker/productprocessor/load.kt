package dev.metinkale.halalapp.worker.productprocessor

import dev.metinkale.halalapp.db.entity.Product
import dev.metinkale.halalapp.db.table.ProcessedProductsTable
import dev.metinkale.halalapp.db.table.ProductParentTable
import dev.metinkale.halalapp.db.table.ProductsTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.exists
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.leftJoin
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.not
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDate
import java.time.ZoneOffset

fun ProductProcessorWorker.load(): List<Product> {
    val missingProducts = loadMissingProducts(10000)
    val oldestProducts = loadOldestProcessedProducts(10000)
    return missingProducts + oldestProducts;
}


private fun ProductProcessorWorker.loadMissingProducts(limit: Int = 1000): List<Product> {
    return (ProductsTable.leftJoin(ProcessedProductsTable) { ProductsTable.id eq ProcessedProductsTable.id })
        .selectAll()
        .where {
            ProcessedProductsTable.id.isNull() and not(
                exists(
                    ProductParentTable.selectAll().where { ProductParentTable.childId eq ProductsTable.id })
            )
        }
        .limit(limit)
        .map { mapToProduct(it) }
}

private fun ProductProcessorWorker.loadOldestProcessedProducts(limit: Int = 1000): List<Product> {
    val startOfToday = LocalDate.now().atTime(0, 0, 0, 0).toInstant(ZoneOffset.UTC);

    return ProductsTable
        .innerJoin(ProcessedProductsTable, { ProductsTable.id }, { ProcessedProductsTable.id })
        .select(ProductsTable.columns)
        .where { ProcessedProductsTable.lastUpdated less startOfToday }
        .orderBy(ProcessedProductsTable.lastUpdated to SortOrder.ASC)
        .limit(limit)
        .map { mapToProduct(it) }
}
