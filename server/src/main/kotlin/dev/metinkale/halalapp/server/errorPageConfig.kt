package dev.metinkale.halalapp.server

import dev.metinkale.halalapp.common.HttpException
import dev.metinkale.halalapp.html.errorPage
import dev.metinkale.halalapp.html.notFoundPage
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.contentType
import io.ktor.server.response.respond

fun Application.errorPageConfig(){
    install(StatusPages) {
        exception<HttpException> { call, cause ->
            if (call.request.contentType() == ContentType.Text.Html) {
                call.respondHtml(cause.statusCode) {
                    errorPage(
                        errorCode = cause.statusCode.value.toString(),
                        title = "Fehler",
                        subtilte = cause.message,
                    )
                }
            } else call.respond(cause.statusCode, cause.message)
        }
        exception<Throwable> { call, cause ->
            cause.printStackTrace()
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respondHtml(status) {
                notFoundPage()
            }
        }
    }
}