package dev.metinkale.halalapp.routes

import dev.metinkale.halalapp.db.entity.ProductImageKind
import dev.metinkale.halalapp.db.entity.ProductImageStatus
import dev.metinkale.halalapp.db.table.ClassificationsTable
import dev.metinkale.halalapp.db.table.ProductImageFilesTable
import dev.metinkale.halalapp.db.table.ProductImagesTable
import dev.metinkale.halalapp.db.table.ProductsCustomTable
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
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondRedirect
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
        respondImage(call, call.parameters["id"], allowPending = false)
    }

    authenticated {
        get("/crm/image/{id}") {
            ensureAdmin()
            respondImage(call, call.parameters["id"], allowPending = true)
        }

        get("/crm/products_custom/new") {
            val barcode = call.request.queryParameters["id"]
                ?: call.request.queryParameters["barcode"]
                ?: ""
            call.respondHtml {
                customProductFormPage(
                    data = CustomProductFormData(barcode = barcode),
                    animalOptions = CustomProductService.classificationOptions(animal = true),
                    alcoholOptions = CustomProductService.classificationOptions(animal = false),
                )
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
                        ingredients = form.files["ingredients"],
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
                        barcodeReadOnly = false,
                        error = e.message,
                    )
                }
            }
        }

        route("/crm/products_custom") {
            get {
                ensureAdmin()
                CustomProductService.cleanupOldImages()
                call.respondHtml {
                    customProductAdminPage(
                        proposals = CustomProductService.findProposals(),
                        products = ProductsCustomTable.allProducts(),
                        classificationNames = ClassificationsTable.all().associate { it.id to it.name },
                    )
                }
            }

            get("/{id}/edit") {
                ensureAdmin()
                val id = call.parameters["id"] ?: throw NotFoundException("ID not found")
                val product = ProductsCustomTable.findById(id)
                    ?: throw NotFoundException("Custom-Produkt nicht gefunden")
                val images = ProductImagesTable.findByProduct(id)
                val front = images.firstOrNull { it.kind == ProductImageKind.FRONT }?.let { image ->
                    ProductImageFilesTable.metadataByImage(image.id).minByOrNull { it.second }
                        ?.let { "/image/${it.first}" }
                }
                val ingredients = images.firstOrNull { it.kind == ProductImageKind.INGREDIENTS }?.let { image ->
                    ProductImageFilesTable.metadataByImage(image.id).minByOrNull { it.second }
                        ?.let { "/image/${it.first}" }
                }
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
                        existingIngredientsUrl = ingredients,
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
                            ingredients = form.files["ingredients"],
                            removeFront = form.first("removeFront") == "true",
                            removeIngredients = form.first("removeIngredients") == "true",
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
                    call.respondRedirect("/crm/products_custom")
                } catch (e: IllegalArgumentException) {
                    call.respondHtml {
                        template {
                            card {
                                h3(classes = "text-lg font-bold text-red-700") { +"Genehmigung fehlgeschlagen" }
                                p(classes = "text-sm text-gray-600") { +(e.message.orEmpty()) }
                                a(classes = "mt-3 inline-block text-sm font-semibold text-indigo-600 hover:underline") {
                                    href = "/crm/products_custom"
                                    +"Zurück"
                                }
                            }
                        }
                    }
                }
            }

            post("/{id}/reject") {
                ensureAdmin()
                val id = call.parameters["id"] ?: throw NotFoundException("ID not found")
                CustomProductService.reject(id)
                call.respondRedirect("/crm/products_custom")
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
                if ((name == "front" || name == "ingredients") && !part.originalFileName.isNullOrBlank()) {
                    val bytes = part.provider().toInputStream().use {
                        it.readNBytes(ProductImageService.MAX_BYTES + 1)
                    }
                    require(bytes.size <= ProductImageService.MAX_BYTES) {
                        "Ein Bild ist zu groß (maximal 10 MB)."
                    }
                    files[name] = UploadedImageInput(
                        kind = if (name == "front") ProductImageKind.FRONT else ProductImageKind.INGREDIENTS,
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

private suspend fun respondImage(call: ApplicationCall, fileId: String?, allowPending: Boolean) {
    val id = fileId ?: throw NotFoundException("Bild nicht gefunden")
    val file = ProductImageFilesTable.findById(id) ?: throw NotFoundException("Bild nicht gefunden")
    val image = ProductImagesTable.findById(file.imageId) ?: throw NotFoundException("Bild nicht gefunden")

    val visible = if (allowPending) {
        image.status in setOf(ProductImageStatus.ATTACHED, ProductImageStatus.PENDING)
    } else {
        image.status == ProductImageStatus.ATTACHED
    }
    if (!visible) throw NotFoundException("Bild nicht gefunden")

    call.response.header(HttpHeaders.ETag, "\"${file.sha256}\"")
    call.respondBytes(file.data, ContentType.parse(file.mime))
}
