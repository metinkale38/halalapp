package dev.metinkale.halalapp.db.entity

import dev.metinkale.halalapp.domain.Confidence
import dev.metinkale.halalapp.domain.Status
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    override val id: String,
    val name: String,
    val brand: String,
    val ingredientsText: String,
    val allergens: List<Ingredient>,
    val traces: List<Ingredient>,
    val images: ProductImages,
    val tags: List<String>,
    val ingredients: List<Ingredient>,
    val animalIngredients: List<Ingredient>,
    val alcoholIngredients: List<Ingredient>,
    val unknownIngredients: List<Ingredient>,
    val status: List<Status>,
    val confidence: Confidence,
) : Entity

@Serializable
data class ProductImages(
    val front: Map<Int, String>,
    val ingredients: Map<Int, String>,
) {
    private fun Map<Int, String>.getPhoto(minWidth: Int = 100): String? {
        return (filter { it.key >= minWidth }.minByOrNull { it.key }
            ?: minByOrNull { it.key })?.value

    }

    fun getFrontPhoto(minWidth: Int = 100): String =
        front.getPhoto(minWidth) ?: "/nophoto.webp"

    fun getIngredientPhoto(minWidth: Int = 100): String? =
        ingredients.getPhoto(minWidth)
}

@Serializable
data class ProductComponent(
    val id: String,
    val name: String,
)
