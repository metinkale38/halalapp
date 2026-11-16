package dev.metinkale.halalapp.ui.customproducts

import dev.metinkale.halalapp.db.entity.CustomProduct
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.template
import kotlinx.html.*

fun HTML.customProductListPage(products: List<CustomProduct>) = template(
    "Bestehende Custom-Produkte - HalalApp",
    titleBlock = {
        h2 { +"Custom-Produkte" }
        h4 { +"Bestehende Produkte verwalten" }
    },
) {
    div(classes = "flex flex-wrap items-center justify-between gap-3") {
        p(classes = "text-sm text-gray-600") {
            +"Bearbeite Name, Marke oder Frontfoto bestehender Custom-Produkte."
        }
        div(classes = "flex flex-wrap gap-2") {
            a(classes = "btn-secondary") {
                href = "/crm/products_custom"
                +"Zu den Vorschlägen"
            }
            a(classes = "btn-secondary") {
                href = "/crm/products_custom/history"
                +"Verlauf"
            }
        }
    }

    if (products.isEmpty()) {
        card {
            p(classes = "text-sm text-gray-600") { +"Noch keine Custom-Produkte vorhanden." }
        }
    } else {
        div(classes = "grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3") {
            products.forEach { product ->
                card("flex flex-row gap-3 items-center !p-3") {
                    val photo = product.images.getFrontPhoto(0)
                        .let { if (it == "/nophoto.webp") "/nophoto.100.webp" else it }
                    div(classes = "h-16 w-16 shrink-0 overflow-hidden rounded-lg bg-gray-100") {
                        img(classes = "h-16 w-16 object-cover") {
                            src = photo
                            alt = product.name
                            loading = ImgLoading.lazy
                            style = "width:64px;height:64px;object-fit:cover;"
                        }
                    }
                    div(classes = "min-w-0 grow") {
                        p(classes = "text-xs font-semibold uppercase tracking-wide text-indigo-600 truncate") {
                            +product.brand
                        }
                        p(classes = "text-sm font-semibold text-gray-900 truncate") { +product.name }
                        p(classes = "text-xs text-gray-500 truncate") { +product.id }
                    }
                    div(classes = "flex flex-col gap-1") {
                        a(classes = "text-sm font-semibold text-indigo-600 hover:underline") {
                            href = "/crm/products_custom/${product.id}/edit"
                            +"Bearbeiten"
                        }
                        a(classes = "text-xs text-gray-500 hover:underline") {
                            href = "/product/${product.id}"
                            +"Ansehen"
                        }
                    }
                }
            }
        }
    }
}
