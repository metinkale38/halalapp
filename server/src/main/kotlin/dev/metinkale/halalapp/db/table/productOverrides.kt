package dev.metinkale.halalapp.db.table

import dev.metinkale.halalapp.db.entity.IgnoredIngredient
import dev.metinkale.halalapp.db.entity.ProductImages
import dev.metinkale.halalapp.db.entity.ProductOverride
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import java.time.Instant

object ProductOverridesTable : Table("product_overrides") {
    val productId = text("product_id")
    val name = text("name").nullable()
    val brand = text("brand").nullable()
    val frontImageVersion = text("front_image_version").nullable()
    val updatedBy = text("updated_by")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(productId)

    private fun map(row: ResultRow) = ProductOverride(
        productId = row[productId],
        name = row[name],
        brand = row[brand],
        frontImageVersion = row[frontImageVersion],
        updatedBy = row[updatedBy],
        updatedAt = row[updatedAt],
    )

    fun find(id: String): ProductOverride? = transaction {
        selectAll().where { productId eq id }.singleOrNull()?.let(::map)
    }

    fun allOverrides(): Map<String, ProductOverride> = transaction {
        selectAll().associate { row -> row[productId] to map(row) }
    }

    fun upsertFields(
        id: String,
        name: String? = null,
        brand: String? = null,
        frontImageVersion: String? = null,
        actor: String,
    ) = transaction {
        val existing = selectAll().where { productId eq id }.singleOrNull()
        if (existing == null) {
            insert {
                it[productId] = id
                it[ProductOverridesTable.name] = name
                it[ProductOverridesTable.brand] = brand
                it[ProductOverridesTable.frontImageVersion] = frontImageVersion
                it[updatedBy] = actor
                it[updatedAt] = Instant.now()
            }
        } else {
            update({ productId eq id }) {
                if (name != null) it[ProductOverridesTable.name] = name
                if (brand != null) it[ProductOverridesTable.brand] = brand
                if (frontImageVersion != null) it[ProductOverridesTable.frontImageVersion] = frontImageVersion
                it[updatedBy] = actor
                it[updatedAt] = Instant.now()
            }
        }
    }

    fun clearName(id: String, actor: String) = transaction {
        update({ productId eq id }) {
            it[name] = null
            it[updatedBy] = actor
            it[updatedAt] = Instant.now()
        }
    }

    fun clearBrand(id: String, actor: String) = transaction {
        update({ productId eq id }) {
            it[brand] = null
            it[updatedBy] = actor
            it[updatedAt] = Instant.now()
        }
    }

    fun clearFrontImage(id: String, actor: String) = transaction {
        update({ productId eq id }) {
            it[frontImageVersion] = null
            it[updatedBy] = actor
            it[updatedAt] = Instant.now()
        }
    }

    fun deleteOverride(id: String) = transaction {
        deleteWhere { productId eq id }
    }
}

fun overrideProductImages(productId: String, version: String): ProductImages = ProductImages(
    front = mapOf(
        0 to "/image/$productId/thumb?v=$version",
        300 to "/image/$productId?v=$version",
    )
)

object ProductIgnoredIngredientsTable : Table("product_ignored_ingredients") {
    val productId = text("product_id")
    val ingredientId = text("ingredient_id")
    val reason = text("reason")
    val proposalId = text("proposal_id").nullable()
    val createdBy = text("created_by")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(productId, ingredientId)

    private fun map(row: ResultRow) = IgnoredIngredient(
        productId = row[productId],
        ingredientId = row[ingredientId],
        reason = row[reason],
        proposalId = row[proposalId],
        createdBy = row[createdBy],
        createdAt = row[createdAt],
    )

    fun find(id: String): List<IgnoredIngredient> = transaction {
        selectAll().where { productId eq id }.map(::map)
    }

    fun allByProduct(): Map<String, Set<String>> = transaction {
        selectAll()
            .groupBy({ it[productId] }, { it[ingredientId] })
            .mapValues { (_, ids) -> ids.toSet() }
    }

    fun add(
        id: String,
        ingredient: String,
        note: String,
        proposal: String?,
        actor: String,
    ) = transaction {
        upsert(productId, ingredientId) {
            it[productId] = id
            it[ingredientId] = ingredient
            it[reason] = note
            it[proposalId] = proposal
            it[createdBy] = actor
            it[createdAt] = Instant.now()
        }
    }

    fun remove(id: String, ingredient: String) = transaction {
        deleteWhere { (productId eq id) and (ingredientId eq ingredient) }
    }

    fun deleteForProduct(id: String) = transaction {
        deleteWhere { productId eq id }
    }
}
