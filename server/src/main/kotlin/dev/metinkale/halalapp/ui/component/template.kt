package dev.metinkale.halalapp.ui.component

import kotlinx.html.*
import java.time.LocalDate


private val year = LocalDate.now().year
fun HTML.template(
    title: String = "HalalApp",
    head: HEAD.() -> Unit = {},
    titleBlock: (FlowContent.() -> Unit)? = null,
    body: FlowContent.() -> Unit,
) {
    lang = "de"

    head {
        meta { charset = "UTF-8" }
        meta {
            name = "viewport"
            content = "width=device-width, initial-scale=1.0"
        }
        link {
            rel = "apple-touch-icon"
            sizes = "180x180"
            href = "/icons/icon-180.webp"
        }
        link {
            rel = "icon"
            type = "image/webp"
            sizes = "32x32"
            href = "/icons/icon-32.webp"
        }
        link {
            rel = "icon"
            type = "image/webp"
            sizes = "16x16"
            href = "/icons/icon-16.webp"
        }
        link {
            rel = "manifest"
            href = "/manifest.json"
        }
        link {
            rel = "stylesheet"
            href = "/style.css"
        }
        script { src = "/script.js" }
        title(title)

        meta {
            attributes["property"] = "og:title"
            content = title
        }
        meta {
            attributes["property"] = "og:description"
            content =
                "Mach deinen Einkauf einfach: Mit der HalalApp erkennst du Halal-Produkte sofort. Dein smarter Begleiter für jeden Supermarkt-Besuch."
        }
        meta {
            attributes["property"] = "og:image"
            content = "/assets/img.webp"
        }
        meta {
            name = "description"
            content =
                "Mach deinen Einkauf einfach: Mit der HalalApp erkennst du Halal-Produkte sofort. Dein smarter Begleiter für jeden Supermarkt-Besuch."
        }
        head.invoke(this)
    }

    body("min-h-screen bg-gray-100 group/body flex flex-col") {
        nav(classes = "bg-primary text-white h-13 sticky top-0") {
            role = "navigation"
            div("mx-auto w-app max-w-screen px-[15px] flex items-center justify-between mx-4 h-full") {
                button(classes = "group-has-[.no-back-button]/body:hidden p-2 hover:bg-primary-700 active:bg-primary-700 rounded-full transition-colors") {
                    attributes["aria-label"] = "Zurück"
                    onClick = "history.back()"
                    unsafe {
                        raw("""<svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M15 19l-7-7 7-7" /></svg>""")
                    }
                }
                a(classes = "text-xl grow") {
                    href = "/"
                    +"HalalApp"
                }

                div(classes = "flex gap-2 items-center") {

                    a {
                        href = "/"
                        button(classes = "p-2 hover:bg-primary-700 active:bg-primary-700 rounded-full transition-colors") {
                            attributes["aria-label"] = "Suche"
                            unsafe {
                                raw("""<svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>""")
                            }
                        }
                    }

                    a {
                        href = "/scan"
                        button(classes = "p-2 hover:bg-primary-700 active:bg-primary-700 rounded-full transition-colors") {
                            attributes["aria-label"] = "Barcode scannen"
                            unsafe {
                                raw("""<svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z" /></svg>""")
                            }
                        }
                    }
                    button(classes = "p-2 hover:bg-primary-700 active:bg-primary-700 rounded-full transition-colors hidden") {
                        id = "share-button"
                        attributes["aria-label"] = "Teilen"
                        onClick = if (title == null)
                            "shareContent(\"HalalApp.de\", \"Schau dir die HalalApp an: https://halalapp.de\")"
                        else
                            """shareContent("HalalApp.de", "Schau dir \"${
                                title.replace(
                                    "\"",
                                    ""
                                )
                            }'\" in der HalalApp an: " + location.href)"""
                        unsafe {
                            raw("""<svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 -960 960 960" width="24px" fill="currentColor"><path d="M680-80q-50 0-85-35t-35-85q0-6 3-28L282-392q-16 15-37 23.5t-45 8.5q-50 0-85-35t-35-85q0-50 35-85t85-35q24 0 45 8.5t37 23.5l281-164q-2-7-2.5-13.5T560-760q0-50 35-85t85-35q50 0 85 35t35 85q0 50-35 85t-85 35q-24 0-45-8.5T598-672L317-508q2 7 2.5 13.5t.5 14.5q0 8-.5 14.5T317-452l281 164q16-15 37-23.5t45-8.5q50 0 85 35t35 85q0 50-35 85t-85 35Zm0-80q17 0 28.5-11.5T720-200q0-17-11.5-28.5T680-240q-17 0-28.5 11.5T640-200q0 17 11.5 28.5T680-160ZM200-440q17 0 28.5-11.5T240-480q0-17-11.5-28.5T200-520q-17 0-28.5 11.5T160-480q0 17 11.5 28.5T200-440Zm508.5-291.5Q720-743 720-760t-11.5-28.5Q697-800 680-800t-28.5 11.5Q640-777 640-760t11.5 28.5Q663-720 680-720t28.5-11.5ZM680-200ZM200-480Zm480-280Z"/></svg>""")
                        }
                    }
                    script {
                        unsafe { +"if(isSharingSupported()) document.getElementById('share-button').classList.remove('hidden')" }
                    }
                }
            }
        }
        titleBlock?.let {
            div("w-fill bg-primary") {
                role = "banner"
                div("mx-auto w-app max-w-screen px-[15px] flex items-center justify-center") {
                    div("p-4 lg:p-8 text-white text-2xl font-bold flex flex-col gap-2 items-center text-center ") {
                        titleBlock()
                    }
                }
            }
        }
        div("mx-auto w-app max-w-screen px-[15px] flex flex-col gap-4 md:gap-8 py-4 md:py-8") {
            div("contents") {
                role = "main"
                body.invoke(this)
            }
            footer("px-4") {
                role = "contentinfo"
                div("flex flex-wrap justify-center gap-x-3 gap-y-1 text-sm text-gray-700") {
                    span { +"© $year HalalApp" }
                    span { +"|" }
                    span { +"Alle Angaben ohne Gewähr" }
                    span { +"|" }
                    a(classes = "hover:text-gray-900 transition-colors underline decoration-dotted") {
                        href = "https://github.com/metinkale38/halalapp"
                        target = "_blank"
                        +"""Open Source"""
                    }
                    span { +"|" }
                    a(classes = "hover:text-gray-900 transition-colors underline decoration-dotted") {
                        href = "/impressum"
                        +"""Impressum"""
                    }
                }
            }

        }
    }
}

