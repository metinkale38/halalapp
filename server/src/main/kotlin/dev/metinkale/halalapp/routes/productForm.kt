package dev.metinkale.halalapp.routes

import dev.metinkale.halalapp.service.ProductImageService
import dev.metinkale.halalapp.service.UploadedImageInput
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.utils.io.jvm.javaio.toInputStream

data class ParsedProductForm(
    val fields: Map<String, List<String>>,
    val files: Map<String, UploadedImageInput>,
) {
    fun first(name: String): String = fields[name]?.firstOrNull().orEmpty()
    fun all(name: String): List<String> = fields[name].orEmpty()
}

suspend fun ApplicationCall.readProductForm(imageField: String = "front"): ParsedProductForm {
    val fields = mutableMapOf<String, MutableList<String>>()
    val files = mutableMapOf<String, UploadedImageInput>()

    receiveMultipart(formFieldLimit = 128 * 1024).forEachPart { part ->
        when (part) {
            is PartData.FormItem -> {
                val name = part.name ?: ""
                fields.getOrPut(name) { mutableListOf() }.add(part.value)
            }

            is PartData.FileItem -> {
                val name = part.name ?: ""
                if (name == imageField && !part.originalFileName.isNullOrBlank()) {
                    val bytes = part.provider().toInputStream().use {
                        it.readNBytes(ProductImageService.MAX_BYTES + 1)
                    }
                    require(bytes.size <= ProductImageService.MAX_BYTES) {
                        "Ein Bild ist zu groß (maximal 10 MB)."
                    }
                    files[name] = UploadedImageInput(
                        mime = part.contentType?.toString() ?: "image/jpeg",
                        bytes = bytes,
                    )
                }
            }

            else -> Unit
        }
        part.release()
    }

    return ParsedProductForm(
        fields = fields.mapValues { it.value.toList() },
        files = files,
    )
}
