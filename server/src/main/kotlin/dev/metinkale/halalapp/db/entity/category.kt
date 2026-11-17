package dev.metinkale.halalapp.db.entity

data class Category(
    override val id: String,
    val name: String,
    val parentId: String?,
    val description: String,
) : Entity

data class CategoryMapping(
    override val id: String,
    val categoryId: String,
) : Entity
