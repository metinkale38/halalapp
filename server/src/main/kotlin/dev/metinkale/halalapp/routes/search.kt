package dev.metinkale.halalapp.routes

import dev.metinkale.halalapp.domain.Confidence
import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.service.SearchQuery
import dev.metinkale.halalapp.service.SearchService
import dev.metinkale.halalapp.ui.search.searchPage
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kotlin.text.trim
import kotlin.text.uppercase

fun Routing.searchRoutes() {
    get("/search") {
        val query = call.request.queryParameters["q"]?.trim()
            ?: throw IllegalArgumentException("Query parameter 'q' is required")
        val page = call.request.queryParameters["page"]?.trim()?.toIntOrNull() ?: 1


        val confidence =
            call.request.queryParameters["confidence"]?.let { Confidence.valueOf(it.uppercase()) }
        val status =
            call.request.queryParameters.getAll("status")?.takeIf { it.isNotEmpty() }
                ?.map { Status.valueOf(it.uppercase()) }?.toSet()

        val search = SearchService.search(
            SearchQuery(
                query,
                page,
                confidence = confidence ?: Confidence.MANUFACTURER,
                status = status ?: setOf(Status.HALAL, Status.DOUBTFUL, Status.UNKNOWN, Status.HARAM)
            )
        )
        call.respondHtml { searchPage(search) }

    }

}