package com.ecobook.material

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File

object ImagePickerHelper {

    fun createCameraImageUri(context: Context): Uri {
        val imagesDir = File(context.cacheDir, "captured-materials").apply { mkdirs() }
        val file = File(imagesDir, "material-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun describeImage(
        context: Context,
        uri: Uri,
        source: ImageSource
    ): SelectedImageUiModel {
        val resolver = context.contentResolver
        val metadata = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else -1L
                    name to size
                } else {
                    null
                }
            }

        val mimeType = resolver.getType(uri).orEmpty()
        val fallbackName = "material-${System.currentTimeMillis()}"
        val resolvedName = metadata?.first?.takeIf { it.isNotBlank() } ?: fallbackName
        val resolvedSize = metadata?.second?.takeIf { it >= 0 } ?: resolveSizeBytes(context, uri)

        return SelectedImageUiModel(
            uri = uri,
            fileName = resolvedName,
            mimeType = mimeType,
            sizeBytes = resolvedSize,
            source = source
        )
    }

    private fun resolveSizeBytes(context: Context, uri: Uri): Long {
        return context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.length.takeIf { it >= 0 } ?: 0L
        } ?: 0L
    }
}
