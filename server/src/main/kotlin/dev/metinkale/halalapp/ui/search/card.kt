package dev.metinkale.halalapp.ui.search

import dev.metinkale.halalapp.db.entity.Product
import dev.metinkale.halalapp.domain.Confidence
import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.statusBadgeComponent
import kotlinx.html.*


fun FlowContent.productCard(product: Product) {
    a(href = "/product/${product.id}") {
        card("flex flex-row w-full !p-0 overflow-hidden items-stretch") {
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
                    product.status.forEach { status -> statusBadgeComponent(status) }
                    if (product.confidence == Confidence.INGREDIENTS) {
                        statusBadgeComponent("Laut Zutatenliste", Status.INCOMPLETE)
                    }
                }
                div(classes = "text-base font-medium text-gray-900 leading-snug truncate") { +(product.name) }
            }
        }
    }
}

