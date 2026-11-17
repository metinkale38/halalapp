package dev.metinkale.halalapp.ui.insights

import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.service.InsightsPage
import dev.metinkale.halalapp.service.ProductInsightsService
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.template
import dev.metinkale.halalapp.ui.search.productCard
import kotlinx.html.*

fun HTML.productInsightsPage(data: InsightsPage, isAdmin: Boolean = false) = template(
    title = "Neue Produktinformationen – HalalApp",
    description = "Zuletzt hinzugefügte Herstellerinformationen zu Produkten aus dem Supermarkt.",
    isAdmin = isAdmin,
    titleBlock = {
        h1 { +"Neue Produktinformationen" }
    },
) {
    script {
        unsafe {
            +"function swapUrl(url) { window.history.replaceState(null, '', url); window.location.reload(); }"
        }
    }

    form(action = "/insights", method = FormMethod.get, classes = "flex gap-3") {
        data.status.sortedBy { it.ordinal }.forEach { status ->
            input(type = InputType.hidden, name = "status") {
                value = status.name.lowercase()
            }
        }
        input(
            type = InputType.text,
            name = "q",
            classes = "w-full px-4 py-3.5 rounded-xl border border-gray-200 text-base focus:outline-none focus:ring-2 focus:ring-primary500 focus:border-transparent transition"
        ) {
            placeholder = "Produkt oder Marke suchen..."
            value = data.query
        }
        button(type = ButtonType.submit, classes = "btn-primary") {
            +"Suchen"
        }
    }

    div(classes = "flex flex-wrap gap-2") {
        Status.entries.forEach { status ->
            if (status == Status.INCOMPLETE) return@forEach
            val active = data.status.contains(status)
            val target = if (active) data.status - status else data.status + status
            button(
                classes = "flex min-h-[44px] cursor-pointer items-center gap-2 rounded-full border px-3 py-2 transition-all ${
                    if (active) status.colorTags else "border-slate-200 bg-white text-slate-500 hover:border-slate-300"
                }".trim()
            ) {
                onClick = "swapUrl('${data.url(status = target)}')"
                span(classes = "h-2 w-2 rounded-full ${if (active) status.selectorColor else "bg-slate-300"}") {}
                span(classes = "text-sm font-medium") { +status.value }
            }
        }
    }

    p(classes = "text-sm text-gray-600") {
        +"${data.totalCount} Produkte"
    }

    if (data.items.isEmpty()) {
        card {
            p(classes = "text-base text-gray-600") {
                if (data.query.isNotBlank() && data.query.length < 3) {
                    +"Suchbegriff muss mindestens 3 Zeichen lang sein."
                } else {
                    +"Keine Produktinformationen für diese Filter gefunden."
                }
            }
            if (data.status != ProductInsightsService.defaultStatus || data.query.isNotBlank()) {
                a(classes = "mt-3 inline-block text-sm font-semibold text-indigo-600 hover:underline") {
                    href = "/insights"
                    +"Filter zurücksetzen"
                }
            }
        }
    } else {
        div(classes = "flex flex-col gap-4") {
            data.items.forEach { item ->
                productCard(item.product, item.date)
            }
        }

        if (data.totalPages > 1) {
            div(classes = "mt-6 flex flex-col gap-2") {
                div(classes = "grid grid-cols-2 gap-2") {
                    if (data.page > 1) {
                        a(classes = "flex min-h-[48px] items-center justify-center rounded-xl bg-white px-4 py-3 text-base font-semibold text-gray-700 shadow-sm transition-colors hover:bg-gray-50 active:bg-gray-100") {
                            href = data.url(page = data.page - 1)
                            +"« Zurück"
                        }
                    } else {
                        span(classes = "flex min-h-[48px] items-center justify-center rounded-xl bg-gray-100 px-4 py-3 text-base font-semibold text-gray-400") {
                            +"« Zurück"
                        }
                    }
                    if (data.page < data.totalPages) {
                        a(classes = "flex min-h-[48px] items-center justify-center rounded-xl bg-white px-4 py-3 text-base font-semibold text-gray-700 shadow-sm transition-colors hover:bg-gray-50 active:bg-gray-100") {
                            href = data.url(page = data.page + 1)
                            +"Weiter »"
                        }
                    } else {
                        span(classes = "flex min-h-[48px] items-center justify-center rounded-xl bg-gray-100 px-4 py-3 text-base font-semibold text-gray-400") {
                            +"Weiter »"
                        }
                    }
                }
                p(classes = "text-center text-sm text-gray-600") {
                    +"Seite ${data.page} von ${data.totalPages}"
                }
            }
        }
    }
}
