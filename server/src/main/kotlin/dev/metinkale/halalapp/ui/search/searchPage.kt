package dev.metinkale.halalapp.ui.search

import dev.metinkale.halalapp.domain.Confidence
import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.service.SearchResult
import dev.metinkale.halalapp.ui.component.template
import kotlinx.html.*

fun HTML.searchPage(entity: SearchResult) = template(
    "Suche nach \"${entity.query}\" - HalalApp",
    titleBlock = {
        h2 { +"Ergebnisse für: \"${entity.query}\"" }
    }) {
    script {
        unsafe {
            +"function swapUrl(url) { window.history.replaceState(null, '', url); window.location.reload(); }"
        }
    }


    div("flex wrap w-full justify-between gap-4 flex-col lg:flex-row") {

        div("grid grid-cols-2 gap-2 md:gap-4 lg:gap-2") {
            listOf(
                Confidence.MANUFACTURER to "Nur mit Produktinformationen",
                Confidence.INGREDIENTS to "Alle Produkte anzeigen"
            ).forEach { (confidence, txt) ->
                if (entity.confidence == confidence || (entity.confidence == null && confidence == Confidence.INGREDIENTS)) {
                    button(classes = "px-2 py-1.5 text-sm font-medium rounded-md bg-white shadow-sm text-slate-900") {
                        +txt
                    }
                } else {
                    button(classes = "cursor-pointer px-2 py-1.5 text-sm font-medium text-slate-500 hover:text-slate-900 transition-colors ${if (entity.confidence == null) "pointer-events-none" else ""}".trim()) {
                        onClick = "swapUrl('${entity.copy(confidence = confidence).url}')"
                        +txt
                    }
                }
            }
        }


        div("grid grid-cols-2 gap-4 sm:grid-cols-[max-content_max-content_max-content_max-content] md:gap-8 lg:gap-2") {
            Status.entries.forEach { status ->
                if (status == Status.INCOMPLETE) return@forEach
                if (entity.status?.contains(status) == true) {
                    button(classes = "cursor-pointer group flex items-center gap-2 px-2 py-1.5 rounded-full border ${status.colorTags} transition-all") {
                        onClick = "swapUrl('${entity.copy(status = entity.status.minus(status)).url}')"
                        span("w-2 h-2 rounded-full ${status.selectorColor}") {
                        }
                        span("text-sm font-medium") {
                                +status.value
                        }
                    }
                } else {
                    button(classes = "cursor-pointer flex items-center gap-2 px-2 py-1.5 rounded-full border border-slate-200 bg-white text-slate-400 hover:border-slate-300 transition-all") {
                        onClick = "swapUrl('${entity.copy(status = (entity.status ?: emptySet()).plus(status)).url}')"
                        span("text-sm font-medium") {
                            +status.value
                        }
                    }
                }
            }
        }
    }

    if (entity.query.length < 3) {
        div(classes = "bg-amber-50 border-l-4 border-amber-500 p-4 rounded-r-lg shadow-sm") {
            p(classes = "text-sm text-amber-700") { +"Suchbegriff muss mindestens 3 Zeichen lang sein." }
        }
    } else {
        if (entity.items.isEmpty()) {
            div(classes = "bg-blue-50 border-l-4 border-blue-500 p-4 rounded-r-lg shadow-sm") {
                p(classes = "text-sm text-blue-700") { +"Keine Produkte gefunden." }
            }
        } else {
            div(classes = "flex flex-col gap-6") {
                entity.items.forEach { product ->
                    productCard(product)
                }
            }

            // Einfache Pagination (Sichtbar, wenn es mehr als eine Seite gibt)
            if (entity.totalPages > 1) {
                div(classes = "flex items-center justify-between border-t border-gray-200 bg-white px-4 py-3 sm:px-6 mt-6 rounded-lg shadow-sm") {
                    // Mobile Ansicht (Kompakt)
                    div(classes = "flex flex-1 justify-between sm:hidden") {
                        if (entity.page > 1) {
                            a(
                                href = entity.copy(page = entity.page - 1).url,
                                classes = "relative inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                            ) { +"Zurück" }
                        } else {
                            div(classes = "relative inline-flex items-center rounded-md border border-gray-300 bg-gray-50 px-4 py-2 text-sm font-medium text-gray-400 cursor-not-allowed") { +"Zurück" }
                        }
                        if (entity.page < entity.totalPages) {
                            a(
                                href = entity.copy(page = entity.page + 1).url,
                                classes = "relative ml-3 inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                            ) { +"Weiter" }
                        } else {
                            div(classes = "relative ml-3 inline-flex items-center rounded-md border border-gray-300 bg-gray-50 px-4 py-2 text-sm font-medium text-gray-400 cursor-not-allowed") { +"Weiter" }
                        }
                    }

                    // Desktop Ansicht (Ausführlicher)
                    div(classes = "hidden sm:flex sm:flex-1 sm:items-center sm:justify-between") {
                        div {
                            p(classes = "text-sm text-gray-700") {
                                +"Seite "
                                span(classes = "font-medium") { +"${entity.page}" }
                                +" von "
                                span(classes = "font-medium") { +"${entity.totalPages}" }
                            }
                        }
                        div {
                            nav(classes = "isolate inline-flex -space-x-px rounded-md shadow-sm") {
                                if (entity.page > 1) {
                                    a(
                                        href = entity.copy(page = entity.page - 1).url,
                                        classes = "relative inline-flex items-center rounded-l-md px-4 py-2 text-sm font-medium text-gray-700 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus:z-20 focus:outline-offset-0"
                                    ) {
                                        +"« Zurück"
                                    }
                                }
                                if (entity.page < entity.totalPages) {
                                    a(
                                        href = entity.copy(page = entity.page + 1).url,
                                        classes =
                                            "relative inline-flex items-center rounded-r-md px-4 py-2 text-sm font-medium text-gray-700 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus:z-20 focus:outline-offset-0"
                                    ) {
                                        +"Weiter »"
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