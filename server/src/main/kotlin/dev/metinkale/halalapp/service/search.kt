package dev.metinkale.halalapp.service

import dev.metinkale.halalapp.db.entity.Product
import dev.metinkale.halalapp.db.table.ProcessedProductsTable
import dev.metinkale.halalapp.domain.Confidence
import dev.metinkale.halalapp.domain.Status
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.math.ceil

object SearchService {
    fun search(searchQuery: SearchQuery): SearchResult = transaction {
        if (searchQuery.query.length < 3)
            return@transaction SearchResult(
                searchQuery.query,
                emptyList(),
                0,
                searchQuery.page,
                pageSize = 0,
                confidence = searchQuery.confidence,
                status = searchQuery.status
            )

        if (isProductBarcode(searchQuery.query)) {
            ProcessedProductsTable.findById(searchQuery.query)?.let {
                return@transaction SearchResult(
                    query = searchQuery.query,
                    items = listOf(it),
                    totalCount = 1,
                    page = searchQuery.page,
                    pageSize = 1,
                    confidence = searchQuery.confidence,
                    status = searchQuery.status
                )
            }
        }

        val offset = ((searchQuery.page - 1) * searchQuery.pageSize).toLong()


        val allowedConfidence = when (searchQuery.confidence) {
            Confidence.INGREDIENTS -> listOf(Confidence.INGREDIENTS, Confidence.MANUFACTURER, Confidence.VERIFIED)
            Confidence.MANUFACTURER -> listOf(Confidence.MANUFACTURER, Confidence.VERIFIED)
            Confidence.VERIFIED -> listOf(Confidence.VERIFIED)
        }
        val allowedStatus = searchQuery.status

        val searchFilter = SearchFilter(searchQuery.query)
        val confidenceFilter =
            (ProcessedProductsTable.confidence eq anyFrom(arrayParam(allowedConfidence.map { it.name })))
        val statusFilter = AllInArray(ProcessedProductsTable.status, arrayParam(allowedStatus.map { it.name }))
        val brandFilter = ProcessedProductsTable.filter

        val query = ProcessedProductsTable
            .selectAll()
            .where(searchFilter and confidenceFilter and statusFilter and brandFilter)

        val totalCount = query.count().toInt()

        if (totalCount == 0 && searchQuery.confidence == Confidence.MANUFACTURER)
            return@transaction search(searchQuery.copy(confidence = Confidence.INGREDIENTS)).copy(confidence = null)

        val items = query
            .offset(offset)
            .limit(searchQuery.pageSize)
            .map { ProcessedProductsTable.map(it) }

        SearchResult(
            query = searchQuery.query,
            items = items,
            totalCount = totalCount,
            page = searchQuery.page,
            pageSize = searchQuery.pageSize,
            confidence = searchQuery.confidence,
            status = searchQuery.status
        )
    }

    private fun isProductBarcode(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.all { it.isDigit() } && trimmed.length in setOf(8, 12, 13)
    }


}

private class AllInArray(expr1: Expression<*>, expr2: Expression<*>) : ComparisonOp(expr1, expr2, "<@")


@Serializable
data class SearchQuery(
    val query: String = "",
    val page: Int = 1,
    val pageSize: Int = 20,
    val confidence: Confidence,
    val status: Set<Status>,
)

@Serializable
data class SearchResult(
    val query: String,
    val items: List<Product>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int = if (totalCount == 0) 0 else ceil(totalCount.toDouble() / pageSize).toInt(),
    val confidence: Confidence?,
    val status: Set<Status>?,
) {
    val url
        get() = buildString {
            append("?q=")
            append(query)
            if (page > 1) append("&page=").append(page)
            if (confidence != null && confidence != Confidence.MANUFACTURER) append("&confidence=").append(confidence.name.lowercase())
            if (status != null && status != setOf(Status.HALAL, Status.DOUBTFUL, Status.UNKNOWN, Status.HARAM))
                status.forEach {
                    append("&status=").append(it.name.lowercase())
                }
        }

}

class SearchFilter(val query: String) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("search_vector @@ plainto_tsquery('german',")
        queryBuilder.registerArgument(ProcessedProductsTable.name, query)
        queryBuilder.append(")")
    }
}

