package dev.metinkale.halalapp.worker.openfoodfacts

import dev.metinkale.halalapp.db.entity.ProductComponent
import dev.metinkale.halalapp.db.entity.ProductImages
import dev.metinkale.halalapp.db.table.ProductsTable
import dev.metinkale.halalapp.worker.productprocessor.CategoryResolver
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.GZIPInputStream

private object ProductTable_OFF : ProductsTable("products_off") {
    val lastSeen = timestamp("last_seen")
    val categoriesHierarchy = array("categories_hierarchy", TextColumnType())
    val categoryId = text("category_id").nullable()
}


private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}
private val veg_by_brand = listOf("en:vegetarian", "en:vegan")
private val veg_by_thirdparty = listOf("en:european-vegetarian-union", "en:vegan-society", "en:v-label")
private val alcohol = listOf(
    "en:beers",
    "en:wines",
    "en:spirits",
    "en:liqueurs",
    "en:ciders",
    "en:aperitifs",
    "en:alcoholic-beverages"
)
private val meat = listOf("en:meats-and-their-products", "en:prepared-meats", "en:meats")


suspend fun OpenFoodFactsWorker.importProducts(url: String) {
    CategoryResolver.reload()
    val now = Instant.now()
    val startTime = System.currentTimeMillis()
    val counter = AtomicInteger(0)
    client.prepareGet(url).execute { response ->
        response.bodyAsChannel().toInputStream().let { inputStream ->
            GZIPInputStream(inputStream).bufferedReader().use { reader ->
                reader.lineSequence().chunked(5000).forEach { chunk ->
                    transaction {
                        ProductTable_OFF.batchUpsert(
                            chunk.onEach {
                                val cnt = counter.incrementAndGet()
                                if (cnt % 10000 == 0) {
                                    val seconds = (System.currentTimeMillis() - startTime) / 1000
                                    val speed = if (seconds > 0) cnt / seconds else 0
                                    println("Gelesen: $cnt | Speed: ~ $speed Zeilen/Sek.")
                                }
                            }.filter { it.contains("en:germany") }
                                .map { json.decodeFromString(OFFProduct.serializer(), it) }
                                .filter {
                                    it.countries_tags.contains("en:germany") && it.languages_tags.contains("en:german") &&
                                            listOfNotNull(
                                                it.product_name_de,
                                                it.product_name_en,
                                                it.product_name
                                            ).any { it.isNotBlank() }
                                }.filter { it.product_name_de?.isBlank() == true }
                        ) { p ->

                            val frontImages: Map<Int, String> =
                                p.images?.selected?.front?.takeIf { it.isNotEmpty() }?.let { langs ->
                                    val lang =
                                        listOf("de", "en", langs.keys.first()).firstOrNull { langs.containsKey(it) }
                                    val image = langs[lang]!!
                                    val padded = p.code.padStart(13, '0')
                                    val p1 = padded.substring(0, 3)
                                    val p2 = padded.substring(3, 6)
                                    val p3 = padded.substring(6, 9)
                                    val p4 = padded.substring(9, 13)

                                    image.sizes.entries.associate { (k, v) ->
                                        val filename = "front_${lang}.${image.rev}.${k}"
                                        val url =
                                            "https://images.openfoodfacts.org/images/products/$p1/$p2/$p3/$p4/$filename.jpg"
                                        v.w to url
                                    }
                                } ?: emptyMap()

                            val tags = mutableListOf("openfoodfacts")

                            if (p.tags_sources?.labels?.packaging?.tags?.any { it in veg_by_thirdparty } == true) {
                                tags.add("verified_v_label")
                            } else if (p.tags_sources?.labels?.packaging?.tags?.any { it in veg_by_brand } == true) {
                                tags.add("v_label")
                            }

                            if (p.tags_sources?.labels?.packaging?.tags?.any { it == "en:halal" } == true) {
                                tags.add("halal_label")
                            }



                            if (p.categories_tags.any { it in alcohol }) tags.add("alcoholic_beverage")
                            if (p.categories_tags.any { it in meat }) tags.add("meat")

                            val components = p.ingredients.flatMap { map(it) }
                            val categoryId = CategoryResolver.resolveForProduct(
                                p.categories_hierarchy,
                                tags,
                                components.size,
                            )

                            this[ProductTable_OFF.id] = p.code
                            this[ProductTable_OFF.name] = listOfNotNull(
                                p.product_name_de, p.product_name_en, p.product_name
                            ).first { it.isNotBlank() }
                            this[ProductTable_OFF.brand] = p.brands ?: "Unbekannt"
                            this[ProductTable_OFF.ingredientsText] =
                                listOfNotNull(
                                    p.ingredients_text_de,
                                    p.ingredients_text_en,
                                    p.ingredients_text
                                ).firstOrNull { it.isNotBlank() } ?: ""
                            this[ProductTable_OFF.ingredients] = components
                            this[ProductTable_OFF.allergens] = p.allergens_tags
                            this[ProductTable_OFF.traces] = p.traces_tags
                            this[ProductTable_OFF.images] = ProductImages(frontImages)
                            this[ProductTable_OFF.tags] = tags
                            this[ProductTable_OFF.categoriesHierarchy] = p.categories_hierarchy
                            this[ProductTable_OFF.categoryId] = categoryId
                            this[ProductTable_OFF.lastSeen] = now
                        }

                    }


                }
                println("Import abgeschlossen.")
            }
        }
    }
}


private fun map(ingredient: OFFProduct.Ingredient): List<ProductComponent> = listOf(
    ProductComponent(
        ingredient.id, ingredient.text
    )
) + ingredient.ingredients.flatMap { map(it) }
