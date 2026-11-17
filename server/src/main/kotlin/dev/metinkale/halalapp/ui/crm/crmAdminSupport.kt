package dev.metinkale.halalapp.ui.crm

import dev.metinkale.halalapp.db.entity.Entity
import dev.metinkale.halalapp.db.table.CategoryMappingTable
import dev.metinkale.halalapp.db.table.CategoriesTable
import dev.metinkale.halalapp.db.table.ClassificationsTable
import dev.metinkale.halalapp.db.table.CrmTable
import dev.metinkale.halalapp.db.table.IngredientsTable
import dev.metinkale.halalapp.db.table.ProductInsightsTable
import dev.metinkale.halalapp.db.table.ProductParentTable
import dev.metinkale.halalapp.db.table.ProductsCustomTable
import dev.metinkale.halalapp.db.table.ProductsOffTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.Locale

fun <T : Entity> CrmTable<T>.readValues(row: ResultRow): Map<String, String> =
    getCrmFields().associate { field -> field.name to field.read(row) }

fun <T : Entity> CrmTable<T>.displayValues(values: Map<String, String>): Map<String, String> {
    if (this !== ProductParentTable) return values
    if (values.containsKey("child_ids")) return values
    return values + mapOf(
        "child_name" to productName(values["child_id"].orEmpty()),
        "parent_name" to productName(values["parent_id"].orEmpty()),
    )
}

fun <T : Entity> CrmTable<T>.fieldLabel(name: String): String = when (name) {
    "child_id" -> "Dublette (Barcode)"
    "child_name" -> "Dublette"
    "child_ids" -> "Verlinkte EANs"
    "parent_id" -> "EAN (Hauptprodukt)"
    "parent_name" -> "Name (Hauptprodukt)"
    else -> getCrmFields().firstOrNull { it.name == name }?.label ?: name
}

private fun productName(id: String): String {
    if (id.isBlank()) return "–"
    return transaction {
        ProductsOffTable.selectAll().where { ProductsOffTable.id eq id }.singleOrNull()
            ?.get(ProductsOffTable.name)
            ?: ProductsCustomTable.selectAll().where { ProductsCustomTable.id eq id }.singleOrNull()
                ?.get(ProductsCustomTable.name)
    } ?: "Unbekannt"
}

val <T : Entity> CrmTable<T>.listFieldNames: List<String>
    get() = when (this) {
        ClassificationsTable -> listOf("id", "name", "status", "animal", "alcohol")
        IngredientsTable -> listOf("id", "name", "classification")
        ProductInsightsTable -> listOf("product_id", "date", "animal", "alcohol")
        ProductParentTable -> listOf("parent_id", "parent_name", "child_ids")
        CategoriesTable -> listOf("id", "name", "parent_id", "description")
        CategoryMappingTable -> listOf("id", "category_id")
    }

val <T : Entity> CrmTable<T>.searchFieldNames: List<String>
    get() = when (this) {
        ClassificationsTable -> listOf("id", "name", "status", "animal", "alcohol")
        IngredientsTable -> listOf("id", "name", "classification")
        ProductInsightsTable -> listOf("id", "product_id", "content", "date", "animal", "alcohol")
        ProductParentTable -> listOf("parent_id", "parent_name", "child_ids")
        CategoriesTable -> listOf("id", "name", "parent_id", "description")
        CategoryMappingTable -> listOf("id", "category_id")
    }

fun <T : Entity> CrmTable<T>.entryTitle(values: Map<String, String>): String = when (this) {
    ClassificationsTable -> values["name"].orEmpty()
    IngredientsTable -> values["name"].orEmpty()
    ProductInsightsTable -> values["product_id"].orEmpty()
    ProductParentTable -> "${values["child_id"].orEmpty()} → ${values["parent_id"].orEmpty()}"
    CategoriesTable -> values["name"].orEmpty()
    CategoryMappingTable -> "${values["id"].orEmpty()} → ${values["category_id"].orEmpty()}"
}

fun <T : Entity> CrmTable<T>.searchMatches(values: Map<String, String>, query: String): Boolean {
    val needle = query.trim().lowercase(Locale.GERMAN)
    if (needle.isEmpty()) return true
    return searchFieldNames.any { field ->
        values[field].orEmpty().lowercase(Locale.GERMAN).contains(needle)
    }
}
