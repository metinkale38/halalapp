package dev.metinkale.halalapp.service

import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.max
import kotlin.math.roundToInt

data class ProcessedImage(
    val jpeg: ByteArray,
    val width: Int,
    val height: Int,
)

object ProductImageService {
    const val MAX_BYTES = 10 * 1024 * 1024
    private const val MAX_DIMENSION = 10_000
    private const val MAX_PIXELS = 60_000_000L
    private const val THUMBNAIL_SIZE = 256

    fun process(bytes: ByteArray, mime: String): ProcessedImage {
        require(bytes.isNotEmpty()) { "Die Bilddatei ist leer." }
        require(bytes.size <= MAX_BYTES) { "Das Bild ist zu groß (maximal 10 MB)." }

        val rgb = toRgb(readImage(bytes))
        return ProcessedImage(
            jpeg = toJpeg(rgb),
            width = rgb.width,
            height = rgb.height,
        )
    }

    fun thumbnail(fullJpeg: ByteArray): ByteArray {
        val source = readImage(fullJpeg)
        val scale = minOf(1.0, THUMBNAIL_SIZE.toDouble() / max(source.width, source.height))
        val width = max(1, (source.width * scale).roundToInt())
        val height = max(1, (source.height * scale).roundToInt())
        return toJpeg(resize(toRgb(source), width, height))
    }

    fun dataUri(jpeg: ByteArray): String =
        "data:image/jpeg;base64," + java.util.Base64.getEncoder().encodeToString(jpeg)

    fun decodeDataUri(value: String): ByteArray {
        val base64 = value.substringAfter("base64,", value)
        return java.util.Base64.getDecoder().decode(base64)
    }

    private fun readImage(bytes: ByteArray): BufferedImage {
        val stream = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
            ?: throw IllegalArgumentException("Das Bild konnte nicht gelesen werden.")
        val readers = ImageIO.getImageReaders(stream)
        if (!readers.hasNext()) {
            stream.close()
            throw IllegalArgumentException(
                "Das Bildformat wird nicht unterstützt. Bitte JPEG oder PNG verwenden."
            )
        }

        val reader = readers.next()
        try {
            reader.input = stream
            val width = reader.getWidth(0)
            val height = reader.getHeight(0)
            require(width > 0 && height > 0) { "Das Bild ist ungültig." }
            require(width <= MAX_DIMENSION && height <= MAX_DIMENSION) {
                "Das Bild ist zu groß (maximal ${MAX_DIMENSION}px Kantenlänge)."
            }
            require(width.toLong() * height.toLong() <= MAX_PIXELS) {
                "Das Bild hat zu viele Pixel."
            }
            return reader.read(0) ?: throw IllegalArgumentException("Das Bild konnte nicht gelesen werden.")
        } finally {
            reader.dispose()
            stream.close()
        }
    }

    private fun toRgb(source: BufferedImage): BufferedImage {
        if (source.type == BufferedImage.TYPE_INT_RGB) return source
        val rgb = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB)
        val graphics = rgb.createGraphics()
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, source.width, source.height)
        graphics.drawImage(source, 0, 0, null)
        graphics.dispose()
        return rgb
    }

    private fun resize(source: BufferedImage, width: Int, height: Int): BufferedImage {
        val target = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = target.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.drawImage(source, 0, 0, width, height, null)
        graphics.dispose()
        return target
    }

    private fun toJpeg(image: BufferedImage): ByteArray {
        val writer = ImageIO.getImageWritersByFormatName("jpg").next()
        return ByteArrayOutputStream().use { output ->
            ImageIO.createImageOutputStream(output).use { imageOutput ->
                writer.output = imageOutput
                val params = writer.defaultWriteParam
                if (params.canWriteCompressed()) {
                    params.compressionMode = ImageWriteParam.MODE_EXPLICIT
                    params.compressionQuality = 0.85f
                }
                writer.write(null, IIOImage(image, null, null), params)
            }
            writer.dispose()
            output.toByteArray()
        }
    }

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
