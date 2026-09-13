package dev.metinkale.halalapp.common

import io.ktor.http.*

class HttpException(
    val statusCode: HttpStatusCode,
    override val message: String
) : RuntimeException(message)