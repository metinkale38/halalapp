package dev.metinkale.halalapp.db.entity

import dev.metinkale.halalapp.domain.RequestStatus
import java.time.Instant

data class Proposal(
    override val id: String,
    val entityType: String,
    val json: Map<String, String>,
    val note: String,
    val creator: String,
    val status: RequestStatus,
    val date: Instant,
) : Entity