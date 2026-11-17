package dev.metinkale.halalapp.worker.productprocessor

import dev.metinkale.halalapp.db.table.CategoryMappingTable

object CategoryResolver {
    const val OTHER = "sonstiges"

    @Volatile
    private var mapping: Map<String, String> = emptyMap()

    fun reload() {
        mapping = CategoryMappingTable.allMappings()
    }

    fun resolve(categoriesHierarchy: List<String>): String {
        for (tag in categoriesHierarchy.asReversed()) {
            mapping[tag]?.let { return it }
        }
        return OTHER
    }

    fun resolveForProduct(
        categoriesHierarchy: List<String>,
        tags: List<String>,
        ingredientCount: Int,
    ): String? {
        if (ingredientCount == 1) return null
        if ("meat" in tags || "alcoholic_beverage" in tags) return null
        if ("en:non-food-products" in categoriesHierarchy) return null
        return resolve(categoriesHierarchy)
    }
}
