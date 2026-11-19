package com.pims.vault.domain.model

enum class NoteFormat {
    PLAIN,
    BULLETS,
    NUMBERED;

    companion object {
        fun fromString(value: String): NoteFormat {
            return when (value.uppercase()) {
                "BULLETS" -> BULLETS
                "NUMBERED" -> NUMBERED
                else -> PLAIN
            }
        }
    }
}

fun String.toNumberedText(): String {
    var number = 1
    return lines().joinToString("\n") { line ->
        val trimmed = line.trimStart()
        when {
            trimmed.isEmpty() -> ""
            Regex("^\\d+\\.\\s").containsMatchIn(trimmed) -> {
                val result = trimmed.replaceFirst(Regex("^\\d+\\.\\s*"), "$number. ")
                number++
                result
            }
            else -> "${number++}. $trimmed"
        }
    }
}

fun String.toNoteFormat(): NoteFormat = NoteFormat.fromString(this)

fun String.toBulletText(): String {
    return this.lines().joinToString("\n") { line ->
        val trimmed = line.trimStart()
        if (trimmed.isEmpty()) {
            ""
        } else if (trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            line
        } else {
            "• $trimmed"
        }
    }
}

fun String.toPlainText(): String {
    return this.lines().joinToString("\n") { line ->
        var trimmed = line.trimStart()
        while (trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            trimmed = trimmed.substring(2).trimStart()
        }
        trimmed
    }
}
