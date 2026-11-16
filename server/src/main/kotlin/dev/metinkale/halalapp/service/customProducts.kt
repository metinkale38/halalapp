package dev.metinkale.halalapp.service

import dev.metinkale.halalapp.db.entity.CustomProductProposal
import dev.metinkale.halalapp.db.entity.ProductComponent
import dev.metinkale.halalapp.db.entity.ProductImages
import dev.metinkale.halalapp.db.table.ClassificationsTable
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

const val CUSTOM_PRODUCT_ENTITY = "products_custom"

data class UploadedImageInput(
    val mime: String,
    val bytes: ByteArray,
)

data class CustomProductSubmission(
    val barcode: String,
    val name: String,
    val brand: String,
    val insightContent: String,
    val insightDate: LocalDate,
    val animal: List<String>,
    val alcohol: List<String>,
    val note: String,
    val creator: String,
    val front: UploadedImageInput?,
)

data class CustomProductEdit(
    val barcode: String,
    val name: String,
    val brand: String,
    val front: UploadedImageInput?,
    val removeFront: Boolean,
    val editor: String,
)

object CustomProductService {
    private val logger = LoggerFactory.getLogger(CustomProductService::class.java)

    fun validateBarcode(raw: String): String {
        val barcode = raw.trim()
        require(barcode.isNotBlank()) { "Barcode ist erforderlich." }
        return barcode
    }

    fun submit(input: CustomProductSubmission): String {
        val barcode = validateBarcode(input.barcode)
        val name = input.name.trim().also { require(it.isNotBlank()) { "Produktname ist erforderlich." } }
        val brand = input.brand.trim().also { require(it.isNotBlank()) { "Marke ist erforderlich." } }
        val content = input.insightContent.trim().also { require(it.isNotBlank()) { "Produktinformation ist erforderlich." } }
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

        val image = input.front?.let {
            ProductImageService.dataUri(ProductImageService.process(it.bytes, it.mime).jpeg)
        }

        return transaction {
            val json = buildMap {
                put("id", barcode)
                put("name", name)
                put("brand", brand)
                put("insightContent", content)
                put("insightDate", input.insightDate.toString())
                put("animal", animal.joinToString(";"))
                put("alcohol", alcohol.joinToString(";"))
                image?.let { put("frontImage", it) }
            }

            ProposalsTable.insertReturning(listOf(ProposalsTable.id)) {
                it[ProposalsTable.entityType] = CUSTOM_PRODUCT_ENTITY
                it[ProposalsTable.json] = json
                it[ProposalsTable.note] = input.note.trim()
                it[ProposalsTable.creator] = input.creator
                it[ProposalsTable.status] = RequestStatus.PENDING
                it[ProposalsTable.date] = Instant.now()
            }.first()[ProposalsTable.id].value
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
                insightContent = json["insightContent"].orEmpty(),
                insightDate = runCatching { LocalDate.parse(json["insightDate"]) }.getOrDefault(LocalDate.now()),
                animal = json["animal"]?.split(";")?.filter { it.isNotBlank() }.orEmpty(),
                alcohol = json["alcohol"]?.split(";")?.filter { it.isNotBlank() }.orEmpty(),
                note = proposal.note,
                creator = proposal.creator,
                status = proposal.status,
                date = proposal.date,
                imageDataUri = json["frontImage"],
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

            val images = json["frontImage"]?.let { dataUri ->
                val full = ProductImageService.decodeDataUri(dataUri)
                val thumbnail = ProductImageService.thumbnail(full)
                ProductImagesTable.put(barcode, thumbnail, full)
                productImagesJson(barcode, thumbnail, full)
            } ?: ProductImages()

            ProductsCustomTable.insert {
                it[ProductsCustomTable.id] = barcode
                it[ProductsCustomTable.name] = json["name"].orEmpty()
                it[ProductsCustomTable.brand] = json["brand"].orEmpty()
                it[ProductsCustomTable.ingredients] = emptyList<ProductComponent>()
                it[ProductsCustomTable.ingredientsText] = ""
                it[ProductsCustomTable.images] = images
                it[ProductsCustomTable.tags] = emptyList()
                it[ProductsCustomTable.traces] = emptyList()
                it[ProductsCustomTable.allergens] = emptyList()
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
            ProposalsTable.update({
                (ProposalsTable.id eq proposalId) and
                        (ProposalsTable.entityType eq CUSTOM_PRODUCT_ENTITY) and
                        (ProposalsTable.status eq RequestStatus.PENDING)
            }) {
                it[ProposalsTable.status] = RequestStatus.REJECTED
            }
        }
    }

    fun update(input: CustomProductEdit): String {
        val barcode = validateBarcode(input.barcode)
        val name = input.name.trim().also { require(it.isNotBlank()) { "Produktname ist erforderlich." } }
        val brand = input.brand.trim().also { require(it.isNotBlank()) { "Marke ist erforderlich." } }
        val upload = input.front?.let { ProductImageService.process(it.bytes, it.mime) }

        transaction {
            val existingRow = ProductsCustomTable.selectAll().where { ProductsCustomTable.id eq barcode }
                .singleOrNull() ?: throw IllegalArgumentException("Custom-Produkt nicht gefunden.")
            val oldValues = mapOf(
                "id" to barcode,
                "name" to existingRow[ProductsCustomTable.name],
                "brand" to existingRow[ProductsCustomTable.brand],
            )

            val newImages: ProductImages? = when {
                input.removeFront -> {
                    ProductImagesTable.delete(barcode)
                    ProductImages()
                }

                upload != null -> {
                    val thumbnail = ProductImageService.thumbnail(upload.jpeg)
                    ProductImagesTable.put(barcode, thumbnail, upload.jpeg)
                    productImagesJson(barcode, thumbnail, upload.jpeg)
                }

                else -> null
            }

            ProductsCustomTable.update({ ProductsCustomTable.id eq barcode }) {
                it[ProductsCustomTable.name] = name
                it[ProductsCustomTable.brand] = brand
                newImages?.let { images -> it[ProductsCustomTable.images] = images }
            }

            ProposalsTable.recordDirectChange(
                entityType = CUSTOM_PRODUCT_ENTITY,
                oldValues = oldValues,
                newValues = mapOf("id" to barcode, "name" to name, "brand" to brand),
                creator = input.editor,
                note = "Direkte Admin-Änderung",
            )
        }
        reprocess(barcode)
        return barcode
    }

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

    private fun productImagesJson(productId: String, thumbnail: ByteArray, full: ByteArray): ProductImages {
        val version = ProductImageService.sha256(thumbnail + full).take(16)
        return ProductImages(
            front = mapOf(
                0 to "/image/$productId/thumb?v=$version",
                300 to "/image/$productId?v=$version",
            )
        )
    }

    private fun reprocess(barcode: String) {
        runCatching { ProductProcessorWorker.processOne(barcode) }
            .onFailure { logger.error("Could not process custom product $barcode", it) }
    }
}
