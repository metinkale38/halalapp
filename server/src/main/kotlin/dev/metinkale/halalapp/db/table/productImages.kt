package dev.metinkale.halalapp.db.table

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert

object ProductImagesTable : IdTable<String>("product_images") {
    override val id = text("product_id").entityId()
    override val primaryKey = PrimaryKey(id)

    val thumbnailImage = binary("thumbnail_image")
    val fullImage = binary("full_image")

    fun put(productId: String, thumbnail: ByteArray, full: ByteArray) = transaction {
        upsert(ProductImagesTable.id) {
            it[ProductImagesTable.id] = productId
            it[thumbnailImage] = thumbnail
            it[fullImage] = full
        }
    }

    fun find(productId: String): Pair<ByteArray, ByteArray>? = transaction {
        selectAll().where { ProductImagesTable.id eq productId }.singleOrNull()?.let { row ->
            row[thumbnailImage] to row[fullImage]
        }
    }

    fun delete(productId: String) = transaction {
        deleteWhere { ProductImagesTable.id eq productId }
    }
}
