package dev.metinkale.halalapp.server

import dev.metinkale.halalapp.db.DatabaseFactory
import dev.metinkale.halalapp.routes.*
import io.ktor.server.application.*
import io.ktor.server.routing.*


fun Application.module() {
    DatabaseFactory.init()

    authConfig()
    errorPageConfig()
    statsConfig()

    routing {
        cachingPolicy()

        staticRoutes()
        detailRoutes()
        searchRoutes()
        crmRoutes()
        deployRoutes()
    }
}
