package dev.metinkale.halalapp.ui.customproducts

import dev.metinkale.halalapp.db.entity.CustomProductProposal
import dev.metinkale.halalapp.html.markdownComponent
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.statusBadgeComponent
import dev.metinkale.halalapp.ui.component.template
import dev.metinkale.halalapp.domain.Status
import kotlinx.html.*

fun HTML.customProductAdminPage(
    proposals: List<CustomProductProposal>,
    classificationNames: Map<String, String>,
) = template(
    "Custom-Produkte - HalalApp",
    titleBlock = {
        h2 { +"Custom-Produkte" }
        h4 { +"Vorschläge prüfen" }
    },
) {
    div(classes = "flex flex-wrap items-center justify-between gap-3") {
        p(classes = "text-sm text-gray-600") {
            +"Hier landen Produkte, die es nicht bei OpenFoodFacts gibt und die nur zusammen mit einer Produktinformation angelegt werden."
        }
        div(classes = "flex flex-wrap gap-2") {
            a(classes = "btn-secondary whitespace-nowrap") {
                href = "/crm/products_custom/manage"
                +"Bestehende Produkte"
            }
            a(classes = "btn-secondary whitespace-nowrap") {
                href = "/crm/products_custom/history"
                +"Verlauf"
            }
        }
    }

    h3(classes = "text-xl font-bold text-gray-900") { +"Offene Vorschläge" }

    card(if (proposals.isEmpty()) null else "hidden") {
        attributes["data-crm-empty"] = ""
        p(classes = "text-sm text-gray-600") { +"Keine offenen Vorschläge." }
    }

    proposals.forEach { proposal ->
        div {
            attributes["data-crm-card"] = ""
            card("flex flex-col gap-4") {
                div(classes = "flex flex-wrap items-start justify-between gap-3") {
                    div(classes = "min-w-0") {
                        h3(classes = "text-lg font-bold text-gray-900") { +proposal.name }
                        p(classes = "text-sm text-gray-500") {
                            +proposal.brand
                            +" · "
                            +proposal.barcode
                        }
                    }
                    statusBadgeComponent("Offen", Status.DOUBTFUL)
                }

                proposal.imageDataUri?.let { dataUri ->
                    img(classes = "max-h-40 w-auto rounded-lg border border-gray-200 object-contain") {
                        src = dataUri
                        alt = "Frontfoto"
                    }
                }

                div(classes = "rounded-xl border border-gray-100 bg-gray-50 p-4 text-sm text-gray-700") {
                    p(classes = "mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500") {
                        +"Produktinformation vom ${proposal.insightDate}"
                    }
                    markdownComponent(proposal.insightContent)
                }

                div(classes = "grid grid-cols-1 gap-3 md:grid-cols-2") {
                    classificationSummary("Tierisch", proposal.animal, classificationNames)
                    classificationSummary("Berauschend", proposal.alcohol, classificationNames)
                }

                if (proposal.note.isNotBlank()) {
                    p(classes = "text-sm text-gray-600") {
                        span(classes = "font-semibold") { +"Notiz: " }
                        +proposal.note
                    }
                }

                p(classes = "text-xs text-gray-500") {
                    +"Eingereicht von ${proposal.creator} am ${proposal.date}"
                }

                div(classes = "flex flex-wrap gap-2") {
                    actionForm(
                        action = "/crm/products_custom/${proposal.proposalId}/approve",
                        kind = "approve",
                        label = "Genehmigen",
                        classes = "px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 cursor-pointer disabled:opacity-60",
                        confirm = "Custom-Produkt wirklich genehmigen?",
                        success = "Custom-Produkt genehmigt.",
                    )
                    actionForm(
                        action = "/crm/products_custom/${proposal.proposalId}/reject",
                        kind = "reject",
                        label = "Ablehnen",
                        classes = "px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 cursor-pointer disabled:opacity-60",
                        confirm = "Vorschlag wirklich ablehnen?",
                        success = "Vorschlag abgelehnt.",
                    )
                }
            }
        }
    }
}

private fun FlowContent.actionForm(
    action: String,
    kind: String,
    label: String,
    classes: String,
    confirm: String,
    success: String,
) {
    form(action = action, method = FormMethod.post) {
        attributes["data-crm-action"] = kind
        attributes["data-crm-confirm"] = confirm
        attributes["data-crm-success"] = success
        input(type = InputType.submit, classes = classes) {
            value = label
        }
    }
}

private fun FlowContent.classificationSummary(
    title: String,
    ids: List<String>,
    names: Map<String, String>,
) {
    div {
        p(classes = "text-xs font-semibold uppercase tracking-wide text-gray-500") { +title }
        if (ids.isEmpty()) {
            p(classes = "text-sm text-gray-500") { +"Keine Angaben" }
        } else {
            div(classes = "mt-1 flex flex-wrap gap-1") {
                ids.forEach { id ->
                    span(classes = "inline-flex rounded-full bg-slate-100 px-2 py-1 text-xs font-medium text-slate-700") {
                        +(names[id] ?: id)
                    }
                }
            }
        }
    }
}
