package dev.metinkale.halalapp.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.metinkale.halalapp.common.HttpException
import io.ktor.http.*
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.uri
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.*
import java.util.Date

private val authToken = System.getenv("AUTH_TOKEN")

fun Application.authConfig() {
    if(authToken == "NO_AUTH") return
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "halalapp"

            authHeader { call ->
                val token = call.request.cookies["halalapp_auth"]
                if (token == null) null
                else HttpAuthHeader.Single("Bearer", token)
            }
            verifier(
                JWT
                    .require(Algorithm.HMAC256(authToken))
                    .withIssuer("halalapp")
                    .build()
            )
            challenge { _, _ ->
                val requestedUri = call.request.uri
                call.respondRedirect("https://forum.halalapp.de/?loginAndRedirectTo=${"https://www.halalapp.de$requestedUri".encodeURLParameter()}")
            }
            validate { credential ->

                try {
                    val username = credential.payload.getClaim("username").asString()
                    val email = credential.payload.getClaim("email").asString()
                    val id = credential.payload.subject
                    val groups = credential.payload.getClaim("groups").asList(String::class.java)

                    if (username != null && id != null) {
                        HalalAppUser(
                            id = id,
                            email = email ?: "",
                            username = username,
                            groups = groups ?: emptyList(),
                            expiresAt = credential.payload.expiresAt
                        )
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
    intercept(ApplicationCallPipeline.Plugins) {
        val principal = call.principal<HalalAppUser>()

        if (principal != null && principal.id != "NoAuth") {
            val now = System.currentTimeMillis()
            if (principal.expiresAt.time - now < 11 * 3600 * 1000) {
                val newExpiresAt = now + (12 * 3600 * 1000)
                val newToken = createJwtToken(principal, newExpiresAt)

                call.response.cookies.append(
                    name = "halalapp_auth",
                    value = newToken,
                    path = "/",
                    httpOnly = true,
                    secure = true,
                    extensions = mapOf("SameSite" to "Lax", "Domain" to ".halalapp.de")
                )
            }
        }
    }
}


private fun createJwtToken(user: HalalAppUser, expiresAt: Long): String {
    return JWT.create()
        .withIssuer("halalapp")
        .withSubject(user.id)
        .withClaim("email", user.email)
        .withClaim("username", user.username)
        .withClaim("groups", user.groups)
        .withIssuedAt(Date())
        .withExpiresAt(Date(expiresAt))
        .sign(Algorithm.HMAC256(authToken))
}

fun Route.authenticated(
    build: Route.() -> Unit,
) {
    if (authToken == "NO_AUTH") build()
    else authenticate("auth-jwt") {
        build()
    }
}

fun RoutingContext.ensureAdmin() {
    if (authToken != "NO_AUTH" && call.principal<HalalAppUser>()?.groups?.contains("Admin") != true) {
        throw HttpException(HttpStatusCode.Forbidden, "Admin-Rechte erforderlich")
    }
}