package dev.metinkale.halalapp.ui.productchanges

import dev.metinkale.halalapp.db.entity.Proposal
import dev.metinkale.halalapp.domain.RequestStatus
import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.html.formGroup
import dev.metinkale.halalapp.service.CriticalIngredientView
import dev.metinkale.halalapp.service.ManagedProductOverride
import dev.metinkale.halalapp.service.PRODUCT_MASTERDATA_ENTITY
import dev.metinkale.halalapp.service.ProductChangeProposalView
import dev.metinkale.halalapp.service.ProductChangeType
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.loadPhotoNormalizer
import dev.metinkale.halalapp.ui.component.photoField
import dev.metinkale.halalapp.ui.component.statusBadgeComponent
import dev.metinkale.halalapp.ui.component.template
import kotlinx.html.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault())

fun HTML.productMasterDataFormPage(
    productId: String,
    currentName: String,
    currentBrand: String,
    currentImageUrl: String?,
    proposedName: String,
    proposedBrand: String,
    note: String = "",
    error: String? = null,
) = template(
    "Stammdaten korrigieren - HalalApp",
    head = { loadPhotoNormalizer() },
    titleBlock = {
        h2 { +"Stammdaten korrigieren" }
        h4 { +(currentBrand.ifBlank { "Unbekannte Marke" } + " - " + currentName) }
    },
) {
    card("m-auto w-full") {
        error?.let { showError(it) }
        p(classes = "mb-4 text-sm text-gray-600") {
            +"Änderungen werden als Vorschlag eingereicht und nach Prüfung dauerhaft übernommen."
        }
        form(
            action = "/crm/product_masterdata/new",
            method = FormMethod.post,
            encType = FormEncType.multipartFormData,
        ) {
            formGroup {
                staticField("Barcode", "id", productId)
                inputField("Produktname", "name", proposedName)
                inputField("Marke", "brand", proposedBrand)
                photoField(
                    label = "Frontfoto (optional)",
                    name = "front",
                    previewId = "front-preview",
                    existingUrl = currentImageUrl,
                )
                textAreaField("Quelle / Begründung (optional)", "note", note) {
                    rows = "4"
                    placeholder = "z. B. Herstellerangabe oder Link zur Quelle"
                }
                submitButton("Vorschlag senden")
            }
        }
    }
}

fun HTML.productIngredientIgnoreFormPage(
    productId: String,
    productName: String,
    ingredients: List<CriticalIngredientView>,
    selected: Set<String>,
    reason: String = "",
    note: String = "",
    error: String? = null,
) = template(
    "Fehlerhafte Zutat melden - HalalApp",
    titleBlock = {
        h2 { +"Fehlerhafte Zutat melden" }
        h4 { +productName }
    },
) {
    card("m-auto w-full") {
        error?.let { showError(it) }
        p(classes = "mb-4 text-sm text-gray-600") {
            +"Wähle Zutaten aus, die für dieses Produkt offensichtlich falsch aus OpenFoodFacts übernommen wurden. "
            +"Sie werden nur für dieses Produkt aus der Bewertung entfernt."
        }
        form(action = "/crm/product_ingredient_ignore/new", method = FormMethod.post) {
            formGroup {
                input(type = InputType.hidden) {
                    name = "id"
                    value = productId
                }
                div(classes = "flex flex-col gap-2") {
                    ingredients.forEach { ingredient ->
                        label(classes = "flex cursor-pointer items-start gap-3 rounded-lg border border-gray-200 p-3") {
                            input(
                                type = InputType.checkBox,
                                classes = "mt-1 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500",
                            ) {
                                name = "ingredients"
                                value = ingredient.id
                                checked = ingredient.id in selected
                            }
                            div(classes = "min-w-0") {
                                div(classes = "text-sm font-semibold text-gray-900") { +ingredient.name }
                                div(classes = "text-xs text-gray-500") {
                                    +"${ingredient.classificationName} · ${ingredient.status.value} · ${ingredient.source} · "
                                    +ingredient.id
                                }
                            }
                        }
                    }
                }
                textAreaField("Begründung (Pflicht)", "reason", reason) {
                    rows = "3"
                    required = true
                    placeholder = "Warum ist diese Zutat für dieses Produkt falsch?"
                }
                textAreaField("Quelle / Notiz (optional)", "note", note) {
                    rows = "3"
                }
                submitButton("Vorschlag senden")
            }
        }
    }
}

fun HTML.productChangeAdminPage(proposals: List<ProductChangeProposalView>) = template(
    "Produktänderungen - CRM",
    titleBlock = {
        h2 { +"Produktänderungen" }
        h4 { +"Offene Vorschläge" }
    },
) {
    changeNav("proposals")
    card(if (proposals.isEmpty()) null else "hidden") {
        attributes["data-crm-empty"] = ""
        p(classes = "text-sm text-gray-600") { +"Keine offenen Vorschläge." }
    }
    proposals.forEach { proposal ->
        div {
            attributes["data-crm-card"] = ""
            card("flex flex-col gap-4") {
                div(classes = "flex flex-wrap items-start justify-between gap-3") {
                    div {
                        h3(classes = "text-lg font-bold text-gray-900") {
                            +proposal.type.label
                            +" · "
                            +proposal.barcode
                        }
                        p(classes = "text-xs text-gray-500") {
                            +"Eingereicht von ${proposal.creator} am ${dateFormat.format(proposal.date)}"
                        }
                        a(classes = "text-sm font-semibold text-indigo-600 hover:underline") {
                            href = "/product/${proposal.barcode}"
                            +"Produkt öffnen"
                        }
                    }
                    statusBadgeComponent("Offen", Status.DOUBTFUL)
                }

                if (proposal.type == ProductChangeType.MASTERDATA) {
                    div(classes = "grid grid-cols-1 gap-3 rounded-xl border border-gray-100 bg-gray-50 p-4 sm:grid-cols-2") {
                        changeValue("Name", proposal.currentName, proposal.proposedName)
                        changeValue("Marke", proposal.currentBrand, proposal.proposedBrand)
                    }
                    proposal.proposedImageDataUri?.let { dataUri ->
                        div(classes = "flex flex-col gap-2") {
                            span(classes = "text-sm font-semibold text-gray-600") { +"Neues Frontfoto" }
                            img(classes = "max-h-64 w-auto rounded-lg border border-gray-200 object-contain") {
                                src = dataUri
                                alt = "Vorgeschlagenes Produktfoto"
                            }
                        }
                    }
                } else {
                    ul(classes = "list-disc space-y-1 pl-5 text-sm text-gray-800") {
                        proposal.ignoredIngredients.forEach { ingredient ->
                            li {
                                +ingredient.name
                                +" (${ingredient.classificationName}, ${ingredient.source}, ${ingredient.id})"
                            }
                        }
                    }
                    if (proposal.reason.isNotBlank()) {
                        p(classes = "text-sm text-gray-700") {
                            span(classes = "font-semibold") { +"Begründung: " }
                            +proposal.reason
                        }
                    }
                }

                if (proposal.note.isNotBlank()) {
                    p(classes = "text-sm text-gray-600") {
                        span(classes = "font-semibold") { +"Notiz: " }
                        +proposal.note
                    }
                }

                div(classes = "flex flex-wrap gap-2") {
                    changeActionForm(
                        action = "/crm/product_changes/${proposal.proposalId}/approve",
                        kind = "approve",
                        label = "Genehmigen",
                        classes = "px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 cursor-pointer disabled:opacity-60",
                        confirm = "Diesen Vorschlag wirklich genehmigen?",
                        success = "Vorschlag genehmigt.",
                    )
                    changeActionForm(
                        action = "/crm/product_changes/${proposal.proposalId}/reject",
                        kind = "reject",
                        label = "Ablehnen",
                        classes = "px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 cursor-pointer disabled:opacity-60",
                        confirm = "Diesen Vorschlag wirklich ablehnen?",
                        success = "Vorschlag abgelehnt.",
                    )
                }
            }
        }
    }
}

fun HTML.productChangeHistoryPage(history: List<Proposal>) = template(
    "Verlauf Produktänderungen - CRM",
    titleBlock = {
        h2 { +"Verlauf" }
        h4 { +"Produktänderungen" }
    },
) {
    changeNav("history")
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
                        +proposal.entityType.let {
                            if (it == PRODUCT_MASTERDATA_ENTITY) "Stammdaten" else "Fehlerhafte Zutat"
                        }
                        +" · "
                        +proposal.json["id"].orEmpty()
                    }
                    p(classes = "text-xs text-gray-500") {
                        +dateFormat.format(proposal.date)
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
                val old = proposal.oldJson.orEmpty()
                val new = proposal.json
                (old.keys + new.keys).filter { it != "id" }.forEach { name ->
                    val beforeRaw = old[name]?.takeIf { it.isNotBlank() }
                    val afterRaw = new[name]?.takeIf { it.isNotBlank() }
                    if (beforeRaw != afterRaw) {
                        val before = displayHistoryValue(name, beforeRaw, proposed = false)
                        val after = displayHistoryValue(name, afterRaw, proposed = true)
                        div(classes = "grid grid-cols-1 gap-1 border-b border-gray-200 py-2 text-sm last:border-b-0 sm:grid-cols-[10rem_1fr]") {
                            span(classes = "font-semibold text-gray-600") { +historyLabel(name) }
                            div(classes = "min-w-0 break-words text-gray-900") {
                                if (before.isNotBlank()) {
                                    span(classes = "mr-2 text-gray-400 line-through") { +before }
                                    +"→ "
                                }
                                +(after.ifBlank { "–" })
                            }
                        }
                    }
                }
            }
        }
    }
}

fun HTML.productChangeManagePage(overrides: List<ManagedProductOverride>) = template(
    "Produkt-Overrides verwalten - CRM",
    head = { loadPhotoNormalizer() },
    titleBlock = {
        h2 { +"Overrides verwalten" }
        h4 { +"Produktänderungen" }
    },
) {
    changeNav("manage")
    if (overrides.isEmpty()) {
        card {
            p(classes = "text-sm text-gray-600") { +"Keine Overrides vorhanden." }
        }
    }
    overrides.forEach { override ->
        card("flex flex-col gap-4") {
            div(classes = "flex flex-wrap items-start justify-between gap-3") {
                div {
                    h3(classes = "text-lg font-bold text-gray-900") { +override.barcode }
                    p(classes = "text-sm text-gray-600") {
                        +override.sourceName
                        +" · "
                        +override.sourceBrand
                    }
                    a(classes = "text-sm font-semibold text-indigo-600 hover:underline") {
                        href = "/product/${override.barcode}"
                        +"Produkt öffnen"
                    }
                }
            }

            form(
                action = "/crm/product_changes/manage/${override.barcode}/update",
                method = FormMethod.post,
                encType = FormEncType.multipartFormData,
            ) {
                formGroup {
                    inputField("Name-Override", "name", override.name.orEmpty())
                    inputField("Marken-Override", "brand", override.brand.orEmpty())
                    photoField(
                        label = "Foto-Override",
                        name = "front",
                        previewId = "preview-${override.barcode}",
                        existingUrl = override.imageUrl,
                    )
                    submitButton("Overrides speichern")
                }
            }

            if (override.name != null || override.brand != null || override.imageUrl != null) {
                div(classes = "flex flex-wrap gap-2") {
                    override.name?.let { clearForm(override.barcode, "name", "Name-Override entfernen") }
                    override.brand?.let { clearForm(override.barcode, "brand", "Marken-Override entfernen") }
                    override.imageUrl?.let { clearForm(override.barcode, "image", "Foto-Override entfernen") }
                }
            }

            if (override.ignoredIngredients.isNotEmpty()) {
                div(classes = "flex flex-col gap-2") {
                    h4(classes = "text-sm font-semibold text-gray-700") { +"Ignorierte Zutaten" }
                    override.ignoredIngredients.forEach { ingredient ->
                        div(classes = "flex flex-wrap items-center justify-between gap-2 rounded-lg border border-gray-200 p-3 text-sm") {
                            div {
                                div(classes = "font-semibold text-gray-900") { +ingredient.name }
                                div(classes = "text-xs text-gray-500") {
                                    +ingredient.ingredientId
                                    if (ingredient.reason.isNotBlank()) {
                                        +" · "
                                        +ingredient.reason
                                    }
                                }
                            }
                            form(
                                action = "/crm/product_changes/manage/${override.barcode}/remove-ingredient",
                                method = FormMethod.post,
                            ) {
                                input(type = InputType.hidden) {
                                    name = "ingredientId"
                                    value = ingredient.ingredientId
                                }
                                input(type = InputType.submit, classes = "btn-secondary cursor-pointer") {
                                    value = "Entfernen"
                                }
                            }
                        }
                    }
                }
            }

            form(action = "/crm/product_changes/manage/${override.barcode}/delete", method = FormMethod.post) {
                input(type = InputType.submit, classes = "px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 cursor-pointer") {
                    value = "Kompletten Override entfernen"
                }
            }
        }
    }
}

private val ProductChangeType.label: String
    get() = when (this) {
        ProductChangeType.MASTERDATA -> "Stammdaten"
        ProductChangeType.IGNORED_INGREDIENTS -> "Fehlerhafte Zutat"
    }

private fun FlowContent.showError(message: String) {
    div(classes = "mb-4 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700") {
        +message
    }
}

private fun FlowContent.changeValue(label: String, current: String, proposed: String?) {
    div(classes = "rounded-lg bg-white p-3 shadow-sm") {
        div(classes = "text-xs font-semibold uppercase tracking-wide text-gray-500") { +label }
        div(classes = "text-sm text-gray-900") {
            if (proposed == null || proposed == current) {
                +current.ifBlank { "–" }
            } else {
                span(classes = "mr-2 text-gray-400 line-through") { +current.ifBlank { "–" } }
                +"→ "
                +proposed
            }
        }
    }
}

private fun FlowContent.clearForm(barcode: String, field: String, label: String) {
    form(action = "/crm/product_changes/manage/$barcode/clear/$field", method = FormMethod.post) {
        input(type = InputType.submit, classes = "btn-secondary cursor-pointer") {
            value = label
        }
    }
}

private fun FlowContent.changeActionForm(
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

private fun FlowContent.changeNav(active: String) {
    div(classes = "flex flex-wrap gap-2 border-b border-gray-200 pb-2") {
        navLink("/crm", "Übersicht", false)
        navLink("/crm/product_changes", "Offene Vorschläge", active == "proposals")
        navLink("/crm/product_changes/history", "Verlauf", active == "history")
        navLink("/crm/product_changes/manage", "Overrides", active == "manage")
    }
}

private fun FlowContent.navLink(href: String, label: String, active: Boolean) {
    a(classes = "rounded-lg px-3 py-2 text-sm font-semibold ${if (active) "bg-primary text-white" else "text-gray-600 hover:bg-gray-100"}") {
        this.href = href
        +label
    }
}

private fun historyLabel(name: String): String = when (name) {
    "name" -> "Name"
    "brand" -> "Marke"
    "frontImage" -> "Frontfoto"
    "ignoredIngredients" -> "Ignorierte Zutaten"
    "ingredients" -> "Zutaten"
    "reason" -> "Begründung"
    else -> name
}

private fun displayHistoryValue(name: String, value: String?, proposed: Boolean): String {
    if (value.isNullOrBlank()) return ""
    if (name == "frontImage") return if (proposed) "Neues Bild" else "Vorhandenes Bild"
    return value
}
