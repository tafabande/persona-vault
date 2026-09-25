package com.pims.vault.domain.model

enum class NoteFormat {
    PLAIN,
    BULLETS;

    companion object {
        fun fromString(value: String): NoteFormat {
            return when (value.uppercase()) {
                "BULLETS" -> BULLETS
                else -> PLAIN
            }
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
