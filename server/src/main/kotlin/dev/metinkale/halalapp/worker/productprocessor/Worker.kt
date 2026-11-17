package dev.metinkale.halalapp.worker.productprocessor

import dev.metinkale.halalapp.db.DatabaseFactory
import dev.metinkale.halalapp.db.table.ClassificationsTable
import dev.metinkale.halalapp.db.table.IngredientsTable
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory

object ProductProcessorWorker {

    private val logger = LoggerFactory.getLogger("WORKER_PRODUCTPROCESSOR")

    fun start() {
        DatabaseFactory.init(2)

        transaction {
            deleteOrphanedProcessedProducts()
            ClassificationsTable.cacheAll()
            IngredientsTable.cacheAll()
            CategoryResolver.reload()
            val products = load()
            val processedProducts = process(products)
            persist(processedProducts)
            logger.info("Updated ${processedProducts.size} products")
        }
    }


    private fun JdbcTransaction.deleteOrphanedProcessedProducts() {
        exec(
            """
            DELETE FROM products_processed pp
            WHERE NOT EXISTS (SELECT 1 FROM products_off o WHERE o.id = pp.id)
              AND NOT EXISTS (SELECT 1 FROM products_custom c WHERE c.id = pp.id)
            """.trimIndent()
        )
        exec("DELETE FROM products_processed p using product_parent pp where p.id = pp.child_id")
    }

    fun processOne(id: String) {
        transaction {
            ClassificationsTable.cacheAll()
            IngredientsTable.cacheAll()
            CategoryResolver.reload()
            val product = loadSource(id) ?: return@transaction
            persist(process(listOf(product)))
            logger.info("Updated product $id")
        }
    }
}
