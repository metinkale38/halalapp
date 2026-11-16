package dev.metinkale.halalapp.ui.detail

import dev.metinkale.halalapp.ui.component.template
import kotlinx.html.*

fun HTML.productNotFoundPage() = template("Produkt nicht gefunden - HalalApp") {
    div(classes = "flex flex-col items-center justify-center py-20 px-4 text-center") {

        div(classes = "text-6xl mb-6") { +"🔍" }

        h1(classes = "text-4xl font-extrabold text-gray-900 tracking-tight mb-4") {
            +"Produkt nicht gefunden"
        }

        p(classes = "text-lg text-gray-600 max-w-md mb-8") {
            +"Tut uns leid, wir konnten das gewünschte Produkt oder die Seite in unserer Datenbank nicht finden. Bitte überprüfe den Barcode oder kehre zur Startseite zurück."
        }

        div(classes = "flex flex-wrap justify-center gap-4") {
            a(classes = "px-6 py-3 bg-primary text-white font-semibold rounded-xl hover:bg-primary-600 transition-colors") {
                href = "/scan"
                +"Erneut scannen"
            }
            a(classes = "px-6 py-3 bg-gray-200 text-gray-900 font-semibold rounded-xl hover:bg-gray-300 transition-colors") {
                href = "/"
                +"Zurück zur Startseite"
            }
        }
    }
}