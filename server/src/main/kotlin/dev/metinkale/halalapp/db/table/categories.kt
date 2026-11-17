package dev.metinkale.halalapp.db.table

import dev.metinkale.halalapp.db.entity.Category
import dev.metinkale.halalapp.db.entity.CategoryMapping
import dev.metinkale.halalapp.db.entity.OTHER_CATEGORY_ID
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private val categoryOrder = compareBy<Category> { it.id == OTHER_CATEGORY_ID }.thenBy { it.name }

object CategoriesTable : CrmTable<Category>("categories") {
    val name = text("name")
    val parentId = text("parent_id").nullable()

    override fun map(row: ResultRow) = Category(
        id = row[id].value,
        name = row[name],
        parentId = row[parentId],
    )

    fun children(id: String): List<Category> = transaction {
        selectAll().where { parentId eq id }.map { map(it) }.sortedWith(categoryOrder)
    }

    fun topLevel(): List<Category> = transaction {
        selectAll().where { parentId.isNull() }.map { map(it) }.sortedWith(categoryOrder)
    }

    fun allOrdered(): List<Category> = transaction {
        selectAll().map { map(it) }.sortedWith(categoryOrder)
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
