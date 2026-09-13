package dev.metinkale.halalapp.db.table

import dev.metinkale.halalapp.db.entity.Ingredient
import org.jetbrains.exposed.v1.core.ResultRow


object IngredientsTable : CrmTable<Ingredient>("ingredients") {
    val name = text("name")
    val classification = reference("classification", ClassificationsTable)

    override fun map(row: ResultRow) = Ingredient(
        id = row[this.id].value,
        name = row[name],
        classification = ClassificationsTable.findById(row[classification].value)!!
    )

}