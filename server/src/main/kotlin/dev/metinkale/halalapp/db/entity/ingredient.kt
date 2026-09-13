package dev.metinkale.halalapp.db.entity

import kotlinx.serialization.Serializable

@Serializable
data class Ingredient(
    override val id: String,
    val name: String,
    val classification: Classification
) : Entity