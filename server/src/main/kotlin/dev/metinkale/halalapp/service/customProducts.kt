package dev.metinkale.halalapp.service

import dev.metinkale.halalapp.db.entity.CustomProductProposal
import dev.metinkale.halalapp.db.entity.ProductComponent
import dev.metinkale.halalapp.db.entity.ProductImage
import dev.metinkale.halalapp.db.entity.ProductImagePreview
import dev.metinkale.halalapp.db.entity.ProductImageKind
import dev.metinkale.halalapp.db.entity.ProductImageStatus
import dev.metinkale.halalapp.db.entity.ProductImages
import dev.metinkale.halalapp.db.table.ClassificationsTable
import dev.metinkale.halalapp.db.table.ProductImageFilesTable
import dev.metinkale.halalapp.db.table.ProductImagesTable
import dev.metinkale.halalapp.db.table.ProductInsightsTable
import dev.metinkale.halalapp.db.table.ProductsCustomTable
import dev.metinkale.halalapp.db.table.ProductsOffTable
import dev.metinkale.halalapp.db.table.ProposalsTable
import dev.metinkale.halalapp.domain.RequestStatus
import dev.metinkale.halalapp.worker.productprocessor.ProductProcessorWorker
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

const val CUSTOM_PRODUCT_ENTITY = "products_custom"

data class UploadedImageInput(
    val kind: ProductImageKind,
    val mime: String,
    val bytes: ByteArray,
)

data class CustomProductSubmission(
    val barcode: String,
    val name: String,
    val brand: String,
    val category: String?,
    val insightContent: String,
    val insightDate: LocalDate,
    val animal: List<String>,
    val alcohol: List<String>,
    val note: String,
    val creator: String,
    val front: UploadedImageInput?,
    val ingredients: UploadedImageInput?,
)

data class CustomProductEdit(
    val barcode: String,
    val name: String,
    val brand: String,
    val category: String?,
    val front: UploadedImageInput?,
    val ingredients: UploadedImageInput?,
    val removeFront: Boolean,
    val removeIngredients: Boolean,
    val editor: String,
)

object CustomProductService {
    private val logger = LoggerFactory.getLogger(CustomProductService::class.java)

    fun validateBarcode(raw: String): String {
        val barcode = raw.trim()
        require(barcode.length in setOf(8, 12, 13, 14) && barcode.all { it.isDigit() }) {
            "Der Barcode muss aus 8, 12, 13 oder 14 Ziffern bestehen."
        }
        require(isValidGtin(barcode)) { "Der Barcode hat eine ungültige Prüfziffer." }
        return barcode
    }

    fun submit(input: CustomProductSubmission): String {
        val barcode = validateBarcode(input.barcode)
        val name = input.name.trim().also { require(it.isNotBlank()) { "Produktname ist erforderlich." } }
        val brand = input.brand.trim().also { require(it.isNotBlank()) { "Marke ist erforderlich." } }
        val content = input.insightContent.trim().also { require(it.isNotBlank()) { "Produktinformation ist erforderlich." } }
        val category = input.category?.trim()?.takeIf { it.isNotBlank() }
        val animal = sanitizeClassifications(input.animal, animal = true)
        val alcohol = sanitizeClassifications(input.alcohol, animal = false)

        require(!ProductsOffTable.exists(barcode)) {
            "Dieses Produkt existiert bereits bei OpenFoodFacts. Bitte reiche stattdessen eine Produktinformation über die Produktseite ein."
        }
        require(ProductsCustomTable.findById(barcode) == null) {
            "Dieses Produkt existiert bereits in der HalalApp."
        }
        val pending = ProposalsTable.findOpenRequests(CUSTOM_PRODUCT_ENTITY)
        require(pending.none { it.json["id"] == barcode }) {
            "Für dieses Produkt gibt es bereits einen offenen Vorschlag."
        }

        val front = input.front?.let { it to ProductImageService.process(it.bytes, it.mime) }
        val ingredients = input.ingredients?.let { it to ProductImageService.process(it.bytes, it.mime) }

        return transaction {
            val json = mapOf(
                "id" to barcode,
                "name" to name,
                "brand" to brand,
                "category" to (category ?: ""),
                "insightContent" to content,
                "insightDate" to input.insightDate.toString(),
                "animal" to animal.joinToString(";"),
                "alcohol" to alcohol.joinToString(";"),
            )

            val proposalId = ProposalsTable.insertReturning(listOf(ProposalsTable.id)) {
                it[ProposalsTable.entityType] = CUSTOM_PRODUCT_ENTITY
                it[ProposalsTable.json] = json
                it[ProposalsTable.note] = input.note.trim()
                it[ProposalsTable.creator] = input.creator
                it[ProposalsTable.status] = RequestStatus.PENDING
                it[ProposalsTable.date] = Instant.now()
            }.first()[ProposalsTable.id].value

            front?.let {
                insertImage(
                    proposalId = proposalId,
                    productId = null,
                    kind = ProductImageKind.FRONT,
                    status = ProductImageStatus.PENDING,
                    upload = it.second,
                    createdBy = input.creator,
                )
            }
            ingredients?.let {
                insertImage(
                    proposalId = proposalId,
                    productId = null,
                    kind = ProductImageKind.INGREDIENTS,
                    status = ProductImageStatus.PENDING,
                    upload = it.second,
                    createdBy = input.creator,
                )
            }

            proposalId
        }
    }

    fun findProposals(status: RequestStatus? = RequestStatus.PENDING): List<CustomProductProposal> {
        val proposals = transaction {
            ProposalsTable.selectAll().where {
                (ProposalsTable.entityType eq CUSTOM_PRODUCT_ENTITY).let {
                    if (status != null) it.and(ProposalsTable.status eq status) else it
                }
            }.map { ProposalsTable.map(it) }
        }
        return proposals.map { proposal ->
            val json = proposal.json
            CustomProductProposal(
                proposalId = proposal.id,
                barcode = json["id"].orEmpty(),
                name = json["name"].orEmpty(),
                brand = json["brand"].orEmpty(),
                category = json["category"]?.takeIf { it.isNotBlank() },
                insightContent = json["insightContent"].orEmpty(),
                insightDate = runCatching { LocalDate.parse(json["insightDate"]) }.getOrDefault(LocalDate.now()),
                animal = json["animal"]?.split(";")?.filter { it.isNotBlank() }.orEmpty(),
                alcohol = json["alcohol"]?.split(";")?.filter { it.isNotBlank() }.orEmpty(),
                note = proposal.note,
                creator = proposal.creator,
                status = proposal.status,
                date = proposal.date,
                imagePreviews = ProductImagesTable.findByProposal(proposal.id).map { image ->
                    ProductImagePreview(image, ProductImageFilesTable.metadataByImage(image.id))
                },
            )
        }
    }

    fun findProposal(proposalId: String): CustomProductProposal? =
        findProposals(null).firstOrNull { it.proposalId == proposalId }

    fun approve(proposalId: String): String {
        val barcode = transaction {
            val proposal = ProposalsTable.selectAll().where {
                (ProposalsTable.id eq proposalId) and
                        (ProposalsTable.entityType eq CUSTOM_PRODUCT_ENTITY) and
                        (ProposalsTable.status eq RequestStatus.PENDING)
            }.singleOrNull() ?: throw IllegalArgumentException("Vorschlag nicht gefunden oder bereits bearbeitet.")

            val json = proposal[ProposalsTable.json]
            val barcode = json["id"] ?: throw IllegalArgumentException("Barcode fehlt im Vorschlag.")
            require(!ProductsOffTable.exists(barcode)) {
                "Dieses Produkt existiert inzwischen bei OpenFoodFacts und kann nicht als Custom-Produkt angelegt werden."
            }
            require(ProductsCustomTable.findById(barcode) == null) {
                "Dieses Produkt existiert inzwischen bereits in der HalalApp."
            }

            val images = ProductImagesTable.findByProposal(proposalId)
            val imageMap = images.map { it.kind to it.id }.toMap()

            ProductsCustomTable.insert {
                it[ProductsCustomTable.id] = barcode
                it[ProductsCustomTable.name] = json["name"].orEmpty()
                it[ProductsCustomTable.brand] = json["brand"].orEmpty()
                it[ProductsCustomTable.ingredients] = emptyList<ProductComponent>()
                it[ProductsCustomTable.ingredientsText] = ""
                it[ProductsCustomTable.images] = ProductImages(
                    front = imageUrlsFor(images, ProductImageKind.FRONT),
                    ingredients = imageUrlsFor(images, ProductImageKind.INGREDIENTS),
                )
                it[ProductsCustomTable.tags] = emptyList()
                it[ProductsCustomTable.traces] = emptyList()
                it[ProductsCustomTable.allergens] = emptyList()
                it[ProductsCustomTable.category] = json["category"]?.takeIf { value -> value.isNotBlank() }
            }

            ProductInsightsTable.insert {
                it[ProductInsightsTable.product_id] = barcode
                it[ProductInsightsTable.content] = json["insightContent"].orEmpty()
                it[ProductInsightsTable.date] = runCatching { LocalDate.parse(json["insightDate"]) }
                    .getOrDefault(LocalDate.now())
                it[ProductInsightsTable.animal] =
                    json["animal"]?.split(";")?.filter { id -> id.isNotBlank() }.orEmpty()
                it[ProductInsightsTable.alcohol] =
                    json["alcohol"]?.split(";")?.filter { id -> id.isNotBlank() }.orEmpty()
            }

            val imageIds = imageMap.values.toList()
            ProductImagesTable.attachToProduct(imageIds, barcode)
            ProposalsTable.update({ ProposalsTable.id eq proposalId }) {
                it[ProposalsTable.status] = RequestStatus.APPROVED
            }
            barcode
        }
        reprocess(barcode)
        return barcode
    }

    fun reject(proposalId: String) {
        transaction {
            val images = ProductImagesTable.findByProposal(proposalId)
            val updated = ProposalsTable.update({
                (ProposalsTable.id eq proposalId) and
                        (ProposalsTable.entityType eq CUSTOM_PRODUCT_ENTITY) and
                        (ProposalsTable.status eq RequestStatus.PENDING)
            }) {
                it[ProposalsTable.status] = RequestStatus.REJECTED
            }
            if (updated > 0) {
                ProductImagesTable.markStatus(images.map { it.id }, ProductImageStatus.REJECTED)
            }
        }
    }

    fun update(input: CustomProductEdit): String {
        val barcode = validateBarcode(input.barcode)
        val name = input.name.trim().also { require(it.isNotBlank()) { "Produktname ist erforderlich." } }
        val brand = input.brand.trim().also { require(it.isNotBlank()) { "Marke ist erforderlich." } }
        val category = input.category?.trim()?.takeIf { it.isNotBlank() }
        val frontUpload = input.front?.let { ProductImageService.process(it.bytes, it.mime) }
        val ingredientsUpload = input.ingredients?.let { ProductImageService.process(it.bytes, it.mime) }

        transaction {
            ProductsCustomTable.selectAll().where { ProductsCustomTable.id eq barcode }
                .singleOrNull() ?: throw IllegalArgumentException("Custom-Produkt nicht gefunden.")
            val existingImages = ProductImagesTable.findByProduct(barcode)

            var front = existingImages.filter { it.kind == ProductImageKind.FRONT }
            var ingredientImages = existingImages.filter { it.kind == ProductImageKind.INGREDIENTS }

            if (input.removeFront || frontUpload != null) {
                ProductImagesTable.markStatus(front.map { it.id }, ProductImageStatus.REPLACED)
                front = emptyList()
            }
            if (input.removeIngredients || ingredientsUpload != null) {
                ProductImagesTable.markStatus(ingredientImages.map { it.id }, ProductImageStatus.REPLACED)
                ingredientImages = emptyList()
            }

            val frontUrls = imageUrlsFor(front, ProductImageKind.FRONT) +
                    (frontUpload?.let {
                        insertImage(
                            proposalId = null,
                            productId = barcode,
                            kind = ProductImageKind.FRONT,
                            status = ProductImageStatus.ATTACHED,
                            upload = it,
                            createdBy = input.editor,
                        )
                    } ?: emptyMap())

            val ingredientUrls = imageUrlsFor(ingredientImages, ProductImageKind.INGREDIENTS) +
                    (ingredientsUpload?.let {
                        insertImage(
                            proposalId = null,
                            productId = barcode,
                            kind = ProductImageKind.INGREDIENTS,
                            status = ProductImageStatus.ATTACHED,
                            upload = it,
                            createdBy = input.editor,
                        )
                    } ?: emptyMap())

            ProductsCustomTable.update({ ProductsCustomTable.id eq barcode }) {
                it[ProductsCustomTable.name] = name
                it[ProductsCustomTable.brand] = brand
                it[ProductsCustomTable.category] = category
                it[ProductsCustomTable.images] = ProductImages(frontUrls, ingredientUrls)
            }
        }
        reprocess(barcode)
        return barcode
    }

    fun cleanupOldImages(): Int = ProductImagesTable.cleanupOldRejected()

    fun classificationOptions(animal: Boolean): List<Pair<String, String>> =
        ClassificationsTable.all()
            .filter { if (animal) it.animal else it.alcohol }
            .sortedBy { it.status }
            .map { it.id to "${it.name} (${it.status.value})" }

    fun classificationNames(ids: List<String>): List<String> =
        ids.mapNotNull { ClassificationsTable.findById(it)?.name }

    private fun sanitizeClassifications(ids: List<String>, animal: Boolean): List<String> =
        ids.distinct().filter { id ->
            ClassificationsTable.findById(id)?.let { if (animal) it.animal else it.alcohol } == true
        }

    private fun imageUrlsFor(images: List<ProductImage>, kind: ProductImageKind): Map<Int, String> =
        images.filter { it.kind == kind }
            .flatMap { image ->
                ProductImageFilesTable.metadataByImage(image.id).map { (fileId, width) ->
                    width to "/image/$fileId"
                }
            }
            .toMap()

    private fun insertImage(
        proposalId: String?,
        productId: String?,
        kind: ProductImageKind,
        status: ProductImageStatus,
        upload: ProcessedImageUpload,
        createdBy: String,
    ): Map<Int, String> {
        val imageId = UUID.randomUUID().toString()
        ProductImagesTable.insert {
            it[ProductImagesTable.id] = imageId
            it[ProductImagesTable.proposalId] = proposalId
            it[ProductImagesTable.productId] = productId
            it[ProductImagesTable.kind] = kind
            it[ProductImagesTable.status] = status
            it[ProductImagesTable.originalMime] = upload.originalMime
            it[ProductImagesTable.originalWidth] = upload.originalWidth
            it[ProductImagesTable.originalHeight] = upload.originalHeight
            it[ProductImagesTable.originalBytes] = upload.originalBytes
            it[ProductImagesTable.sha256] = upload.sha256
            it[ProductImagesTable.createdAt] = Instant.now()
            it[ProductImagesTable.createdBy] = createdBy
        }

        return upload.variants.associate { variant ->
            val fileId = UUID.randomUUID().toString()
            ProductImageFilesTable.insert {
                it[ProductImageFilesTable.id] = fileId
                it[ProductImageFilesTable.imageId] = imageId
                it[ProductImageFilesTable.width] = variant.width
                it[ProductImageFilesTable.mime] = variant.mime
                it[ProductImageFilesTable.byteSize] = variant.bytes.size
                it[ProductImageFilesTable.sha256] = variant.sha256
                it[ProductImageFilesTable.data] = variant.bytes
            }
            variant.width to "/image/$fileId"
        }
    }

    private fun reprocess(barcode: String) {
        runCatching { ProductProcessorWorker.processOne(barcode) }
            .onFailure { logger.error("Could not process custom product $barcode", it) }
    }

    private fun isValidGtin(code: String): Boolean {
        if (code.length < 8) return false
        val sum = code.dropLast(1).reversed().mapIndexed { index, char ->
            char.digitToInt() * if (index % 2 == 0) 3 else 1
        }.sum()
        val checkDigit = (10 - sum % 10) % 10
        return checkDigit == code.last().digitToInt()
    }
}
