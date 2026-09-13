package dev.metinkale.halalapp.server.public.html

import dev.metinkale.halalapp.ui.component.template
import dev.metinkale.halalapp.ui.component.card
import kotlinx.html.*

fun HTML.impressumPage() = template("Impressum", {
    meta(name = "robots", content = "noindex, nofollow")
},titleBlock = {
    h1 { +"Impressum" }
}) {
    card {

        // Anbieterkennzeichnung
        section(classes = "mb-8") {
            h2(classes = "text-xl font-semibold mb-2") { +"Angaben gemäß § 5 TMG" }
            div(classes = "text-gray-600 space-y-1") {
                listOf("Metin Kale", "Starenweg 77", "38122 Braunschweig")
                    .forEach { line -> p { obfuscatedText(line) } }
            }
        }

        section(classes = "mb-8") {
            h2(classes = "text-xl font-semibold mb-2") { +"Kontakt" }
            p(classes = "text-gray-600") {
                +"E-Mail: "
                obfuscatedText("info@halalapp.de")
            }
        }

        // Deine spezielle Rolle als Aggregator
        section(classes = "mb-8") {
            h2(classes = "text-xl font-semibold mb-2") { +"Haftungsausschluss & Transparenz" }
            p(classes = "text-gray-600 leading-relaxed mb-4") {
                +"Diese App versteht sich als reiner Informations-Aggregator. Wir stellen Daten über Lebensmittelzutaten, Herstellerinformationen sowie weiterer Quellen zur Verfügung, um Verbrauchern eine transparente Entscheidungsfindung zu ermöglichen."
            }
            p(classes = "text-gray-600 leading-relaxed mb-4") {
                +"Die in der App dargestellten Einstufungen (z.B. 'Unbedenklich', 'Haram' etc.) sind automatisierte Auswertungen basierend auf den von der OpenFoodFacts-Datenbank bereitgestellten Daten, Herstellerangaben oder dritten Quellen. "
                +"Sie stellen keine verbindliche rechtliche oder religiöse Empfehlung dar."
            }
            p(classes = "text-gray-600 font-medium") {
                +"Wichtiger Hinweis: Daten basieren auf externen Quellen (OFF) und können unvollständig oder fehlerhaft sein. Wir übernehmen keine Haftung für die Richtigkeit der Zutatenlisten oder der daraus abgeleiteten Einstufungen. Bitte prüfen Sie vor dem Verzehr stets das Etikett am Originalprodukt."
            }
        }

        section(classes = "mb-8") {
            h2(classes = "text-xl font-semibold mb-2") { +"Quellen & Lizenzen" }
            p(classes = "text-gray-600") {
                +"Diese App nutzt Daten der "
                a(
                    href = "https://world.openfoodfacts.org/",
                    classes = "text-blue-600 hover:underline"
                ) { +"OpenFoodFacts-Datenbank" }
                +", welche unter der "
                a(
                    href = "https://opendatacommons.org/licenses/odbl/1.0/",
                    classes = "text-blue-600 hover:underline"
                ) { +"Open Database License (ODbL)" }
                +" stehen."
            }
        }
    }
}

private fun FlowContent.obfuscatedText(text: String) {
    val mapped = text.mapIndexed { index, char ->
        val displayChar = if (char == ' ') "\u00A0" else char.toString()
        Pair(displayChar, index)
    }.shuffled()

    // 2. HTML-Struktur aufbauen
    div(classes = "inline-flex flex-row select-none") {
        mapped.forEach { (char, position) ->
            span {
                style = "order: $position;"
                +char
            }
        }
    }
}
