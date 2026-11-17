package dev.metinkale.halalapp.db.entity

const val OTHER_CATEGORY_ID = "sonstiges"

data class Category(
    override val id: String,
    val name: String,
    val parentId: String?,
) : Entity

data class CategoryMapping(
    override val id: String,
    val categoryId: String,
) : Entity
