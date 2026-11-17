package dev.metinkale.halalapp.service

import dev.metinkale.halalapp.db.entity.Product
import dev.metinkale.halalapp.db.table.ProcessedProductsTable
import dev.metinkale.halalapp.db.table.ProductInsightsTable
import dev.metinkale.halalapp.domain.Status
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toKotlinLocalDate
import org.jetbrains.exposed.v1.core.ComparisonOp
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.arrayParam
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.math.ceil

data class InsightProduct(
    val product: Product,
    val date: LocalDate,
)

data class InsightsPage(
    val items: List<InsightProduct>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val status: Set<Status>,
) {
    val totalPages: Int = if (totalCount == 0) 0 else ceil(totalCount.toDouble() / pageSize).toInt()

    fun url(
        page: Int = this.page,
        status: Set<Status> = this.status,
    ): String = buildString {
        append("/insights")
        val params = mutableListOf<String>()
        if (page > 1) params += "page=$page"
        if (status != ProductInsightsService.defaultStatus) {
            status.sortedBy { it.ordinal }.forEach { params += "status=${it.name.lowercase()}" }
        }
        if (params.isNotEmpty()) append("?").append(params.joinToString("&"))
    }
}

object ProductInsightsService {
    val defaultStatus: Set<Status> = setOf(Status.HALAL, Status.DOUBTFUL, Status.UNKNOWN, Status.HARAM)

    fun latestDates(productIds: List<String>): Map<String, LocalDate> = transaction {
        if (productIds.isEmpty()) return@transaction emptyMap()
        val maxDate = ProductInsightsTable.date.max()
        ProductInsightsTable
            .select(ProductInsightsTable.product_id, maxDate)
            .where { ProductInsightsTable.product_id inList productIds }
            .groupBy(ProductInsightsTable.product_id)
            .associate { row ->
                row[ProductInsightsTable.product_id] to row[maxDate]!!.toKotlinLocalDate()
            }
    }

    fun preview(limit: Int = 8): List<InsightProduct> = transaction {
        val (query, latestDate) = insightsQuery(defaultStatus)
        query
            .orderBy(latestDate to SortOrder.DESC, ProcessedProductsTable.name to SortOrder.ASC)
            .limit(limit)
            .map { map(it, latestDate) }
    }

    fun page(
        requestedPage: Int,
        pageSize: Int,
        status: Set<Status> = defaultStatus,
    ): InsightsPage = transaction {
        val (query, latestDate) = insightsQuery(status)
        val totalCount = query.count().toInt()
        val totalPages = if (totalCount == 0) 0 else ceil(totalCount.toDouble() / pageSize).toInt()
        val page = requestedPage.coerceIn(1, totalPages.coerceAtLeast(1))
        val offset = ((page - 1) * pageSize).toLong()

        val items = query
            .orderBy(latestDate to SortOrder.DESC, ProcessedProductsTable.name to SortOrder.ASC)
            .limit(pageSize)
            .offset(offset)
            .map { map(it, latestDate) }

        InsightsPage(
            items = items,
            totalCount = totalCount,
            page = page,
            pageSize = pageSize,
            status = status,
        )
    }

    private fun insightsQuery(
        status: Set<Status>,
    ): Pair<Query, ExpressionWithColumnType<java.time.LocalDate?>> {
        val maxDate = ProductInsightsTable.date.max()
        val latestInsights = ProductInsightsTable
            .select(ProductInsightsTable.product_id, maxDate.alias("latest_date"))
            .groupBy(ProductInsightsTable.product_id)
            .alias("latest_insights")

        val latestDate = latestInsights[maxDate]
        val latestProductId = latestInsights[ProductInsightsTable.product_id]

        val filters = buildList<Op<Boolean>> {
            add(ProcessedProductsTable.filter)
            add(InsightsStatusAllInArray(ProcessedProductsTable.status, arrayParam(status.map { it.name })))
        }

        val fields: List<Expression<*>> = ProcessedProductsTable.columns
        val query = ProcessedProductsTable
            .innerJoin(latestInsights, { ProcessedProductsTable.id }, { latestProductId })
            .select(fields + latestDate)
            .where { filters.reduce { acc, filter -> acc and filter } }

        return query to latestDate
    }

    private fun map(row: ResultRow, latestDate: Expression<java.time.LocalDate?>): InsightProduct =
        InsightProduct(
            product = ProcessedProductsTable.map(row),
            date = row[latestDate]!!.toKotlinLocalDate(),
        )
}

private class InsightsStatusAllInArray(expr1: Expression<*>, expr2: Expression<*>) : ComparisonOp(expr1, expr2, "<@")
