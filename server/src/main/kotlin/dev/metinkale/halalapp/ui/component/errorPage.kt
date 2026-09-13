package dev.metinkale.halalapp.html

import dev.metinkale.halalapp.ui.component.template
import kotlinx.html.*

fun HTML.errorPage(
    errorCode: String,
    title: String,
    subtilte: String,
) = template("Fehler - HalalApp") {
    div(classes = "flex flex-col items-center justify-center min-h-[60vh] px-4 text-center") {
        h1(classes = "text-8xl font-black text-gray-200 mb-2") { +errorCode }

        h2(classes = "text-2xl font-bold text-gray-900 mb-4") {
            +errorCode
        }

        p(classes = "text-gray-500 max-w-sm mb-8") {
            +subtilte
        }

        // Haupt-CTA
        a(classes = "px-8 py-3 bg-indigo-600 text-white font-semibold rounded-xl hover:bg-indigo-700 transition-all shadow-lg hover:shadow-indigo-200") {
            href = "/"
            +"Zurück zur Startseite"
        }
    }
}

fun HTML.notFoundPage() = errorPage(
    errorCode = "404",
    title = "Seite nicht gefunden",
    subtilte = "Hoppla! Die von dir gesuchte Seite existiert nicht oder wurde verschoben.",
)