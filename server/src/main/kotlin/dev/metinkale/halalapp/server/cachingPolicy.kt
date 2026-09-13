package dev.metinkale.halalapp.server

import io.ktor.http.*
import io.ktor.http.CacheControl.Visibility
import io.ktor.http.content.CachingOptions
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.request.path
import io.ktor.server.routing.*

fun Route.cachingPolicy() {
    install(ConditionalHeaders)
    install(CachingHeaders) {
        options { call, _ ->
            val path = call.request.path()

            if (path.startsWith("/crm")) {
                CachingOptions(CacheControl.NoStore(Visibility.Private))
            } else {
                CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 43200, visibility = Visibility.Public))
            }
        }
    }
}