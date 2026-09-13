package dev.metinkale.halalapp.feature.product


import dev.metinkale.halalapp.domain.Confidence
import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.db.entity.Classification
import dev.metinkale.halalapp.db.entity.Ingredient
import dev.metinkale.halalapp.db.entity.Product
import dev.metinkale.halalapp.db.table.ProductInsightsTable
import dev.metinkale.halalapp.html.*
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.statusBadgeComponent
import dev.metinkale.halalapp.ui.component.template
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.html.*


fun HTML.productPageProposal(product: Product) = template(
    product.brand + " " + product.name + " Änderung vorschlagen - HalalApp",
    titleBlock = {
        h1 {
            +"Änderung vorschlagen"
        }
        h2 {
            +(product.brand.ifBlank { "Unbekannte Marke" })
            +" - "
            +product.name
        }
        h4 {
            +"Barcode: ${product.id}"
        }
    }) {
    card("flex flex-col items-center [&>h5]:mt-4 gap-4") {

        h5 {
            +"Produktinformationen einreichen"
        }
        +"Hast du eine Mail vom Hersteller oder eine andere Quelle, welche den Status dieses Produktes bestätigt?"
        a(classes = "btn-primary") {
            href = "/crm/product_insights/new?product_id=${product.id}"
            +"Produktinformation einreichen"
        }

        hr { }

        if (product.confidence == Confidence.INGREDIENTS) {
            h5 {
                +"Dublette melden"
            }
            +"Gibt es bereits ein identisches Produkt in der Datenbank, wozu wir Produktinformationen haben?"
            p("italic") { +"(Dubletten können nur zwischen Produkten ohne Produktinformation und Produkten mit Produktinformationen angelegt werden.)" }
            a(classes = "btn-primary") {
                href = "/crm/product_parent/new?child_id=${product.id}"
                +"Dublette melden"
            }

            if (product.unknownIngredients.isNotEmpty()) {
                h5 {
                    +"Unvollständige Zutaten pflegen"
                }
                +"Falls die Zutatenliste nicht richtig erkannt wird, muss die Zutatenliste auf OpenFoodFacts aktualisiert werden. Mit einem Benutzeraccount auf OpenFoodFacts kannst du einfach selber die Zutatenliste aktualisieren."
                a(classes = "btn-secondary") {
                    target = "_blank"
                    href = "https://de.openfoodfacts.org/produkt/${product.id}/"
                    +"Produkt in OpenFoodFacts öffnen"
                }
                +"Für einzelne unbekannte Zutaten, kannst du hier die Informationen in OpenHalal pflegen:"
                div("flex flex-wrap gap-2") {
                    product.unknownIngredients.forEach {
                        a(classes = "btn-primary") {
                            href = "/crm/ingredients/new?id=${it.id}"
                            +it.name
                        }
                    }
                }
            }
        } else {
            h5 {
                +"Dublette melden"
            }
            +"Möchtest du identische Produkte ohne Produktinformationen mit diesem Produkt verbinden?"
            p("italic") { +"(Dubletten können nur zwischen Produkten ohne Produktinformation und Produkten mit Produktinformationen angelegt werden.)" }
            a(classes = "btn-primary") {
                href = "/crm/product_parent/new?parent_id=${product.id}"
                +"Dublette melden"
            }
        }


        h5 {
            +"Anderes Problem melden"
        }
        +"Schreib uns einfach eine E-Mail mit deinem Anliegen oder tausche dich direkt mit unserer Community im Forum aus."
        div("flex gap-2") {
            button(classes = "btn-primary") {
                onClick="sendMail(event, 'Product: ${product.id}')"
                +"E-Mail senden"
            }
            a(classes = "btn-primary") {
                href =
                    "https://forum.halalapp.de"
                +"Zum Forum"
            }
        }

    }
}
