package com.pims.vault.presentation.navigation

data class NoteEditorRoute(
    val noteId: String? = null
)

data class VaultEditorRoute(
    val category: String,
    val itemId: String? = null
)
