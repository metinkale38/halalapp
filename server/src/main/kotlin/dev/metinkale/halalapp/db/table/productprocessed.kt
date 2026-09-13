package dev.metinkale.halalapp.db.table

import dev.metinkale.halalapp.db.entity.Ingredient
import dev.metinkale.halalapp.db.entity.Product
import dev.metinkale.halalapp.db.entity.ProductImages
import dev.metinkale.halalapp.domain.Confidence
import dev.metinkale.halalapp.domain.Status
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.json.jsonb

private val json = Json { ignoreUnknownKeys = true }

object ProcessedProductsTable : EntityTable<Product>("products_processed") {
    val brand = text("brand")
    val name = text("name")
    val ingredientsText = text("ingredients_text")
    val traces = array("traces", TextColumnType())
    val allergens = array("allergens", TextColumnType())
    val animalIngredients = jsonb<List<Ingredient>>("animal_ingredients", json)
    val alcoholIngredients = jsonb<List<Ingredient>>("alcohol_ingredients", json)
    val unknownIngredients = jsonb<List<Ingredient>>("unknown_ingredients", json)
    val images = jsonb<ProductImages>("images", json)
    val tags = array("tags", TextColumnType())
    val status = array("status", TextColumnType())
    val confidence = enumerationByName<Confidence>("confidence", 16)
    val lastUpdated = timestamp("last_updated")

    override val filter: Op<Boolean> = stringLiteral("HALAL") eq anyFrom(status) or
            notExists(
                BrandBlacklistTable.selectAll()
                    .where { unaccentLower(BrandBlacklistTable.brand) eq unaccentLower(brand) })
    private val halal = ClassificationsTable.findById("halal")!!
    override fun map(row: ResultRow): Product {
        val id = row[id].value
        return Product(
            id = id,
            name = row[name],
            brand = row[brand],
            ingredientsText = row[ingredientsText],
            allergens = row[allergens].map { Ingredient(it, it, halal) },
            traces = row[traces].map { Ingredient(it, it, halal) },
            images = row[images],
            tags = row[tags],
            ingredients = emptyList(),
            animalIngredients = row[animalIngredients],
            alcoholIngredients = row[alcoholIngredients],
            unknownIngredients = row[unknownIngredients],
            status = row[status].map { Status.valueOf(it) },
            confidence = row[confidence]
        )
    }

    private fun unaccentLower(expr: Expression<String>): CustomFunction<String> {
        return CustomFunction(
            "unaccent", TextColumnType(),
            CustomFunction("lower", TextColumnType(), expr)
        )
    }
}