package dev.metinkale.halalapp.ui.crm

import dev.metinkale.halalapp.db.entity.Entity
import dev.metinkale.halalapp.db.entity.Proposal
import dev.metinkale.halalapp.db.table.CrmTable
import dev.metinkale.halalapp.db.table.ProductParentTable
import dev.metinkale.halalapp.domain.RequestStatus
import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.html.formGroup
import dev.metinkale.halalapp.html.loadEasyMDE
import dev.metinkale.halalapp.html.markdownField
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.statusBadgeComponent
import dev.metinkale.halalapp.ui.component.template
import kotlinx.html.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CrmEntryView(
    val id: String,
    val title: String,
    val values: Map<String, String>,
)

fun <T : Entity> HTML.crmEntityListPage(
    table: CrmTable<T>,
    entries: List<CrmEntryView>,
    query: String,
    page: Int,
    totalPages: Int,
    totalCount: Int,
    openProposals: Long,
    saved: Boolean,
) = template(
    "${table.germanName} - CRM",
    titleBlock = {
        h2 { +table.germanName }
        h4 { +"Admin-CRM" }
    },
) {
    if (saved) {
        div(classes = "rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800") {
            +"Änderungen gespeichert."
        }
    }

    crmTabs(table, active = "entries", openProposals = openProposals)

    div(classes = "flex flex-wrap items-end justify-between gap-3") {
        form(action = "/crm/${table.tableName}", method = FormMethod.get, classes = "flex flex-1 flex-wrap items-end gap-2") {
            label(classes = "flex min-w-[16rem] flex-1 flex-col gap-1") {
                span(classes = "text-xs font-semibold uppercase tracking-wide text-gray-500") { +"Suchen" }
                input(classes = "w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500") {
                    type = InputType.text
                    name = "q"
                    value = query
                    placeholder = "ID, Name, Status ..."
                }
            }
            button(type = ButtonType.submit, classes = "btn-secondary") { +"Filtern" }
            if (query.isNotBlank()) {
                a(classes = "btn-secondary") {
                    href = "/crm/${table.tableName}"
                    +"Zurücksetzen"
                }
            }
        }
        a(classes = "btn-primary whitespace-nowrap") {
            href = "/crm/${table.tableName}/create"
            +"Neuer Eintrag"
        }
    }

    p(classes = "text-xs text-gray-500") {
        +"$totalCount Einträge"
        if (query.isNotBlank()) +" für „$query“"
    }

    if (entries.isEmpty()) {
        card {
            p(classes = "text-sm text-gray-600") { +"Keine Einträge gefunden." }
        }
    } else {
        card("mx-auto w-full max-w-full overflow-hidden !p-0") {
            div(classes = "w-full overflow-x-auto") {
                table(classes = "w-full min-w-[44rem] divide-y divide-gray-200 text-sm") {
                thead(classes = "bg-gray-50") {
                    tr {
                        table.listFieldNames.forEach { name ->
                            th(classes = "whitespace-nowrap px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500") {
                                +table.fieldLabel(name)
                            }
                        }
                        th(classes = "px-4 py-3") { }
                    }
                }
                tbody(classes = "divide-y divide-gray-100 bg-white") {
                    entries.forEach { entry ->
                        tr {
                            table.listFieldNames.forEach { name ->
                                td(classes = "max-w-[24rem] truncate px-4 py-3 text-gray-700") {
                                    if (table === ProductParentTable && (name == "child_name" || name == "parent_name")) {
                                        val id = entry.values[if (name == "child_name") "child_id" else "parent_id"].orEmpty()
                                        a(classes = "font-semibold text-indigo-600 hover:underline") {
                                            href = "/product/$id"
                                            +displayValue(entry.values[name])
                                        }
                                    } else {
                                        +displayValue(entry.values[name])
                                    }
                                }
                            }
                            td(classes = "whitespace-nowrap px-4 py-3 text-right") {
                                a(classes = "font-semibold text-indigo-600 hover:underline") {
                                    href = "/crm/${table.tableName}/${entry.id}/edit"
                                    +"Bearbeiten"
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }

    if (totalPages > 1) {
        div(classes = "flex items-center justify-between gap-3") {
            if (page > 1) {
                a(classes = "btn-secondary") {
                    href = buildCrmListUrl(table.tableName, query, page - 1)
                    +"Zurück"
                }
            } else {
                span(classes = "btn-secondary pointer-events-none opacity-50") { +"Zurück" }
            }
            span(classes = "text-sm text-gray-500") { +"Seite $page von $totalPages" }
            if (page < totalPages) {
                a(classes = "btn-secondary") {
                    href = buildCrmListUrl(table.tableName, query, page + 1)
                    +"Weiter"
                }
            } else {
                span(classes = "btn-secondary pointer-events-none opacity-50") { +"Weiter" }
            }
        }
    }
}

fun <T : Entity> HTML.editCrmEntityPage(
    table: CrmTable<T>,
    values: Map<String, String>,
    action: String,
    editing: Boolean,
    errors: List<String> = emptyList(),
    version: String? = null,
    impact: String? = null,
) = template(
    if (editing) "${table.germanName} bearbeiten - CRM" else "Neuer Eintrag - ${table.germanName}",
    head = { loadEasyMDE() },
    titleBlock = {
        h2 { +if (editing) table.germanName else "Neuer Eintrag" }
        h4 { +table.germanName }
    },
) {
    div(classes = "flex flex-wrap items-center justify-between gap-2") {
        a(classes = "text-sm font-semibold text-indigo-600 hover:underline") {
            href = "/crm/${table.tableName}"
            +"← Zurück zur Liste"
        }
        if (editing) {
            a(classes = "text-sm font-semibold text-gray-600 hover:underline") {
                href = "/crm/${table.tableName}/history"
                +"Verlauf ansehen"
            }
        }
    }

    if (errors.isNotEmpty()) {
        div(classes = "rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700") {
            ul(classes = "list-disc space-y-1 pl-5") {
                errors.forEach { li { +it } }
            }
        }
    }

    if (impact != null) {
        div(classes = "rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800") {
            span(classes = "font-semibold") { +"Auswirkung: " }
            +impact
        }
    }

    if (table === ProductParentTable) {
        val childId = values["child_id"].orEmpty()
        val parentId = values["parent_id"].orEmpty()
        if (childId.isNotBlank() || parentId.isNotBlank()) {
            div(classes = "grid grid-cols-1 gap-3 rounded-xl border border-gray-200 bg-gray-50 p-4 md:grid-cols-2") {
                linkedProduct("Dublette", childId, values["child_name"])
                linkedProduct("Hauptprodukt", parentId, values["parent_name"])
            }
        }
    }

    card("m-auto w-full") {
        form(action = action, method = FormMethod.post) {
            formGroup {
                table.getCrmFields().forEach { field ->
                    val readOnly = editing && field.name == table.id.name
                    when (field) {
                        is CrmLens.CheckBox -> checkBoxField(
                            field.label,
                            field.name,
                            values[field.name] == "true",
                        )

                        is CrmLens.Markdown -> markdownField(
                            field.label,
                            field.name,
                            values[field.name],
                        )

                        is CrmLens.Select<*> -> selectField(
                            field.label,
                            field.name,
                            field.options.let { options ->
                                val current = values[field.name]
                                if (current != null && options.none { it.first == current }) {
                                    options + (current to current)
                                } else options
                            },
                            values[field.name],
                        )

                        is CrmLens.MultiSelect<*> -> multiSelect(
                            field.label,
                            field.name,
                            field.options,
                            values[field.name]?.split(";")?.filter { it.isNotBlank() }.orEmpty(),
                        )

                        is CrmLens.Date -> inputField(
                            field.label,
                            field.name,
                            values[field.name].orEmpty(),
                        ) {
                            type = InputType.date
                        }

                        is CrmLens.StaticText -> if (readOnly) {
                            staticField(field.label, field.name, values[field.name].orEmpty())
                        } else {
                            inputField(field.label, field.name, values[field.name].orEmpty())
                        }

                        is CrmLens.Text -> if (readOnly) {
                            staticField(field.label, field.name, values[field.name].orEmpty())
                        } else {
                            inputField(field.label, field.name, values[field.name].orEmpty())
                        }
                    }
                }

                textAreaField("Änderungsnotiz (optional)", "note") {
                    rows = "3"
                    placeholder = "Warum wird dieser Eintrag geändert?"
                }

                if (version != null) {
                    input(type = InputType.hidden) {
                        name = "version"
                        value = version
                    }
                }

                div(classes = "flex flex-wrap gap-2") {
                    input(
                        type = InputType.submit,
                        classes = "btn-primary cursor-pointer",
                    ) {
                        value = if (editing) "Speichern" else "Anlegen"
                    }
                    a(classes = "btn-secondary") {
                        href = "/crm/${table.tableName}"
                        +"Abbrechen"
                    }
                }
            }
        }
    }
}

fun <T : Entity> HTML.crmHistoryPage(
    table: CrmTable<T>,
    history: List<Proposal>,
    openProposals: Long,
) = template(
    "Verlauf ${table.germanName} - CRM",
    titleBlock = {
        h2 { +"Verlauf" }
        h4 { +table.germanName }
    },
) {
    crmTabs(table, active = "history", openProposals = openProposals)

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
                        +DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault()).format(proposal.date)
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
                val names = table.getCrmFields().map { it.name }.toSet() + old?.keys.orEmpty() + new.keys
                names.forEach { name ->
                    val before = old?.get(name)
                    val after = new[name]
                    if (before != after) {
                        div(classes = "grid grid-cols-1 gap-1 border-b border-gray-200 py-2 text-sm last:border-b-0 sm:grid-cols-[10rem_1fr]") {
                            span(classes = "font-semibold text-gray-600") { +table.fieldLabel(name) }
                            div(classes = "min-w-0 break-words text-gray-900") {
                                if (before != null && before.isNotBlank()) {
                                    span(classes = "mr-2 text-gray-400 line-through") { +before }
                                    +"→ "
                                }
                                +displayValue(after)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun FlowContent.linkedProduct(label: String, id: String, name: String?) {
    if (id.isBlank()) return
    a(classes = "block rounded-lg bg-white p-3 shadow-sm transition hover:shadow-md") {
        href = "/product/$id"
        p(classes = "text-xs font-semibold uppercase tracking-wide text-gray-500") { +label }
        p(classes = "text-sm font-semibold text-gray-900") { +(name.orEmpty().ifBlank { id }) }
        p(classes = "text-xs text-gray-400") { +id }
    }
}

private fun displayValue(value: String?): String {
    if (value.isNullOrBlank()) return "–"
    return when (value) {
        "true" -> "Ja"
        "false" -> "Nein"
        else -> Status.entries.firstOrNull { it.name == value }?.value ?: value
    }
}

private fun <T : Entity> FlowContent.crmTabs(
    table: CrmTable<T>,
    active: String,
    openProposals: Long,
) {
    div(classes = "flex flex-wrap gap-2 border-b border-gray-200 pb-2") {
        crmTab("/crm/${table.tableName}", "Einträge", active == "entries")
        crmTab("/crm/${table.tableName}/proposals", "Vorschläge ($openProposals)", active == "proposals")
        crmTab("/crm/${table.tableName}/history", "Verlauf", active == "history")
    }
}

private fun FlowContent.crmTab(href: String, label: String, active: Boolean) {
    a(classes = "rounded-lg px-3 py-2 text-sm font-semibold ${if (active) "bg-primary text-white" else "text-gray-600 hover:bg-gray-100"}") {
        this.href = href
        +label
    }
}

private fun buildCrmListUrl(table: String, query: String, page: Int): String = buildString {
    append("/crm/")
    append(table)
    append("?page=")
    append(page)
    if (query.isNotBlank()) {
        append("&q=")
        append(java.net.URLEncoder.encode(query, Charsets.UTF_8))
    }
}
