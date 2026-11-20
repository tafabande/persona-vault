package com.pims.vault.presentation.vault.screens

import com.pims.vault.presentation.ui.theme.PersonaIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun IdCategoryThemedBadge(
    categoryName: String,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val icon = when {
        categoryName.contains("National", ignoreCase = true) -> PersonaIcons.NationalId
        categoryName.contains("School", ignoreCase = true) || categoryName.contains("Student", ignoreCase = true) -> PersonaIcons.SchoolId
        categoryName.contains("Library", ignoreCase = true) -> PersonaIcons.LibraryId
        categoryName.contains("Medical", ignoreCase = true) || categoryName.contains("Health", ignoreCase = true) -> PersonaIcons.MedicalAid
        else -> PersonaIcons.Membership
    }

    val badgeBg = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(badgeBg)
,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = categoryName,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun SecretTypeThemedBadge(
    type: SecretPickerType,
    size: Dp = 36.dp,
    iconSize: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    val icon = when (type) {
        SecretPickerType.PAYMENT_CARD -> PersonaIcons.CreditCard
        SecretPickerType.BANK_ACCOUNT -> PersonaIcons.AccountBalance
        SecretPickerType.PASSWORD -> PersonaIcons.Password
        SecretPickerType.IDENTITY_CREDENTIAL -> PersonaIcons.Identity
        SecretPickerType.LOGIN -> PersonaIcons.Key
        SecretPickerType.LICENSE_KEY -> PersonaIcons.VerifiedUser
        SecretPickerType.PRODUCT_KEY -> PersonaIcons.WorkspacePremium
        SecretPickerType.SERIAL_NUMBER -> PersonaIcons.QrCode
        SecretPickerType.API_KEY -> PersonaIcons.Code
        SecretPickerType.ACCESS_TOKEN -> PersonaIcons.Security
        SecretPickerType.RECOVERY_CODE -> PersonaIcons.LockReset
        SecretPickerType.DEVICE_KEY -> PersonaIcons.Biometric
        SecretPickerType.SSH_KEY -> PersonaIcons.Terminal
        SecretPickerType.CLOUD_CREDENTIAL -> PersonaIcons.Cloud
        SecretPickerType.GAME_CODE -> PersonaIcons.Star
        SecretPickerType.CERTIFICATE -> PersonaIcons.TrustedContact
        SecretPickerType.CUSTOM_SECRET -> PersonaIcons.Lock
    }

    val badgeBg = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(badgeBg)
,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = type.displayName,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(iconSize)
        )
    }
}
