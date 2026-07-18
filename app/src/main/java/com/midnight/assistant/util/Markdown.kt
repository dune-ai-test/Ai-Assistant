package com.midnight.assistant.util

/**
 * Strips common Markdown syntax down to plain, readable text — used specifically to clean
 * an assistant reply before handing it to TextToSpeech, so it doesn't literally read out
 * "asterisk asterisk" for **bold**, backticks for `code`, "hashtag" for headers, etc.
 *
 * This is deliberately simple regex-based cleanup rather than a full Markdown parser: the
 * goal is "sounds natural when spoken", not "renders pixel-perfect HTML". For visual
 * rendering in the chat transcript, see [com.midnight.assistant.ui.components.MarkdownText].
 */
fun markdownToPlainText(input: String): String {
    if (input.isBlank()) return input

    var text = input

    // Fenced code blocks: drop the ``` fences (and optional language tag), keep the code text
    // itself since silently dropping a whole answer's worth of code would be worse.
    text = text.replace(Regex("```[a-zA-Z0-9]*\\n"), "")
    text = text.replace("```", "")

    // Inline code
    text = text.replace(Regex("`([^`]+)`"), "$1")

    // Bold + italic combined, then bold, then italic (order matters: longest markers first)
    text = text.replace(Regex("\\*\\*\\*(.+?)\\*\\*\\*"), "$1")
    text = text.replace(Regex("___(.+?)___"), "$1")
    text = text.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
    text = text.replace(Regex("__(.+?)__"), "$1")
    text = text.replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)"), "$1")
    text = text.replace(Regex("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)"), "$1")

    // Strikethrough
    text = text.replace(Regex("~~(.+?)~~"), "$1")

    // Images ![alt](url) -> alt   (before links, since the pattern is a superset of it)
    text = text.replace(Regex("!\\[([^\\]]*)\\]\\([^)]+\\)"), "$1")
    // Links [text](url) -> text
    text = text.replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")

    // Headers, blockquotes, and bullet markers — strip the leading symbol per line
    text = text.lineSequence().joinToString("\n") { line ->
        line
            .replaceFirst(Regex("^(\\s*)#{1,6}\\s+"), "$1")
            .replaceFirst(Regex("^(\\s*)>\\s?"), "$1")
            .replaceFirst(Regex("^(\\s*)[-*+]\\s+"), "$1")
    }

    // Horizontal rules on their own line
    text = text.replace(Regex("(?m)^\\s*([-*_])\\s*(\\1\\s*){2,}$"), "")

    // Collapse runs of blank lines so pauses don't feel unnaturally long
    text = text.replace(Regex("\\n{3,}"), "\n\n")

    return text.trim()
}
