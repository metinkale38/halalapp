package dev.metinkale.halalapp.server

import java.util.Date


data class HalalAppUser(
    val id: String,
    val email: String,
    val username: String,
    val groups: List<String>,
    val expiresAt: Date
)