package dev.metinkale.halalapp

import dev.metinkale.halalapp.server.module
import dev.metinkale.halalapp.worker.openfoodfacts.OpenFoodFactsWorker
import dev.metinkale.halalapp.worker.productprocessor.ProductProcessorWorker
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer

suspend fun main() {
    when (System.getenv("WORKER")) {
        "OPENFOODFACTS" -> OpenFoodFactsWorker.start()
        "PRODUCTPROCESSOR" -> ProductProcessorWorker.start()
        else -> embeddedServer(CIO, port = 8080) { module() }.start(wait = true)
    }
}