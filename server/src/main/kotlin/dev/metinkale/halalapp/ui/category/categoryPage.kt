package dev.metinkale.halalapp.ui.category

import dev.metinkale.halalapp.db.entity.Category
import dev.metinkale.halalapp.html.markdownComponent
import dev.metinkale.halalapp.service.CategoryHub
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.template
import dev.metinkale.halalapp.ui.search.productCard
import kotlinx.html.*

private const val BASE_URL = "https://halalapp.de"

fun HTML.categoryHubPage(hub: CategoryHub, isAdmin: Boolean = false) = template(
    title = "${hub.category.name} – HalalApp",
    isAdmin = isAdmin,
    head = {
        meta {
            name = "robots"
            content = if (hub.indexable) "index, follow" else "noindex, follow"
        }
        link {
            rel = "canonical"
            href = "$BASE_URL${hub.path}"
        }
    },
    titleBlock = {
        hub.parent?.let { parent ->
            a(classes = "inline-flex min-h-10 items-center text-base text-primary-100 underline-offset-2 hover:underline") {
                href = "/category/${parent.id}"
                +"← ${parent.name}"
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
        div(classes = "grid grid-cols-1 gap-2 sm:grid-cols-2") {
            hub.children.forEach { child ->
                a(classes = "flex min-h-[52px] items-center justify-between gap-3 rounded-xl bg-white px-4 py-3 text-base font-medium text-gray-800 shadow-sm transition-colors hover:bg-gray-50 active:bg-gray-100") {
                    href = "${hub.path}/${child.id}"
                    span { +child.name }
                    span(classes = "text-gray-400") { +"→" }
                }
            }
        }
    }

    if (hub.products.isEmpty()) {
        card {
            p(classes = "text-base text-gray-600") {
                +"In dieser Kategorie sind noch keine Produkte vorhanden."
            }
        }
    } else {
        div(classes = "flex flex-col gap-4") {
            hub.products.forEach { productCard(it) }
        }

        if (hub.totalPages > 1) {
            div(classes = "mt-6 flex flex-col gap-2") {
                div(classes = "grid grid-cols-2 gap-2") {
                    if (hub.page > 1) {
                        a(classes = "flex min-h-[48px] items-center justify-center rounded-xl bg-white px-4 py-3 text-base font-semibold text-gray-700 shadow-sm transition-colors hover:bg-gray-50 active:bg-gray-100") {
                            href = "${hub.path}?page=${hub.page - 1}"
                            +"« Zurück"
                        }
                    } else {
                        span(classes = "flex min-h-[48px] items-center justify-center rounded-xl bg-gray-100 px-4 py-3 text-base font-semibold text-gray-400") {
                            +"« Zurück"
                        }
                    }
                    if (hub.page < hub.totalPages) {
                        a(classes = "flex min-h-[48px] items-center justify-center rounded-xl bg-white px-4 py-3 text-base font-semibold text-gray-700 shadow-sm transition-colors hover:bg-gray-50 active:bg-gray-100") {
                            href = "${hub.path}?page=${hub.page + 1}"
                            +"Weiter »"
                        }
                    } else {
                        span(classes = "flex min-h-[48px] items-center justify-center rounded-xl bg-gray-100 px-4 py-3 text-base font-semibold text-gray-400") {
                            +"Weiter »"
                        }
                    }
                }
                p(classes = "text-center text-sm text-gray-600") {
                    +"Seite ${hub.page} von ${hub.totalPages}"
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
    div(classes = "flex flex-col gap-3") {
        h1(classes = "text-lg font-bold text-gray-900") { +"Kategorien" }
        if (categories.isEmpty()) {
            card {
                p(classes = "text-base text-gray-600") { +"Noch keine Kategorien vorhanden." }
            }
        } else {
            categories.forEach { (top, children) ->
                div(classes = "overflow-hidden rounded-xl bg-white shadow-sm") {
                    a(classes = "flex min-h-[56px] items-center justify-between gap-3 px-4 py-4 text-base font-semibold text-indigo-700 transition-colors hover:bg-gray-50 active:bg-gray-100") {
                        href = "/category/${top.id}"
                        span { +top.name }
                        span(classes = "text-gray-400") { +"→" }
                    }
                    if (children.isNotEmpty()) {
                        div(classes = "grid grid-cols-1 gap-1 border-t border-gray-100 p-2 sm:grid-cols-2") {
                            children.forEach { child ->
                                a(classes = "flex min-h-[46px] items-center rounded-lg px-3 py-2 text-base text-gray-700 transition-colors hover:bg-gray-50 active:bg-gray-100") {
                                    href = "/category/${top.id}/${child.id}"
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
