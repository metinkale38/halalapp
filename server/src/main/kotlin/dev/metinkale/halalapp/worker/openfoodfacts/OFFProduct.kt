package dev.metinkale.halalapp.worker.openfoodfacts

import kotlinx.serialization.Serializable

@Serializable
data class OFFProduct(
    val code: String,
    val product_name: String? = null,
    val product_name_de: String? = null,
    val product_name_en: String? = null,
    val countries_tags: List<String> = emptyList(),
    val brands: String? = null,
    val categories_hierarchy: List<String> = emptyList(),

    val categories_tags: List<String> = emptyList(),
    val ingredients_text: String? = null,
    val ingredients_text_de: String? = null,
    val ingredients_text_en: String? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val allergens_tags: List<String> = emptyList(),
    val traces_tags: List<String> = emptyList(),
    val images: Images? = null,
    val tags_sources: TagSources? = null,
    val states_tags: List<String> = emptyList(),
    val languages_tags: List<String> = emptyList(),
    val pnns_groups_1: String? = null,
    val pnns_groups_2: String? = null,
) {
    @Serializable
    data class Ingredient(
        val id: String,
        val text: String,
        val ingredients: List<Ingredient> = emptyList(),
        val is_in_taxonomy: Int = 0,
    )


    @Serializable
    data class Images(
        val selected: Selected? = null,
    )


    @Serializable
    data class Selected(
        val front: Map<String, Image>? = null,
        val ingredients: Map<String, Image>? = null,
    )

    @Serializable
    data class Image(
        val rev: Int,
        val sizes: Map<String, Size>,
    )

    @Serializable
    data class Size(val w: Int, val h: Int)

    @Serializable
    data class TagSources(val labels: Labels? = null)

    @Serializable
    data class Labels(val packaging: Packaging? = null)

    @Serializable
    data class Packaging(val tags: List<String>? = null)
}