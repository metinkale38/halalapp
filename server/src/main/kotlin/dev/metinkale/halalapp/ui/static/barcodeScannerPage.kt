package dev.metinkale.halalapp.server.public.html

import dev.metinkale.halalapp.html.title
import dev.metinkale.halalapp.ui.component.template
import kotlinx.html.ButtonType
import kotlinx.html.HTML
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.id
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.unsafe

private const val TORCH_ICON =
    """<svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" /></svg>"""

private const val SWITCH_CAMERA_ICON =
    """<svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" /></svg>"""

fun HTML.barcodeScannerPage(isAdmin: Boolean = false) = template(
    title = "HalalApp - Barcode scannen",
    head = { script { src = "/libs/html5-qrcode/html5-qrcode.min.js"; defer = true } },
    isAdmin = isAdmin,
    titleBlock = {
        h2 { +"Barcode scannen" }
    }) {
    div("bg-white shadow-md rounded-lg overflow-hidden") {
        div("relative w-full aspect-square bg-black") {
            div("h-full w-full") {
                id = "scanner"
            }

            div("absolute inset-0 z-10 flex flex-col items-center justify-center gap-3 bg-black/60 px-6 text-center text-white") {
                id = "scanner-status"
                div("h-10 w-10 animate-spin rounded-full border-4 border-white/30 border-t-white") {}
                p("text-base font-medium") {
                    id = "scanner-status-text"
                    +"Kamera wird gestartet…"
                }
            }

            div("pointer-events-none absolute inset-0 flex items-center justify-center") {
                div("scanner-frame h-1/4 w-4/5 rounded-lg") {}
            }

            div("pointer-events-none absolute inset-x-0 top-4 mx-auto w-max rounded-full bg-black/60 px-4 py-2 text-sm text-white") {
                id = "scanner-hint"
                style = "display: none;"
                +"Barcode in den Rahmen halten"
            }

            div("absolute inset-x-0 bottom-0 flex justify-center gap-4 bg-gradient-to-t from-black/60 to-transparent p-4") {
                button(classes = "hidden rounded-full bg-black/60 p-3 text-white transition-colors hover:bg-black/80 active:bg-black/80 disabled:opacity-40") {
                    id = "torch-button"
                    type = ButtonType.button
                    attributes["aria-label"] = "Blitzlicht umschalten"
                    attributes["aria-pressed"] = "false"
                    unsafe { raw(TORCH_ICON) }
                }
                button(classes = "hidden rounded-full bg-black/60 p-3 text-white transition-colors hover:bg-black/80 active:bg-black/80 disabled:opacity-40") {
                    id = "switch-camera-button"
                    type = ButtonType.button
                    attributes["aria-label"] = "Kamera wechseln"
                    unsafe { raw(SWITCH_CAMERA_ICON) }
                }
            }

            div("absolute inset-0 z-20 flex-col items-center justify-center gap-4 bg-black/80 p-6 text-center text-white") {
                id = "scanner-error"
                style = "display: none;"
                p("text-base font-medium") {
                    id = "scanner-error-text"
                }
                button(classes = "btn-primary") {
                    id = "scanner-retry"
                    type = ButtonType.button
                    +"Erneut versuchen"
                }
            }
        }
    }
    p("text-center text-sm text-gray-600") {
        +"Die Kamera startet automatisch. Erlaube dazu den Kamerazugriff im Browser."
    }
    script {
        src = "/barcode.js"
        defer = true
    }
}
