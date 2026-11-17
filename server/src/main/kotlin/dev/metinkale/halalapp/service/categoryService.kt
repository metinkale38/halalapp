package dev.metinkale.halalapp.service

import dev.metinkale.halalapp.db.entity.Category
import dev.metinkale.halalapp.db.entity.Product
import dev.metinkale.halalapp.db.table.CategoriesTable
import dev.metinkale.halalapp.db.table.ProcessedProductsTable
import dev.metinkale.halalapp.domain.Confidence
import dev.metinkale.halalapp.domain.Status
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.math.ceil

data class CategoryHub(
    val category: Category,
    val parent: Category?,
    val children: List<Category>,
    val products: List<Product>,
    val totalCount: Int,
    val page: Int,
    val totalPages: Int,
    val indexable: Boolean,
    val path: String,
    val confidence: Confidence?,
    val status: Set<Status>,
) {
    val isNavigation: Boolean get() = children.isNotEmpty()

    fun url(
        confidence: Confidence? = this.confidence,
        status: Set<Status> = this.status,
        page: Int = this.page,
    ): String = buildString {
        append(path)
        val params = mutableListOf<String>()
        if (page > 1) params += "page=$page"
        if (confidence != null && confidence != Confidence.MANUFACTURER) {
            params += "confidence=${confidence.name.lowercase()}"
        }
        if (status != CategoryService.defaultStatus) {
            status.forEach { params += "status=${it.name.lowercase()}" }
        }
        if (params.isNotEmpty()) append("?").append(params.joinToString("&"))
    }
}

object CategoryService {
    val defaultStatus: Set<Status> = setOf(Status.HALAL, Status.DOUBTFUL, Status.HARAM)

    fun hub(
        id: String,
        requestedPage: Int,
        pageSize: Int,
        confidence: Confidence = Confidence.MANUFACTURER,
        status: Set<Status> = defaultStatus,
    ): CategoryHub? = transaction {
        val category = CategoriesTable.findById(id) ?: return@transaction null
        val parent = category.parentId?.let { CategoriesTable.findById(it) }
        val children = CategoriesTable.children(id)
        val path = categoryPath(category)

        if (children.isNotEmpty()) {
            return@transaction CategoryHub(
                category = category,
                parent = parent,
                children = children,
                products = emptyList(),
                totalCount = 0,
                page = 1,
                totalPages = 1,
                indexable = requestedPage == 1 && status == defaultStatus && confidence != Confidence.INGREDIENTS,
                path = path,
                confidence = confidence,
                status = status,
            )
        }

        fun buildQuery(queryConfidence: Confidence) = ProcessedProductsTable
            .selectAll()
            .where {
                val allowedConfidence = when (queryConfidence) {
                    Confidence.INGREDIENTS -> listOf(Confidence.INGREDIENTS, Confidence.MANUFACTURER, Confidence.VERIFIED)
                    Confidence.MANUFACTURER -> listOf(Confidence.MANUFACTURER, Confidence.VERIFIED)
                    Confidence.VERIFIED -> listOf(Confidence.VERIFIED)
                }
                (ProcessedProductsTable.categoryId eq category.id) and
                        scopeFilter() and
                        (ProcessedProductsTable.confidence eq anyFrom(arrayParam(allowedConfidence.map { it.name }))) and
                        StatusAllInArray(ProcessedProductsTable.status, arrayParam(status.map { it.name }))
            }

        var effectiveConfidence: Confidence? = confidence
        var query = buildQuery(confidence)
        var totalCount = query.count().toInt()

        if (totalCount == 0 && confidence == Confidence.MANUFACTURER) {
            effectiveConfidence = null
            query = buildQuery(Confidence.INGREDIENTS)
            totalCount = query.count().toInt()
        }

        val totalPages = if (totalCount == 0) 1 else ceil(totalCount.toDouble() / pageSize).toInt()
        val page = requestedPage.coerceIn(1, totalPages)
        val offset = ((page - 1) * pageSize).toLong()

        val products = query
            .orderBy(ProcessedProductsTable.name to SortOrder.ASC)
            .limit(pageSize)
            .offset(offset)
            .map { ProcessedProductsTable.map(it) }

        CategoryHub(
            category = category,
            parent = parent,
            children = children,
            products = products,
            totalCount = totalCount,
            page = page,
            totalPages = totalPages,
            indexable = page == 1 && status == defaultStatus &&
                    effectiveConfidence != Confidence.INGREDIENTS && totalCount > 0,
            path = path,
            confidence = effectiveConfidence,
            status = status,
        )
    }

    fun categoryPath(category: Category): String =
        category.parentId?.let { "/category/$it/${category.id}" } ?: "/category/${category.id}"

    fun topLevelWithChildren(): List<Pair<Category, List<Category>>> = transaction {
        CategoriesTable.topLevel().map { category ->
            category to CategoriesTable.children(category.id)
        }
    }

    fun indexableCategories(): List<Category> = transaction {
        CategoriesTable.allOrdered().filter { category ->
            CategoriesTable.children(category.id).isNotEmpty() || countFor(category) > 0
        }
    }

    private fun countFor(category: Category): Int = transaction {
        ProcessedProductsTable
            .selectAll()
            .where {
                (ProcessedProductsTable.categoryId eq category.id) and
                        scopeFilter() and
                        StatusAllInArray(ProcessedProductsTable.status, arrayParam(defaultStatus.map { it.name }))
            }
            .count()
            .toInt()
    }

    private fun scopeFilter(): Op<Boolean> =
        ProcessedProductsTable.filter and
                not(stringLiteral("meat") eq anyFrom(ProcessedProductsTable.tags)) and
                not(stringLiteral("alcoholic_beverage") eq anyFrom(ProcessedProductsTable.tags))
}

private class StatusAllInArray(expr1: Expression<*>, expr2: Expression<*>) : ComparisonOp(expr1, expr2, "<@")
