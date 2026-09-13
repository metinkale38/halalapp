package dev.metinkale.halalapp.ui.crm

import dev.metinkale.halalapp.db.table.CrmTable
import dev.metinkale.halalapp.db.table.ProposalsTable
import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.ui.component.template
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.statusBadgeComponent
import kotlinx.html.HTML
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.h4

fun HTML.proposalOverviewPage() = template(
    "Vorschläge",
    titleBlock = {
        h2 { +"Vorschläge" }
        h4 { +"Adminübersicht" }
    }) {

    CrmTable::class.sealedSubclasses.mapNotNull { it.objectInstance }.forEach {
        a {
            href = "/crm/${it.tableName}"
            card {
                div("flex flex-row items-center") {
                    h2(classes = "text-2xl font-extrabold tracking-tight grow mb-4") {
                        +it.germanName
                    }
                    statusBadgeComponent("${ProposalsTable.numberOfOpenRequests(it)} Vorschläge", Status.HALAL)
                }
            }
        }
    }
}
