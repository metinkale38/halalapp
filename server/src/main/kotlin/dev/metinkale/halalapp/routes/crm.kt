package dev.metinkale.halalapp.routes

import dev.metinkale.halalapp.crm.createRequestPage
import dev.metinkale.halalapp.db.table.CrmTable
import dev.metinkale.halalapp.db.table.ProposalsTable
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.template
import dev.metinkale.halalapp.server.HalalAppUser
import dev.metinkale.halalapp.server.authenticated
import dev.metinkale.halalapp.server.ensureAdmin
import dev.metinkale.halalapp.ui.crm.proposalOverviewPage
import dev.metinkale.halalapp.ui.crm.proposalsPage
import io.ktor.server.auth.*
import io.ktor.server.html.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction


fun Routing.crmRoutes() = authenticated {

    route("/crm") {
        get {
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
                        call.principal<HalalAppUser>()?.email!!
                    )
                    call.respondHtml { template { card { +"Vorschlag gesendet. Vielen Dank." } } }
                }


                get {
                    ensureAdmin()
                    call.respondHtml { proposalsPage(table) }
                }

                post("/{id}/approve") {
                    ensureAdmin()
                    val id = call.parameters["id"] ?: throw NotFoundException("ID not found")
                    ProposalsTable.approveRequest(table, id)
                    call.respondHtml { template { card { +"Vorschlag akzeptiert." } } }
                }
                post("/{id}/reject") {
                    ensureAdmin()
                    val id = call.parameters["id"] ?: throw NotFoundException("ID not found")
                    ProposalsTable.rejectRequest(id)
                    call.respondHtml { template { card { +"Vorschlag abgelehnt." } } }

                }
            }


        }


    }
}
