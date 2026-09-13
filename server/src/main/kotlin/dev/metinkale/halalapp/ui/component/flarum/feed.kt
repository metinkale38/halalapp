package dev.metinkale.halalapp.ui.component.flarum

import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.h3
import kotlinx.html.h4
import java.time.Duration
import java.time.Instant


fun FlowContent.renderFlarumFeed(feed: FlarumResponse?) {
    div(classes = "overflow-hidden bg-white shadow-md rounded-lg") {
        // Header
        div(classes = "p-4 md:p-6 border-b border-gray-100 bg-gray-50") {
            h4 {
                +"Ankündigungen aus dem HalalApp-Forum"
            }
        }

        // Feed-Liste
        div(classes = "divide-y divide-gray-100") {
            feed?.data?.take(3)?.forEach { discussion ->
                a(
                    href = "https://forum.halalapp.de/d/${discussion.id}-${discussion.attributes.slug}",
                    classes = "block p-4 md:px-6 hover:bg-gray-50 transition-colors"
                ) {
                    // Titel
                    h3(classes = "text-gray-900 mb-1") { +discussion.attributes.title }

                    // Zeit & Meta
                    div(classes = "text-xs text-gray-600") {
                        +formatTimeAgo(discussion.attributes.createdAt)
                    }
                }
            } ?: div(classes = "p-6 text-gray-500") { +"Keine Ankündigungen gefunden." }
        }

        // Footer Link
        div(classes = "p-3 bg-gray-50 text-center border-t border-gray-100") {
            a(href = "https://forum.halalapp.de", classes = "text-sm text-blue-600 hover:underline font-medium") {
                +"Alle Themen ansehen →"
            }
        }
    }
}


private fun formatTimeAgo(isoString: String?): String {
    if (isoString == null) return ""
    val past = Instant.parse(isoString)
    val now = Instant.now()
    val minutes = Duration.between(past, now).toMinutes()

    return when {
        minutes < 60 -> "vor $minutes Minuten"
        minutes < 1440 -> "vor ${minutes / 60} Stunden"
        else -> "vor ${minutes / 1440} Tagen"
    }
}