package dev.metinkale.halalapp.ui.customproducts

import dev.metinkale.halalapp.db.entity.CustomProduct
import dev.metinkale.halalapp.db.entity.CustomProductProposal
import dev.metinkale.halalapp.html.markdownComponent
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.statusBadgeComponent
import dev.metinkale.halalapp.ui.component.template
import dev.metinkale.halalapp.domain.Status
import kotlinx.html.*

fun HTML.customProductAdminPage(
    proposals: List<CustomProductProposal>,
    products: List<CustomProduct>,
    classificationNames: Map<String, String>,
) = template(
    "Custom-Produkte - HalalApp",
    titleBlock = {
        h2 { +"Custom-Produkte" }
        h4 { +"Vorschläge prüfen und bestehende Produkte bearbeiten" }
    },
) {
    div(classes = "flex flex-wrap items-center justify-between gap-3") {
        p(classes = "text-sm text-gray-600") {
            +"Hier landen Produkte, die es nicht bei OpenFoodFacts gibt und die nur zusammen mit einer Produktinformation angelegt werden."
        }
        a(classes = "btn-primary") {
            href = "/crm/products_custom/new"
            +"Neues Custom-Produkt"
        }
    }

    h3(classes = "text-xl font-bold text-gray-900") { +"Offene Vorschläge" }

    if (proposals.isEmpty()) {
        card {
            p(classes = "text-sm text-gray-600") { +"Keine offenen Vorschläge." }
        }
    }

    proposals.forEach { proposal ->
        card("flex flex-col gap-4") {
            div(classes = "flex flex-wrap items-start justify-between gap-3") {
                div {
                    h3(classes = "text-lg font-bold text-gray-900") {
                        +proposal.name
                    }
                    p(classes = "text-sm text-gray-500") {
                        +proposal.brand
                        +" · "
                        +proposal.barcode
                        if (proposal.category != null) {
                            +" · "
                            +proposal.category
                        }
                    }
                }
                statusBadgeComponent("Offen", Status.DOUBTFUL)
            }

            if (proposal.imagePreviews.isNotEmpty()) {
                div(classes = "flex flex-wrap gap-3") {
                    proposal.imagePreviews.forEach { preview ->
                        val file = preview.files.minByOrNull { it.second }
                        if (file != null) {
                            a {
                                href = "/crm/image/${file.first}"
                                target = "_blank"
                                img(classes = "max-h-40 w-auto rounded-lg border border-gray-200 object-contain") {
                                    src = "/crm/image/${file.first}"
                                    alt = preview.image.kind.name
                                }
                            }
                        }
                    }
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
                form(
                    action = "/crm/products_custom/${proposal.proposalId}/approve",
                    method = FormMethod.post,
                ) {
                    input(
                        type = InputType.submit,
                        classes = "px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 cursor-pointer",
                    ) {
                        value = "Genehmigen"
                        attributes["onclick"] = "return confirm('Custom-Produkt wirklich genehmigen?');"
                    }
                }
                form(
                    action = "/crm/products_custom/${proposal.proposalId}/reject",
                    method = FormMethod.post,
                ) {
                    input(
                        type = InputType.submit,
                        classes = "px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 cursor-pointer",
                    ) {
                        value = "Ablehnen"
                        attributes["onclick"] = "return confirm('Vorschlag wirklich ablehnen?');"
                    }
                }
            }
        }
    }

    h3(classes = "mt-4 text-xl font-bold text-gray-900") { +"Bestehende Custom-Produkte" }

    if (products.isEmpty()) {
        card {
            p(classes = "text-sm text-gray-600") { +"Noch keine Custom-Produkte vorhanden." }
        }
    } else {
        div(classes = "grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3") {
            products.forEach { product ->
                card("flex flex-row gap-3 items-center !p-3") {
                    img(classes = "h-16 w-16 rounded-lg object-cover bg-gray-100 flex-shrink-0") {
                        src = product.images.getFrontPhoto(100)
                        alt = product.name
                    }
                    div(classes = "min-w-0 grow") {
                        p(classes = "text-xs font-semibold uppercase tracking-wide text-indigo-600 truncate") {
                            +product.brand
                        }
                        p(classes = "text-sm font-semibold text-gray-900 truncate") { +product.name }
                        p(classes = "text-xs text-gray-500 truncate") {
                            +product.id
                            if (product.category != null) {
                                +" · "
                                +product.category
                            }
                        }
                    }
                    div(classes = "flex flex-col gap-1") {
                        a(classes = "text-sm font-semibold text-indigo-600 hover:underline") {
                            href = "/crm/products_custom/${product.id}/edit"
                            +"Bearbeiten"
                        }
                        a(classes = "text-xs text-gray-500 hover:underline") {
                            href = "/product/${product.id}"
                            +"Ansehen"
                        }
                    }
                }
            }
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
