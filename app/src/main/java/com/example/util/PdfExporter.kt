package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun exportAsText(context: Context, session: ChatSessionEntity, messages: List<ChatMessageEntity>): File? {
        return try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanTitle = session.title.replace(Regex("[^a-zA-Z0-9أ-ي]"), "_")
            val file = File(exportDir, "Chat_${cleanTitle}_${System.currentTimeMillis()}.txt")

            val sb = StringBuilder()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            sb.append("=== AI Chatbot Conversation Export ===\n")
            sb.append("Title: ${session.title}\n")
            sb.append("Persona: ${session.personaName}\n")
            sb.append("Date: ${dateFormat.format(Date(session.createdAt))}\n")
            sb.append("=====================================\n\n")

            for (msg in messages) {
                val senderName = if (msg.sender == "user") "User (أنت)" else "AI Chatbot"
                val timeStr = dateFormat.format(Date(msg.timestamp))
                sb.append("[$timeStr] $senderName:\n")
                sb.append("${msg.content}\n\n")
                sb.append("-------------------------------------\n\n")
            }

            FileOutputStream(file).use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportAsPdf(context: Context, session: ChatSessionEntity, messages: List<ChatMessageEntity>): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 dimensions in points
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.parseColor("#4F46E5")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            isAntiAlias = true
        }

        val userLabelPaint = Paint().apply {
            color = Color.parseColor("#4F46E5")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val botLabelPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        var yPos = 40f
        val margin = 40f
        val pageBottom = 800f

        // Draw Title
        canvas.drawText("AI Chatbot - ${session.title}", margin, yPos, titlePaint)
        yPos += 20f

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        canvas.drawText("Persona: ${session.personaName} | Date: ${dateFormat.format(Date(session.createdAt))}", margin, yPos, headerPaint)
        yPos += 15f

        // Divider
        canvas.drawLine(margin, yPos, 555f, yPos, headerPaint)
        yPos += 25f

        var pageNumber = 1

        for (msg in messages) {
            if (yPos > pageBottom - 60f) {
                pdfDocument.finishPage(page)
                pageNumber++
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas
                yPos = 40f
            }

            val isUser = msg.sender == "user"
            val label = if (isUser) "[User]" else "[AI Chatbot]"
            val currentLabelPaint = if (isUser) userLabelPaint else botLabelPaint

            canvas.drawText(label, margin, yPos, currentLabelPaint)
            yPos += 16f

            // Split content into lines to fit page width
            val lines = wrapText(msg.content, paint, 515f)
            for (line in lines) {
                if (yPos > pageBottom) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(newPageInfo)
                    canvas = page.canvas
                    yPos = 40f
                }
                canvas.drawText(line, margin, yPos, paint)
                yPos += 16f
            }
            yPos += 12f
        }

        pdfDocument.finishPage(page)

        return try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanTitle = session.title.replace(Regex("[^a-zA-Z0-9أ-ي]"), "_")
            val pdfFile = File(exportDir, "Chat_${cleanTitle}_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(FileOutputStream(pdfFile))
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        val rawLines = text.split("\n")
        for (rawLine in rawLines) {
            if (rawLine.isBlank()) {
                result.add("")
                continue
            }
            val words = rawLine.split(" ")
            var currentLine = ""
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val width = paint.measureText(testLine)
                if (width <= maxWidth) {
                    currentLine = testLine
                } else {
                    if (currentLine.isNotEmpty()) result.add(currentLine)
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) {
                result.add(currentLine)
            }
        }
        return result
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة المحادثة"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
