package com.midnight.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.midnight.assistant.ui.theme.MidnightColors

/**
 * Renders a small, common subset of Markdown — the LLM's replies frequently include
 * **bold**, *italic*, `code`, fenced code blocks, headers, bullet lists, and links, and
 * showing the raw asterisks/backticks/hashes looked broken. This is deliberately a
 * lightweight regex-based renderer rather than a full Markdown/CommonMark implementation:
 * enough to look right for chat-style replies without pulling in a Markdown library.
 *
 * For the parallel plain-text cleanup used before TTS speaks a reply, see
 * [com.midnight.assistant.util.markdownToPlainText].
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MidnightColors.onSurface,
    style: TextStyle = LocalTextStyle.current
) {
    val blocks = remember(text) { splitIntoBlocks(text) }

    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Code -> {
                    Text(
                        text = block.content,
                        style = style.copy(fontFamily = FontFamily.Monospace, fontSize = style.fontSize * 0.9f),
                        color = color,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .background(MidnightColors.surfaceContainerHighest.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                }
                is MarkdownBlock.Line -> {
                    Text(
                        text = renderInline(block.text, color),
                        style = if (block.isHeader) {
                            style.copy(fontWeight = FontWeight.Bold)
                        } else {
                            style
                        },
                        color = color
                    )
                }
            }
        }
    }
}

private sealed class MarkdownBlock {
    data class Line(val text: String, val isHeader: Boolean = false) : MarkdownBlock()
    data class Code(val content: String) : MarkdownBlock()
}

/** Splits raw text into fenced-code-block chunks and regular lines (with bullet/header
 *  markers normalized), preserving order. */
private fun splitIntoBlocks(raw: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = raw.replace("\r\n", "\n").split("\n")
    var i = 0
    val codeBuffer = StringBuilder()

    while (i < lines.size) {
        val line = lines[i]
        if (line.trimStart().startsWith("```")) {
            codeBuffer.clear()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                if (codeBuffer.isNotEmpty()) codeBuffer.append("\n")
                codeBuffer.append(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.Code(codeBuffer.toString()))
            i++ // skip closing fence
            continue
        }

        val trimmed = line.trimStart()
        val leadingSpace = line.takeWhile { it == ' ' }
        val headerMatch = Regex("^#{1,6}\\s+(.*)").find(trimmed)
        val bulletMatch = Regex("^[-*+]\\s+(.*)").find(trimmed)
        val blockquoteMatch = Regex("^>\\s?(.*)").find(trimmed)

        val normalized = when {
            headerMatch != null -> headerMatch.groupValues[1]
            bulletMatch != null -> "$leadingSpace•  ${bulletMatch.groupValues[1]}"
            blockquoteMatch != null -> "$leadingSpace${blockquoteMatch.groupValues[1]}"
            else -> line
        }
        blocks.add(MarkdownBlock.Line(text = normalized, isHeader = headerMatch != null))
        i++
    }
    return blocks
}

/** Parses inline **bold**, *italic*, `code`, ~~strike~~, and [text](url) markers within a
 *  single line into a styled [AnnotatedString]. */
private fun renderInline(text: String, baseColor: Color): AnnotatedString {
    val pattern = Regex(
        "\\*\\*(.+?)\\*\\*" + "|" +
            "__(.+?)__" + "|" +
            "`(.+?)`" + "|" +
            "\\*(.+?)\\*" + "|" +
            "_(.+?)_" + "|" +
            "~~(.+?)~~" + "|" +
            "\\[(.+?)\\]\\((.+?)\\)"
    )

    return buildAnnotatedString {
        var lastIndex = 0
        for (match in pattern.findAll(text)) {
            if (match.range.first > lastIndex) {
                append(text.substring(lastIndex, match.range.first))
            }
            val g = match.groupValues
            when {
                g[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(g[1]) }
                g[2].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(g[2]) }
                g[3].isNotEmpty() -> withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = MidnightColors.surfaceContainerHighest.copy(alpha = 0.6f)
                    )
                ) { append(g[3]) }
                g[4].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(g[4]) }
                g[5].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(g[5]) }
                g[6].isNotEmpty() -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(g[6]) }
                g[7].isNotEmpty() -> withStyle(
                    SpanStyle(textDecoration = TextDecoration.Underline, color = MidnightColors.tertiary)
                ) { append(g[7]) }
                else -> append(match.value)
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
        if (length == 0) {
            // Empty line — keep it as a blank line rather than collapsing spacing entirely.
            append(" ")
        }
        addStyle(SpanStyle(color = baseColor), 0, length)
    }
}
