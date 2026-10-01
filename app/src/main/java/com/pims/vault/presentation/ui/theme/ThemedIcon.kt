package com.pims.vault.presentation.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class PersonaIconSize(val dp: Dp) {
    Micro(16.dp),
    Small(20.dp),
    Medium(24.dp),
    Large(26.dp),
    Hero(36.dp)
}

object PersonaIcons {
    // Filled = selected/active. Outlined = unselected/idle.
    // Naming preserved; no new icon library introduced.
    val Vault: ImageVector = Icons.Default.Shield
    val VaultOutlined: ImageVector = Icons.Outlined.Shield
    val Lock: ImageVector = Icons.Default.Lock
    val LockOutlined: ImageVector = Icons.Outlined.Lock
    val Key: ImageVector = Icons.Default.Key
    val Security: ImageVector = Icons.Default.Security
    val Biometric: ImageVector = Icons.Default.Fingerprint
    val Home: ImageVector = Icons.Default.Home
    val HomeOutlined: ImageVector = Icons.Outlined.Home
    // Notes tab icons — filled = selected, outlined = idle
    val Notes: ImageVector = Icons.Default.Description
    val NotesOutlined: ImageVector = Icons.Outlined.Article
    val Me: ImageVector = Icons.Default.Person
    val MeOutlined: ImageVector = Icons.Outlined.Person
    val People: ImageVector = Icons.Default.People
    val PeopleOutlined: ImageVector = Icons.Outlined.People
    val Settings: ImageVector = Icons.Default.Settings
    val SettingsOutlined: ImageVector = Icons.Outlined.Settings
    // Documents alias (kept for backward compat)
    val Documents: ImageVector = Icons.Default.Description
    val DocumentsOutlined: ImageVector = Icons.Outlined.Description
    // Trusted contact shield badge
    val TrustedContact: ImageVector = Icons.Default.Shield
    val TrustedContactOutlined: ImageVector = Icons.Outlined.Shield
    val LocalStorage: ImageVector = Icons.Default.Lock
    val Cloud: ImageVector = Icons.Default.Cloud
    val Sync: ImageVector = Icons.Default.Sync
    val ChevronRight: ImageVector = Icons.Default.ChevronRight
    val ArrowBack: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val ArrowForward: ImageVector = Icons.AutoMirrored.Filled.ArrowForward
    val Close: ImageVector = Icons.Default.Close
    val Add: ImageVector = Icons.Default.Add
    val Edit: ImageVector = Icons.Default.Edit
    val Delete: ImageVector = Icons.Default.Delete
    val Check: ImageVector = Icons.Default.Check
    val Search: ImageVector = Icons.Default.Search
    val Share: ImageVector = Icons.Default.Share
    val ShareOutlined: ImageVector = Icons.Outlined.Share
    val Success: ImageVector = Icons.Default.CheckCircle
    val Warning: ImageVector = Icons.Default.Warning
    val Danger: ImageVector = Icons.Default.ErrorOutline
    val Info: ImageVector = Icons.Default.Info
    val Notifications: ImageVector = Icons.Default.Notifications
    val NotificationsOutlined: ImageVector = Icons.Outlined.Notifications
    val Email: ImageVector = Icons.Default.Email
    val EmailOutlined: ImageVector = Icons.Outlined.Email
    val Phone: ImageVector = Icons.Default.Phone
    // No outlined Phone in Material set — filled serves both states.
    val PhoneOutlined: ImageVector = Icons.Default.Phone
    val PersonAdd: ImageVector = Icons.Default.PersonAdd
    val Photo: ImageVector = Icons.Default.AddPhotoAlternate
    val PhotoOutlined: ImageVector = Icons.Outlined.AddPhotoAlternate
    val Calendar: ImageVector = Icons.Default.DateRange
    val Birthday: ImageVector = Icons.Default.Cake
    val Bookmark: ImageVector = Icons.Default.Bookmark
    val Comment: ImageVector = Icons.AutoMirrored.Filled.Comment

    /** Returns filled icon when selected, outlined when idle. Falls back to filled if no outlined variant. */
    fun navIcon(selected: Boolean, filled: ImageVector, outlined: ImageVector): ImageVector =
        if (selected) filled else outlined
}

@Composable
fun PersonaIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: PersonaIconSize = PersonaIconSize.Medium,
    tint: Color? = null,
    interactive: Boolean = false,
    pulseOnUpdate: Boolean = false
) {
    val targetTint = tint ?: LocalContentColor.current
    val animatedTint by animateColorAsState(
        targetValue = targetTint,
        animationSpec = tween(durationMillis = 200),
        label = "iconTint"
    )
    val scale by animateFloatAsState(
        targetValue = if (pulseOnUpdate) 1.15f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "iconScale"
    )

    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier
            .size(size.dp)
            .scale(scale),
        tint = animatedTint
    )
}

@Composable
fun PersonaIconBadge(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 40.dp,
    iconSize: PersonaIconSize = PersonaIconSize.Small,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    border: BorderStroke? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, shape)
            .then(if (border != null) Modifier.border(border, shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        PersonaIcon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            size = iconSize,
            tint = contentColor
        )
    }
}

@Composable
fun PersonaBrandEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    iconSize: PersonaIconSize = PersonaIconSize.Hero
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            PersonaIcon(
                imageVector = PersonaIcons.Vault,
                contentDescription = "Persona Emblem",
                size = iconSize,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
