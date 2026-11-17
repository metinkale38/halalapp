package dev.metinkale.halalapp.routes

import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.server.isAdmin
import dev.metinkale.halalapp.service.ProductInsightsService
import dev.metinkale.halalapp.ui.insights.productInsightsPage
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

private const val INSIGHTS_PAGE_SIZE = 20

fun Routing.productInsightsRoutes() {
    get("/insights") {
        val query = call.request.queryParameters["q"]?.trim().orEmpty()
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val status = call.request.queryParameters.getAll("status")
            ?.takeIf { it.isNotEmpty() }
            ?.mapNotNull { runCatching { Status.valueOf(it.uppercase()) }.getOrNull() }
            ?.toSet()
            ?: ProductInsightsService.defaultStatus

        val data = ProductInsightsService.page(query, page, INSIGHTS_PAGE_SIZE, status)
        call.respondHtml { productInsightsPage(data, call.isAdmin()) }
    }
}
