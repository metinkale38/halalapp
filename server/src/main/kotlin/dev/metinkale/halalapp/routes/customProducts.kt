package dev.metinkale.halalapp.routes

import dev.metinkale.halalapp.db.table.ClassificationsTable
import dev.metinkale.halalapp.db.table.ProductImagesTable
import dev.metinkale.halalapp.db.table.ProductsCustomTable
import dev.metinkale.halalapp.db.table.ProposalsTable
import dev.metinkale.halalapp.server.HalalAppUser
import dev.metinkale.halalapp.server.authenticated
import dev.metinkale.halalapp.server.ensureAdmin
import dev.metinkale.halalapp.service.CustomProductEdit
import dev.metinkale.halalapp.service.CustomProductService
import dev.metinkale.halalapp.service.CustomProductSubmission
import dev.metinkale.halalapp.service.ProductImageService
import dev.metinkale.halalapp.service.UploadedImageInput
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.template
import dev.metinkale.halalapp.ui.customproducts.CustomProductFormData
import dev.metinkale.halalapp.ui.customproducts.customProductAdminPage
import dev.metinkale.halalapp.ui.customproducts.customProductFormPage
import dev.metinkale.halalapp.ui.customproducts.customProductHistoryPage
import dev.metinkale.halalapp.ui.customproducts.customProductListPage
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.html.*
import java.time.LocalDate

private data class ParsedProductForm(
    val fields: Map<String, List<String>>,
    val files: Map<String, UploadedImageInput>,
) {
    fun first(name: String): String = fields[name]?.firstOrNull().orEmpty()
    fun all(name: String): List<String> = fields[name].orEmpty()
}

fun Routing.customProductRoutes() {
    get("/image/{id}") {
        respondStoredImage(call, call.parameters["id"], thumbnail = false)
    }

    get("/image/{id}/thumb") {
        respondStoredImage(call, call.parameters["id"], thumbnail = true)
    }

    authenticated {
        get("/crm/products_custom/new") {
            val barcode = call.request.queryParameters["id"]
                ?: call.request.queryParameters["barcode"]
                ?: ""
            if (barcode.isBlank()) {
                call.respondRedirect("/scan")
            } else {
                call.respondHtml {
                    customProductFormPage(
                        data = CustomProductFormData(barcode = barcode),
                        animalOptions = CustomProductService.classificationOptions(animal = true),
                        alcoholOptions = CustomProductService.classificationOptions(animal = false),
                    )
                }
            }
        }

        post("/crm/products_custom/new") {
            val form = call.readProductForm()
            try {
                CustomProductService.submit(
                    CustomProductSubmission(
                        barcode = form.first("id"),
                        name = form.first("name"),
                        brand = form.first("brand"),
                        insightContent = form.first("insightContent"),
                        insightDate = runCatching { LocalDate.parse(form.first("insightDate")) }
                            .getOrDefault(LocalDate.now()),
                        animal = listOfNotNull(form.first("animal").takeIf { it.isNotBlank() }),
                        alcohol = listOfNotNull(form.first("alcohol").takeIf { it.isNotBlank() }),
                        note = form.first("note"),
                        creator = call.userIdentity(),
                        front = form.files["front"],
                    )
                )
                call.respondHtml {
                    template {
                        card {
                            h3(classes = "text-lg font-bold text-gray-900") { +"Vorschlag gesendet" }
                            p(classes = "text-sm text-gray-600") {
                                +"Vielen Dank. Ein Admin prüft den Vorschlag und die Bilder."
                            }
                            a(classes = "mt-3 inline-block text-sm font-semibold text-indigo-600 hover:underline") {
                                href = "/"
                                +"Zurück zur Startseite"
                            }
                        }
                    }
                }
            } catch (e: IllegalArgumentException) {
                call.respondHtml {
                    customProductFormPage(
                        data = form.toFormData(),
                        animalOptions = CustomProductService.classificationOptions(animal = true),
                        alcoholOptions = CustomProductService.classificationOptions(animal = false),
                        error = e.message,
                    )
                }
            }
        }

        route("/crm/products_custom") {
            get {
                ensureAdmin()
                call.respondHtml {
                    customProductAdminPage(
                        proposals = CustomProductService.findProposals(),
                        classificationNames = ClassificationsTable.all().associate { it.id to it.name },
                    )
                }
            }

            get("/manage") {
                ensureAdmin()
                call.respondHtml {
                    customProductListPage(ProductsCustomTable.allProducts())
                }
            }

            get("/history") {
                ensureAdmin()
                call.respondHtml {
                    customProductHistoryPage(
                        ProposalsTable.findHistory(dev.metinkale.halalapp.service.CUSTOM_PRODUCT_ENTITY)
                    )
                }
            }

            get("/{id}/edit") {
                ensureAdmin()
                val id = call.parameters["id"] ?: throw NotFoundException("ID not found")
                val product = ProductsCustomTable.findById(id)
                    ?: throw NotFoundException("Custom-Produkt nicht gefunden")
                val front = product.images.getFrontPhoto(300).takeIf { it != "/nophoto.webp" }
                call.respondHtml {
                    customProductFormPage(
                        data = CustomProductFormData(
                            barcode = product.id,
                            name = product.name,
                            brand = product.brand,
                        ),
                        animalOptions = CustomProductService.classificationOptions(animal = true),
                        alcoholOptions = CustomProductService.classificationOptions(animal = false),
                        editing = true,
                        existingFrontUrl = front,
                    )
                }
            }

            post("/{id}/edit") {
                ensureAdmin()
                val id = call.parameters["id"] ?: throw NotFoundException("ID not found")
                val form = call.readProductForm()
                try {
                    CustomProductService.update(
                        CustomProductEdit(
                            barcode = id,
                            name = form.first("name"),
                            brand = form.first("brand"),
                            front = form.files["front"],
                            removeFront = form.first("removeFront") == "true",
                            editor = call.userIdentity(),
                        )
                    )
                    call.respondRedirect("/crm/products_custom")
                } catch (e: IllegalArgumentException) {
                    val product = ProductsCustomTable.findById(id)
                    call.respondHtml {
                        customProductFormPage(
                            data = CustomProductFormData(
                                barcode = id,
                                name = form.first("name").ifBlank { product?.name.orEmpty() },
                                brand = form.first("brand").ifBlank { product?.brand.orEmpty() },
                            ),
                            animalOptions = CustomProductService.classificationOptions(animal = true),
                            alcoholOptions = CustomProductService.classificationOptions(animal = false),
                            editing = true,
                            error = e.message,
                        )
                    }
                }
            }

            post("/{id}/approve") {
                ensureAdmin()
                val id = call.parameters["id"] ?: throw NotFoundException("ID not found")
                try {
                    CustomProductService.approve(id)
                    call.respondHtml { template { card { +"Custom-Produkt genehmigt." } } }
                } catch (e: IllegalArgumentException) {
                    call.respondText(e.message.orEmpty(), status = HttpStatusCode.BadRequest)
                }
            }

            post("/{id}/reject") {
                ensureAdmin()
                val id = call.parameters["id"] ?: throw NotFoundException("ID not found")
                try {
                    CustomProductService.reject(id)
                    call.respondHtml { template { card { +"Vorschlag abgelehnt." } } }
                } catch (e: IllegalArgumentException) {
                    call.respondText(e.message.orEmpty(), status = HttpStatusCode.BadRequest)
                }
            }
        }
    }
}

private suspend fun ApplicationCall.readProductForm(): ParsedProductForm {
    val fields = mutableMapOf<String, MutableList<String>>()
    val files = mutableMapOf<String, UploadedImageInput>()

    receiveMultipart(formFieldLimit = 128 * 1024).forEachPart { part ->
        when (part) {
            is PartData.FormItem -> {
                val name = part.name ?: ""
                fields.getOrPut(name) { mutableListOf() }.add(part.value)
            }

            is PartData.FileItem -> {
                val name = part.name ?: ""
                if (name == "front" && !part.originalFileName.isNullOrBlank()) {
                    val bytes = part.provider().toInputStream().use {
                        it.readNBytes(ProductImageService.MAX_BYTES + 1)
                    }
                    require(bytes.size <= ProductImageService.MAX_BYTES) {
                        "Ein Bild ist zu groß (maximal 10 MB)."
                    }
                    files[name] = UploadedImageInput(
                        mime = part.contentType?.toString() ?: "image/jpeg",
                        bytes = bytes,
                    )
                }
            }

            else -> Unit
        }
        part.release()
    }

    return ParsedProductForm(
        fields = fields.mapValues { it.value.toList() },
        files = files,
    )
}

private fun ParsedProductForm.toFormData() = CustomProductFormData(
    barcode = first("id"),
    name = first("name"),
    brand = first("brand"),
    insightContent = first("insightContent"),
    insightDate = first("insightDate").ifBlank { LocalDate.now().toString() },
    animal = first("animal"),
    alcohol = first("alcohol"),
    note = first("note"),
)

private fun ApplicationCall.userIdentity(): String {
    val user = principal<HalalAppUser>()
    return user?.email?.takeIf { it.isNotBlank() } ?: user?.username ?: "Unbekannt"
}

private suspend fun respondStoredImage(call: ApplicationCall, productId: String?, thumbnail: Boolean) {
    val id = productId ?: throw NotFoundException("Bild nicht gefunden")
    val images = ProductImagesTable.find(id) ?: throw NotFoundException("Bild nicht gefunden")
    call.respondBytes(
        if (thumbnail) images.first else images.second,
        ContentType.parse("image/jpeg"),
    )
}
