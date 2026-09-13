package dev.metinkale.halalapp.html

import kotlinx.html.HEAD
import kotlinx.html.TEXTAREA
import kotlinx.html.id
import kotlinx.html.link
import kotlinx.html.script
import kotlinx.html.unsafe

fun HEAD.loadEasyMDE() {
    link {
        rel = "stylesheet"
        href = "/libs/easymde/easymde.min.css"
    }
    script {
        src = "/libs/easymde/easymde.min.js"
    }
}

fun FormGroupContent.markdownField(
    label: String,
    name: String,
    value: String? = null,
    block: TEXTAREA.() -> Unit = {},
) {
    textAreaField(label, name, value) {
        id = "$name-mde"
        block()
    }
    script {
        unsafe { +"const easyMDE = new EasyMDE({element: document.getElementById('$name-mde'), spellChecker: false});" }
    }
}