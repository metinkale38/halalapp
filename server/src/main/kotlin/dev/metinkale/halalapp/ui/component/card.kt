package dev.metinkale.halalapp.ui.component

import kotlinx.html.DIV
import kotlinx.html.FlowContent
import kotlinx.html.div

fun FlowContent.card(classes: String? = null, content: DIV.() -> Unit) {
    div(classes = "bg-white shadow-md rounded-lg p-3 md:p-6 $classes".trim()) {
        content()
    }
}