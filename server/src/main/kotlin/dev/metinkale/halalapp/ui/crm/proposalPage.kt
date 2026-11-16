package dev.metinkale.halalapp.ui.crm

import dev.metinkale.halalapp.db.entity.Entity
import dev.metinkale.halalapp.db.table.CrmTable
import dev.metinkale.halalapp.db.table.ProposalsTable
import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.statusBadgeComponent
import dev.metinkale.halalapp.ui.component.template
import kotlinx.html.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun <T : Entity> HTML.proposalsPage(table: CrmTable<T>) {
    val proposals = ProposalsTable.findOpenRequests(table)

    template(
        "Vorschläge - " + table.germanName,
        titleBlock = {
            h2 { +"Vorschläge" }
            h4 { +table.germanName }
        },
    ) {
        p(classes = "text-sm text-gray-600") {
            +"Prüfe die eingereichten Änderungen. Nach dem Genehmigen oder Ablehnen verschwindet der Vorschlag automatisch."
        }

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
                            h3(classes = "text-lg font-bold text-gray-900") {
                                +table.germanName
                                +" #"
                                +proposal.id
                            }
                            p(classes = "text-xs text-gray-500") {
                                +"Eingereicht von ${proposal.creator} am ${proposal.date}"
                            }
                        }
                        statusBadgeComponent("Offen", Status.DOUBTFUL)
                    }

                    div(classes = "rounded-xl border border-gray-100 bg-gray-50 p-4") {
                        fun FlowContent.print(name: String, value: String?, valueOld: String?) {
                            div(classes = "grid grid-cols-1 gap-1 border-b border-gray-200 py-2 text-sm last:border-b-0 sm:grid-cols-[10rem_1fr]") {
                                span(classes = "font-semibold text-gray-600") { +name }
                                div(classes = "min-w-0 break-words text-gray-900") {
                                    if (valueOld != null && valueOld != value) {
                                        span(classes = "mr-2 text-gray-400 line-through") { +valueOld }
                                        +"→ "
                                    }
                                    +(value.orEmpty().ifBlank { "–" })
                                }
                            }
                        }

                        transaction {
                            proposal.json["id"]?.let { id ->
                                table.selectAll().where { table.id eq id }.limit(1).firstOrNull()
                            }?.let { row ->
                                table.getCrmFields().forEach { field ->
                                    print(field.label, proposal.json[field.name], field.read(row))
                                }
                            } ?: run {
                                table.getCrmFields().forEach { field ->
                                    print(field.label, proposal.json[field.name], null)
                                }
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
                        actionForm(
                            action = "/crm/${table.tableName}/${proposal.id}/approve",
                            kind = "approve",
                            label = "Genehmigen",
                            classes = "px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 cursor-pointer disabled:opacity-60",
                            confirm = "Diesen Vorschlag wirklich genehmigen?",
                            success = "Vorschlag genehmigt.",
                        )
                        actionForm(
                            action = "/crm/${table.tableName}/${proposal.id}/reject",
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
