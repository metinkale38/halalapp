package dev.metinkale.halalapp.routes

import dev.metinkale.halalapp.server.isAdmin
import dev.metinkale.halalapp.server.public.html.barcodeScannerPage
import dev.metinkale.halalapp.server.public.html.impressumPage
import dev.metinkale.halalapp.ui.static.datenschutzPage
import dev.metinkale.halalapp.ui.static.indexPage
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.header
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

fun Routing.staticRoutes() {
    get("") { call.respondHtml { indexPage(call.isAdmin()) } }
    get("/scan") { call.respondHtml { barcodeScannerPage(call.isAdmin()) } }
    get("/impressum") {
        call.response.header("X-Robots-Tag", "noindex")
        call.respondHtml { impressumPage(call.isAdmin()) }
    }
    get("/datenschutz") { call.respondHtml { datenschutzPage(call.isAdmin()) } }
    staticResources("/", "static")
}