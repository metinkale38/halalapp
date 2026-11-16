package dev.metinkale.halalapp.ui.customproducts

import dev.metinkale.halalapp.db.entity.Proposal
import dev.metinkale.halalapp.domain.RequestStatus
import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.statusBadgeComponent
import dev.metinkale.halalapp.ui.component.template
import kotlinx.html.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val labels = mapOf(
    "id" to "Barcode",
    "name" to "Produktname",
    "brand" to "Marke",
)

fun HTML.customProductHistoryPage(history: List<Proposal>) = template(
    "Verlauf Custom-Produkte - CRM",
    titleBlock = {
        h2 { +"Verlauf" }
        h4 { +"Custom-Produkte" }
    },
) {
    div(classes = "flex flex-wrap gap-2 border-b border-gray-200 pb-2") {
        a(classes = "rounded-lg px-3 py-2 text-sm font-semibold text-gray-600 hover:bg-gray-100") {
            href = "/crm/products_custom"
            +"Vorschläge"
        }
        a(classes = "rounded-lg px-3 py-2 text-sm font-semibold text-gray-600 hover:bg-gray-100") {
            href = "/crm/products_custom/manage"
            +"Bestehende Produkte"
        }
        a(classes = "rounded-lg bg-primary px-3 py-2 text-sm font-semibold text-white") {
            href = "/crm/products_custom/history"
            +"Verlauf"
        }
    }

    if (history.isEmpty()) {
        card {
            p(classes = "text-sm text-gray-600") { +"Noch keine Änderungen vorhanden." }
        }
    }

    history.forEach { proposal ->
        card("flex flex-col gap-3") {
            div(classes = "flex flex-wrap items-start justify-between gap-2") {
                div {
                    p(classes = "text-sm font-semibold text-gray-900") {
                        +DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                            .withZone(ZoneId.systemDefault())
                            .format(proposal.date)
                        +" · "
                        +proposal.creator
                    }
                    if (proposal.note.isNotBlank()) {
                        p(classes = "text-sm text-gray-600") { +proposal.note }
                    }
                }
                statusBadgeComponent(
                    if (proposal.status == RequestStatus.APPROVED) "Genehmigt" else "Abgelehnt",
                    if (proposal.status == RequestStatus.APPROVED) Status.HALAL else Status.HARAM,
                )
            }

            div(classes = "rounded-xl border border-gray-100 bg-gray-50 p-4") {
                val old = proposal.oldJson
                val new = proposal.json
                (old?.keys.orEmpty() + new.keys).forEach { key ->
                    val before = old?.get(key)
                    val after = new[key]
                    if (before != after) {
                        div(classes = "grid grid-cols-1 gap-1 border-b border-gray-200 py-2 text-sm last:border-b-0 sm:grid-cols-[10rem_1fr]") {
                            span(classes = "font-semibold text-gray-600") { +(labels[key] ?: key) }
                            div(classes = "min-w-0 break-words text-gray-900") {
                                if (before != null && before.isNotBlank()) {
                                    span(classes = "mr-2 text-gray-400 line-through") { +before }
                                    +"→ "
                                }
                                +(after.orEmpty().ifBlank { "–" })
                            }
                        }
                    }
                }
            }
        }
    }
}
