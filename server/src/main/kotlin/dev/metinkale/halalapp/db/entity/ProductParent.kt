package dev.metinkale.halalapp.db.entity

import kotlinx.serialization.Serializable

@Serializable
data class ProductParent(override val id: String, val parent: String) : Entity