package dev.metinkale.halalapp.worker.openfoodfacts

import dev.metinkale.halalapp.db.table.IngredientsTable
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert

@Serializable
private data class Entry(
    val name: Map<String, String>,
    val vegan: EnValue? = null,
    val vegetarian: EnValue? = null,
)

@Serializable
private data class EnValue(
    val en: String,
)


suspend fun OpenFoodFactsWorker.importIngredientTaxonomies(url: String) {
    val entries: Map<String, Entry> = client.get(url).body()

    transaction {
        entries.entries.forEach { (key, value) ->
            val localizedName = value.name["de"] ?: value.name["en"] ?: key
            IngredientsTable.upsert {
                it[id] = key
                it[name] = localizedName
            }
        }
    }
}