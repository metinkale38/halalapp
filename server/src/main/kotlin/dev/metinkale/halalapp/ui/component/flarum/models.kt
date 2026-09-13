package dev.metinkale.halalapp.ui.component.flarum

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.time.Duration
import java.time.Instant

@Serializable
data class FlarumResponse(
    val data: List<Discussion>,
    val included: List<JsonObject>? = null, // Wir nehmen erst mal JsonObject, um Abstürze zu vermeiden
)


@Serializable
data class DiscussionAttributes(
    val title: String,
    val slug: String,
    @SerialName("isSticky")
    val isSticky: Boolean = false,
    @SerialName("commentCount")
    val commentCount: Int = 0,
    @SerialName("createdAt")
    val createdAt: String? = null,
)

@Serializable
data class Discussion(
    val id: String,
    val attributes: DiscussionAttributes,
    val relationships: Relationships,
)

@Serializable
data class Relationships(
    val firstPost: RelationshipData,
)

@Serializable
data class RelationshipData(
    val data: RelationshipLink,
)

@Serializable
data class RelationshipLink(val id: String)

