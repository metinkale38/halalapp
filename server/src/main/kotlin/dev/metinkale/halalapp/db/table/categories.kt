package dev.metinkale.halalapp.db.table

import dev.metinkale.halalapp.db.entity.Category
import dev.metinkale.halalapp.db.entity.CategoryMapping
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object CategoriesTable : CrmTable<Category>("categories") {
    val name = text("name")
    val parentId = text("parent_id").nullable()
    val description = text("description")

    override fun map(row: ResultRow) = Category(
        id = row[id].value,
        name = row[name],
        parentId = row[parentId],
        description = row[description],
    )

    fun children(id: String): List<Category> = transaction {
        selectAll().where { parentId eq id }.orderBy(name to SortOrder.ASC).map { map(it) }
    }

    fun topLevel(): List<Category> = transaction {
        selectAll().where { parentId.isNull() }.orderBy(name to SortOrder.ASC).map { map(it) }
    }

    fun allOrdered(): List<Category> = transaction {
        selectAll().orderBy(name to SortOrder.ASC).map { map(it) }
    }
}

object CategoryMappingTable : CrmTable<CategoryMapping>("category_mapping") {
    val categoryId = text("category_id")

    override fun map(row: ResultRow) = CategoryMapping(
        id = row[id].value,
        categoryId = row[categoryId],
    )

    fun allMappings(): Map<String, String> = transaction {
        val result = LinkedHashMap<String, String>()
        for (row in selectAll()) {
            result[row[CategoryMappingTable.id].value] = row[CategoryMappingTable.categoryId]
        }
        result
    }
}
