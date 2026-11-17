package dev.metinkale.halalapp.worker.productprocessor

import dev.metinkale.halalapp.db.entity.OTHER_CATEGORY_ID
import dev.metinkale.halalapp.db.table.CategoryMappingTable
import org.slf4j.LoggerFactory

object CategoryResolver {
    const val OTHER = OTHER_CATEGORY_ID
    private val logger = LoggerFactory.getLogger("CATEGORY_MAPPING")

    @Volatile
    private var mapping: Map<String, String> = emptyMap()

    fun reload() {
        mapping = CategoryMappingTable.allMappings()
        logger.info("Loaded ${mapping.size} category mappings")
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
