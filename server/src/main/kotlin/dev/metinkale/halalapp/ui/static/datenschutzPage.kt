package dev.metinkale.halalapp.ui.static

import dev.metinkale.halalapp.ui.component.template
import dev.metinkale.halalapp.ui.component.card
import kotlinx.html.*

fun HTML.datenschutzPage() = template(
    "Datenschutzerklärung",
    titleBlock = {
        h1 { +"Datenschutzerklärung" }
        h3 { +"Transparenz und Datenschutz für unsere Halal-App" }
    }) {
    card("flex flex-col gap-4") {
        div(classes = "space-y-8 text-slate-700") {

            // Header Area with a clean subtle badge
            div(classes = "space-y-3") {

                p(classes = "text-slate-600 leading-relaxed text-sm sm:text-base") {
                    +"Der Schutz deiner Daten ist uns wichtig. Nachfolgend informieren wir dich kurz und transparent darüber, wie wir mit deinen Daten umgehen. Verantwortlich für die Datenverarbeitung ist der Betreiber dieser App (siehe "
                    a(
                        href = "/impressum",
                        classes = "text-emerald-600 font-medium hover:text-emerald-700 underline decoration-emerald-300 underline-offset-4 transition-colors"
                    ) { +"Impressum" }
                    +")."
                }
            }

            // Divider
            div(classes = "h-px bg-gradient-to-r from-transparent via-slate-200 to-transparent")

            // Content Sections
            div(classes = "space-y-6 text-sm sm:text-base") {

                // Section 1
                div(classes = "group relative pl-4 border-l-2 border-emerald-500 space-y-1.5") {
                    h3(classes = "font-semibold text-slate-900 text-base") { +"1. Nutzung ohne Login" }
                    p(classes = "text-slate-600 leading-relaxed") {
                        +"Solange du nicht eingeloggt bist, werden "
                        span(classes = "font-semibold text-slate-900 bg-slate-100 px-1.5 py-0.5 rounded") { +"keinerlei" }
                        +" Daten gespeichert. Die App kann komplett anonym genutzt werden."
                    }
                }

                // Section 2
                div(classes = "group relative pl-4 border-l-2 border-emerald-500 space-y-2") {
                    h3(classes = "font-semibold text-slate-900 text-base") { +"2. Nutzung mit Login" }
                    p(classes = "text-slate-600 leading-relaxed") {
                        +"Wenn du dich in dein Benutzerkonto einloggst, werden ausschließlich folgende Daten gespeichert, um die Funktionalität des Forums und deiner Änderungsvorschläge zu gewährleisten:"
                    }

                    // Modern Grid for Login Data Points
                    div(classes = "grid grid-cols-1 sm:grid-cols-3 gap-2.5 pt-1") {
                        div(classes = "p-3 rounded-xl bg-slate-50 border border-slate-200 text-center font-medium text-slate-800 text-xs sm:text-sm shadow-2xs") {
                            +"Benutzername"
                        }
                        div(classes = "p-3 rounded-xl bg-slate-50 border border-slate-200 text-center font-medium text-slate-800 text-xs sm:text-sm shadow-2xs") {
                            +"E-Mail-Adresse"
                        }
                        div(classes = "p-3 rounded-xl bg-slate-50 border border-slate-200 text-center font-medium text-slate-800 text-xs sm:text-sm shadow-2xs") {
                            +"Passwort-Hash"
                        }
                    }

                    p(classes = "text-slate-400 text-xs italic pt-0.5") {
                        +"Darüber hinaus findet keine Datenspeicherung statt."
                    }
                }

                // Section 3
                div(classes = "group relative pl-4 border-l-2 border-emerald-500 space-y-1.5") {
                    h3(classes = "font-semibold text-slate-900 text-base") { +"3. Verwendung der E-Mail-Adresse" }
                    p(classes = "text-slate-600 leading-relaxed") {
                        +"Deine E-Mail-Adresse kann vom Administrator oder Moderator verwendet werden, um dich bei Rückfragen zu deinen eingereichten Änderungsvorschlägen zu kontaktieren. Deine E-Mail-Adresse wird darüber hinaus "
                        span(classes = "font-semibold text-red-600") { +"niemals" }
                        +" mit Dritten geteilt."
                    }
                }

                // Section 4
                div(classes = "group relative pl-4 border-l-2 border-emerald-500 space-y-1.5") {
                    h3(classes = "font-semibold text-slate-900 text-base") { +"4. Deine Rechte" }
                    p(classes = "text-slate-600 leading-relaxed") {
                        +"Du hast jederzeit das Recht auf Auskunft über deine gespeicherten Daten sowie auf deren Löschung (z. B. durch die Löschung deines Accounts)."
                    }
                }

            }
        }
    }
}