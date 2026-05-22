package com.ecobook.material

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream

data class PreparedImage(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
)

object ImageCompressionHelper {

    private const val MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L
    private const val TARGET_MAX_DIMENSION = 1024

    fun prepareForUpload(context: Context, uri: Uri, originalFileName: String): PreparedImage {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Não foi possível ler a imagem selecionada.")

        val mimeType = detectMimeType(rawBytes, context.contentResolver.getType(uri))
        require(isSupportedMimeType(mimeType)) {
            "Selecione uma imagem JPEG ou PNG para continuar."
        }

        if (rawBytes.size.toLong() <= MAX_FILE_SIZE_BYTES) {
            return PreparedImage(
                fileName = ensureExtension(originalFileName, mimeType),
                mimeType = mimeType,
                bytes = rawBytes
            )
        }

        val bitmap = decodeBitmap(context, uri)
            ?: throw IllegalArgumentException("Não foi possível decodificar a imagem selecionada.")
        val scaled = scaleBitmap(bitmap, TARGET_MAX_DIMENSION)

        if (mimeType == "image/png") {
            val pngBytes = compressBitmap(scaled, Bitmap.CompressFormat.PNG, 100)
            if (pngBytes.size.toLong() <= MAX_FILE_SIZE_BYTES) {
                return PreparedImage(
                    fileName = ensureExtension(originalFileName, "image/png"),
                    mimeType = "image/png",
                    bytes = pngBytes
                )
            }
        }

        var quality = 90
        var compressedBytes = ByteArray(0)
        while (quality >= 65) {
            compressedBytes = compressBitmap(scaled, Bitmap.CompressFormat.JPEG, quality)
            if (compressedBytes.size.toLong() <= MAX_FILE_SIZE_BYTES) {
                return PreparedImage(
                    fileName = ensureExtension(originalFileName.substringBeforeLast('.'), "image/jpeg"),
                    mimeType = "image/jpeg",
                    bytes = compressedBytes
                )
            }
            quality -= 5
        }

        throw IllegalArgumentException("Não foi possível reduzir a imagem para até 5MB.")
    }

    private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largestSide = maxOf(bitmap.width, bitmap.height)
        if (largestSide <= maxDimension) {
            return bitmap
        }

        val ratio = maxDimension.toFloat() / largestSide.toFloat()
        val targetWidth = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun compressBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(format, quality, outputStream)
        return outputStream.toByteArray()
    }

    private fun detectMimeType(rawBytes: ByteArray, fallbackMimeType: String?): String {
        return when {
            rawBytes.startsWith(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())) -> "image/jpeg"
            rawBytes.startsWith(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)) -> "image/png"
            else -> fallbackMimeType.orEmpty()
        }
    }

    private fun isSupportedMimeType(mimeType: String): Boolean {
        return mimeType == "image/jpeg" || mimeType == "image/png"
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        prefix.indices.forEach { index ->
            if (this[index] != prefix[index]) return false
        }
        return true
    }

    private fun ensureExtension(fileName: String, mimeType: String): String {
        val baseName = fileName.substringBeforeLast('.', fileName)
        val extension = if (mimeType == "image/png") "png" else "jpg"
        return if (fileName.endsWith(".$extension", ignoreCase = true)) fileName else "$baseName.$extension"
    }
}
