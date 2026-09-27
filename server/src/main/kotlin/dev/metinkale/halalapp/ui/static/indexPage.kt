package dev.metinkale.halalapp.ui.static

import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.flarum.FlarumResponse
import dev.metinkale.halalapp.ui.component.flarum.renderFlarumFeed
import dev.metinkale.halalapp.ui.component.template
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.html.*
import kotlinx.serialization.json.Json


private val forumCache = flow {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    while (true) {
        val response: FlarumResponse =
            runBlocking {
                client.get("https://forum.halalapp.de/api/discussions?filter[tag]=ank%C3%BCndigungen&sort=-createdAt")
                    .body()
            }
        emit(response)
        delay(3600_000) // 1 Stunde
    }
}.stateIn(CoroutineScope(Dispatchers.IO + SupervisorJob()), started = kotlinx.coroutines.flow.SharingStarted.Eagerly, initialValue = null)

fun HTML.indexPage() = template(
    title = "HalalApp – Dein smarter Begleiter im Supermarkt",
    head = {
        script { src = "/pwa.js" }
    },
    titleBlock = {
        img(classes = "w-24") {
            src = "icons/icon.svg"
            alt = "HalalApp Logo"
        }
        h1 { +"HalalApp" }
        h3("text-center") { +"Dein smarter Begleiter im Supermarkt." }
    }) {
    card("no-back-button") {
        h4 { +"Produkte suchen" }
        div(classes = "flex flex-col gap-6") {


            form(action = "/search", method = FormMethod.get, classes = "flex gap-3") {
                input(
                    type = InputType.text,
                    name = "q",
                    classes = "w-full px-4 py-3.5 rounded-xl border border-gray-200 text-base focus:outline-none focus:ring-2 focus:ring-primary500 focus:border-transparent transition"
                ) {
                    placeholder = "Produkt suchen..."
                    required = true
                }
                button(
                    type = ButtonType.submit,
                    classes = "btn-primary"
                ) {
                    +"Suchen"
                }
            }


            button(classes = "w-full btn-secondary") {
                id = "installBtn"
                onClick = "installApp()"
                attributes["type"] = "button"
                style = "display: none;"
                +"Auf dem Smartphone installieren"
            }
        }
    }

    forumCache.value?.let { feed ->
        renderFlarumFeed(feed)
    }
}