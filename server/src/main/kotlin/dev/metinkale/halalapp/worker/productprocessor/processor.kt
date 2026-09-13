package dev.metinkale.halalapp.worker.productprocessor

import dev.metinkale.halalapp.db.entity.Ingredient
import dev.metinkale.halalapp.db.entity.Product
import dev.metinkale.halalapp.db.entity.ProductInsights
import dev.metinkale.halalapp.db.table.ClassificationsTable
import dev.metinkale.halalapp.db.table.ProductInsightsTable
import dev.metinkale.halalapp.domain.Confidence
import dev.metinkale.halalapp.domain.Status
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun ProductProcessorWorker.process(products: List<Product>): List<Product> = transaction {
    val insights = ProductInsightsTable.findAllByBarcode(products.map { it.id }).groupBy { it.product_id }
    products.map { it.processLecithine().processTags().processInsights(insights[it.id] ?: emptyList()) }
}


private fun Product.processLecithine(): Product {
    val lecithine = animalIngredients.find { it.id == "en:e322" }
    if (lecithine != null) {
        val ingredientsText = ingredientsText.lowercase().replace("_", "")
            .replace("[", "(").replace("]", ")")
        if (ingredientsText.contains("lecithine (soja)")
            || ingredientsText.contains("lecithins (soy)")
            || ingredientsText.contains("lécithines (soy)")
        ) return copy(animalIngredients = animalIngredients.filter { it.id != "en:e322" }).run {
            copy(
                status = if (ingredients.isEmpty()) listOf(Status.INCOMPLETE) else buildStatus(
                    animalIngredients,
                    alcoholIngredients,
                    unknownIngredients
                )
            )
        }
    }
    return this
}

private fun Product.processTags(): Product {
    val classifications = tags.mapNotNull { ClassificationsTable.findById(it) }
    return copy(
        animalIngredients = if (classifications.any { it.animal }) classifications.filter { it.animal }
            .map { Ingredient("", "", it) } else animalIngredients,
        alcoholIngredients = if (classifications.any { it.alcohol }) classifications.filter { it.alcohol }
            .map { Ingredient("", "", it) } else alcoholIngredients,
        unknownIngredients = if (classifications.any { it.animal } && classifications.any { it.alcohol })
            emptyList() else unknownIngredients,
    )
}

private fun Product.processInsights(insights: List<ProductInsights>): Product {
    val insight = insights.maxByOrNull { it.date } ?: return this
    val alcohol = insight.alcohol.filter { it.alcohol }.map { Ingredient("", "", it) }
    val animal = insight.animal.filter { it.animal }.map { Ingredient("", "", it) }
    return copy(
        unknownIngredients = emptyList(),
        alcoholIngredients = alcohol,
        animalIngredients = animal,
        status = buildStatus(animal, alcohol, emptyList()),
        confidence = Confidence.MANUFACTURER
    )
}





