package dev.metinkale.halalapp.html

import kotlinx.html.FlowContent
import kotlinx.html.h1

fun FlowContent.title(title: String) {
    h1(classes = "text-2xl font-extrabold text-primary-600 mb-8 tracking-tight text-center") {
        +title
    }
}