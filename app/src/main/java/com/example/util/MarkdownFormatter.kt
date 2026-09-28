package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: AnnotatedString) : MarkdownBlock()
    data class Paragraph(val text: AnnotatedString) : MarkdownBlock()
    data class BulletItem(val text: AnnotatedString) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: AnnotatedString) : MarkdownBlock()
    data class Blockquote(val text: AnnotatedString) : MarkdownBlock()
    data class Code(val language: String, val rawCode: String, val highlightedCode: AnnotatedString) : MarkdownBlock()
    object Divider : MarkdownBlock()
}

object MarkdownFormatter {

    fun parseMarkdown(content: String, primaryColor: Color, isUser: Boolean = false): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val lines = content.split("\n")

        var inCodeBlock = false
        var currentCodeLang = ""
        val codeBuilder = StringBuilder()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]

            // Code block delimiter ```
            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    val rawCode = codeBuilder.toString().trimEnd()
                    val highlighted = highlightCode(currentCodeLang, rawCode)
                    blocks.add(MarkdownBlock.Code(currentCodeLang.ifBlank { "code" }, rawCode, highlighted))
                    codeBuilder.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                    currentCodeLang = line.trim().removePrefix("```").trim()
                }
                i++
                continue
            }

            if (inCodeBlock) {
                if (codeBuilder.isNotEmpty()) codeBuilder.append("\n")
                codeBuilder.append(line)
                i++
                continue
            }

            // Horizontal Divider Rule
            val trimmedLine = line.trim()
            if (trimmedLine == "---" || trimmedLine == "***" || trimmedLine == "___") {
                blocks.add(MarkdownBlock.Divider)
                i++
                continue
            }

            // Headers (# Header)
            if (line.startsWith("#")) {
                val level = line.takeWhile { it == '#' }.length
                if (level in 1..6 && line.length > level && line[level] == ' ') {
                    val headerText = line.substring(level + 1).trim()
                    blocks.add(MarkdownBlock.Header(level, formatInlineMarkdown(headerText, primaryColor, isUser)))
                    i++
                    continue
                }
            }

            // Blockquote (> Quote)
            if (trimmedLine.startsWith(">")) {
                val quoteText = trimmedLine.removePrefix(">").trim()
                blocks.add(MarkdownBlock.Blockquote(formatInlineMarkdown(quoteText, primaryColor, isUser)))
                i++
                continue
            }

            // Bullet List (- Item, * Item)
            if ((trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ")) && !trimmedLine.startsWith("**")) {
                val itemText = trimmedLine.substring(2).trim()
                blocks.add(MarkdownBlock.BulletItem(formatInlineMarkdown(itemText, primaryColor, isUser)))
                i++
                continue
            }

            // Numbered List (1. Item)
            val numberMatch = Regex("^(\\d+)\\.\\s+(.*)").find(trimmedLine)
            if (numberMatch != null) {
                val num = numberMatch.groupValues[1]
                val itemText = numberMatch.groupValues[2]
                blocks.add(MarkdownBlock.NumberedItem(num, formatInlineMarkdown(itemText, primaryColor, isUser)))
                i++
                continue
            }

            // Normal Paragraph
            if (line.isNotBlank()) {
                blocks.add(MarkdownBlock.Paragraph(formatInlineMarkdown(line, primaryColor, isUser)))
            }

            i++
        }

        // Close dangling code block if any
        if (inCodeBlock) {
            val rawCode = codeBuilder.toString().trimEnd()
            val highlighted = highlightCode(currentCodeLang, rawCode)
            blocks.add(MarkdownBlock.Code(currentCodeLang.ifBlank { "code" }, rawCode, highlighted))
        }

        if (blocks.isEmpty() && content.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(formatInlineMarkdown(content, primaryColor, isUser)))
        }

        return blocks
    }

    private fun formatInlineMarkdown(text: String, primaryColor: Color, isUser: Boolean): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                // Markdown Link [label](url)
                if (text[i] == '[') {
                    val closeBracket = text.indexOf(']', i + 1)
                    if (closeBracket != -1 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') {
                        val closeParen = text.indexOf(')', closeBracket + 2)
                        if (closeParen != -1) {
                            val label = text.substring(i + 1, closeBracket)
                            val url = text.substring(closeBracket + 2, closeParen)
                            val linkColor = if (isUser) Color(0xFF93C5FD) else Color(0xFF2563EB)
                            pushStringAnnotation(tag = "URL", annotation = url)
                            withStyle(
                                SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) {
                                append(label)
                            }
                            pop()
                            i = closeParen + 1
                            continue
                        }
                    }
                }

                // Raw URL (http:// or https://)
                if (text.startsWith("http://", i, ignoreCase = true) || text.startsWith("https://", i, ignoreCase = true)) {
                    var end = i
                    while (end < text.length && !text[end].isWhitespace() && text[end] != ')' && text[end] != ']' && text[end] != '>') {
                        end++
                    }
                    val rawUrl = text.substring(i, end)
                    val linkColor = if (isUser) Color(0xFF93C5FD) else Color(0xFF2563EB)
                    pushStringAnnotation(tag = "URL", annotation = rawUrl)
                    withStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(rawUrl)
                    }
                    pop()
                    i = end
                    continue
                }

                // Bold ** or __
                if ((i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') ||
                    (i + 1 < text.length && text[i] == '_' && text[i + 1] == '_')
                ) {
                    val symbol = text.substring(i, i + 2)
                    val end = text.indexOf(symbol, i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                        continue
                    }
                }

                // Strikethrough ~~
                if (i + 1 < text.length && text[i] == '~' && text[i + 1] == '~') {
                    val end = text.indexOf("~~", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                        continue
                    }
                }

                // Italic * or _
                if ((text[i] == '*' || text[i] == '_') && (i == 0 || text[i - 1] != '\\')) {
                    val symbol = text[i]
                    val end = text.indexOf(symbol, i + 1)
                    if (end != -1 && end > i + 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                        continue
                    }
                }

                // Inline code `
                if (text[i] == '`') {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1) {
                        val inlineBg = if (isUser) Color.White.copy(alpha = 0.25f) else primaryColor.copy(alpha = 0.15f)
                        val inlineTextColor = if (isUser) Color.White else primaryColor
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = inlineBg,
                                color = inlineTextColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append(" ${text.substring(i + 1, end)} ")
                        }
                        i = end + 1
                        continue
                    }
                }

                append(text[i])
                i++
            }
        }
    }

    private fun highlightCode(language: String, code: String): AnnotatedString {
        val keywordColor = Color(0xFFC678DD) // Purple
        val stringColor = Color(0xFF98C379)  // Green
        val numberColor = Color(0xFFD19A66)  // Orange
        val commentColor = Color(0xFF7F848E) // Gray
        val typeColor = Color(0xFF61AFEF)    // Light Blue
        val defaultColor = Color(0xFFABB2BF) // Soft White

        val keywords = setOf(
            "val", "var", "fun", "class", "object", "interface", "import", "package",
            "return", "if", "else", "when", "for", "while", "do", "try", "catch",
            "finally", "throw", "null", "true", "false", "this", "super", "is", "as",
            "in", "sealed", "data", "override", "private", "protected", "public",
            "def", "lambda", "const", "let", "function", "async", "await", "export",
            "struct", "enum", "fn", "pub", "impl", "use", "trait", "select", "from",
            "where", "insert", "update", "delete", "join", "group", "by", "order", "having"
        )

        return buildAnnotatedString {
            val lines = code.split("\n")
            for ((lineIdx, line) in lines.withIndex()) {
                var idx = 0
                while (idx < line.length) {
                    // Comments // or #
                    if ((idx + 1 < line.length && line.substring(idx, idx + 2) == "//") ||
                        (line[idx] == '#' && (language.equals("python", true) || language.equals("sh", true) || language.equals("bash", true)))
                    ) {
                        withStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)) {
                            append(line.substring(idx))
                        }
                        idx = line.length
                        continue
                    }

                    // Strings "..."
                    if (line[idx] == '"') {
                        val end = line.indexOf('"', idx + 1)
                        if (end != -1) {
                            withStyle(SpanStyle(color = stringColor)) {
                                append(line.substring(idx, end + 1))
                            }
                            idx = end + 1
                            continue
                        }
                    }

                    // Strings '...'
                    if (line[idx] == '\'') {
                        val end = line.indexOf('\'', idx + 1)
                        if (end != -1) {
                            withStyle(SpanStyle(color = stringColor)) {
                                append(line.substring(idx, end + 1))
                            }
                            idx = end + 1
                            continue
                        }
                    }

                    // Identifiers / Keywords
                    if (line[idx].isLetter() || line[idx] == '_') {
                        val start = idx
                        while (idx < line.length && (line[idx].isLetterOrDigit() || line[idx] == '_')) {
                            idx++
                        }
                        val word = line.substring(start, idx)
                        when {
                            keywords.contains(word) -> {
                                withStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)) {
                                    append(word)
                                }
                            }
                            word.first().isUpperCase() -> {
                                withStyle(SpanStyle(color = typeColor)) {
                                    append(word)
                                }
                            }
                            else -> {
                                withStyle(SpanStyle(color = defaultColor)) {
                                    append(word)
                                }
                            }
                        }
                        continue
                    }

                    // Numbers
                    if (line[idx].isDigit()) {
                        val start = idx
                        while (idx < line.length && (line[idx].isDigit() || line[idx] == '.')) {
                            idx++
                        }
                        withStyle(SpanStyle(color = numberColor)) {
                            append(line.substring(start, idx))
                        }
                        continue
                    }

                    // Operators / Symbols
                    withStyle(SpanStyle(color = defaultColor)) {
                        append(line[idx])
                    }
                    idx++
                }

                if (lineIdx < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }
}
