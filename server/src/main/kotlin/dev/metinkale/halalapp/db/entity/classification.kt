package dev.metinkale.halalapp.db.entity

import dev.metinkale.halalapp.domain.Confidence
import dev.metinkale.halalapp.domain.Status
import kotlinx.serialization.Serializable

@Serializable
data class Classification(
    override val id: String,
    val status: Status,
    val animal: Boolean,
    val alcohol: Boolean,
    val name: String,
    val url: String? = null,
    val confidence: Confidence = Confidence.INGREDIENTS,
) : Entity