package dev.metinkale.halalapp.service

import dev.metinkale.halalapp.db.entity.IgnoredIngredient
import dev.metinkale.halalapp.db.entity.ProductImages
import dev.metinkale.halalapp.db.entity.ProductOverride
import dev.metinkale.halalapp.db.table.ClassificationsTable
import dev.metinkale.halalapp.db.table.IngredientsTable
import dev.metinkale.halalapp.db.table.ProcessedProductsTable
import dev.metinkale.halalapp.db.table.ProductIgnoredIngredientsTable
import dev.metinkale.halalapp.db.table.ProductImagesTable
import dev.metinkale.halalapp.db.table.ProductOverridesTable
import dev.metinkale.halalapp.db.table.ProductsCustomTable
import dev.metinkale.halalapp.db.table.ProductsOffTable
import dev.metinkale.halalapp.db.table.ProposalsTable
import dev.metinkale.halalapp.domain.RequestStatus
import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.worker.productprocessor.ProductProcessorWorker
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import java.time.Instant

const val PRODUCT_MASTERDATA_ENTITY = "product_masterdata"
const val PRODUCT_INGREDIENT_IGNORE_ENTITY = "product_ingredient_ignore"

enum class ProductChangeType(val entityType: String) {
    MASTERDATA(PRODUCT_MASTERDATA_ENTITY),
    IGNORED_INGREDIENTS(PRODUCT_INGREDIENT_IGNORE_ENTITY),
    ;
}

data class ProductMasterDataSubmission(
    val barcode: String,
    val name: String,
    val brand: String,
    val front: UploadedImageInput?,
    val note: String,
    val creator: String,
)

data class ProductIngredientIgnoreSubmission(
    val barcode: String,
    val ingredientIds: List<String>,
    val reason: String,
    val note: String,
    val creator: String,
)

data class CriticalIngredientView(
    val id: String,
    val name: String,
    val classificationName: String,
    val status: Status,
    val source: String,
)

data class ProductChangeProposalView(
    val proposalId: String,
    val type: ProductChangeType,
    val barcode: String,
    val currentName: String,
    val currentBrand: String,
    val currentImageUrl: String,
    val proposedName: String?,
    val proposedBrand: String?,
    val proposedImageDataUri: String?,
    val ignoredIngredients: List<CriticalIngredientView>,
    val reason: String,
    val note: String,
    val creator: String,
    val date: Instant,
)

data class ManagedIgnoredIngredient(
    val ingredientId: String,
    val name: String,
    val reason: String,
)

data class ManagedProductOverride(
    val barcode: String,
    val sourceName: String,
    val sourceBrand: String,
    val name: String?,
    val brand: String?,
    val imageUrl: String?,
    val ignoredIngredients: List<ManagedIgnoredIngredient>,
)

object ProductChangeService {
    private val logger = LoggerFactory.getLogger(ProductChangeService::class.java)

    fun isOffProduct(barcode: String): Boolean = transaction {
        ProductsOffTable.selectAll()
            .where { ProductsOffTable.id eq barcode }
            .limit(1)
            .any()
    }

    fun currentProduct(barcode: String): Pair<String, String>? {
        val product = ProcessedProductsTable.findById(barcode) ?: return null
        return product.name to product.brand
    }

    private fun requireOffProduct(barcode: String) {
        require(isOffProduct(barcode)) { "Produkt nicht gefunden." }
    }

    fun submitMasterData(input: ProductMasterDataSubmission): String {
        val barcode = input.barcode.trim()
        require(barcode.isNotBlank()) { "Barcode ist erforderlich." }
        requireOffProduct(barcode)
        val current = currentProduct(barcode)
            ?: throw IllegalArgumentException("Produkt nicht gefunden.")
        val override = ProductOverridesTable.find(barcode)
        val currentName = override?.name ?: current.first
        val currentBrand = override?.brand ?: current.second
        val name = input.name.trim()
        val brand = input.brand.trim()
        require(name.isNotBlank()) { "Produktname ist erforderlich." }
        require(brand.isNotBlank()) { "Marke ist erforderlich." }
        val changes = buildMap {
            if (name != currentName) put("name", name)
            if (brand != currentBrand) put("brand", brand)
        }
        val image = input.front?.let {
            ProductImageService.dataUri(ProductImageService.process(it.bytes, it.mime).jpeg)
        }
        if (changes.isEmpty() && image == null) {
            throw IllegalArgumentException("Es wurde keine Änderung eingereicht.")
        }

        requireNoOpenProposal(PRODUCT_MASTERDATA_ENTITY, barcode)

        return transaction {
            ProposalsTable.insertReturning(listOf(ProposalsTable.id)) {
                it[ProposalsTable.entityType] = PRODUCT_MASTERDATA_ENTITY
                it[ProposalsTable.json] = buildMap {
                    put("id", barcode)
                    putAll(changes)
                    image?.let { value -> put("frontImage", value) }
                }
                it[ProposalsTable.note] = input.note.trim()
                it[ProposalsTable.creator] = input.creator
                it[ProposalsTable.status] = RequestStatus.PENDING
                it[ProposalsTable.date] = Instant.now()
            }.first()[ProposalsTable.id].value
        }
    }

    fun submitIgnoredIngredients(input: ProductIngredientIgnoreSubmission): String {
        val barcode = input.barcode.trim()
        require(barcode.isNotBlank()) { "Barcode ist erforderlich." }
        requireOffProduct(barcode)
        require(ProcessedProductsTable.findById(barcode) != null) { "Produkt nicht gefunden." }
        val reason = input.reason.trim()
        require(reason.isNotBlank()) { "Begründung ist erforderlich." }
        val allowed = criticalIngredients(barcode).associateBy { it.id }
        val ids = input.ingredientIds.distinct().filter { it in allowed }
        require(ids.isNotEmpty()) { "Bitte mindestens eine Zutat auswählen." }

        requireNoOpenProposal(PRODUCT_INGREDIENT_IGNORE_ENTITY, barcode)

        return transaction {
            ProposalsTable.insertReturning(listOf(ProposalsTable.id)) {
                it[ProposalsTable.entityType] = PRODUCT_INGREDIENT_IGNORE_ENTITY
                it[ProposalsTable.json] = mapOf(
                    "id" to barcode,
                    "ingredients" to ids.joinToString(";"),
                    "reason" to reason,
                )
                it[ProposalsTable.note] = input.note.trim()
                it[ProposalsTable.creator] = input.creator
                it[ProposalsTable.status] = RequestStatus.PENDING
                it[ProposalsTable.date] = Instant.now()
            }.first()[ProposalsTable.id].value
        }
    }

    fun criticalIngredients(barcode: String): List<CriticalIngredientView> = transaction {
        ClassificationsTable.cacheAll()
        IngredientsTable.cacheAll()
        val off = ProductsOffTable.selectAll().where { ProductsOffTable.id eq barcode }
            .singleOrNull() ?: return@transaction emptyList()
        val ignored = ProductIgnoredIngredientsTable.find(barcode)
            .map { it.ingredientId }
            .toSet()
        val seen = mutableSetOf<String>()
        buildList {
            off[ProductsOffTable.ingredients].forEach { component ->
                addIngredient(component.id, component.name, "Zutat", seen)
            }
            off[ProductsOffTable.allergens].forEach { id ->
                addIngredient(id, ingredientName(id), "Allergen", seen)
            }
            off[ProductsOffTable.traces].forEach { id ->
                addIngredient(id, ingredientName(id), "Spur", seen)
            }
        }.filter { it.id !in ignored }
            .sortedWith(compareBy({ it.status }, { it.name }))
    }

    private fun MutableList<CriticalIngredientView>.addIngredient(
        id: String,
        fallbackName: String,
        source: String,
        seen: MutableSet<String>,
    ) {
        if (id.isBlank() || !seen.add(id)) return
        val ingredient = IngredientsTable.findById(id)
        val classification = ingredient?.classification ?: ClassificationsTable.findById("unknown_ingredient")!!
        if (
            !classification.animal &&
            !classification.alcohol &&
            classification.status != Status.INCOMPLETE
        ) return
        add(
            CriticalIngredientView(
                id = id,
                name = ingredient?.name?.takeIf { it.isNotBlank() } ?: fallbackName,
                classificationName = classification.name,
                status = classification.status,
                source = source,
            )
        )
    }

    private fun ingredientName(id: String): String =
        IngredientsTable.findById(id)?.name ?: id

    fun findProposals(): List<ProductChangeProposalView> = transaction {
        val proposals = ProposalsTable.selectAll().where {
            (ProposalsTable.status eq RequestStatus.PENDING) and
                    (ProposalsTable.entityType inList listOf(
                        PRODUCT_MASTERDATA_ENTITY,
                        PRODUCT_INGREDIENT_IGNORE_ENTITY,
                    ))
        }.orderBy(ProposalsTable.date).map { ProposalsTable.map(it) }

        proposals.mapNotNull { proposal ->
            val barcode = proposal.json["id"].orEmpty()
            if (barcode.isBlank()) return@mapNotNull null
            val product = ProcessedProductsTable.findById(barcode) ?: return@mapNotNull null
            val type = when (proposal.entityType) {
                PRODUCT_MASTERDATA_ENTITY -> ProductChangeType.MASTERDATA
                PRODUCT_INGREDIENT_IGNORE_ENTITY -> ProductChangeType.IGNORED_INGREDIENTS
                else -> return@mapNotNull null
            }
            ProductChangeProposalView(
                proposalId = proposal.id,
                type = type,
                barcode = barcode,
                currentName = product.name,
                currentBrand = product.brand,
                currentImageUrl = product.images.getFrontPhoto(300),
                proposedName = proposal.json["name"],
                proposedBrand = proposal.json["brand"],
                proposedImageDataUri = proposal.json["frontImage"],
                ignoredIngredients = if (type == ProductChangeType.IGNORED_INGREDIENTS) {
                    val allowed = criticalIngredients(barcode).associateBy { it.id }
                    proposal.json["ingredients"].orEmpty()
                        .split(";")
                        .mapNotNull { allowed[it] }
                } else {
                    emptyList()
                },
                reason = proposal.json["reason"].orEmpty(),
                note = proposal.note,
                creator = proposal.creator,
                date = proposal.date,
            )
        }
    }

    fun approve(proposalId: String): String {
        val barcode = transaction {
            val proposal = ProposalsTable.selectAll().where {
                (ProposalsTable.id eq proposalId) and
                        (ProposalsTable.status eq RequestStatus.PENDING) and
                        (ProposalsTable.entityType inList listOf(
                            PRODUCT_MASTERDATA_ENTITY,
                            PRODUCT_INGREDIENT_IGNORE_ENTITY,
                        ))
            }.map { ProposalsTable.map(it) }
                .singleOrNull() ?: throw IllegalArgumentException("Vorschlag nicht gefunden oder bereits bearbeitet.")

            val json = proposal.json
            val barcode = json["id"] ?: throw IllegalArgumentException("Barcode fehlt im Vorschlag.")
            val product = ProcessedProductsTable.findById(barcode)
                ?: throw IllegalArgumentException("Produkt nicht gefunden.")

            when (proposal.entityType) {
                PRODUCT_MASTERDATA_ENTITY -> {
                    val oldValues = buildMap {
                        if (json.containsKey("name")) put("name", product.name)
                        if (json.containsKey("brand")) put("brand", product.brand)
                        if (json.containsKey("frontImage")) put("frontImage", product.images.getFrontPhoto(300))
                    }
                    val version = json["frontImage"]?.let { storeImage(barcode, it) }
                    ProductOverridesTable.upsertFields(
                        id = barcode,
                        name = json["name"]?.takeIf { it.isNotBlank() },
                        brand = json["brand"]?.takeIf { it.isNotBlank() },
                        frontImageVersion = version,
                        actor = proposal.creator,
                    )
                    markApproved(proposalId, oldValues, json)
                }

                PRODUCT_INGREDIENT_IGNORE_ENTITY -> {
                    val reason = json["reason"].orEmpty()
                    val old = ProductIgnoredIngredientsTable.find(barcode)
                        .map { it.ingredientId }
                        .sorted()
                        .joinToString(";")
                    json["ingredients"].orEmpty().split(";").filter { it.isNotBlank() }.forEach { ingredient ->
                        ProductIgnoredIngredientsTable.add(
                            id = barcode,
                            ingredient = ingredient,
                            note = reason,
                            proposal = proposalId,
                            actor = proposal.creator,
                        )
                    }
                    val updated = ProductIgnoredIngredientsTable.find(barcode)
                        .map { it.ingredientId }
                        .sorted()
                        .joinToString(";")
                    markApproved(proposalId, mapOf("id" to barcode, "ignoredIngredients" to old), mapOf("id" to barcode, "ignoredIngredients" to updated))
                }
            }
            barcode
        }
        reprocess(barcode)
        return barcode
    }

    fun reject(proposalId: String) = transaction {
        ProposalsTable.update({
            (ProposalsTable.id eq proposalId) and
                    (ProposalsTable.status eq RequestStatus.PENDING) and
                    (ProposalsTable.entityType inList listOf(
                        PRODUCT_MASTERDATA_ENTITY,
                        PRODUCT_INGREDIENT_IGNORE_ENTITY,
                    ))
        }) {
            it[ProposalsTable.status] = RequestStatus.REJECTED
        }
    }

    fun findHistory(limit: Int = 100) = transaction {
        (ProposalsTable.findHistory(PRODUCT_MASTERDATA_ENTITY, limit) +
                ProposalsTable.findHistory(PRODUCT_INGREDIENT_IGNORE_ENTITY, limit))
            .sortedByDescending { it.date }
    }

    fun managedOverrides(): List<ManagedProductOverride> = transaction {
        val overrides = ProductOverridesTable.allOverrides()
        val ignored = ProductIgnoredIngredientsTable.allByProduct()
        (overrides.keys + ignored.keys).sorted().map { barcode ->
            val override = overrides[barcode]
            val source = ProductsOffTable.selectAll().where { ProductsOffTable.id eq barcode }
                .singleOrNull()
            ManagedProductOverride(
                barcode = barcode,
                sourceName = source?.get(ProductsOffTable.name).orEmpty(),
                sourceBrand = source?.get(ProductsOffTable.brand).orEmpty(),
                name = override?.name,
                brand = override?.brand,
                imageUrl = override?.frontImageVersion?.let { "/image/$barcode?v=$it" },
                ignoredIngredients = ProductIgnoredIngredientsTable.find(barcode).map { entry ->
                    ManagedIgnoredIngredient(
                        ingredientId = entry.ingredientId,
                        name = ingredientName(entry.ingredientId),
                        reason = entry.reason,
                    )
                },
            )
        }
    }

    fun openProposalCount(): Long =
        ProposalsTable.numberOfOpenRequests(PRODUCT_MASTERDATA_ENTITY) +
                ProposalsTable.numberOfOpenRequests(PRODUCT_INGREDIENT_IGNORE_ENTITY)

    fun updateOverride(
        barcode: String,
        name: String,
        brand: String,
        front: UploadedImageInput?,
        actor: String,
    ) {
        validateProductExists(barcode)
        val newName = name.trim().takeIf { it.isNotBlank() }
        val newBrand = brand.trim().takeIf { it.isNotBlank() }
        val imageVersion = front?.let {
            val processed = ProductImageService.process(it.bytes, it.mime)
            val thumbnail = ProductImageService.thumbnail(processed.jpeg)
            ProductImagesTable.put(barcode, thumbnail, processed.jpeg)
            ProductImageService.sha256(thumbnail + processed.jpeg).take(16)
        }
        require(newName != null || newBrand != null || imageVersion != null) {
            "Bitte mindestens ein Feld ausfüllen."
        }
        val old = ProductOverridesTable.find(barcode)
        val oldValues = mutableMapOf("id" to barcode)
        val newValues = mutableMapOf("id" to barcode)
        if (newName != null && newName != old?.name) {
            old?.name?.let { oldValues["name"] = it }
            newValues["name"] = newName
        }
        if (newBrand != null && newBrand != old?.brand) {
            old?.brand?.let { oldValues["brand"] = it }
            newValues["brand"] = newBrand
        }
        if (imageVersion != null && imageVersion != old?.frontImageVersion) {
            old?.frontImageVersion?.let { oldValues["frontImage"] = it }
            newValues["frontImage"] = imageVersion
        }
        transaction {
            ProductOverridesTable.upsertFields(
                id = barcode,
                name = newName,
                brand = newBrand,
                frontImageVersion = imageVersion,
                actor = actor,
            )
            if (newValues.size > 1) {
                ProposalsTable.recordDirectChange(
                    entityType = PRODUCT_MASTERDATA_ENTITY,
                    oldValues = oldValues,
                    newValues = newValues,
                    creator = actor,
                    note = "Direkte Admin-Änderung",
                )
            }
        }
        reprocess(barcode)
    }

    fun clearOverrideField(barcode: String, field: String, actor: String) {
        requireOffProduct(barcode)
        val old = ProductOverridesTable.find(barcode)
            ?: throw IllegalArgumentException("Override nicht gefunden.")
        val oldValues = buildMap {
            put("id", barcode)
            if (field == "name") put("name", old.name.orEmpty())
            if (field == "brand") put("brand", old.brand.orEmpty())
            if (field == "image") put("frontImage", old.frontImageVersion.orEmpty())
        }
        transaction {
            when (field) {
                "name" -> ProductOverridesTable.clearName(barcode, actor)
                "brand" -> ProductOverridesTable.clearBrand(barcode, actor)
                "image" -> {
                    ProductOverridesTable.clearFrontImage(barcode, actor)
                    ProductImagesTable.delete(barcode)
                }
                else -> throw IllegalArgumentException("Unbekanntes Feld.")
            }
            ProposalsTable.recordDirectChange(
                entityType = PRODUCT_MASTERDATA_ENTITY,
                oldValues = oldValues,
                newValues = mapOf("id" to barcode),
                creator = actor,
                note = "Direkte Admin-Änderung",
            )
        }
        reprocess(barcode)
    }

    fun removeIgnoredIngredient(barcode: String, ingredientId: String, actor: String) {
        requireOffProduct(barcode)
        require(ingredientId.isNotBlank()) { "Zutat fehlt." }
        val old = ProductIgnoredIngredientsTable.find(barcode).map { it.ingredientId }.sorted().joinToString(";")
        transaction {
            ProductIgnoredIngredientsTable.remove(barcode, ingredientId)
            val new = ProductIgnoredIngredientsTable.find(barcode).map { it.ingredientId }.sorted().joinToString(";")
            ProposalsTable.recordDirectChange(
                entityType = PRODUCT_INGREDIENT_IGNORE_ENTITY,
                oldValues = mapOf("id" to barcode, "ignoredIngredients" to old),
                newValues = mapOf("id" to barcode, "ignoredIngredients" to new),
                creator = actor,
                note = "Direkte Admin-Änderung",
            )
        }
        reprocess(barcode)
    }

    fun deleteOverride(barcode: String, actor: String) {
        requireOffProduct(barcode)
        val old = ProductOverridesTable.find(barcode)
        val oldIgnored = ProductIgnoredIngredientsTable.find(barcode).map { it.ingredientId }.sorted().joinToString(";")
        transaction {
            if (old != null) {
                ProposalsTable.recordDirectChange(
                    entityType = PRODUCT_MASTERDATA_ENTITY,
                    oldValues = buildMap {
                        put("id", barcode)
                        old.name?.let { put("name", it) }
                        old.brand?.let { put("brand", it) }
                        old.frontImageVersion?.let { put("frontImage", it) }
                    },
                    newValues = mapOf("id" to barcode),
                    creator = actor,
                    note = "Override entfernt",
                )
            }
            if (oldIgnored.isNotBlank()) {
                ProposalsTable.recordDirectChange(
                    entityType = PRODUCT_INGREDIENT_IGNORE_ENTITY,
                    oldValues = mapOf("id" to barcode, "ignoredIngredients" to oldIgnored),
                    newValues = mapOf("id" to barcode),
                    creator = actor,
                    note = "Override entfernt",
                )
            }
            ProductOverridesTable.deleteOverride(barcode)
            ProductIgnoredIngredientsTable.deleteForProduct(barcode)
            ProductImagesTable.delete(barcode)
        }
        reprocess(barcode)
    }

    fun validateProductExists(barcode: String) {
        requireOffProduct(barcode)
    }

    private fun requireNoOpenProposal(entityType: String, barcode: String) {
        val open = ProposalsTable.findOpenRequests(entityType)
        require(open.none { it.json["id"] == barcode }) {
            "Für dieses Produkt gibt es bereits einen offenen Vorschlag."
        }
    }

    private fun storeImage(barcode: String, dataUri: String): String {
        val full = ProductImageService.decodeDataUri(dataUri)
        val thumbnail = ProductImageService.thumbnail(full)
        ProductImagesTable.put(barcode, thumbnail, full)
        return ProductImageService.sha256(thumbnail + full).take(16)
    }

    private fun markApproved(
        proposalId: String,
        oldValues: Map<String, String>,
        newValues: Map<String, String>,
    ) {
        ProposalsTable.update({ ProposalsTable.id eq proposalId }) {
            it[ProposalsTable.status] = RequestStatus.APPROVED
            it[ProposalsTable.oldJson] = oldValues
            it[ProposalsTable.json] = newValues
        }
    }

    private fun reprocess(barcode: String) {
        runCatching { ProductProcessorWorker.processOne(barcode) }
            .onFailure { logger.error("Could not process product $barcode", it) }
    }
}
