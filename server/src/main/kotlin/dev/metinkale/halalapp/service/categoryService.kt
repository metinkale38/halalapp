package dev.metinkale.halalapp.service

import dev.metinkale.halalapp.db.entity.Category
import dev.metinkale.halalapp.db.entity.Product
import dev.metinkale.halalapp.db.table.CategoriesTable
import dev.metinkale.halalapp.db.table.ProcessedProductsTable
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
)

object CategoryService {
    fun hub(id: String, requestedPage: Int, pageSize: Int): CategoryHub? = transaction {
        val category = CategoriesTable.findById(id) ?: return@transaction null
        val parent = category.parentId?.let { CategoriesTable.findById(it) }
        val children = CategoriesTable.children(id)
        val ids = listOf(category.id) + children.map { it.id }

        val query = ProcessedProductsTable
            .selectAll()
            .where { scopeFilter() and (ProcessedProductsTable.categoryId inList ids) }

        val totalCount = query.count().toInt()
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
            indexable = totalCount > 0 && category.description.isNotBlank(),
            path = categoryPath(category),
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
            category.description.isNotBlank() && countFor(category) > 0
        }
    }

    private fun countFor(category: Category): Int = transaction {
        val ids = listOf(category.id) + CategoriesTable.children(category.id).map { it.id }
        ProcessedProductsTable
            .selectAll()
            .where { scopeFilter() and (ProcessedProductsTable.categoryId inList ids) }
            .count()
            .toInt()
    }

    fun scopeFilter(): Op<Boolean> =
        not(stringLiteral("INCOMPLETE") eq anyFrom(ProcessedProductsTable.status)) and
                not(stringLiteral("UNKNOWN") eq anyFrom(ProcessedProductsTable.status)) and
                not(stringLiteral("meat") eq anyFrom(ProcessedProductsTable.tags)) and
                not(stringLiteral("alcoholic_beverage") eq anyFrom(ProcessedProductsTable.tags))
}
