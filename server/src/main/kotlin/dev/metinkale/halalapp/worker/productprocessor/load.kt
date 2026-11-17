package dev.metinkale.halalapp.worker.productprocessor

import dev.metinkale.halalapp.db.entity.ProductComponent
import dev.metinkale.halalapp.db.entity.ProductImages
import dev.metinkale.halalapp.db.table.ProcessedProductsTable
import dev.metinkale.halalapp.db.table.ProductParentTable
import dev.metinkale.halalapp.db.table.ProductsCustomTable
import dev.metinkale.halalapp.db.table.ProductsOffTable
import dev.metinkale.halalapp.db.table.REPROCESS_PRIORITY_THRESHOLD
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.exists
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.leftJoin
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.not
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDate
import java.time.ZoneOffset

enum class ProductSource {
    OFF,
    CUSTOM,
}

data class SourceProduct(
    val id: String,
    val name: String,
    val brand: String,
    val ingredients: List<ProductComponent>,
    val ingredientsText: String,
    val allergens: List<String>,
    val traces: List<String>,
    val images: ProductImages,
    val tags: List<String>,
    val categoriesHierarchy: List<String>,
    val customCategoryId: String?,
    val source: ProductSource,
)

fun ProductProcessorWorker.load(): List<SourceProduct> {
    val prioritizedProducts = loadPrioritizedProducts(10000)
    val missingProducts = loadMissingProducts(10000)
    val oldestProducts = loadOldestProcessedProducts(10000)
    return prioritizedProducts + missingProducts + oldestProducts
}

private fun loadPrioritizedProducts(limit: Int = 1000): List<SourceProduct> {
    val off = ProductsOffTable
        .innerJoin(ProcessedProductsTable, { ProductsOffTable.id }, { ProcessedProductsTable.id })
        .select(ProductsOffTable.columns)
        .where { ProcessedProductsTable.lastUpdated less REPROCESS_PRIORITY_THRESHOLD }
        .orderBy(ProcessedProductsTable.lastUpdated to SortOrder.ASC)
        .limit(limit)
        .map { mapOff(it) }

    val custom = ProductsCustomTable
        .innerJoin(ProcessedProductsTable, { ProductsCustomTable.id }, { ProcessedProductsTable.id })
        .select(ProductsCustomTable.columns)
        .where { ProcessedProductsTable.lastUpdated less REPROCESS_PRIORITY_THRESHOLD }
        .orderBy(ProcessedProductsTable.lastUpdated to SortOrder.ASC)
        .limit(limit)
        .map { mapCustom(it) }

    return mergeCustomFirst(off, custom)
}

private fun loadMissingProducts(limit: Int = 1000): List<SourceProduct> {
    val off = (ProductsOffTable.leftJoin(ProcessedProductsTable) { ProductsOffTable.id eq ProcessedProductsTable.id })
        .selectAll()
        .where {
            ProcessedProductsTable.id.isNull() and not(
                exists(
                    ProductParentTable.selectAll().where { ProductParentTable.childId eq ProductsOffTable.id })
            )
        }
        .limit(limit)
        .map { mapOff(it) }

    val custom = (ProductsCustomTable.leftJoin(ProcessedProductsTable) { ProductsCustomTable.id eq ProcessedProductsTable.id })
        .selectAll()
        .where {
            ProcessedProductsTable.id.isNull() and not(
                exists(
                    ProductParentTable.selectAll().where { ProductParentTable.childId eq ProductsCustomTable.id })
            )
        }
        .limit(limit)
        .map { mapCustom(it) }

    return mergeCustomFirst(off, custom)
}

private fun loadOldestProcessedProducts(limit: Int = 1000): List<SourceProduct> {
    val startOfToday = LocalDate.now().atTime(0, 0, 0, 0).toInstant(ZoneOffset.UTC)

    val off = ProductsOffTable
        .innerJoin(ProcessedProductsTable, { ProductsOffTable.id }, { ProcessedProductsTable.id })
        .select(ProductsOffTable.columns)
        .where {
            (ProcessedProductsTable.lastUpdated greaterEq REPROCESS_PRIORITY_THRESHOLD) and
                    (ProcessedProductsTable.lastUpdated less startOfToday)
        }
        .orderBy(ProcessedProductsTable.lastUpdated to SortOrder.ASC)
        .limit(limit)
        .map { mapOff(it) }

    val custom = ProductsCustomTable
        .innerJoin(ProcessedProductsTable, { ProductsCustomTable.id }, { ProcessedProductsTable.id })
        .select(ProductsCustomTable.columns)
        .where {
            (ProcessedProductsTable.lastUpdated greaterEq REPROCESS_PRIORITY_THRESHOLD) and
                    (ProcessedProductsTable.lastUpdated less startOfToday)
        }
        .orderBy(ProcessedProductsTable.lastUpdated to SortOrder.ASC)
        .limit(limit)
        .map { mapCustom(it) }

    return mergeCustomFirst(off, custom)
}

fun loadSource(id: String): SourceProduct? {
    val custom = ProductsCustomTable.selectAll()
        .where { ProductsCustomTable.id eq id }
        .singleOrNull()
        ?.let { mapCustom(it) }
    if (custom != null) return custom
    return ProductsOffTable.selectAll()
        .where { ProductsOffTable.id eq id }
        .singleOrNull()
        ?.let { mapOff(it) }
}

private fun mergeCustomFirst(off: List<SourceProduct>, custom: List<SourceProduct>): List<SourceProduct> {
    val customIds = ProductsCustomTable.select(ProductsCustomTable.id)
        .map { it[ProductsCustomTable.id].value }
        .toSet()
    return custom + off.filter { it.id !in customIds }
}

private fun mapOff(row: org.jetbrains.exposed.v1.core.ResultRow) = SourceProduct(
    id = row[ProductsOffTable.id].value,
    name = row[ProductsOffTable.name],
    brand = row[ProductsOffTable.brand],
    ingredients = row[ProductsOffTable.ingredients],
    ingredientsText = row[ProductsOffTable.ingredientsText],
    allergens = row[ProductsOffTable.allergens],
    traces = row[ProductsOffTable.traces],
    images = row[ProductsOffTable.images],
    tags = row[ProductsOffTable.tags],
    categoriesHierarchy = row[ProductsOffTable.categoriesHierarchy],
    customCategoryId = null,
    source = ProductSource.OFF,
)

private fun mapCustom(row: org.jetbrains.exposed.v1.core.ResultRow) = SourceProduct(
    id = row[ProductsCustomTable.id].value,
    name = row[ProductsCustomTable.name],
    brand = row[ProductsCustomTable.brand],
    ingredients = row[ProductsCustomTable.ingredients],
    ingredientsText = row[ProductsCustomTable.ingredientsText],
    allergens = row[ProductsCustomTable.allergens],
    traces = row[ProductsCustomTable.traces],
    images = row[ProductsCustomTable.images],
    tags = row[ProductsCustomTable.tags],
    categoriesHierarchy = emptyList(),
    customCategoryId = row[ProductsCustomTable.categoryId],
    source = ProductSource.CUSTOM,
)
