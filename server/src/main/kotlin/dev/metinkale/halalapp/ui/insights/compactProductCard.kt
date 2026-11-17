package dev.metinkale.halalapp.ui.insights

import dev.metinkale.halalapp.service.InsightProduct
import dev.metinkale.halalapp.ui.component.card
import kotlinx.html.*

fun FlowContent.insightPreviewCard(item: InsightProduct) {
    val product = item.product
    a(href = "/product/${product.id}", classes = "block h-full") {
        card("flex h-full flex-col !p-0 overflow-hidden") {
            img(classes = "h-28 w-full shrink-0 bg-gray-100 object-contain") {
                attributes.put("referrerpolicy", "no-referrer")
                loading = ImgLoading.lazy
                src = product.images.getFrontPhoto(0)
            }
            div(classes = "flex flex-1 flex-col gap-1 p-2") {
                div(classes = "truncate text-[10px] font-semibold uppercase tracking-wider text-indigo-600") {
                    +product.brand.ifBlank { "Unbekannt" }
                }
                div(classes = "line-clamp-2 text-sm font-medium leading-snug text-gray-900") {
                    +product.name
                }
                div(classes = "mt-auto flex flex-wrap items-center gap-1 pt-1") {
                    product.status.forEach { status ->
                        span(classes = "inline-flex items-center rounded-md border px-1.5 py-0.5 text-[10px] font-semibold ${status.colorTags}") {
                            +status.value
                        }
                    }
                    span(classes = "ml-auto whitespace-nowrap text-[10px] text-gray-500") {
                        +"${item.date.day.toString().padStart(2, '0')}.${(item.date.month.ordinal + 1).toString().padStart(2, '0')}.${item.date.year}"
                    }
                }
            }
        }
    }
}
