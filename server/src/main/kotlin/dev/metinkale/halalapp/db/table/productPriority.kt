package dev.metinkale.halalapp.db.table

import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.JavaInstantColumnType
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant

val REPROCESS_PRIORITY_TIMESTAMP: Instant = Instant.EPOCH
val REPROCESS_PRIORITY_THRESHOLD: Instant = Instant.parse("2000-01-01T00:00:00Z")

fun JdbcTransaction.markProductsForReprocessing(crmTable: CrmTable<*>, json: Map<String, String>) {
    when (crmTable) {
        ClassificationsTable -> json["id"]?.takeIf { it.isNotBlank() }?.let { markClassificationProducts(it) }
        IngredientsTable -> json["id"]?.takeIf { it.isNotBlank() }?.let { markIngredientProducts(it) }
        ProductInsightsTable -> json["product_id"]?.takeIf { it.isNotBlank() }?.let { markProductForReprocessing(it) }
        ProductParentTable -> Unit
    }
}

private fun JdbcTransaction.markProductForReprocessing(productId: String) {
    ProcessedProductsTable.update({ ProcessedProductsTable.id eq productId }) {
        it[ProcessedProductsTable.lastUpdated] = REPROCESS_PRIORITY_TIMESTAMP
    }
}

private fun JdbcTransaction.markIngredientProducts(ingredientId: String) {
    exec(
        """
        UPDATE products_processed
        SET last_updated = ?
        WHERE id IN (
            SELECT p.id
            FROM products p
            WHERE EXISTS (
                SELECT 1
                FROM jsonb_array_elements(p.ingredients) AS ingredient
                WHERE ingredient->>'id' = ?
            )
            OR ?::text = ANY(p.allergens)
            OR ?::text = ANY(p.traces)
        )
        """.trimIndent(),
        listOf<Pair<IColumnType<*>, Any>>(
            JavaInstantColumnType() to REPROCESS_PRIORITY_TIMESTAMP,
            TextColumnType() to ingredientId,
            TextColumnType() to ingredientId,
            TextColumnType() to ingredientId,
        )
    )
}

private fun JdbcTransaction.markClassificationProducts(classificationId: String) {
    exec(
        """
        UPDATE products_processed
        SET last_updated = ?
        WHERE id IN (
            SELECT p.id
            FROM products p
            WHERE ?::text = ANY(p.tags)
            OR EXISTS (
                SELECT 1
                FROM jsonb_array_elements(p.ingredients) AS ingredient
                JOIN ingredients i ON i.id = ingredient->>'id'
                WHERE i.classification = ?
            )
            OR EXISTS (
                SELECT 1
                FROM ingredients i
                WHERE i.classification = ?
                AND (i.id = ANY(p.allergens) OR i.id = ANY(p.traces))
            )
            UNION
            SELECT pi.product_id
            FROM product_insights pi
            WHERE ?::text = ANY(pi.animal) OR ?::text = ANY(pi.alcohol)
        )
        """.trimIndent(),
        listOf<Pair<IColumnType<*>, Any>>(
            JavaInstantColumnType() to REPROCESS_PRIORITY_TIMESTAMP,
            TextColumnType() to classificationId,
            TextColumnType() to classificationId,
            TextColumnType() to classificationId,
            TextColumnType() to classificationId,
            TextColumnType() to classificationId,
        )
    )
}
