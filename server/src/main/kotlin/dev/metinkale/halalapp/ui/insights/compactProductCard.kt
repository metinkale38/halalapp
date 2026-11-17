package dev.metinkale.halalapp.ui.insights

import dev.metinkale.halalapp.service.InsightProduct
import dev.metinkale.halalapp.ui.component.card
import kotlinx.html.*

fun FlowContent.insightPreviewCard(item: InsightProduct) {
    val product = item.product
    a(href = "/product/${product.id}", classes = "group block h-full") {
        card("flex h-full flex-col !p-0 overflow-hidden transition-shadow group-hover:shadow-lg") {
            div(classes = "relative aspect-[4/3] w-full shrink-0 overflow-hidden bg-gray-50") {
                if (product.images.front.isEmpty()) {
                    div(classes = "absolute inset-0 flex items-center justify-center p-3") {
                        span(classes = "text-xs font-semibold uppercase tracking-wider text-gray-400") {
                            +"Kein Bild"
                        }
                    }
                } else {
                    img(classes = "absolute inset-0 h-full w-full object-contain p-3") {
                        attributes.put("referrerpolicy", "no-referrer")
                        loading = ImgLoading.lazy
                        src = product.images.getFrontPhoto(400)
                    }
                }
            }
            div(classes = "flex flex-1 flex-col gap-1 p-3") {
                div(classes = "truncate text-xs font-semibold uppercase tracking-wider text-indigo-600") {
                    +product.brand.ifBlank { "Unbekannt" }
                }
                div(classes = "line-clamp-2 min-h-[2.5rem] text-sm font-medium leading-snug text-gray-900") {
                    +product.name
                }
                div(classes = "mt-auto flex items-center justify-between gap-2 pt-1") {
                    div(classes = "flex flex-wrap gap-1") {
                        product.status.forEach { status ->
                            span(classes = "inline-flex items-center rounded-md border px-1.5 py-0.5 text-[10px] font-semibold ${status.colorTags}") {
                                +status.value
                            }
                        }
                    }
                    span(classes = "whitespace-nowrap text-[11px] text-gray-500") {
                        +"${item.date.day.toString().padStart(2, '0')}.${(item.date.month.ordinal + 1).toString().padStart(2, '0')}.${item.date.year}"
                    }
                }
            }
        }
    }
}
