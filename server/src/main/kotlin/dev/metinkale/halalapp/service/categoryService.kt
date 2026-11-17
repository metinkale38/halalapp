package dev.metinkale.halalapp.service

import dev.metinkale.halalapp.db.entity.Category
import dev.metinkale.halalapp.db.entity.Product
import dev.metinkale.halalapp.db.table.CategoriesTable
import dev.metinkale.halalapp.db.table.ProcessedProductsTable
import dev.metinkale.halalapp.domain.Status
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.math.ceil
import java.time.Instant

data class CategoryStats(
    val halal: Int,
    val doubtful: Int,
    val haram: Int,
    val manufacturer: Int = 0,
    val products: Int = 0,
) {
    val total: Int get() = halal + doubtful + haram

    operator fun plus(other: CategoryStats) = CategoryStats(
        halal + other.halal,
        doubtful + other.doubtful,
        haram + other.haram,
        manufacturer + other.manufacturer,
        products + other.products,
    )

    companion object {
        val ZERO = CategoryStats(0, 0, 0, 0, 0)
    }
}

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
    val status: Set<Status>,
) {
    val isNavigation: Boolean get() = children.isNotEmpty()

    fun url(
        status: Set<Status> = this.status,
        page: Int = this.page,
    ): String = buildString {
        append(path)
        val params = mutableListOf<String>()
        if (page > 1) params += "page=$page"
        if (status != CategoryService.defaultStatus) {
            status.forEach { params += "status=${it.name.lowercase()}" }
        }
        if (params.isNotEmpty()) append("?").append(params.joinToString("&"))
    }
}

object CategoryService {
    val defaultStatus: Set<Status> = setOf(Status.HALAL, Status.DOUBTFUL, Status.HARAM)

    private data class StatsSnapshot(val at: Instant, val stats: Map<String, CategoryStats>)

    @Volatile
    private var statsSnapshot: StatsSnapshot? = null

    fun hub(
        id: String,
        requestedPage: Int,
        pageSize: Int,
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
                indexable = requestedPage == 1 && status == defaultStatus,
                path = path,
                status = status,
            )
        }

        val query = ProcessedProductsTable
            .selectAll()
            .where {
                (ProcessedProductsTable.categoryId eq category.id) and
                        scopeFilter() and
                        StatusAllInArray(ProcessedProductsTable.status, arrayParam(status.map { it.name }))
            }

        val totalCount = query.count().toInt()
        val totalPages = if (totalCount == 0) 1 else ceil(totalCount.toDouble() / pageSize).toInt()
        val page = requestedPage.coerceIn(1, totalPages)
        val offset = ((page - 1) * pageSize).toLong()

        val products = query
            .orderBy(
                ProcessedProductsTable.confidence to SortOrder.DESC,
                ProcessedProductsTable.name to SortOrder.ASC,
            )
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
            indexable = page == 1 && status == defaultStatus && totalCount > 0,
            path = path,
            status = status,
        )
    }

    fun statistics(): Map<String, CategoryStats> {
        val now = Instant.now()
        statsSnapshot?.takeIf { now.epochSecond - it.at.epochSecond < 60 }?.let { return it.stats }
        val fresh = loadStatistics()
        statsSnapshot = StatsSnapshot(now, fresh)
        return fresh
    }

    private fun loadStatistics(): Map<String, CategoryStats> = transaction {
        val own = HashMap<String, CategoryStats>()
        exec(
            """
            SELECT pp.category_id,
                   count(*) FILTER (WHERE 'HALAL' = ANY(pp.status)) AS halal,
                   count(*) FILTER (WHERE 'DOUBTFUL' = ANY(pp.status)) AS doubtful,
                   count(*) FILTER (WHERE 'HARAM' = ANY(pp.status)) AS haram,
                   count(*) FILTER (WHERE pp.confidence IN ('MANUFACTURER', 'VERIFIED')) AS manufacturer,
                   count(*) AS products
            FROM products_processed pp
            WHERE pp.category_id IS NOT NULL
              AND NOT ('meat' = ANY(pp.tags))
              AND NOT ('alcoholic_beverage' = ANY(pp.tags))
              AND ( ('HALAL' = ANY(pp.status)) OR NOT EXISTS (
                    SELECT 1 FROM brand_blacklist b
                    WHERE unaccent(lower(b.brand)) = unaccent(lower(pp.brand))
              ))
            GROUP BY pp.category_id
            """.trimIndent(),
            explicitStatementType = StatementType.SELECT,
        ) { rs ->
            while (rs.next()) {
                own[rs.getString(1)] = CategoryStats(
                    rs.getInt(2),
                    rs.getInt(3),
                    rs.getInt(4),
                    rs.getInt(5),
                    rs.getInt(6),
                )
            }
        }

        val all = CategoriesTable.allOrdered()
        val byParent = all.filter { it.parentId != null }.groupBy { it.parentId!! }
        val result = HashMap<String, CategoryStats>()

        fun resolve(id: String): CategoryStats {
            result[id]?.let { return it }
            val combined = (byParent[id] ?: emptyList()).fold(own[id] ?: CategoryStats.ZERO) { acc, child ->
                acc + resolve(child.id)
            }
            result[id] = combined
            return combined
        }

        all.forEach { resolve(it.id) }
        result
    }

    fun categoryPath(category: Category): String =
        category.parentId?.let { "/category/$it/${category.id}" } ?: "/category/${category.id}"

    fun topLevelWithChildren(): List<Pair<Category, List<Category>>> = transaction {
        CategoriesTable.topLevel().map { category ->
            category to CategoriesTable.children(category.id)
        }
    }

    fun indexableCategories(): List<Category> = transaction {
        CategoriesTable.allOrdered().filter { countFor(it) > 0 }
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
