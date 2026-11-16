package dev.metinkale.halalapp.db.table

import dev.metinkale.halalapp.db.entity.ProductImage
import dev.metinkale.halalapp.db.entity.ProductImageFile
import dev.metinkale.halalapp.db.entity.ProductImageKind
import dev.metinkale.halalapp.db.entity.ProductImageStatus
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant

object ProductImagesTable : IdTable<String>("product_images") {
    override val id = text("id").entityId()
    override val primaryKey = PrimaryKey(id)

    val proposalId = text("proposal_id").nullable()
    val productId = text("product_id").nullable()
    val kind = enumerationByName("kind", 20, ProductImageKind::class)
    val status = enumerationByName("status", 20, ProductImageStatus::class)
    val originalMime = text("original_mime")
    val originalWidth = integer("original_width")
    val originalHeight = integer("original_height")
    val originalBytes = integer("original_bytes")
    val sha256 = text("sha256")
    val createdAt = timestamp("created_at")
    val createdBy = text("created_by")

    private fun map(row: ResultRow) = ProductImage(
        id = row[id].value,
        proposalId = row[proposalId],
        productId = row[productId],
        kind = row[kind],
        status = row[status],
        originalMime = row[originalMime],
        originalWidth = row[originalWidth],
        originalHeight = row[originalHeight],
        originalBytes = row[originalBytes],
        sha256 = row[sha256],
        createdAt = row[createdAt],
        createdBy = row[createdBy],
    )

    fun findById(id: String): ProductImage? = transaction {
        selectAll().where { ProductImagesTable.id eq id }.singleOrNull()?.let { map(it) }
    }

    fun findByProposal(proposalId: String): List<ProductImage> = transaction {
        selectAll().where { ProductImagesTable.proposalId eq proposalId }
            .orderBy(createdAt to SortOrder.ASC)
            .map { map(it) }
    }

    fun findByProduct(productId: String): List<ProductImage> = transaction {
        selectAll().where {
            (ProductImagesTable.productId eq productId) and
                    (status inList listOf(ProductImageStatus.ATTACHED, ProductImageStatus.PENDING))
        }.orderBy(createdAt to SortOrder.ASC).map { map(it) }
    }

    fun attachToProduct(imageIds: List<String>, productId: String) = transaction {
        if (imageIds.isEmpty()) return@transaction
        update({ ProductImagesTable.id inList imageIds }) {
            it[this.productId] = productId
            it[status] = ProductImageStatus.ATTACHED
        }
    }

    fun markStatus(imageIds: List<String>, status: ProductImageStatus) = transaction {
        if (imageIds.isEmpty()) return@transaction
        update({ ProductImagesTable.id inList imageIds }) {
            it[this.status] = status
        }
    }

    fun cleanupOldRejected(cutoff: Instant = Instant.now().minusSeconds(30L * 24 * 60 * 60)) = transaction {
        val ids = selectAll().where {
            (status inList listOf(ProductImageStatus.REJECTED, ProductImageStatus.REPLACED)) and
                    (createdAt less cutoff)
        }.map { it[ProductImagesTable.id].value }

        if (ids.isNotEmpty()) {
            ProductImageFilesTable.deleteWhere { ProductImageFilesTable.imageId inList ids }
            deleteWhere { ProductImagesTable.id inList ids }
        }
        ids.size
    }
}

object ProductImageFilesTable : IdTable<String>("product_image_files") {
    override val id = text("id").entityId()
    override val primaryKey = PrimaryKey(id)

    val imageId = text("image_id")
    val width = integer("width")
    val mime = text("mime")
    val byteSize = integer("byte_size")
    val sha256 = text("sha256")
    val data = binary("data")

    private fun map(row: ResultRow) = ProductImageFile(
        id = row[id].value,
        imageId = row[imageId],
        width = row[width],
        mime = row[mime],
        byteSize = row[byteSize],
        sha256 = row[sha256],
        data = row[data],
    )

    fun findById(id: String): ProductImageFile? = transaction {
        selectAll().where { ProductImageFilesTable.id eq id }.singleOrNull()?.let { map(it) }
    }

    fun metadataByImage(imageId: String): List<Pair<String, Int>> = transaction {
        selectAll()
            .where { ProductImageFilesTable.imageId eq imageId }
            .orderBy(width to SortOrder.ASC)
            .map { it[ProductImageFilesTable.id].value to it[ProductImageFilesTable.width] }
    }
}
