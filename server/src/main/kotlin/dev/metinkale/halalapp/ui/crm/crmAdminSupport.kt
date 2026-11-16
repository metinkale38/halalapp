package dev.metinkale.halalapp.ui.crm

import dev.metinkale.halalapp.db.entity.Entity
import dev.metinkale.halalapp.db.table.ClassificationsTable
import dev.metinkale.halalapp.db.table.CrmTable
import dev.metinkale.halalapp.db.table.IngredientsTable
import dev.metinkale.halalapp.db.table.ProductInsightsTable
import dev.metinkale.halalapp.db.table.ProductParentTable
import org.jetbrains.exposed.v1.core.ResultRow
import java.util.Locale

fun <T : Entity> CrmTable<T>.readValues(row: ResultRow): Map<String, String> =
    getCrmFields().associate { field -> field.name to field.read(row) }

fun <T : Entity> CrmTable<T>.fieldLabel(name: String): String =
    getCrmFields().firstOrNull { it.name == name }?.label ?: name

val <T : Entity> CrmTable<T>.listFieldNames: List<String>
    get() = when (this) {
        ClassificationsTable -> listOf("id", "name", "status", "animal", "alcohol")
        IngredientsTable -> listOf("id", "name", "classification")
        ProductInsightsTable -> listOf("product_id", "date", "animal", "alcohol")
        ProductParentTable -> listOf("child_id", "parent_id")
    }

val <T : Entity> CrmTable<T>.searchFieldNames: List<String>
    get() = when (this) {
        ClassificationsTable -> listOf("id", "name", "status", "animal", "alcohol")
        IngredientsTable -> listOf("id", "name", "classification")
        ProductInsightsTable -> listOf("id", "product_id", "content", "date", "animal", "alcohol")
        ProductParentTable -> listOf("child_id", "parent_id")
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
