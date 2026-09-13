package dev.metinkale.halalapp.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.system.exitProcess

private val HMAC_SECRET = System.getenv("DEPLOY_SECRET")

fun Routing.deployRoutes() {
    post("/deploy") {
        val signature = call.request.headers["X-Signature"] ?: return@post call.respond(HttpStatusCode.Unauthorized)

        val fileBytes = call.receive<ByteArray>()
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(HMAC_SECRET.toByteArray(), "HmacSHA256"))
        val expectedSignature = Base64.getEncoder().encodeToString(mac.doFinal(fileBytes))

        if (signature != expectedSignature) {
            call.application.environment.log.error("Failed deployment, invalid signature: $signature")
            call.respond(HttpStatusCode.Forbidden, "Invalid Signature")
            return@post
        }

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val newJar = File("/app/app-$timestamp.jar")
        newJar.writeBytes(fileBytes)

        File("/app").listFiles { _, name -> name.startsWith("app-") && name.endsWith(".jar") }
            ?.sortedByDescending { it.name }
            ?.drop(5)
            ?.forEach { it.delete() }

        call.application.environment.log.info("Deploying version $timestamp...")
        System.out.flush()
        call.respondText("Deploying version $timestamp...")
        CoroutineScope(Dispatchers.IO).launch {
            delay(3000)
            exitProcess(0)
        }
    }
}