package dev.metinkale.halalapp.worker.productprocessor

import dev.metinkale.halalapp.db.entity.Ingredient
import dev.metinkale.halalapp.db.entity.Product
import dev.metinkale.halalapp.db.table.ClassificationsTable
import dev.metinkale.halalapp.db.table.IngredientsTable
import dev.metinkale.halalapp.domain.Confidence
import dev.metinkale.halalapp.domain.Status

private val unknownIngredient = ClassificationsTable.findById("unknown_ingredient")!!

fun mapToProduct(source: SourceProduct): Product {
    val allergens = source.allergens.map { IngredientsTable.findById(it) ?: Ingredient(it, it, unknownIngredient) }
    val traces = source.traces.map { IngredientsTable.findById(it) ?: Ingredient(it, it, unknownIngredient) }
    val ingredients = source.ingredients.map {
        IngredientsTable.findById(it.id)?.copy(name = it.name) ?: Ingredient(it.id, it.name, unknownIngredient)
    }
    val relevant = (ingredients + traces + allergens)
        .filter { it.id !in source.ignoredIngredients }
    val allIngredients = relevant
        .filter { it.classification.status != Status.HALAL }
    val animal = allIngredients.filter { it.classification.animal }
        .filter { it.classification.status != Status.INCOMPLETE }.distinctBy { it.id }
    val alcohol = allIngredients.filter { it.classification.alcohol }
        .filter { it.classification.status != Status.INCOMPLETE }.distinctBy { it.id }
    val unknown = allIngredients.filter { it.classification.status == Status.INCOMPLETE }
        .distinctBy { it.id }
    return Product(
        id = source.id,
        name = source.name,
        brand = source.brand,
        ingredientsText = source.ingredientsText,
        allergens = allergens,
        traces = traces,
        images = source.images,
        tags = source.tags,
        ingredients = ingredients,
        animalIngredients = animal,
        alcoholIngredients = alcohol,
        unknownIngredients = unknown,
        status = if (ingredients.isEmpty() || relevant.isEmpty()) listOf(Status.INCOMPLETE) else buildStatus(
            animal,
            alcohol,
            unknown
        ),
        confidence = Confidence.INGREDIENTS,
    )
}
