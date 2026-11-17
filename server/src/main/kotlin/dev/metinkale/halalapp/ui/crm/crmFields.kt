package dev.metinkale.halalapp.ui.crm

import dev.metinkale.halalapp.ui.crm.CrmLens.*
import dev.metinkale.halalapp.db.entity.Entity
import dev.metinkale.halalapp.db.table.CategoryMappingTable
import dev.metinkale.halalapp.db.table.CategoriesTable
import dev.metinkale.halalapp.db.table.CrmTable
import dev.metinkale.halalapp.db.table.ClassificationsTable
import dev.metinkale.halalapp.db.table.IngredientsTable
import dev.metinkale.halalapp.db.table.ProductInsightsTable
import dev.metinkale.halalapp.db.table.ProductParentTable
import dev.metinkale.halalapp.domain.Status

fun <T : Entity> CrmTable<T>.getCrmFields(): List<CrmLens<out Any?>> {
    return when (this) {
        ClassificationsTable -> listOf(
            StaticText("ID", "id", ClassificationsTable.idRaw),
            Text("Name", "name", ClassificationsTable.name),
            CheckBox("Betrifft tierische Zutaten", "animal", ClassificationsTable.animal),
            CheckBox("Betrifft berauschende Zutaten", "alcohol", ClassificationsTable.alcohol),
            Text("URL", "url", ClassificationsTable.url),
            Select(
                "Status",
                "status",
                Status.entries.filter { it != Status.INCOMPLETE }.map { it.name to it.value },
                ClassificationsTable.status,
                { this[ClassificationsTable.status].name },
                { this[ClassificationsTable.status] = Status.valueOf(it) }
            )
        )

        IngredientsTable -> listOf(
            StaticText("ID", "id", IngredientsTable.idRaw),
            Text("Name", "name", IngredientsTable.name),
            Select(
                "Einstufung",
                "classification",
                ClassificationsTable.all().filter { !it.name.startsWith("halalcheck") }
                    .map { it.id to it.name } + ("other" to "Andere (siehe Notiz)"),
                IngredientsTable.classification,
                { this[IngredientsTable.classification].value },
                {
                    this[IngredientsTable.classification] = it
                })
        )

        ProductInsightsTable -> listOf(
            StaticText("Barcode", "product_id", ProductInsightsTable.product_id),
            Markdown("Inhalt", "content", ProductInsightsTable.content),
            Date("Datum", "date", ProductInsightsTable.date),
            MultiSelect(
                "Tierisch",
                "animal",
                ClassificationsTable.all().filter { !it.name.startsWith("halalcheck") && it.animal }
                    .sortedBy { it.status }
                    .map { it.id to "${it.name} (${it.status.value})" },
                ProductInsightsTable.animal,
                { this[ProductInsightsTable.animal] },
                { this[ProductInsightsTable.animal] = it }
            ), MultiSelect(
                "Berauschend",
                "alcohol",
                ClassificationsTable.all().filter { !it.name.startsWith("halalcheck") && it.alcohol }
                    .sortedBy { it.status }
                    .map { it.id to "${it.name} (${it.status.value})" },
                ProductInsightsTable.alcohol,
                { this[ProductInsightsTable.alcohol] },
                { this[ProductInsightsTable.alcohol] = it }
            ))

        ProductParentTable -> listOf(
            Text("Barcode-Hauptprodukt", "parent_id", ProductParentTable.parentId),
            Text("Barcode-Dublette", "child_id", ProductParentTable.childId)
        )

        CategoriesTable -> listOf(
            StaticText("ID / Slug", "id", CategoriesTable.idRaw),
            Text("Name", "name", CategoriesTable.name),
            Select<String?>(
                "Überkategorie",
                "parent_id",
                listOf("" to "— keine (Top-Level) —") +
                        CategoriesTable.allOrdered().filter { it.parentId == null }.map { it.id to it.name },
                CategoriesTable.parentId,
                { this[CategoriesTable.parentId].orEmpty() },
                { this[CategoriesTable.parentId] = it.ifBlank { null } },
            ),
        )

        CategoryMappingTable -> listOf(
            StaticText("OFF-Kategorie", "id", CategoryMappingTable.idRaw),
            Select(
                "Kategorie",
                "category_id",
                CategoriesTable.leafOrdered().map { it.id to it.name },
                CategoryMappingTable.categoryId,
                { this[CategoryMappingTable.categoryId] },
                { this[CategoryMappingTable.categoryId] = it },
            ),
        )




    }
}
