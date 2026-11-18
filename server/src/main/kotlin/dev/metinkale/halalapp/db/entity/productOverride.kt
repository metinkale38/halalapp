package dev.metinkale.halalapp.db.entity

import java.time.Instant

data class ProductOverride(
    val productId: String,
    val name: String?,
    val brand: String?,
    val frontImageVersion: String?,
    val updatedBy: String,
    val updatedAt: Instant,
)

data class IgnoredIngredient(
    val productId: String,
    val ingredientId: String,
    val reason: String,
    val proposalId: String?,
    val createdBy: String,
    val createdAt: Instant,
)
