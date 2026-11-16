package dev.metinkale.halalapp.ui.crm

import dev.metinkale.halalapp.db.entity.Entity
import dev.metinkale.halalapp.db.table.ClassificationsTable
import dev.metinkale.halalapp.db.table.CrmTable
import dev.metinkale.halalapp.db.table.IngredientsTable
import dev.metinkale.halalapp.db.table.ProductInsightsTable
import dev.metinkale.halalapp.db.table.ProductParentTable
import dev.metinkale.halalapp.db.table.ProductsTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.Locale

fun <T : Entity> CrmTable<T>.readValues(row: ResultRow): Map<String, String> =
    getCrmFields().associate { field -> field.name to field.read(row) }

fun <T : Entity> CrmTable<T>.displayValues(values: Map<String, String>): Map<String, String> {
    if (this !== ProductParentTable) return values
    return values + mapOf(
        "child_name" to productName(values["child_id"].orEmpty()),
        "parent_name" to productName(values["parent_id"].orEmpty()),
    )
}

fun <T : Entity> CrmTable<T>.fieldLabel(name: String): String = when (name) {
    "child_id" -> "Dublette (Barcode)"
    "child_name" -> "Dublette"
    "parent_id" -> "Hauptprodukt (Barcode)"
    "parent_name" -> "Hauptprodukt"
    else -> getCrmFields().firstOrNull { it.name == name }?.label ?: name
}

private fun productName(id: String): String {
    if (id.isBlank()) return "–"
    return transaction {
        ProductsTable.selectAll().where { ProductsTable.id eq id }.singleOrNull()
            ?.get(ProductsTable.name)
    } ?: "Unbekannt"
}

val <T : Entity> CrmTable<T>.listFieldNames: List<String>
    get() = when (this) {
        ClassificationsTable -> listOf("id", "name", "status", "animal", "alcohol")
        IngredientsTable -> listOf("id", "name", "classification")
        ProductInsightsTable -> listOf("product_id", "date", "animal", "alcohol")
        ProductParentTable -> listOf("child_id", "child_name", "parent_id", "parent_name")
    }

val <T : Entity> CrmTable<T>.searchFieldNames: List<String>
    get() = when (this) {
        ClassificationsTable -> listOf("id", "name", "status", "animal", "alcohol")
        IngredientsTable -> listOf("id", "name", "classification")
        ProductInsightsTable -> listOf("id", "product_id", "content", "date", "animal", "alcohol")
        ProductParentTable -> listOf("child_id", "child_name", "parent_id", "parent_name")
    }

fun <T : Entity> CrmTable<T>.entryTitle(values: Map<String, String>): String = when (this) {
    ClassificationsTable -> values["name"].orEmpty()
    IngredientsTable -> values["name"].orEmpty()
    ProductInsightsTable -> values["product_id"].orEmpty()
    ProductParentTable -> "${values["child_id"].orEmpty()} → ${values["parent_id"].orEmpty()}"
}

fun <T : Entity> CrmTable<T>.searchMatches(values: Map<String, String>, query: String): Boolean {
    val needle = query.trim().lowercase(Locale.GERMAN)
    if (needle.isEmpty()) return true
    return searchFieldNames.any { field ->
        values[field].orEmpty().lowercase(Locale.GERMAN).contains(needle)
    }
}
