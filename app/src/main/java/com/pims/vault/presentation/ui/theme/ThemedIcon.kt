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
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.CollectionsBookmark
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
    val Vault: ImageVector get() = Icons.Default.Description
    val VaultOutlined: ImageVector get() = Icons.Outlined.Description
    val Lock: ImageVector get() = Icons.Default.Lock
    val LockOutlined: ImageVector get() = Icons.Outlined.Lock
    val Key: ImageVector get() = Icons.Default.Key
    val Security: ImageVector get() = Icons.Default.Security
    val Biometric: ImageVector get() = Icons.Default.Fingerprint
    val Home: ImageVector get() = Icons.Default.Home
    val HomeOutlined: ImageVector get() = Icons.Outlined.Home
    // Notes tab icons — filled = selected, outlined = idle
    val Notes: ImageVector get() = Icons.Default.Description
    val NotesOutlined: ImageVector get() = Icons.Outlined.Article
    val Me: ImageVector get() = Icons.Default.Person
    val MeOutlined: ImageVector get() = Icons.Outlined.Person
    val People: ImageVector get() = Icons.Default.People
    val PeopleOutlined: ImageVector get() = Icons.Outlined.People
    val Settings: ImageVector get() = Icons.Default.Settings
    val SettingsOutlined: ImageVector get() = Icons.Outlined.Settings
    // Documents alias (kept for backward compat)
    val Documents: ImageVector get() = Icons.Default.Description
    val DocumentsOutlined: ImageVector get() = Icons.Outlined.Description
    // Trusted contact shield badge
    val TrustedContact: ImageVector get() = Icons.Default.Shield
    val TrustedContactOutlined: ImageVector get() = Icons.Outlined.Shield
    val LocalStorage: ImageVector get() = Icons.Default.Lock
    val Cloud: ImageVector get() = Icons.Default.Cloud
    val Sync: ImageVector get() = Icons.Default.Sync
    val ChevronRight: ImageVector get() = Icons.Default.ChevronRight
    val ArrowBack: ImageVector get() = Icons.AutoMirrored.Filled.ArrowBack
    val ArrowForward: ImageVector get() = Icons.AutoMirrored.Filled.ArrowForward
    val Close: ImageVector get() = Icons.Default.Close
    val Add: ImageVector get() = Icons.Default.Add
    val Edit: ImageVector get() = Icons.Default.Edit
    val Delete: ImageVector get() = Icons.Default.Delete
    val Check: ImageVector get() = Icons.Default.Check
    val Search: ImageVector get() = Icons.Default.Search
    val Share: ImageVector get() = Icons.Default.Share
    val ShareOutlined: ImageVector get() = Icons.Outlined.Share
    val Success: ImageVector get() = Icons.Default.CheckCircle
    val Warning: ImageVector get() = Icons.Default.Warning
    val Danger: ImageVector get() = Icons.Default.ErrorOutline
    val Info: ImageVector get() = Icons.Default.Info
    val Notifications: ImageVector get() = Icons.Default.Notifications
    val NotificationsOutlined: ImageVector get() = Icons.Outlined.Notifications
    val Email: ImageVector get() = Icons.Default.Email
    val EmailOutlined: ImageVector get() = Icons.Outlined.Email
    val Phone: ImageVector get() = Icons.Default.Phone
    // No outlined Phone in Material set — filled serves both states.
    val PhoneOutlined: ImageVector get() = Icons.Default.Phone
    val PersonAdd: ImageVector get() = Icons.Default.PersonAdd
    val Photo: ImageVector get() = Icons.Default.AddPhotoAlternate
    val PhotoOutlined: ImageVector get() = Icons.Outlined.AddPhotoAlternate
    val Calendar: ImageVector get() = Icons.Default.DateRange
    val Birthday: ImageVector get() = Icons.Default.Cake
    val Bookmark: ImageVector get() = Icons.Default.Bookmark
    val Comment: ImageVector get() = Icons.AutoMirrored.Filled.Comment
    val Identity: ImageVector get() = Icons.Default.Badge
    val Library: ImageVector get() = Icons.Outlined.CollectionsBookmark
    val Education: ImageVector get() = Icons.Default.School
    val Work: ImageVector get() = Icons.Default.Work
    val Location: ImageVector get() = Icons.Default.LocationOn
    val MoreActions: ImageVector get() = Icons.Default.MoreVert
    val MoveUp: ImageVector get() = Icons.Default.KeyboardArrowUp
    val MoveDown: ImageVector get() = Icons.Default.KeyboardArrowDown

    val AcUnit: ImageVector get() = Icons.Default.AcUnit
    val AccessTime: ImageVector get() = Icons.Default.AccessTime
    val AccountBalance: ImageVector get() = Icons.Default.AccountBalance
    val Alarm: ImageVector get() = Icons.Default.Alarm
    val ArrowDropDown: ImageVector get() = Icons.Default.ArrowDropDown
    val AssignmentTurnedIn: ImageVector get() = Icons.Default.AssignmentTurnedIn
    val AttachFile: ImageVector get() = Icons.Default.AttachFile
    val Block: ImageVector get() = Icons.Default.Block
    val CalendarMonth: ImageVector get() = Icons.Default.CalendarMonth
    val CalendarToday: ImageVector get() = Icons.Default.CalendarToday
    val CameraAlt: ImageVector get() = Icons.Default.CameraAlt
    val CheckBox: ImageVector get() = Icons.Default.CheckBox
    val ChevronLeft: ImageVector get() = Icons.Default.ChevronLeft
    val CleaningServices: ImageVector get() = Icons.Default.CleaningServices
    val Clear: ImageVector get() = Icons.Default.Clear
    val CloudDownload: ImageVector get() = Icons.Default.CloudDownload
    val Code: ImageVector get() = Icons.Default.Code
    val Computer: ImageVector get() = Icons.Default.Computer
    val ContentCopy: ImageVector get() = Icons.Default.ContentCopy
    val ContentPaste: ImageVector get() = Icons.Default.ContentPaste
    val CreditCard: ImageVector get() = Icons.Default.CreditCard
    val DeleteOutline: ImageVector get() = Icons.Default.DeleteOutline
    val DeleteSweep: ImageVector get() = Icons.Default.DeleteSweep
    val EditOutlined: ImageVector get() = Icons.Outlined.Edit
    val ExpandMore: ImageVector get() = Icons.Default.ExpandMore
    val Favorite: ImageVector get() = Icons.Default.Favorite
    val FileDownload: ImageVector get() = Icons.Default.FileDownload
    val FileUpload: ImageVector get() = Icons.Default.FileUpload
    val FolderZip: ImageVector get() = Icons.Default.FolderZip
    val FormatBold: ImageVector get() = Icons.Default.FormatBold
    val FormatListBulleted: ImageVector get() = Icons.Default.FormatListBulleted
    val FormatListNumbered: ImageVector get() = Icons.Default.FormatListNumbered
    val History: ImageVector get() = Icons.Default.History
    val HourglassEmpty: ImageVector get() = Icons.Default.HourglassEmpty
    val Image: ImageVector get() = Icons.Default.Image
    val Inbox: ImageVector get() = Icons.Default.Inbox
    val Language: ImageVector get() = Icons.Default.Language
    val Link: ImageVector get() = Icons.Default.Link
    val LockOpen: ImageVector get() = Icons.Default.LockOpen
    val LockReset: ImageVector get() = Icons.Default.LockReset
    val MedicalServices: ImageVector get() = Icons.Default.MedicalServices
    val MedicalServicesOutlined: ImageVector get() = Icons.Outlined.MedicalServices
    val Medication: ImageVector get() = Icons.Default.Medication
    val Note: ImageVector get() = Icons.AutoMirrored.Filled.Note
    val NotesSymbol: ImageVector get() = Icons.AutoMirrored.Filled.Notes
    val NotificationsActive: ImageVector get() = Icons.Default.NotificationsActive
    val OpenInBrowser: ImageVector get() = Icons.Default.OpenInBrowser
    val OpenInNew: ImageVector get() = Icons.Default.OpenInNew
    val Password: ImageVector get() = Icons.Default.Password
    val PauseCircle: ImageVector get() = Icons.Default.PauseCircle
    val PersonSearch: ImageVector get() = Icons.Default.PersonSearch
    val PhoneAndroid: ImageVector get() = Icons.Default.PhoneAndroid
    val Pin: ImageVector get() = Icons.Default.Pin
    val Public: ImageVector get() = Icons.Default.Public
    val PushPin: ImageVector get() = Icons.Default.PushPin
    val QrCode: ImageVector get() = Icons.Default.QrCode
    val ReceiptLong: ImageVector get() = Icons.Default.ReceiptLong
    val Redo: ImageVector get() = Icons.AutoMirrored.Filled.Redo
    val Refresh: ImageVector get() = Icons.Default.Refresh
    val Schedule: ImageVector get() = Icons.Default.Schedule
    val SchoolOutlined: ImageVector get() = Icons.Outlined.School
    val Star: ImageVector get() = Icons.Default.Star
    val Storage: ImageVector get() = Icons.Default.Storage
    val Tag: ImageVector get() = Icons.Default.Tag
    val Terminal: ImageVector get() = Icons.Default.Terminal
    val Timer: ImageVector get() = Icons.Default.Timer
    val Title: ImageVector get() = Icons.Default.Title
    val Tune: ImageVector get() = Icons.Default.Tune
    val Undo: ImageVector get() = Icons.AutoMirrored.Filled.Undo
    val Verified: ImageVector get() = Icons.Default.Verified
    val VerifiedUser: ImageVector get() = Icons.Default.VerifiedUser
    val Visibility: ImageVector get() = Icons.Default.Visibility
    val VisibilityOff: ImageVector get() = Icons.Default.VisibilityOff
    val WarningAmber: ImageVector get() = Icons.Default.WarningAmber
    val Wifi: ImageVector get() = Icons.Default.Wifi
    val WorkspacePremium: ImageVector get() = Icons.Default.WorkspacePremium
    val ZoomIn: ImageVector get() = Icons.Default.ZoomIn

    val NationalId: ImageVector get() = NationalIdVector
    val SchoolId: ImageVector get() = SchoolIdVector
    val LibraryId: ImageVector get() = LibraryIdVector
    val MedicalAid: ImageVector get() = MedicalAidVector
    val Membership: ImageVector get() = CustomMembershipVector
    val Bank: ImageVector get() = BankVector

    /** Returns filled icon when selected, outlined when idle. Falls back to filled if no outlined variant. */
    fun navIcon(selected: Boolean, filled: ImageVector, outlined: ImageVector): ImageVector =
        if (selected) filled else outlined

    // Cute Minimalist Vector Resources (Used across Widget and In-App Hub)
    @androidx.annotation.DrawableRes val CuteDocumentsRes: Int = com.pims.vault.R.drawable.ic_persona_documents
    @androidx.annotation.DrawableRes val CutePeopleRes: Int = com.pims.vault.R.drawable.ic_persona_people
    @androidx.annotation.DrawableRes val CuteVaultRes: Int = com.pims.vault.R.drawable.ic_persona_vault
    @androidx.annotation.DrawableRes val CuteNotesRes: Int = com.pims.vault.R.drawable.ic_persona_notes

    // Authentic Brand & Social Vector Drawables
    @androidx.annotation.DrawableRes val SocialWhatsApp: Int = com.pims.vault.R.drawable.ic_social_whatsapp
    @androidx.annotation.DrawableRes val SocialInstagram: Int = com.pims.vault.R.drawable.ic_social_instagram
    @androidx.annotation.DrawableRes val SocialFacebook: Int = com.pims.vault.R.drawable.ic_social_facebook
    @androidx.annotation.DrawableRes val SocialTikTok: Int = com.pims.vault.R.drawable.ic_social_tiktok
    @androidx.annotation.DrawableRes val SocialXTwitter: Int = com.pims.vault.R.drawable.ic_social_x_twitter
    @androidx.annotation.DrawableRes val SocialLinkedIn: Int = com.pims.vault.R.drawable.ic_social_linkedin
    @androidx.annotation.DrawableRes val SocialTelegram: Int = com.pims.vault.R.drawable.ic_social_telegram
    @androidx.annotation.DrawableRes val SocialYouTube: Int = com.pims.vault.R.drawable.ic_social_youtube
    @androidx.annotation.DrawableRes val SocialGitHub: Int = com.pims.vault.R.drawable.ic_social_github
    @androidx.annotation.DrawableRes val SocialDiscord: Int = com.pims.vault.R.drawable.ic_social_discord
    @androidx.annotation.DrawableRes val SocialWebsite: Int = com.pims.vault.R.drawable.ic_social_website
    @androidx.annotation.DrawableRes val SocialBlog: Int = com.pims.vault.R.drawable.ic_social_blog
}

@Composable
fun CutePersonaIcon(
    @androidx.annotation.DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: PersonaIconSize = PersonaIconSize.Medium,
    tint: Color? = null
) {
    val targetTint = tint ?: LocalContentColor.current
    Icon(
        painter = androidx.compose.ui.res.painterResource(id = resId),
        contentDescription = contentDescription,
        modifier = modifier.size(size.dp),
        tint = targetTint
    )
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
    val isDark = LocalPimsDarkTheme.current
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(24.dp),
        color = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
        border = null
    ) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.pims.vault.R.drawable.ic_splash_logo),
                contentDescription = "Persona Emblem",
                modifier = Modifier.size(size * 0.72f)
            )
        }
    }
}
