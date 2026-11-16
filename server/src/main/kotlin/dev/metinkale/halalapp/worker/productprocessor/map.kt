package dev.metinkale.halalapp.worker.productprocessor

import dev.metinkale.halalapp.db.entity.Ingredient
import dev.metinkale.halalapp.db.entity.Product
import dev.metinkale.halalapp.db.table.ClassificationsTable
import dev.metinkale.halalapp.db.table.IngredientsTable
import dev.metinkale.halalapp.db.table.ProductsTable
import dev.metinkale.halalapp.domain.Confidence
import dev.metinkale.halalapp.domain.Status
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.collections.map


private val unknownIngredient = ClassificationsTable.findById("unknown_ingredient")!!

fun ProductProcessorWorker.mapToProduct(row: ResultRow): Product = ProductsTable.Companion.run {
    val allergens = row[allergens].map { IngredientsTable.findById(it) ?: Ingredient(it, it, unknownIngredient) }
    val traces = row[traces].map { IngredientsTable.findById(it) ?: Ingredient(it, it, unknownIngredient) }
    val ingredients = row[ingredients].map {
        IngredientsTable.findById(it.id)?.copy(name = it.name) ?: Ingredient(it.id, it.name, unknownIngredient)
    }
    val allIngredients = (ingredients + traces + allergens)
        .filter { it.classification.status != Status.HALAL }
    val animal = allIngredients.filter { it.classification.animal }
        .filter { it.classification.status != Status.INCOMPLETE }.distinctBy { it.id }
    val alcohol = allIngredients.filter { it.classification.alcohol }
        .filter { it.classification.status != Status.INCOMPLETE }.distinctBy { it.id }
    val unknown = allIngredients.filter { it.classification.status == Status.INCOMPLETE }
        .distinctBy { it.id }
    val barcode = row[id].value
    return Product(
        id = barcode,
        name = row[name],
        brand = row[brand],
        ingredientsText = row[ingredientsText],
        allergens = allergens,
        traces = traces,
        images = row[images],
        tags = row[tags],
        ingredients = ingredients,
        animalIngredients = animal,
        alcoholIngredients = alcohol,
        unknownIngredients = unknown,
        status = if (ingredients.isEmpty()) listOf(Status.INCOMPLETE) else buildStatus(
            animal,
            alcohol,
            unknown
        ),
        confidence = Confidence.INGREDIENTS,
    )
}
