package dev.metinkale.halalapp.ui.category

import dev.metinkale.halalapp.db.entity.Category
import dev.metinkale.halalapp.html.markdownComponent
import dev.metinkale.halalapp.service.CategoryHub
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.template
import dev.metinkale.halalapp.ui.search.productCard
import kotlinx.html.*

fun HTML.categoryHubPage(hub: CategoryHub, isAdmin: Boolean = false) = template(
    title = "${hub.category.name} – HalalApp",
    isAdmin = isAdmin,
    head = {
        meta {
            name = "robots"
            content = if (hub.indexable) "index, follow" else "noindex, follow"
        }
    },
    titleBlock = {
        hub.parent?.let { parent ->
            a(classes = "text-sm text-primary-200 hover:underline") {
                href = "/kategorie/${parent.id}"
                +parent.name
            }
        }
        h1 { +hub.category.name }
    },
) {
    if (hub.category.description.isNotBlank()) {
        card {
            markdownComponent(hub.category.description)
        }
    }

    if (hub.children.isNotEmpty()) {
        div(classes = "flex flex-wrap gap-2") {
            hub.children.forEach { child ->
                a(classes = "rounded-full bg-white px-3 py-1.5 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50") {
                    href = "/kategorie/${child.id}"
                    +child.name
                }
            }
        }
    }

    if (hub.products.isEmpty()) {
        card {
            p(classes = "text-sm text-gray-600") {
                +"In dieser Kategorie sind noch keine Produkte vorhanden."
            }
        }
    } else {
        div(classes = "flex flex-col gap-6") {
            hub.products.forEach { productCard(it) }
        }

        if (hub.totalPages > 1) {
            div(classes = "flex items-center justify-between border-t border-gray-200 bg-white px-4 py-3 mt-6 rounded-lg shadow-sm") {
                if (hub.page > 1) {
                    a(classes = "text-sm font-medium text-gray-700 hover:bg-gray-50 rounded-md px-3 py-2") {
                        href = "/kategorie/${hub.category.id}?page=${hub.page - 1}"
                        +"« Zurück"
                    }
                } else {
                    span(classes = "text-sm text-gray-400 px-3 py-2") { +"« Zurück" }
                }
                span(classes = "text-sm text-gray-700") {
                    +"Seite ${hub.page} von ${hub.totalPages}"
                }
                if (hub.page < hub.totalPages) {
                    a(classes = "text-sm font-medium text-gray-700 hover:bg-gray-50 rounded-md px-3 py-2") {
                        href = "/kategorie/${hub.category.id}?page=${hub.page + 1}"
                        +"Weiter »"
                    }
                } else {
                    span(classes = "text-sm text-gray-400 px-3 py-2") { +"Weiter »" }
                }
            }
        }
    }
}

fun HTML.categoriesPage(
    categories: List<Pair<Category, List<Category>>>,
    isAdmin: Boolean = false,
) = template(
    title = "Kategorien – HalalApp",
    isAdmin = isAdmin,
) {
    card {
        h1(classes = "text-lg font-bold text-gray-900 mb-3") { +"Kategorien" }
        if (categories.isEmpty()) {
            p(classes = "text-sm text-gray-600") { +"Noch keine Kategorien vorhanden." }
        } else {
            div(classes = "flex flex-col gap-4") {
                categories.forEach { (top, children) ->
                    div {
                        a(classes = "text-base font-semibold text-indigo-600 hover:underline") {
                            href = "/kategorie/${top.id}"
                            +top.name
                        }
                        if (children.isNotEmpty()) {
                            div(classes = "mt-1 flex flex-wrap gap-x-4 gap-y-1 text-sm text-gray-600") {
                                children.forEach { child ->
                                    a(classes = "hover:text-indigo-600 hover:underline") {
                                        href = "/kategorie/${child.id}"
                                        +child.name
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
