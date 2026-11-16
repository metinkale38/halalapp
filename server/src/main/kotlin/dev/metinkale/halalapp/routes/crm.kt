package dev.metinkale.halalapp.routes

import dev.metinkale.halalapp.crm.createRequestPage
import dev.metinkale.halalapp.db.table.ClassificationsTable
import dev.metinkale.halalapp.db.table.CrmTable
import dev.metinkale.halalapp.db.table.ProposalsTable
import dev.metinkale.halalapp.server.HalalAppUser
import dev.metinkale.halalapp.server.authenticated
import dev.metinkale.halalapp.server.ensureAdmin
import dev.metinkale.halalapp.service.CrmAdminService
import dev.metinkale.halalapp.service.CrmConflictException
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.template
import dev.metinkale.halalapp.ui.crm.CrmEntryView
import dev.metinkale.halalapp.ui.crm.crmEntityListPage
import dev.metinkale.halalapp.ui.crm.crmHistoryPage
import dev.metinkale.halalapp.ui.crm.displayValues
import dev.metinkale.halalapp.ui.crm.editCrmEntityPage
import dev.metinkale.halalapp.ui.crm.entryTitle
import dev.metinkale.halalapp.ui.crm.getCrmFields
import dev.metinkale.halalapp.ui.crm.paramsToValues
import dev.metinkale.halalapp.ui.crm.proposalOverviewPage
import dev.metinkale.halalapp.ui.crm.proposalsPage
import dev.metinkale.halalapp.ui.crm.readValues
import dev.metinkale.halalapp.ui.crm.searchMatches
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.html.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import kotlin.math.ceil

private const val CRM_PAGE_SIZE = 50

fun Routing.crmRoutes() = authenticated {

    route("/crm") {
        get {
            ensureAdmin()
            call.respondHtml {
                proposalOverviewPage()
            }
        }

        get("/") {
            ensureAdmin()
            call.respondHtml {
                proposalOverviewPage()
            }
        }

        CrmTable::class.sealedSubclasses.mapNotNull { it.objectInstance }.forEach { table ->
            route("/${table.tableName}") {
                get("/new") {
                    val id = call.queryParameters["id"]
                    val queryMap = call.queryParameters.toMap()
                    suspendTransaction {
                        val resultRow = table.selectAll().where { table.id eq id }.singleOrNull()
                        val params = table.columns.associate {
                            it.name to (resultRow?.get(it)?.toString() ?: "")
                        } + queryMap.mapValues { it.value.last() }
                        call.respondHtml {
                            createRequestPage(table, params, queryMap.keys)
                        }
                    }
                }

                post("/new") {
                    val params = call.receiveParameters()
                    ProposalsTable.newRequest(
                        table,
                        params,
                        call.userIdentity()
                    )
                    call.respondHtml { template { card { +"Vorschlag gesendet. Vielen Dank." } } }
                }

                get {
                    ensureAdmin()
                    val query = call.queryParameters["q"].orEmpty().trim()
                    val requestedPage = call.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1

                    val allEntries = suspendTransaction {
                        ClassificationsTable.cacheAll()
                        table.selectAll().map { row ->
                            val values = table.readValues(row)
                            CrmEntryView(
                                id = row[table.id].value,
                                title = table.entryTitle(values),
                                values = table.displayValues(values),
                            )
                        }
                    }
                    val filtered = allEntries.filter { table.searchMatches(it.values, query) }
                    val totalPages = if (filtered.isEmpty()) 1 else ceil(filtered.size.toDouble() / CRM_PAGE_SIZE).toInt()
                    val page = requestedPage.coerceAtMost(totalPages)
                    val entries = filtered.drop((page - 1) * CRM_PAGE_SIZE).take(CRM_PAGE_SIZE)

                    call.respondHtml {
                        crmEntityListPage(
                            table = table,
                            entries = entries,
                            query = query,
                            page = page,
                            totalPages = totalPages,
                            totalCount = filtered.size,
                            openProposals = ProposalsTable.numberOfOpenRequests(table),
                            saved = call.queryParameters["saved"] == "1",
                        )
                    }
                }

                get("/proposals") {
                    ensureAdmin()
                    call.respondHtml { proposalsPage(table) }
                }

                get("/history") {
                    ensureAdmin()
                    call.respondHtml {
                        crmHistoryPage(
                            table = table,
                            history = ProposalsTable.findHistory(table),
                            openProposals = ProposalsTable.numberOfOpenRequests(table),
                        )
                    }
                }

                get("/create") {
                    ensureAdmin()
                    val values = table.getCrmFields().associate { it.name to "" }
                    call.respondHtml {
                        editCrmEntityPage(
                            table = table,
                            values = values,
                            action = "/crm/${table.tableName}/create",
                            editing = false,
                        )
                    }
                }

                post("/create") {
                    ensureAdmin()
                    val params = call.receiveParameters()
                    val values = table.paramsToValues(params)
                    val note = params["note"].orEmpty()
                    val errors = try {
                        CrmAdminService.create(table, values, call.adminIdentity(), note)
                    } catch (e: IllegalArgumentException) {
                        listOf(e.message.orEmpty())
                    }
                    if (errors.isEmpty()) {
                        call.respondRedirect("/crm/${table.tableName}?saved=1")
                    } else {
                        call.respondHtml {
                            editCrmEntityPage(
                                table = table,
                                values = values,
                                action = "/crm/${table.tableName}/create",
                                editing = false,
                                errors = errors,
                            )
                        }
                    }
                }

                get("/{id}/edit") {
                    ensureAdmin()
                    val id = call.parameters["id"] ?: throw NotFoundException("ID not found")
                    val values = CrmAdminService.findValues(table, id)
                        ?: throw NotFoundException("Eintrag nicht gefunden")
                    call.respondHtml {
                        editCrmEntityPage(
                            table = table,
                            values = table.displayValues(values),
                            action = "/crm/${table.tableName}/$id/edit",
                            editing = true,
                            version = CrmAdminService.version(values),
                            impact = CrmAdminService.impactText(table, values),
                        )
                    }
                }

                post("/{id}/edit") {
                    ensureAdmin()
                    val id = call.parameters["id"] ?: throw NotFoundException("ID not found")
                    val params = call.receiveParameters()
                    val values = table.paramsToValues(params).toMutableMap().also {
                        if (it.containsKey(table.id.name)) it[table.id.name] = id
                    }
                    val note = params["note"].orEmpty()
                    val version = params["version"]
                    val errors = try {
                        CrmAdminService.update(table, id, values, version, call.adminIdentity(), note)
                    } catch (e: CrmConflictException) {
                        listOf(e.message.orEmpty())
                    } catch (e: IllegalArgumentException) {
                        listOf(e.message.orEmpty())
                    }
                    if (errors.isEmpty()) {
                        call.respondRedirect("/crm/${table.tableName}?saved=1")
                    } else {
                        call.respondHtml {
                            editCrmEntityPage(
                                table = table,
                                values = table.displayValues(values),
                                action = "/crm/${table.tableName}/$id/edit",
                                editing = true,
                                errors = errors,
                                version = version,
                                impact = CrmAdminService.impactText(table, values),
                            )
                        }
                    }
                }

                post("/{id}/approve") {
                    ensureAdmin()
                    val id = call.parameters["id"] ?: throw NotFoundException("ID not found")
                    try {
                        ProposalsTable.approveRequest(table, id)
                        call.respondHtml { template { card { +"Vorschlag genehmigt." } } }
                    } catch (e: IllegalArgumentException) {
                        call.respondText(e.message.orEmpty(), status = HttpStatusCode.BadRequest)
                    }
                }

                post("/{id}/reject") {
                    ensureAdmin()
                    val id = call.parameters["id"] ?: throw NotFoundException("ID not found")
                    try {
                        ProposalsTable.rejectRequest(id)
                        call.respondHtml { template { card { +"Vorschlag abgelehnt." } } }
                    } catch (e: IllegalArgumentException) {
                        call.respondText(e.message.orEmpty(), status = HttpStatusCode.BadRequest)
                    }
                }
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.userIdentity(): String {
    val user = principal<HalalAppUser>()
    return user?.email?.takeIf { it.isNotBlank() } ?: user?.username ?: "Unbekannt"
}

private fun io.ktor.server.application.ApplicationCall.adminIdentity(): String {
    val user = principal<HalalAppUser>()
    return user?.email?.takeIf { it.isNotBlank() } ?: user?.username ?: "Admin"
}
