package dev.metinkale.halalapp.routes

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
    get("") { call.respondHtml { indexPage() } }
    get("/scan") { call.respondHtml { barcodeScannerPage() } }
    get("/impressum") {
        call.response.header("X-Robots-Tag", "noindex")
        call.respondHtml { impressumPage() }
    }
    get("/datenschutz") { call.respondHtml { datenschutzPage() } }
    staticResources("/", "static")
}