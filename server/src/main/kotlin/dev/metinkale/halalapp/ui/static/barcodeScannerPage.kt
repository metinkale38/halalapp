package dev.metinkale.halalapp.server.public.html

import dev.metinkale.halalapp.ui.component.template
import dev.metinkale.halalapp.html.title
import dev.metinkale.halalapp.ui.component.card
import kotlinx.html.HTML
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.id
import kotlinx.html.script

fun HTML.barcodeScannerPage() = template(
    title = "HalalApp - Batcode Scannen",
    head = { script { src = "/libs/html5-qrcode/html5-qrcode.min.js" } },
    titleBlock = {
        h2 { +"Barcode Scannen" }
    }) {
    card {
        div("w-full aspect-square") {
            id = "scanner"
        }
        script { src = "/barcode.js" }
    }
}