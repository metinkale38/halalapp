package dev.metinkale.halalapp.ui.crm

import dev.metinkale.halalapp.db.table.CrmTable
import dev.metinkale.halalapp.db.table.ClassificationsTable
import dev.metinkale.halalapp.db.table.IngredientsTable
import dev.metinkale.halalapp.db.table.ProductInsightsTable
import dev.metinkale.halalapp.db.table.ProductParentTable

val CrmTable<*>.germanName
    get() = when (this) {
        ClassificationsTable -> "Einstufung"
        IngredientsTable -> "Zutat"
        ProductInsightsTable -> "Produktinformation"
        ProductParentTable -> "Produktdubletten"
    }
