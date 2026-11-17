package dev.metinkale.halalapp.ui.search

import dev.metinkale.halalapp.db.entity.Product
import dev.metinkale.halalapp.domain.Confidence
import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.statusBadgeComponent
import kotlinx.html.*


fun FlowContent.productCard(product: Product) {
    val hasInsights = product.confidence != Confidence.INGREDIENTS
    a(href = "/product/${product.id}") {
        card(
            "flex flex-row w-full !p-0 overflow-hidden items-stretch ${
                if (hasInsights) "ring-2 ring-indigo-200" else ""
            }".trim()
        ) {
            img(classes = "h-24 w-24 object-cover flex-shrink-0") {
                attributes.put("referrerpolicy", "no-referrer")
                loading = ImgLoading.lazy
                src = product.images.getFrontPhoto(0)
            }
            div("flex flex-col p-3 gap-1 flex-1 min-w-0") {

                div(classes = "text-xs font-semibold tracking-wider text-indigo-600 uppercase truncate") {
                    +product.brand.ifBlank { "Unbekannt" }
                }
                div(classes = "flex flex-wrap items-center gap-1.5") {
                    if (hasInsights) {
                        span(classes = "inline-flex items-center rounded-lg border border-indigo-200 bg-indigo-50 px-2.5 py-0.5 text-xs font-bold text-indigo-700") {
                            +when (product.confidence) {
                                Confidence.MANUFACTURER -> "Herstellerinfo"
                                Confidence.VERIFIED -> "Verifiziert"
                                Confidence.INGREDIENTS -> "Laut Zutatenliste"
                            }
                        }
                    }
                    product.status.forEach { status -> statusBadgeComponent(status) }
                    if (!hasInsights) {
                        statusBadgeComponent("Laut Zutatenliste", Status.INCOMPLETE)
                    }
                }
                div(classes = "text-base font-medium text-gray-900 leading-snug truncate") { +(product.name) }
            }
        }
    }
}
