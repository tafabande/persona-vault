package com.pims.vault.presentation.notes

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

data class NoteStats(
    val wordCount: Int,
    val charCount: Int,
    val readingTimeMinutes: Int
)

object NoteEditorLogic {

    fun handleEnterKeyForBullets(
        oldValue: TextFieldValue,
        newValue: TextFieldValue
    ): TextFieldValue {
        // If text hasn't changed or isn't an enter newline addition, return newValue
        if (newValue.text.length <= oldValue.text.length) return newValue
        val cursorPos = newValue.selection.start
        if (cursorPos <= 0 || newValue.text[cursorPos - 1] != '\n') return newValue

        val textBefore = newValue.text.substring(0, cursorPos - 1)
        val lastNewline = textBefore.lastIndexOf('\n')
        val prevLine = if (lastNewline == -1) textBefore else textBefore.substring(lastNewline + 1)
        val trimmedPrev = prevLine.trim()

        // 1. Checklist continuation: [ ] or [x]
        if (trimmedPrev == "[ ]" || trimmedPrev == "[x]" || trimmedPrev == "[X]") {
            // User pressed enter on an empty checklist item -> terminate checklist
            val startOfLine = if (lastNewline == -1) 0 else lastNewline + 1
            val textWithoutEmpty = newValue.text.substring(0, startOfLine) + newValue.text.substring(cursorPos)
            return TextFieldValue(text = textWithoutEmpty, selection = TextRange(startOfLine))
        } else if (prevLine.trimStart().startsWith("[ ] ") || prevLine.trimStart().startsWith("[x] ")) {
            val prefix = "[ ] "
            val newText = newValue.text.substring(0, cursorPos) + prefix + newValue.text.substring(cursorPos)
            return TextFieldValue(text = newText, selection = TextRange(cursorPos + prefix.length))
        }

        // 2. Bullet continuation: • or - or *
        if (trimmedPrev == "•" || trimmedPrev == "-" || trimmedPrev == "*") {
            val startOfLine = if (lastNewline == -1) 0 else lastNewline + 1
            val textWithoutEmptyBullet = newValue.text.substring(0, startOfLine) + newValue.text.substring(cursorPos)
            return TextFieldValue(text = textWithoutEmptyBullet, selection = TextRange(startOfLine))
        }
        if (prevLine.trimStart().startsWith("• ") || prevLine.trimStart().startsWith("- ")) {
            val prefix = "• "
            val newText = newValue.text.substring(0, cursorPos) + prefix + newValue.text.substring(cursorPos)
            return TextFieldValue(text = newText, selection = TextRange(cursorPos + prefix.length))
        }

        // 3. Numbered list continuation: 1. , 2. , etc.
        val numberedRegex = Regex("""^(\d+)\.\s.*""")
        val match = numberedRegex.find(prevLine.trimStart())
        if (match != null) {
            val num = match.groupValues[1].toIntOrNull() ?: 1
            if (prevLine.trim() == "$num.") {
                // Empty numbered line -> terminate
                val startOfLine = if (lastNewline == -1) 0 else lastNewline + 1
                val textWithoutEmpty = newValue.text.substring(0, startOfLine) + newValue.text.substring(cursorPos)
                return TextFieldValue(text = textWithoutEmpty, selection = TextRange(startOfLine))
            } else {
                val nextPrefix = "${num + 1}. "
                val newText = newValue.text.substring(0, cursorPos) + nextPrefix + newValue.text.substring(cursorPos)
                return TextFieldValue(text = newText, selection = TextRange(cursorPos + nextPrefix.length))
            }
        }

        return newValue
    }

    fun toggleBulletAtCurrentLine(value: TextFieldValue): TextFieldValue {
        return toggleLinePrefix(value, "• ", listOf("- ", "* ", "[ ] ", "[x] "))
    }

    fun toggleChecklistAtCurrentLine(value: TextFieldValue): TextFieldValue {
        return toggleLinePrefix(value, "[ ] ", listOf("• ", "- ", "* ", "[x] "))
    }

    fun toggleNumberedListAtCurrentLine(value: TextFieldValue): TextFieldValue {
        return toggleLinePrefix(value, "1. ", listOf("• ", "- ", "* ", "[ ] ", "[x] "))
    }

    fun toggleHeadingAtCurrentLine(value: TextFieldValue, level: Int = 1): TextFieldValue {
        val prefix = if (level == 2) "## " else "# "
        val otherPrefixes = if (level == 2) listOf("# ") else listOf("## ")
        return toggleLinePrefix(value, prefix, otherPrefixes)
    }

    private fun toggleLinePrefix(
        value: TextFieldValue,
        targetPrefix: String,
        replacePrefixes: List<String>
    ): TextFieldValue {
        val cursor = value.selection.start
        val text = value.text
        val lastNewline = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
        val nextNewline = text.indexOf('\n', cursor)
        val lineStart = if (lastNewline == -1) 0 else lastNewline + 1
        val lineEnd = if (nextNewline == -1) text.length else nextNewline
        val currentLine = text.substring(lineStart, lineEnd)

        var cleanLine = currentLine
        for (p in replacePrefixes) {
            if (cleanLine.trimStart().startsWith(p)) {
                cleanLine = cleanLine.replaceFirst(p, "")
                break
            }
        }

        val isTarget = cleanLine.trimStart().startsWith(targetPrefix)
        val updatedLine = if (isTarget) {
            cleanLine.replaceFirst(targetPrefix, "")
        } else {
            "$targetPrefix$cleanLine"
        }

        val newText = text.substring(0, lineStart) + updatedLine + text.substring(lineEnd)
        val diff = updatedLine.length - currentLine.length
        val newCursor = (cursor + diff).coerceIn(0, newText.length)

        return TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }

    fun wrapSelectionWith(value: TextFieldValue, prefix: String, suffix: String = prefix): TextFieldValue {
        val start = value.selection.min
        val end = value.selection.max
        val text = value.text

        if (start == end) {
            // No selection: insert wrapper and place cursor inside
            val newText = text.substring(0, start) + prefix + suffix + text.substring(end)
            return TextFieldValue(
                text = newText,
                selection = TextRange(start + prefix.length)
            )
        }

        val selected = text.substring(start, end)
        // If already wrapped, unwrap
        if (selected.startsWith(prefix) && selected.endsWith(suffix) && selected.length >= prefix.length + suffix.length) {
            val unwrapped = selected.substring(prefix.length, selected.length - suffix.length)
            val newText = text.substring(0, start) + unwrapped + text.substring(end)
            return TextFieldValue(
                text = newText,
                selection = TextRange(start, start + unwrapped.length)
            )
        }

        val wrapped = "$prefix$selected$suffix"
        val newText = text.substring(0, start) + wrapped + text.substring(end)
        return TextFieldValue(
            text = newText,
            selection = TextRange(start, start + wrapped.length)
        )
    }

    fun toggleCheckboxOnLine(content: String, targetLineIndex: Int): String {
        val lines = content.split('\n').toMutableList()
        if (targetLineIndex in lines.indices) {
            val line = lines[targetLineIndex]
            val updated = when {
                line.contains("[ ]") -> line.replaceFirst("[ ]", "[x]")
                line.contains("[x]") -> line.replaceFirst("[x]", "[ ]")
                line.contains("[X]") -> line.replaceFirst("[X]", "[ ]")
                else -> line
            }
            lines[targetLineIndex] = updated
        }
        return lines.joinToString("\n")
    }

    fun calculateStats(text: String): NoteStats {
        val trimmed = text.trim()
        val chars = trimmed.length
        if (trimmed.isEmpty()) {
            return NoteStats(wordCount = 0, charCount = 0, readingTimeMinutes = 0)
        }
        val words = trimmed.split(Regex("""\s+""")).count { it.isNotEmpty() }
        val readingTime = (words / 200).coerceAtLeast(if (words > 0) 1 else 0)
        return NoteStats(
            wordCount = words,
            charCount = chars,
            readingTimeMinutes = readingTime
        )
    }
}
