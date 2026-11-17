package dev.metinkale.halalapp.ui.category

import dev.metinkale.halalapp.db.entity.Category
import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.service.CategoryHub
import dev.metinkale.halalapp.service.CategoryService
import dev.metinkale.halalapp.service.CategoryStats
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.template
import dev.metinkale.halalapp.ui.search.productCard
import kotlinx.html.*

private const val BASE_URL = "https://halalapp.de"

fun HTML.categoryHubPage(
    hub: CategoryHub,
    stats: Map<String, CategoryStats>,
    isAdmin: Boolean = false,
) = template(
    title = "${hub.category.name} – HalalApp",
    description = if (hub.isNavigation) {
        "Kategorien und Produkte zu ${hub.category.name} im Halal-Check."
    } else {
        "${hub.totalCount} Produkte in ${hub.category.name}: Zutatenanalyse, Status und Hinweise auf tierische oder alkoholische Bestandteile."
    },
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
        stats[hub.category.id]?.let { summary ->
            div(classes = "flex flex-col gap-0.5 text-sm text-primary-100/90") {
                div {
                    +"${summary.halal} Halal · ${summary.doubtful} Zweifelhaft · ${summary.haram} Haram"
                }
                div(classes = "text-primary-100/70") {
                    +"${summary.manufacturer} Herstellerinformationen"
                }
            }
        }
    },
) {
    if (hub.isNavigation) {
        div(classes = "flex flex-col gap-2") {
            hub.children.filter { (stats[it.id]?.products ?: 0) > 0 }.forEach { child ->
                a(classes = "block rounded-xl bg-white p-3 shadow-sm transition-colors hover:bg-gray-50 active:bg-gray-100") {
                    href = "${hub.path}/${child.id}"
                    div(classes = "flex flex-wrap items-start justify-between gap-x-4 gap-y-1") {
                        div {
                            h2(classes = "text-base font-semibold text-gray-900") { +child.name }
                            div(classes = "text-xs text-gray-500") {
                                +"${stats[child.id]?.products ?: 0} Produkte"
                            }
                        }
                        categoryStatsLine(stats[child.id])
                    }
                }
            }
        }
        return@template
    }

    script {
        unsafe {
            +"function swapUrl(url) { window.history.replaceState(null, '', url); window.location.reload(); }"
        }
    }

    div(classes = "flex flex-col gap-3") {
        div(classes = "flex flex-wrap gap-2") {
            Status.entries.forEach { status ->
                if (status == Status.INCOMPLETE) return@forEach
                val active = hub.status.contains(status)
                val target = if (active) hub.status - status else hub.status + status
                button(
                    classes = "flex min-h-[44px] cursor-pointer items-center gap-2 rounded-full border px-3 py-2 transition-all ${
                        if (active) status.colorTags else "border-slate-200 bg-white text-slate-500 hover:border-slate-300"
                    }".trim()
                ) {
                    onClick = "swapUrl('${hub.url(status = target)}')"
                    span(classes = "h-2 w-2 rounded-full ${if (active) status.selectorColor else "bg-slate-300"}") {}
                    span(classes = "text-sm font-medium") { +status.value }
                }
            }
        }

        p(classes = "text-sm text-gray-600") {
            +"${hub.totalCount} Produkte"
        }
    }

    if (hub.products.isEmpty()) {
        card {
            p(classes = "text-base text-gray-600") {
                +"Keine Produkte für diese Filter gefunden."
            }
            if (hub.status != CategoryService.defaultStatus) {
                a(classes = "mt-3 inline-block text-sm font-semibold text-indigo-600 hover:underline") {
                    href = hub.path
                    +"Filter zurücksetzen"
                }
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
                            href = hub.url(page = hub.page - 1)
                            +"« Zurück"
                        }
                    } else {
                        span(classes = "flex min-h-[48px] items-center justify-center rounded-xl bg-gray-100 px-4 py-3 text-base font-semibold text-gray-400") {
                            +"« Zurück"
                        }
                    }
                    if (hub.page < hub.totalPages) {
                        a(classes = "flex min-h-[48px] items-center justify-center rounded-xl bg-white px-4 py-3 text-base font-semibold text-gray-700 shadow-sm transition-colors hover:bg-gray-50 active:bg-gray-100") {
                            href = hub.url(page = hub.page + 1)
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
    stats: Map<String, CategoryStats>,
    isAdmin: Boolean = false,
) = template(
    title = "Kategorien – HalalApp",
    description = "Alle Kategorien im HalalApp-Halal-Check: Produkte nach Zutaten und Status durchsuchen.",
    isAdmin = isAdmin,
    titleBlock = {
        h1 { +"Kategorien" }
    },
) {
    val visibleCategories = categories.mapNotNull { (top, children) ->
        if ((stats[top.id]?.products ?: 0) == 0) null
        else top to children.filter { (stats[it.id]?.products ?: 0) > 0 }
    }

    div(classes = "flex flex-col gap-3") {
        if (visibleCategories.isEmpty()) {
            card {
                p(classes = "text-base text-gray-600") { +"Noch keine Kategorien vorhanden." }
            }
        } else {
            visibleCategories.forEach { (top, children) ->
                a(classes = "block rounded-xl bg-white p-3 shadow-sm transition-colors hover:bg-gray-50 active:bg-gray-100") {
                    href = "/category/${top.id}"
                    div(classes = "flex flex-wrap items-start justify-between gap-x-4 gap-y-1") {
                        h2(classes = "text-base font-semibold text-gray-900") { +top.name }
                        categoryStatsLine(stats[top.id])
                    }
                    if (children.isNotEmpty()) {
                        p(classes = "mt-1 text-sm text-gray-500") {
                            +children.joinToString(", ") { it.name }
                        }
                    } else {
                        p(classes = "mt-1 text-sm text-gray-500") {
                            +"\u00A0"
                        }
                    }
                }
            }
        }
    }
}

private fun FlowContent.categoryStatsLine(stats: CategoryStats?) {
    if (stats == null || stats.products == 0) {
        span(classes = "text-xs text-gray-400") { +"Keine Produkte" }
        return
    }
    div(classes = "flex flex-col gap-0.5 text-xs text-gray-500") {
        div {
            span(classes = "font-medium text-emerald-600") { +"${stats.halal} Halal" }
            +" / "
            span(classes = "font-medium text-amber-600") { +"${stats.doubtful} Zweifelhaft" }
            +" / "
            span(classes = "font-medium text-red-600") { +"${stats.haram} Haram" }
        }
        div(classes = "text-right text-gray-400") {
            +"${stats.manufacturer} Herstellerinformationen"
        }
    }
}
