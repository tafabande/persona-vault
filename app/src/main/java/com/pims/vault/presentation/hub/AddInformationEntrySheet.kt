package com.pims.vault.presentation.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import com.pims.vault.core.domain.SemanticFieldRecognizer
import com.pims.vault.core.domain.SemanticFieldRecognizer.CanonicalField
import com.pims.vault.core.model.InformationCategory
import com.pims.vault.core.model.InformationSensitivity
import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaDropdownSelector
import com.pims.vault.presentation.ui.components.PersonaIconAction
import com.pims.vault.presentation.ui.components.PersonaSheet
import com.pims.vault.presentation.ui.components.PersonaSheetHeader
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.PimsSpacing
import com.pims.vault.presentation.ui.theme.pimsPageGutter

internal data class InformationEntrySuggestion(val title: String, val field: CanonicalField?, val category: InformationCategory)

private val entrySuggestions = CanonicalField.entries.map { field ->
    val title = when (field) {
        CanonicalField.RESIDENTIAL_ADDRESS -> "Address"
        CanonicalField.PHONE_NUMBER -> "Phone number"
        CanonicalField.EMAIL_ADDRESS -> "Email address"
        else -> field.canonicalName
    }
    InformationEntrySuggestion(title, field, field.category)
} + InformationEntrySuggestion("Link", null, InformationCategory.SOCIAL)

internal fun suggestInformationEntry(title: String): InformationEntrySuggestion? {
    val input = title.trim()
    if (input.isEmpty()) return null
    val exact = SemanticFieldRecognizer.resolve(input)
    return entrySuggestions.firstOrNull { it.title.equals(input, ignoreCase = true) || exact != null && it.field == exact }
        ?: entrySuggestions.singleOrNull { it.title.startsWith(input, ignoreCase = true) }
}

private val entryCategories = InformationCategory.entries.filterNot {
    it.defaultSensitivity == InformationSensitivity.HIGHLY_PROTECTED
}.associateBy {
    when (it) {
        InformationCategory.OTHER -> "Custom"
        InformationCategory.SOCIAL -> "Links"
        else -> it.displayName
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddInformationEntrySheet(
    onDismiss: () -> Unit,
    onAdd: (title: String, value: String, category: InformationCategory, field: CanonicalField?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var chosenCategory by remember { mutableStateOf<InformationCategory?>(null) }
    val suggestion = remember(title) { suggestInformationEntry(title) }
    val missingSuffix = suggestion?.title?.takeIf {
        it.startsWith(title, ignoreCase = true) && it.length > title.length
    }?.drop(title.length)
    val looksLikeLink = value.trimStart().startsWith("https://", ignoreCase = true) ||
        value.trimStart().startsWith("http://", ignoreCase = true)
    val category = chosenCategory ?: suggestion?.category?.takeIf { it in entryCategories.values }
        ?: if (looksLikeLink) InformationCategory.SOCIAL else InformationCategory.OTHER
    val needsVault = suggestion?.category?.defaultSensitivity == InformationSensitivity.HIGHLY_PROTECTED
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
    val completionStyle = remember(title, missingSuffix, hintColor) {
        if (missingSuffix == null) VisualTransformation.None else VisualTransformation { original ->
            if (original.text != title) return@VisualTransformation TransformedText(original, OffsetMapping.Identity)
            val length = original.length
            TransformedText(
                buildAnnotatedString {
                    append(original)
                    withStyle(SpanStyle(color = hintColor)) { append(missingSuffix) }
                },
                object : OffsetMapping {
                    override fun originalToTransformed(offset: Int): Int = offset
                    override fun transformedToOriginal(offset: Int): Int = offset.coerceAtMost(length)
                }
            )
        }
    }

    PersonaSheet(onDismiss, rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = pimsPageGutter(), vertical = PimsSpacing.m),
            verticalArrangement = Arrangement.spacedBy(PimsSpacing.m)
        ) {
            PersonaSheetHeader("Add information", onDismiss)
            PersonaTextInput(
                value = title,
                onValueChange = { title = it },
                label = "Title",
                visualTransformation = completionStyle,
                trailingIcon = {
                    if (missingSuffix != null && suggestion != null) {
                        PersonaIconAction(PersonaIcons.Check, "Use ${suggestion.title}", { title = suggestion.title })
                    }
                }
            )
            if (needsVault) {
                Text("Add this credential in Vault.", style = MaterialTheme.typography.bodyMedium)
            } else {
                PersonaTextInput(
                    value = value,
                    onValueChange = { value = it },
                    label = if (category == InformationCategory.SOCIAL) "Link" else "Value",
                    keyboardType = if (category == InformationCategory.SOCIAL) KeyboardType.Uri else KeyboardType.Text
                )
                PersonaDropdownSelector(
                    selectedOption = entryCategories.entries.first { it.value == category }.key,
                    options = entryCategories.keys.toList(),
                    onOptionSelected = { chosenCategory = entryCategories[it] },
                    label = "Category",
                    suggested = chosenCategory == null
                )
                PersonaAction(
                    label = "Add",
                    onClick = {
                        val field = suggestion?.let { match ->
                            match.field.takeIf { chosenCategory == null || chosenCategory == match.category }
                        }
                        val label = if (missingSuffix != null && chosenCategory == null) suggestion?.title ?: title else title.trim()
                        onAdd(label, value.trim(), category, field)
                    },
                    enabled = title.isNotBlank() && value.isNotBlank(),
                    icon = PersonaIcons.Add,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
