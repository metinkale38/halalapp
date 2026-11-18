package dev.metinkale.halalapp.routes

import dev.metinkale.halalapp.db.table.ProcessedProductsTable
import dev.metinkale.halalapp.db.table.ProductOverridesTable
import dev.metinkale.halalapp.server.HalalAppUser
import dev.metinkale.halalapp.server.authenticated
import dev.metinkale.halalapp.server.ensureAdmin
import dev.metinkale.halalapp.service.ProductChangeService
import dev.metinkale.halalapp.service.ProductIngredientIgnoreSubmission
import dev.metinkale.halalapp.service.ProductMasterDataSubmission
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.template
import dev.metinkale.halalapp.ui.productchanges.productChangeAdminPage
import dev.metinkale.halalapp.ui.productchanges.productChangeHistoryPage
import dev.metinkale.halalapp.ui.productchanges.productChangeManagePage
import dev.metinkale.halalapp.ui.productchanges.productIngredientIgnoreFormPage
import dev.metinkale.halalapp.ui.productchanges.productMasterDataFormPage
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import kotlinx.html.HTML
import kotlinx.html.a
import kotlinx.html.h3
import kotlinx.html.p

fun Routing.productChangeRoutes() = authenticated {
    get("/crm/product_masterdata/new") {
        val barcode = call.productBarcode()
        if (!ProductChangeService.isOffProduct(barcode)) {
            call.respondRedirect("/product/$barcode/proposal")
            return@get
        }
        val product = ProcessedProductsTable.findById(barcode)
            ?: throw NotFoundException("Produkt nicht gefunden")
        val override = ProductOverridesTable.find(barcode)
        call.respondHtml {
            productMasterDataFormPage(
                productId = barcode,
                currentName = product.name,
                currentBrand = product.brand,
                currentImageUrl = override?.frontImageVersion
                    ?.let { "/image/$barcode?v=$it" }
                    ?: product.images.getFrontPhoto(300),
                proposedName = override?.name ?: product.name,
                proposedBrand = override?.brand ?: product.brand,
            )
        }
    }

    post("/crm/product_masterdata/new") {
        val form = call.readProductForm()
        val barcode = form.first("id")
        try {
            ProductChangeService.submitMasterData(
                ProductMasterDataSubmission(
                    barcode = barcode,
                    name = form.first("name"),
                    brand = form.first("brand"),
                    front = form.files["front"],
                    note = form.first("note"),
                    creator = call.productChangeUserIdentity(),
                )
            )
            call.respondHtml { proposalSent(barcode) }
        } catch (e: IllegalArgumentException) {
            val product = ProcessedProductsTable.findById(barcode)
                ?: throw NotFoundException("Produkt nicht gefunden")
            val override = ProductOverridesTable.find(barcode)
            call.respondHtml {
                productMasterDataFormPage(
                    productId = barcode,
                    currentName = product.name,
                    currentBrand = product.brand,
                    currentImageUrl = override?.frontImageVersion
                        ?.let { "/image/$barcode?v=$it" }
                        ?: product.images.getFrontPhoto(300),
                    proposedName = form.first("name"),
                    proposedBrand = form.first("brand"),
                    note = form.first("note"),
                    error = e.message,
                )
            }
        }
    }

    get("/crm/product_ingredient_ignore/new") {
        val barcode = call.productBarcode()
        if (!ProductChangeService.isOffProduct(barcode)) {
            call.respondRedirect("/product/$barcode/proposal")
            return@get
        }
        val product = ProcessedProductsTable.findById(barcode)
            ?: throw NotFoundException("Produkt nicht gefunden")
        val ingredients = ProductChangeService.criticalIngredients(barcode)
        if (ingredients.isEmpty()) {
            call.respondRedirect("/product/$barcode/proposal")
            return@get
        }
        call.respondHtml {
            productIngredientIgnoreFormPage(
                productId = barcode,
                productName = product.name,
                ingredients = ingredients,
                selected = emptySet(),
            )
        }
    }

    post("/crm/product_ingredient_ignore/new") {
        val params = call.receiveParameters()
        val barcode = params["id"].orEmpty()
        val selected = params.getAll("ingredients").orEmpty().filter { it.isNotBlank() }
        try {
            ProductChangeService.submitIgnoredIngredients(
                ProductIngredientIgnoreSubmission(
                    barcode = barcode,
                    ingredientIds = selected,
                    reason = params["reason"].orEmpty(),
                    note = params["note"].orEmpty(),
                    creator = call.productChangeUserIdentity(),
                )
            )
            call.respondHtml { proposalSent(barcode) }
        } catch (e: IllegalArgumentException) {
            val product = ProcessedProductsTable.findById(barcode)
                ?: throw NotFoundException("Produkt nicht gefunden")
            call.respondHtml {
                productIngredientIgnoreFormPage(
                    productId = barcode,
                    productName = product.name,
                    ingredients = ProductChangeService.criticalIngredients(barcode),
                    selected = selected.toSet(),
                    reason = params["reason"].orEmpty(),
                    note = params["note"].orEmpty(),
                    error = e.message,
                )
            }
        }
    }

    route("/crm/product_changes") {
        get {
            ensureAdmin()
            call.respondHtml {
                productChangeAdminPage(ProductChangeService.findProposals())
            }
        }

        get("/history") {
            ensureAdmin()
            call.respondHtml {
                productChangeHistoryPage(ProductChangeService.findHistory())
            }
        }

        get("/manage") {
            ensureAdmin()
            call.respondHtml {
                productChangeManagePage(ProductChangeService.managedOverrides())
            }
        }

        post("/{id}/approve") {
            ensureAdmin()
            val id = call.parameters["id"] ?: throw NotFoundException("ID not found")
            try {
                ProductChangeService.approve(id)
                call.respondHtml { template { card { +"Vorschlag genehmigt." } } }
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message.orEmpty(), status = HttpStatusCode.BadRequest)
            }
        }

        post("/{id}/reject") {
            ensureAdmin()
            val id = call.parameters["id"] ?: throw NotFoundException("ID not found")
            try {
                ProductChangeService.reject(id)
                call.respondHtml { template { card { +"Vorschlag abgelehnt." } } }
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message.orEmpty(), status = HttpStatusCode.BadRequest)
            }
        }

        post("/manage/{barcode}/update") {
            ensureAdmin()
            val barcode = call.parameters["barcode"] ?: throw NotFoundException("ID not found")
            val form = call.readProductForm()
            try {
                ProductChangeService.updateOverride(
                    barcode = barcode,
                    name = form.first("name"),
                    brand = form.first("brand"),
                    front = form.files["front"],
                    actor = call.productChangeUserIdentity(),
                )
                call.respondRedirect("/crm/product_changes/manage")
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message.orEmpty(), status = HttpStatusCode.BadRequest)
            }
        }

        post("/manage/{barcode}/clear/{field}") {
            ensureAdmin()
            val barcode = call.parameters["barcode"] ?: throw NotFoundException("ID not found")
            val field = call.parameters["field"] ?: throw NotFoundException("Feld not found")
            if (field !in setOf("name", "brand", "image")) {
                call.respondText("Unbekanntes Feld.", status = HttpStatusCode.BadRequest)
                return@post
            }
            try {
                ProductChangeService.clearOverrideField(
                    barcode = barcode,
                    field = field,
                    actor = call.productChangeUserIdentity(),
                )
                call.respondRedirect("/crm/product_changes/manage")
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message.orEmpty(), status = HttpStatusCode.BadRequest)
            }
        }

        post("/manage/{barcode}/remove-ingredient") {
            ensureAdmin()
            val barcode = call.parameters["barcode"] ?: throw NotFoundException("ID not found")
            val ingredientId = call.receiveParameters()["ingredientId"].orEmpty()
            try {
                ProductChangeService.removeIgnoredIngredient(
                    barcode = barcode,
                    ingredientId = ingredientId,
                    actor = call.productChangeUserIdentity(),
                )
                call.respondRedirect("/crm/product_changes/manage")
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message.orEmpty(), status = HttpStatusCode.BadRequest)
            }
        }

        post("/manage/{barcode}/delete") {
            ensureAdmin()
            val barcode = call.parameters["barcode"] ?: throw NotFoundException("ID not found")
            try {
                ProductChangeService.deleteOverride(
                    barcode = barcode,
                    actor = call.productChangeUserIdentity(),
                )
                call.respondRedirect("/crm/product_changes/manage")
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message.orEmpty(), status = HttpStatusCode.BadRequest)
            }
        }
    }
}

private fun ApplicationCall.productBarcode(): String =
    request.queryParameters["product_id"]
        ?: request.queryParameters["id"]
        ?: throw NotFoundException("Produkt nicht gefunden")

private fun HTML.proposalSent(barcode: String) = template {
    card {
        h3(classes = "text-lg font-bold text-gray-900") { +"Vorschlag gesendet" }
        p(classes = "text-sm text-gray-600") {
            +"Vielen Dank. Ein Admin prüft den Vorschlag."
        }
        a(classes = "mt-3 inline-block text-sm font-semibold text-indigo-600 hover:underline") {
            href = "/product/$barcode"
            +"Zurück zum Produkt"
        }
    }
}

private fun ApplicationCall.productChangeUserIdentity(): String {
    val user = principal<HalalAppUser>()
    return user?.email?.takeIf { it.isNotBlank() } ?: user?.username ?: "Unbekannt"
}
