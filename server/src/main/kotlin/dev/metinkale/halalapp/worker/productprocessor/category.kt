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
}
