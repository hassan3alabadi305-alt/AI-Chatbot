package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStreamReader

data class AttachedFileInfo(
    val uri: String,
    val name: String,
    val mimeType: String?,
    val sizeFormatted: String,
    val textContent: String? = null
)

object FileHelper {

    fun getFileInfo(context: Context, uri: Uri): AttachedFileInfo {
        var name = "ملف_مرفق"
        var sizeBytes = 0L
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        val displayName = cursor.getString(nameIndex)
                        if (!displayName.isNullOrBlank()) {
                            name = displayName
                        }
                    }
                    if (sizeIndex != -1) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sizeFormatted = formatFileSize(sizeBytes)
        val textContent = readTextContentIfPossible(context, uri, mimeType, name)

        return AttachedFileInfo(
            uri = uri.toString(),
            name = name,
            mimeType = mimeType,
            sizeFormatted = sizeFormatted,
            textContent = textContent
        )
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes <= 0 -> "حجم غير معروف"
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
        }
    }

    private fun readTextContentIfPossible(context: Context, uri: Uri, mimeType: String?, fileName: String): String? {
        val isTextMime = mimeType?.startsWith("text/") == true ||
                mimeType == "application/json" ||
                mimeType == "application/xml" ||
                mimeType == "application/javascript"

        val isTextExt = fileName.endsWith(".txt", ignoreCase = true) ||
                fileName.endsWith(".json", ignoreCase = true) ||
                fileName.endsWith(".csv", ignoreCase = true) ||
                fileName.endsWith(".md", ignoreCase = true) ||
                fileName.endsWith(".kt", ignoreCase = true) ||
                fileName.endsWith(".java", ignoreCase = true) ||
                fileName.endsWith(".py", ignoreCase = true) ||
                fileName.endsWith(".xml", ignoreCase = true) ||
                fileName.endsWith(".html", ignoreCase = true) ||
                fileName.endsWith(".log", ignoreCase = true)

        if (isTextMime || isTextExt) {
            return try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.readText().take(12000)
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
        return null
    }
}
