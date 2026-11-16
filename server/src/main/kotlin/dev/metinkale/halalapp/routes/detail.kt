package dev.metinkale.halalapp.routes

import dev.metinkale.halalapp.db.table.ProcessedProductsTable
import dev.metinkale.halalapp.db.table.ProductParentTable
import dev.metinkale.halalapp.feature.product.productPage
import dev.metinkale.halalapp.feature.product.productPageProposal
import dev.metinkale.halalapp.server.isAdmin
import io.ktor.http.HttpStatusCode
import io.ktor.server.html.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Routing.detailRoutes() {
    route("/product") {
        get("/{id}") {
            val id = call.parameters["id"] ?: throw NotFoundException("ID not found")

            ProductParentTable.findByChildId(id)?.let {
                call.respondRedirect("/product/$it", permanent = true)
            } ?: run {
                val entity = ProcessedProductsTable.findById(id)
                if (entity != null) call.respondHtml { productPage(entity, call.isAdmin()) }
                else call.respond(HttpStatusCode.NotFound)
            }
        }
        get("/{id}/proposal") {
            val id = call.parameters["id"] ?: throw NotFoundException("ID not found")


            val entity = ProcessedProductsTable.findById(id)
            if (entity != null) call.respondHtml { productPageProposal(entity, call.isAdmin()) }
            else call.respond(HttpStatusCode.NotFound)
        }
    }
}


