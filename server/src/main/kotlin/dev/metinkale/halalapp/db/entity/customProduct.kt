package dev.metinkale.halalapp.db.entity

import java.time.Instant

enum class ProductImageKind {
    FRONT,
    INGREDIENTS
}

enum class ProductImageStatus {
    PENDING,
    ATTACHED,
    REJECTED,
    REPLACED
}

data class ProductImage(
    val id: String,
    val proposalId: String?,
    val productId: String?,
    val kind: ProductImageKind,
    val status: ProductImageStatus,
    val originalMime: String,
    val originalWidth: Int,
    val originalHeight: Int,
    val originalBytes: Int,
    val sha256: String,
    val createdAt: Instant,
    val createdBy: String,
)

data class ProductImageFile(
    val id: String,
    val imageId: String,
    val width: Int,
    val mime: String,
    val byteSize: Int,
    val sha256: String,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProductImageFile) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

data class CustomProduct(
    override val id: String,
    val name: String,
    val brand: String,
    val images: ProductImages,
) : Entity

data class ProductImagePreview(
    val image: ProductImage,
    val files: List<Pair<String, Int>>,
)

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
    val imagePreviews: List<ProductImagePreview>,
)
