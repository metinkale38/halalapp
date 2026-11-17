package dev.metinkale.halalapp.routes

import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.server.FeatureFlags
import dev.metinkale.halalapp.server.isAdmin
import dev.metinkale.halalapp.service.CategoryService
import dev.metinkale.halalapp.ui.category.categoriesPage
import dev.metinkale.halalapp.ui.category.categoryHubPage
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.queryString
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kotlin.text.uppercase

private const val BASE_URL = "https://halalapp.de"
private const val PAGE_SIZE = 24

fun Routing.categoryRoutes() {
    get("/categories") {
        call.requireCategoriesVisible()
        val stats = CategoryService.statistics()
        call.respondHtml { categoriesPage(CategoryService.topLevelWithChildren(), stats, call.isAdmin()) }
    }

    get("/category/{parent}/{child}") {
        call.requireCategoriesVisible()
        val parentId = call.parameters["parent"] ?: throw NotFoundException("Kategorie nicht gefunden")
        val childId = call.parameters["child"] ?: throw NotFoundException("Kategorie nicht gefunden")
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val hub = CategoryService.hub(
            id = childId,
            requestedPage = page,
            pageSize = PAGE_SIZE,
            status = call.categoryStatus(),
        ) ?: throw NotFoundException("Kategorie nicht gefunden")
        if (hub.category.parentId != parentId) throw NotFoundException("Kategorie nicht gefunden")
        val stats = CategoryService.statistics()
        call.respondHtml { categoryHubPage(hub, stats, call.isAdmin()) }
    }

    get("/category/{id}") {
        call.requireCategoriesVisible()
        val id = call.parameters["id"] ?: throw NotFoundException("Kategorie nicht gefunden")
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val hub = CategoryService.hub(
            id = id,
            requestedPage = page,
            pageSize = PAGE_SIZE,
            status = call.categoryStatus(),
        ) ?: throw NotFoundException("Kategorie nicht gefunden")
        val parentId = hub.category.parentId
        if (parentId != null) {
            val query = call.request.queryString()
            val target = "/category/$parentId/${hub.category.id}" + if (query.isBlank()) "" else "?$query"
            call.respondRedirect(target, permanent = true)
        } else {
            val stats = CategoryService.statistics()
            call.respondHtml { categoryHubPage(hub, stats, call.isAdmin()) }
        }
    }

    if (FeatureFlags.CATEGORIES_VISIBLE_FOR_USERS) {
        get("/sitemap.xml") {
            val urls = CategoryService.indexableCategories().joinToString("\n") {
                "  <url><loc>$BASE_URL${CategoryService.categoryPath(it)}</loc></url>"
            }
            val xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                $urls
                </urlset>
            """.trimIndent()
            call.respondText(xml, ContentType.Application.Xml)
        }
    }
}

private fun ApplicationCall.requireCategoriesVisible() {
    if (!FeatureFlags.CATEGORIES_VISIBLE_FOR_USERS && !isAdmin()) {
        throw NotFoundException("Kategorie nicht gefunden")
    }
}

private fun ApplicationCall.categoryStatus(): Set<Status> =
    request.queryParameters.getAll("status")
        ?.takeIf { it.isNotEmpty() }
        ?.mapNotNull { runCatching { Status.valueOf(it.uppercase()) }.getOrNull() }
        ?.toSet()
        ?: CategoryService.defaultStatus
