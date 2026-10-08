package com.pims.vault.presentation.vault.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.pims.vault.presentation.ui.theme.LocalPimsDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// -----------------------------------------------------------------
// Bespoke Vector Icons for ID Categories
// -----------------------------------------------------------------

val NationalIdVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "NationalIdVector",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // ID card outline
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 5f)
            lineTo(21f, 5f)
            lineTo(21f, 19f)
            lineTo(3f, 19f)
            close()
            // Head
            moveTo(9f, 9.5f)
            lineTo(9f, 9.6f)
            // Body curve
            moveTo(6.5f, 15f)
            lineTo(11.5f, 15f)
            // Details lines
            moveTo(14f, 9f)
            lineTo(18.5f, 9f)
            moveTo(14f, 12f)
            lineTo(18.5f, 12f)
            moveTo(14f, 15f)
            lineTo(17f, 15f)
        }
    }.build()
}

val SchoolIdVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "SchoolIdVector",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Mortarboard cap top
            moveTo(12f, 3f)
            lineTo(22f, 8f)
            lineTo(12f, 13f)
            lineTo(2f, 8f)
            close()
            // Cap lower body
            moveTo(6f, 10f)
            lineTo(6f, 16f)
            lineTo(12f, 19f)
            lineTo(18f, 16f)
            lineTo(18f, 10f)
            // Tassel
            moveTo(22f, 8f)
            lineTo(22f, 15f)
        }
    }.build()
}

val LibraryIdVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "LibraryIdVector",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Open book spine & pages
            moveTo(12f, 6f)
            lineTo(12f, 20f)
            moveTo(12f, 6f)
            lineTo(4f, 3f)
            lineTo(4f, 17f)
            lineTo(12f, 20f)
            moveTo(12f, 6f)
            lineTo(20f, 3f)
            lineTo(20f, 17f)
            lineTo(12f, 20f)
        }
    }.build()
}

val MedicalAidVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "MedicalAidVector",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Shield outline
            moveTo(12f, 2f)
            lineTo(4f, 5f)
            lineTo(4f, 12f)
            lineTo(12f, 21f)
            lineTo(20f, 12f)
            lineTo(20f, 5f)
            close()
            // First aid cross inside
            moveTo(12f, 7f)
            lineTo(12f, 15f)
            moveTo(8f, 11f)
            lineTo(16f, 11f)
        }
    }.build()
}

val CustomMembershipVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "CustomMembershipVector",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Ribbon star / medal
            moveTo(12f, 3f)
            lineTo(15f, 9f)
            lineTo(21f, 10f)
            lineTo(16.5f, 14.5f)
            lineTo(18f, 21f)
            lineTo(12f, 17.5f)
            lineTo(6f, 21f)
            lineTo(7.5f, 14.5f)
            lineTo(3f, 10f)
            lineTo(9f, 9f)
            close()
        }
    }.build()
}

// -----------------------------------------------------------------
// Themed Badge Composables with Dynamic Palette
// -----------------------------------------------------------------

@Composable
fun IdCategoryThemedBadge(
    categoryName: String,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val isDark = LocalPimsDarkTheme.current
    val icon = when {
        categoryName.contains("National", ignoreCase = true) -> NationalIdVector
        categoryName.contains("School", ignoreCase = true) || categoryName.contains("Student", ignoreCase = true) -> SchoolIdVector
        categoryName.contains("Library", ignoreCase = true) -> LibraryIdVector
        categoryName.contains("Medical", ignoreCase = true) || categoryName.contains("Health", ignoreCase = true) -> MedicalAidVector
        else -> CustomMembershipVector
    }

    val badgeBg = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(badgeBg)
            .border(1.dp, getVaultBorder(), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = categoryName,
            tint = getVaultTextPrimary(),
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
    val isDark = LocalPimsDarkTheme.current
    val icon = when (type) {
        SecretPickerType.PAYMENT_CARD -> Icons.Default.CreditCard
        SecretPickerType.BANK_ACCOUNT -> Icons.Default.AccountBalance
        SecretPickerType.PASSWORD -> Icons.Default.Password
        SecretPickerType.IDENTITY_CREDENTIAL -> Icons.Default.Badge
        SecretPickerType.LOGIN -> Icons.Default.Key
        SecretPickerType.LICENSE_KEY -> Icons.Default.VerifiedUser
        SecretPickerType.PRODUCT_KEY -> Icons.Default.WorkspacePremium
        SecretPickerType.SERIAL_NUMBER -> Icons.Default.QrCode
        SecretPickerType.API_KEY -> Icons.Default.Code
        SecretPickerType.ACCESS_TOKEN -> Icons.Default.Security
        SecretPickerType.RECOVERY_CODE -> Icons.Default.LockReset
        SecretPickerType.DEVICE_KEY -> Icons.Default.Fingerprint
        SecretPickerType.SSH_KEY -> Icons.Default.Terminal
        SecretPickerType.CLOUD_CREDENTIAL -> Icons.Default.Cloud
        SecretPickerType.GAME_CODE -> Icons.Default.Star
        SecretPickerType.CERTIFICATE -> Icons.Default.Shield
        SecretPickerType.CUSTOM_SECRET -> Icons.Default.Lock
    }

    val badgeBg = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(badgeBg)
            .border(1.dp, getVaultBorder(), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = type.displayName,
            tint = getVaultTextPrimary(),
            modifier = Modifier.size(iconSize)
        )
    }
}
