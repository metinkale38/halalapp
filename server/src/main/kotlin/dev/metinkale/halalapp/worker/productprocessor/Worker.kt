package dev.metinkale.halalapp.worker.productprocessor

import dev.metinkale.halalapp.db.DatabaseFactory
import dev.metinkale.halalapp.db.table.ClassificationsTable
import dev.metinkale.halalapp.db.table.IngredientsTable
import dev.metinkale.halalapp.db.table.ProductsTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.selectAll
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
            val products = load()
            val processedProducts = process(products)
            persist(processedProducts)
            logger.info("Updated ${processedProducts.size} products")
        }
    }


    private fun JdbcTransaction.deleteOrphanedProcessedProducts() {
        exec("DELETE FROM products_processed pp USING products p WHERE pp.id = p.id AND p.id IS NULL;")
        exec("DELETE FROM products_processed p using product_parent pp where p.id = pp.child_id")
    }

    fun processOne(id: String) {
        transaction {
            ClassificationsTable.cacheAll()
            IngredientsTable.cacheAll()
            val product = ProductsTable.selectAll()
                .where { ProductsTable.id eq id }
                .singleOrNull()
                ?.let { mapToProduct(it) }
                ?: return@transaction
            persist(process(listOf(product)))
            logger.info("Updated product $id")
        }
    }
}
