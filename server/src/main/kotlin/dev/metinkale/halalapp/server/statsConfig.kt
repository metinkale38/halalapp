package dev.metinkale.halalapp.server

import dev.metinkale.halalapp.service.StatsService
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.path

fun Application.statsConfig() {
    if (System.getenv("ENABLE_STATS") == "true") {
        intercept(ApplicationCallPipeline.Plugins) {
            proceed()
            val statusCode = call.response.status()?.value ?: 0
            if (statusCode in 200..299) {
                val path = call.request.path()
                StatsService.increment(path)
            }
        }
    }
}