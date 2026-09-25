package com.pims.vault.presentation.notes

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

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
        if (trimmedPrev == "•" || trimmedPrev == "-" || trimmedPrev == "*") {
            // User pressed enter on an empty bullet line -> terminate bullet list
            val startOfLine = if (lastNewline == -1) 0 else lastNewline + 1
            val textWithoutEmptyBullet = newValue.text.substring(0, startOfLine) + newValue.text.substring(cursorPos)
            return TextFieldValue(
                text = textWithoutEmptyBullet,
                selection = TextRange(startOfLine)
            )
        }

        if (prevLine.trimStart().startsWith("• ") || prevLine.trimStart().startsWith("- ")) {
            val prefix = "• "
            val newText = newValue.text.substring(0, cursorPos) + prefix + newValue.text.substring(cursorPos)
            return TextFieldValue(
                text = newText,
                selection = TextRange(cursorPos + prefix.length)
            )
        }

        return newValue
    }

    fun toggleBulletAtCurrentLine(value: TextFieldValue): TextFieldValue {
        val cursor = value.selection.start
        val text = value.text
        val lastNewline = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
        val nextNewline = text.indexOf('\n', cursor)
        val lineStart = if (lastNewline == -1) 0 else lastNewline + 1
        val lineEnd = if (nextNewline == -1) text.length else nextNewline
        val currentLine = text.substring(lineStart, lineEnd)

        val isBullet = currentLine.trimStart().startsWith("• ")
        val updatedLine = if (isBullet) {
            currentLine.replaceFirst("• ", "")
        } else {
            "• $currentLine"
        }

        val newText = text.substring(0, lineStart) + updatedLine + text.substring(lineEnd)
        val diff = updatedLine.length - currentLine.length
        val newCursor = (cursor + diff).coerceIn(0, newText.length)

        return TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }
}
