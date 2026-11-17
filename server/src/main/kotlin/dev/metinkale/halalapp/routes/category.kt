package dev.metinkale.halalapp.routes

import dev.metinkale.halalapp.server.isAdmin
import dev.metinkale.halalapp.service.CategoryService
import dev.metinkale.halalapp.ui.category.categoriesPage
import dev.metinkale.halalapp.ui.category.categoryHubPage
import io.ktor.http.ContentType
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

private const val BASE_URL = "https://halalapp.de"

fun Routing.categoryRoutes() {
    get("/categories") {
        call.respondHtml { categoriesPage(CategoryService.topLevelWithChildren(), call.isAdmin()) }
    }

    get("/category/{id}") {
        val id = call.parameters["id"] ?: throw NotFoundException("Kategorie nicht gefunden")
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val hub = CategoryService.hub(id, page, 24)
            ?: throw NotFoundException("Kategorie nicht gefunden")
        call.respondHtml { categoryHubPage(hub, call.isAdmin()) }
    }

    get("/sitemap.xml") {
        val urls = CategoryService.indexableCategories().joinToString("\n") {
            "  <url><loc>$BASE_URL/category/${it.id}</loc></url>"
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
