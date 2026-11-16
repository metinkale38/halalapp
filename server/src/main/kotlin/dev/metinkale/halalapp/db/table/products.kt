package dev.metinkale.halalapp.db.table

import dev.metinkale.halalapp.db.entity.ProductComponent
import dev.metinkale.halalapp.db.entity.ProductImages
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.json.jsonb

open class ProductsTable(table: String = "products") : IdTable<String>(table) {

    override val id = text("id").entityId()
    override val primaryKey = PrimaryKey(id)
    val name = text("name")
    val brand = text("brand")
    val ingredients = jsonb<List<ProductComponent>>("ingredients", Json.Default)
    val ingredientsText = text("ingredients_text")
    val allergens = array("allergens", TextColumnType())
    val traces = array("traces", TextColumnType())
    val images = jsonb<ProductImages>("images", Json.Default)
    val tags = array("tags", TextColumnType())

    companion object : ProductsTable()
}

