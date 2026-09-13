package dev.metinkale.halalapp.ui.component

import dev.metinkale.halalapp.domain.Status
import kotlinx.html.FlowContent
import kotlinx.html.span




fun FlowContent.statusBadgeComponent(status: Status) {
    span(classes = "inline-flex px-2.5 py-0.5 rounded-lg text-xs font-bold border font-medium ${status.colorTags}") { +status.value }
}

fun FlowContent.statusBadgeComponent(text: String, status: Status) {
    span(classes = "inline-flex px-2.5 py-0.5 rounded-lg text-xs font-bold border ${status.colorTags}") { +text }
}