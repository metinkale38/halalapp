package dev.metinkale.halalapp.ui.category

import dev.metinkale.halalapp.db.entity.Category
import dev.metinkale.halalapp.domain.Confidence
import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.service.CategoryHub
import dev.metinkale.halalapp.service.CategoryService
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.template
import dev.metinkale.halalapp.ui.search.productCard
import kotlinx.html.*

private const val BASE_URL = "https://halalapp.de"

fun HTML.categoryHubPage(hub: CategoryHub, isAdmin: Boolean = false) = template(
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
    },
) {
    if (hub.isNavigation) {
        div(classes = "grid grid-cols-1 gap-2 sm:grid-cols-2") {
            hub.children.forEach { child ->
                a(classes = "flex min-h-[52px] items-center justify-between gap-3 rounded-xl bg-white px-4 py-3 text-base font-medium text-gray-800 shadow-sm transition-colors hover:bg-gray-50 active:bg-gray-100") {
                    href = "${hub.path}/${child.id}"
                    span { +child.name }
                    span(classes = "text-gray-400") { +"→" }
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
        div(classes = "grid grid-cols-2 gap-2") {
            listOf(
                Confidence.MANUFACTURER to "Mit Produktinformationen",
                Confidence.INGREDIENTS to "Alle Produkte",
            ).forEach { (confidence, text) ->
                val active = hub.confidence == confidence ||
                        (hub.confidence == null && confidence == Confidence.INGREDIENTS)
                if (active) {
                    button(classes = "min-h-[44px] rounded-xl bg-white px-3 py-2 text-sm font-semibold text-slate-900 shadow-sm") {
                        +text
                    }
                } else {
                    button(
                        classes = "min-h-[44px] rounded-xl bg-gray-100 px-3 py-2 text-sm font-medium text-slate-600 transition-colors hover:bg-white active:bg-gray-200 ${
                            if (hub.confidence == null) "pointer-events-none opacity-50" else ""
                        }".trim()
                    ) {
                        onClick = "swapUrl('${hub.url(confidence = confidence)}')"
                        +text
                    }
                }
            }
        }

        div(classes = "flex flex-wrap gap-2") {
            Status.entries.forEach { status ->
                if (status == Status.INCOMPLETE || status == Status.UNKNOWN) return@forEach
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
            if (hub.status != CategoryService.defaultStatus || hub.confidence == Confidence.INGREDIENTS) {
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
    isAdmin: Boolean = false,
) = template(
    title = "Kategorien – HalalApp",
    description = "Alle Kategorien im HalalApp-Halal-Check: Produkte nach Zutaten und Status durchsuchen.",
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
                val hasChildren = children.isNotEmpty()
                div(classes = "overflow-hidden rounded-xl bg-white shadow-sm") {
                    a(
                        classes = if (hasChildren)
                            "block px-4 pt-4 pb-1 text-base font-semibold text-gray-900 transition-colors hover:text-indigo-700"
                        else
                            "flex min-h-[56px] items-center justify-between px-4 py-4 text-base font-semibold text-gray-900 transition-colors hover:text-indigo-700"
                    ) {
                        href = "/category/${top.id}"
                        span { +top.name }
                        if (!hasChildren) span(classes = "text-gray-400") { +"→" }
                    }
                    if (hasChildren) {
                        div(classes = "grid grid-cols-1 gap-1 p-2 sm:grid-cols-2") {
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
