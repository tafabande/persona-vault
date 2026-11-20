package com.pims.vault.presentation.onboarding

import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaIconAction
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.theme.PersonaIcons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import com.pims.vault.presentation.ui.theme.pimsPageGutter
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.util.rememberPimsHaptics

/**
 * Streamlined Display Name Screen.
 * Strictly asks for Display Name only; nukes redundant email/password/country steps.
 */
@Composable
fun MinimalProfileSetupScreen(
    isAccountMode: Boolean,
    isSignInMode: Boolean = false,
    onComplete: (preferredName: String, country: String?, dob: String?, email: String?, password: String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()
    var preferredName by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    val submit: () -> Unit = {
        if (preferredName.isBlank()) {
            haptics.warning()
            nameError = "Please enter your display name"
        } else {
            haptics.success()
            onComplete(preferredName.trim(), null, null, null, null)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = pimsPageGutter(), vertical = 16.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                PersonaIconAction(PersonaIcons.ArrowBack, "Back", onClick = { haptics.light(); onBack() })

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "What should we call you?",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Enter your display name or nickname. Persona uses this to greet and identify you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }

                PersonaTextInput(
                    label = "Display name",
                    value = preferredName,
                    onValueChange = {
                        preferredName = it
                        nameError = null
                    },
                    placeholder = "e.g. Alex, Tinashe, Sam",
                    isError = nameError != null,
                    errorMessage = nameError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )
            }

            PersonaAction(
                label = "Continue",
                onClick = submit,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
