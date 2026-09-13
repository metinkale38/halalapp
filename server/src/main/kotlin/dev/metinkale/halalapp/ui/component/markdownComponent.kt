package dev.metinkale.halalapp.html

import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.unsafe
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

fun FlowContent.markdownComponent(src: String) {
    val flavour = CommonMarkFlavourDescriptor()
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(src)
    val html = HtmlGenerator(src, parsedTree, flavour).generateHtml()

    div("prose prose-slate max-w-none prose-a:text-blue-600 prose-a:underline hover:prose-a:text-blue-800 prose-hr:my-4 prose-hr:border-t-2") {
        unsafe { +html }
    }
}