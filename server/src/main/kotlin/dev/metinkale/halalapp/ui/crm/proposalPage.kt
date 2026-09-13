package dev.metinkale.halalapp.ui.crm

import dev.metinkale.halalapp.db.entity.Entity
import dev.metinkale.halalapp.db.table.CrmTable
import dev.metinkale.halalapp.db.table.ProposalsTable
import dev.metinkale.halalapp.ui.component.template
import dev.metinkale.halalapp.ui.component.card
import kotlinx.html.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction


fun <T : Entity> HTML.proposalsPage(table: CrmTable<T>) =
    template(
        "Vorschläge - " + table.germanName,
        titleBlock = {
            h2 { +"Vorschläge" }
            h4 { +table.germanName }
        }) {
        ProposalsTable.findOpenRequests(table).also {
            if (it.isEmpty())
                card {
                    h6 { +"Keine Vorschläge gefunden" }
                }
        }.forEach {
            card {
                p {
                    span("font-bold") { +"Datum: " }
                    +"${it.date}"
                }
                div("py-4") {

                    fun FlowContent.print(name: String, value: String?, valueOld: String?) {
                        if (valueOld != value && valueOld != null) p {
                            span("font-bold") { +"${name}: " }
                            span("line-through") { +valueOld }
                            +"-> $value"
                        } else p {
                            span("font-bold") { +"${name}: " }
                            +"$value"
                        }
                    }

                    transaction {
                        it.json["id"]?.let { id ->
                            table.selectAll().where { table.id eq id }.limit(1).firstOrNull()
                        }?.let { row ->
                            table.getCrmFields().forEach { field ->
                                print(field.name, it.json[field.name], field.read(row))
                            }
                        } ?: run {
                            table.getCrmFields().forEach { field ->
                                print(field.name, it.json[field.name], null)
                            }
                        }
                    }
                }
                p {
                    span("font-bold") { +"Notiz: " }
                    +it.note
                }
                p {
                    span("font-bold") { +"Author: " }
                    +it.creator
                }

                div(classes = "flex gap-2 pt-3") {
                    // Approve Form
                    form(
                        action = "${table.tableName}/${it.id}/approve",
                        method = FormMethod.post
                    ) {
                        input(
                            type = InputType.submit,
                            classes = "px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
                        ) {
                            attributes["onclick"] =
                                "return confirm('Möchtest du diese Zutat wirklich genehmigen?');"
                            value = "Approve"
                        }
                    }

                    // Reject Form
                    form(
                        action = "${table.tableName}/${it.id}/reject",
                        method = FormMethod.post
                    ) {
                        input(
                            type = InputType.submit,
                            classes = "px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                        ) {
                            attributes["onclick"] =
                                "return confirm('Bist du sicher, dass du diese Zutat ablehnen möchtest?');"
                            value = "Reject"
                        }
                    }
                }
            }
        }
    }