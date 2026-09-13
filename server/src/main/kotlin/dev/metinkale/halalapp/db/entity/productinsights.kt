package dev.metinkale.halalapp.db.entity

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class ProductInsights(
    override val id: String,
    val product_id: String,
    val content: String?,
    val date: LocalDate,
    val animal: List<Classification>,
    val alcohol: List<Classification>
) : Entity