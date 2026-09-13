package dev.metinkale.halalapp.html

import kotlinx.html.*

fun FlowContent.errorComponent(content: Tag.() -> Unit) {
    div(classes = "mt-4 p-3 bg-red-100 border border-red-400 rounded-md") {
        div(classes = "flex items-center") {
            span(classes = "text-red-700 mr-2") { +"❌" }
            div {
                p(classes = "text-xs text-red-700") {
                    content()
                }
            }
        }
    }
}

fun FlowContent.warningComponent(content: Tag.() -> Unit) {
    div(classes = "mt-4 p-3 bg-red-50 border border-red-200 rounded-md") {
        div(classes = "flex items-center") {
            span(classes = "text-red-600 mr-2") { +"⚠️" }
            div {
                p(classes = "text-xs text-red-600") {
                    content()
                }
            }
        }
    }
}

fun FlowContent.infoComponent(content: Tag.() -> Unit) {
    div(classes = "mt-4 p-3 bg-blue-50 border border-blue-200 rounded-md") {
        div(classes = "flex items-center") {
            span(classes = "text-blue-600 mr-2") { +"ℹ️" }
            div {
                p(classes = "text-xs text-blue-600") {
                    content()
                }
            }
        }
    }
}