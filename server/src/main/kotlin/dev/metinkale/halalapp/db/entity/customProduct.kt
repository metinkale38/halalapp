package dev.metinkale.halalapp.db.entity

import java.time.Instant

data class CustomProduct(
    override val id: String,
    val name: String,
    val brand: String,
    val images: ProductImages,
    val categoryId: String? = null,
) : Entity

data class CustomProductProposal(
    val proposalId: String,
    val barcode: String,
    val name: String,
    val brand: String,
    val insightContent: String,
    val insightDate: java.time.LocalDate,
    val animal: List<String>,
    val alcohol: List<String>,
    val note: String,
    val creator: String,
    val status: dev.metinkale.halalapp.domain.RequestStatus,
    val date: Instant,
    val imageDataUri: String?,
    val categoryId: String? = null,
)
