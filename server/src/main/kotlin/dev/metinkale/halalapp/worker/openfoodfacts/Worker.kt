package dev.metinkale.halalapp.worker.openfoodfacts

import dev.metinkale.halalapp.db.DatabaseFactory
import dev.metinkale.halalapp.db.table.MetadataTable
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

object OpenFoodFactsWorker {

    val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }
    }

    suspend fun start() {
        val logger = LoggerFactory.getLogger("WORKER_OFF")
        DatabaseFactory.init(2)

        val lastFull = MetadataTable.getValue("LAST_OFF_FULL_IMPORT")?.let { Instant.parse(it) }
        var lastIncremental = MetadataTable.getValue("LAST_OFF_INCREMENTAL_IMPORT") ?: "0"
        var nextTimestampStart: String? = null

        var needsFullSync =
            if (lastFull == null) true else ChronoUnit.DAYS.between(lastFull, Instant.now()) >= 14

        client.get("https://static.openfoodfacts.org/data/delta/index.txt").bodyAsText().lines().sorted()
            .filter { it.isNotBlank() }.forEach {
                val start = it.split("_")[2]
                val end = it.split("_")[3].substringBefore(".")

                if (lastIncremental == start) {
                    logger.info("Importing $it")
                    importProducts("https://static.openfoodfacts.org/data/delta/$it")
                    lastIncremental = end
                    MetadataTable.setValue("LAST_OFF_INCREMENTAL_IMPORT", end)
                }
                nextTimestampStart = end
            }
        if (lastIncremental == "0") {
            logger.info("No incremental import available, doing full sync")
            needsFullSync = true
        }


        if (needsFullSync) {
            importProducts("https://static.openfoodfacts.org/data/openfoodfacts-products.jsonl.gz")
            importIngredientTaxonomies("https://static.openfoodfacts.org/data/taxonomies/ingredients.json")
            importIngredientTaxonomies("https://static.openfoodfacts.org/data/taxonomies/allergens.json")
            MetadataTable.setValue("LAST_OFF_FULL_IMPORT", Instant.now().toString())
            nextTimestampStart?.let { MetadataTable.setValue("LAST_OFF_INCREMENTAL_IMPORT", it) }
        }
    }
}