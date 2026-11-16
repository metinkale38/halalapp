package dev.metinkale.halalapp.db.table

import dev.metinkale.halalapp.db.entity.CustomProduct
import dev.metinkale.halalapp.db.entity.ProductComponent
import dev.metinkale.halalapp.db.entity.ProductImages
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.json.jsonb

private val customProductJson = Json { ignoreUnknownKeys = true }

object ProductsCustomTable : IdTable<String>("products_custom") {
    override val id = text("id").entityId()
    override val primaryKey = PrimaryKey(id)

    val name = text("name")
    val brand = text("brand")
    val ingredients = jsonb<List<ProductComponent>>("ingredients", customProductJson)
    val ingredientsText = text("ingredients_text")
    val images = jsonb<ProductImages>("images", customProductJson)
    val tags = array("tags", TextColumnType())
    val traces = array("traces", TextColumnType())
    val allergens = array("allergens", TextColumnType())

    fun map(row: ResultRow) = CustomProduct(
        id = row[id].value,
        name = row[name],
        brand = row[brand],
        images = row[images],
    )

    fun findById(id: String): CustomProduct? = transaction {
        selectAll().where { ProductsCustomTable.id eq id }.singleOrNull()?.let { map(it) }
    }

    fun allProducts(): List<CustomProduct> = transaction {
        selectAll().map { map(it) }
    }
}
