package com.pims.vault.presentation.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.pimsApplePress
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import java.security.MessageDigest
import java.util.UUID

enum class SocialPlatform(
    val displayName: String,
    val primaryColor: Color,
    val gradientColors: List<Color>,
    val urlPrefix: String,
    val placeholder: String,
    @androidx.annotation.DrawableRes val iconRes: Int
) {
    WHATSAPP(
        displayName = "WhatsApp",
        primaryColor = Color(0xFF25D366),
        gradientColors = listOf(Color(0xFF25D366), Color(0xFF128C7E)),
        urlPrefix = "https://wa.me/",
        placeholder = "Phone number with country code",
        iconRes = PersonaIcons.SocialWhatsApp
    ),
    INSTAGRAM(
        displayName = "Instagram",
        primaryColor = Color(0xFFE1306C),
        gradientColors = listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045)),
        urlPrefix = "https://instagram.com/",
        placeholder = "@username",
        iconRes = PersonaIcons.SocialInstagram
    ),
    FACEBOOK(
        displayName = "Facebook",
        primaryColor = Color(0xFF1877F2),
        gradientColors = listOf(Color(0xFF1877F2), Color(0xFF0D65D9)),
        urlPrefix = "https://facebook.com/",
        placeholder = "profile.username or id",
        iconRes = PersonaIcons.SocialFacebook
    ),
    TIKTOK(
        displayName = "TikTok",
        primaryColor = Color(0xFFFE2C55),
        gradientColors = listOf(Color(0xFF111827), Color(0xFF000000)),
        urlPrefix = "https://tiktok.com/@",
        placeholder = "@username",
        iconRes = PersonaIcons.SocialTikTok
    ),
    X_TWITTER(
        displayName = "X (Twitter)",
        primaryColor = Color(0xFF0F1419),
        gradientColors = listOf(Color(0xFF1F2937), Color(0xFF111827)),
        urlPrefix = "https://x.com/",
        placeholder = "@handle",
        iconRes = PersonaIcons.SocialXTwitter
    ),
    LINKEDIN(
        displayName = "LinkedIn",
        primaryColor = Color(0xFF0A66C2),
        gradientColors = listOf(Color(0xFF0A66C2), Color(0xFF004182)),
        urlPrefix = "https://linkedin.com/in/",
        placeholder = "vanity-name or handle",
        iconRes = PersonaIcons.SocialLinkedIn
    ),
    TELEGRAM(
        displayName = "Telegram",
        primaryColor = Color(0xFF2AABEE),
        gradientColors = listOf(Color(0xFF2AABEE), Color(0xFF229ED9)),
        urlPrefix = "https://t.me/",
        placeholder = "username",
        iconRes = PersonaIcons.SocialTelegram
    ),
    YOUTUBE(
        displayName = "YouTube",
        primaryColor = Color(0xFFFF0000),
        gradientColors = listOf(Color(0xFFFF0000), Color(0xFFCC0000)),
        urlPrefix = "https://youtube.com/@",
        placeholder = "@channel",
        iconRes = PersonaIcons.SocialYouTube
    ),
    GITHUB(
        displayName = "GitHub",
        primaryColor = Color(0xFF24292F),
        gradientColors = listOf(Color(0xFF24292F), Color(0xFF161B22)),
        urlPrefix = "https://github.com/",
        placeholder = "username",
        iconRes = PersonaIcons.SocialGitHub
    ),
    DISCORD(
        displayName = "Discord",
        primaryColor = Color(0xFF5865F2),
        gradientColors = listOf(Color(0xFF5865F2), Color(0xFF4752C4)),
        urlPrefix = "https://discord.com/users/",
        placeholder = "username or tag",
        iconRes = PersonaIcons.SocialDiscord
    ),
    WEBSITE(
        displayName = "Website",
        primaryColor = Color(0xFF0D9488),
        gradientColors = listOf(Color(0xFF0D9488), Color(0xFF0F766E)),
        urlPrefix = "https://",
        placeholder = "yourwebsite.com",
        iconRes = PersonaIcons.SocialWebsite
    ),
    BLOG(
        displayName = "Blog / Substack",
        primaryColor = Color(0xFF7C3AED),
        gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
        urlPrefix = "https://",
        placeholder = "newsletter.substack.com",
        iconRes = PersonaIcons.SocialBlog
    ),
    OTHER(
        displayName = "Custom Link",
        primaryColor = Color(0xFF475569),
        gradientColors = listOf(Color(0xFF475569), Color(0xFF334155)),
        urlPrefix = "https://",
        placeholder = "url or resource link",
        iconRes = PersonaIcons.SocialWebsite
    );

    companion object {
        fun fromName(name: String): SocialPlatform {
            return values().firstOrNull {
                it.name.equals(name, ignoreCase = true) ||
                it.displayName.equals(name, ignoreCase = true) ||
                (name.contains("twitter", ignoreCase = true) && it == X_TWITTER)
            } ?: OTHER
        }
    }
}

data class SocialProfileItem(
    val id: String = UUID.randomUUID().toString(),
    val platform: SocialPlatform,
    val handleOrUrl: String,
    val customPlatformName: String? = null,
    val isPublic: Boolean = true,
    val directUrl: String? = null,
    val passwordHash: String? = null,
    val hasVaultPassword: Boolean = false
) {
    fun getFullUrl(canonicalPhone: String? = null): String {
        if (!directUrl.isNullOrBlank()) {
            val d = directUrl.trim()
            if (d.startsWith("http://") || d.startsWith("https://")) return d
            return "https://$d"
        }
        val trimmed = handleOrUrl.trim()
        if (platform == SocialPlatform.WHATSAPP) {
            val phoneDigits = (canonicalPhone ?: trimmed).filter { it.isDigit() }
            return "https://wa.me/$phoneDigits"
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        val cleanHandle = trimmed.removePrefix("@")
        return "${platform.urlPrefix}$cleanHandle"
    }
}

/**
 * Authentic brand vector badge leveraging centralized icon management.
 */
@Composable
fun SocialPlatformBadge(
    platform: SocialPlatform,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    val cornerRadius = size * 0.28f
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(platform.gradientColors))
            .border(
                0.5.dp,
                Color.White.copy(alpha = 0.25f),
                RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = platform.iconRes),
            contentDescription = null,
            modifier = Modifier.size(size * 0.56f),
            tint = Color.White
        )
    }
}

/**
 * Computes deterministic SHA-256 integrity hash for password verification.
 */
fun computeSha256(input: String): String {
    if (input.isEmpty()) return ""
    return MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

/**
 * Clean, centralized Social Profiles Section conforming strictly to central theme
 * and icon management consoles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialProfilesSection(
    socialProfiles: List<SocialProfileItem>,
    canonicalPhone: String? = null,
    onAddProfile: (SocialProfileItem) -> Unit,
    onRemoveProfile: (String) -> Unit,
    onSyncCloud: (() -> Unit)? = null,
    onRestoreCloud: (() -> Unit)? = null,
    onSavePasswordToVault: ((platform: String, username: String, password: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberPimsHaptics()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header with Title + Quick Action Icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "Socials & Direct Links",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Top action buttons: Use icons instead of text to prevent cut-off
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onSyncCloud != null) {
                    IconButton(
                        onClick = {
                            haptics.selection()
                            onSyncCloud()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Sync",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onRestoreCloud != null) {
                    IconButton(
                        onClick = {
                            haptics.selection()
                            onRestoreCloud()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Restore",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    onClick = {
                        haptics.light()
                        showAddDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = PersonaIcons.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // List of Active Profiles or Clean Empty State
        if (socialProfiles.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = "No Connected Profiles",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        onClick = {
                            haptics.light()
                            showAddDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(PersonaIcons.Add, null, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Connect Profile",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                socialProfiles.forEach { item ->
                    val fullUrl = item.getFullUrl(canonicalPhone)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .pimsApplePress {
                                haptics.selection()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Official Brand Badge
                                SocialPlatformBadge(
                                    platform = item.platform,
                                    size = 42.dp
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.customPlatformName ?: item.platform.displayName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (item.hasVaultPassword || !item.passwordHash.isNullOrBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Text(
                                                        text = "Vault",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = item.handleOrUrl,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (!item.directUrl.isNullOrBlank() && item.directUrl != item.handleOrUrl) {
                                        Text(
                                            text = item.directUrl,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        haptics.selection()
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Open link",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        haptics.warning()
                                        onRemoveProfile(item.id)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = PersonaIcons.Delete,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSocialProfileDialog(
            canonicalPhone = canonicalPhone,
            onAdd = { item, optPassword ->
                onAddProfile(item)
                if (!optPassword.isNullOrBlank() && onSavePasswordToVault != null) {
                    onSavePasswordToVault(
                        item.customPlatformName ?: item.platform.displayName,
                        item.handleOrUrl,
                        optPassword
                    )
                }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

/**
 * Add Social Profile Sheet with full overlay gradient conforming to the app's color scheme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSocialProfileDialog(
    canonicalPhone: String?,
    onAdd: (SocialProfileItem, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberPimsHaptics()
    var selectedPlatform by remember { mutableStateOf(SocialPlatform.WHATSAPP) }
    // Clean inputs: No prefilling username or URL
    var handle by remember { mutableStateOf("") }
    var directLink by remember { mutableStateOf("") }
    var customPlatformName by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var saveToVault by remember { mutableStateOf(false) }

    val computedHash = remember(passwordInput) { computeSha256(passwordInput) }

    val computedUrl = remember(selectedPlatform, handle) {
        val trimmed = handle.trim()
        if (trimmed.isEmpty()) "" else {
            val clean = trimmed.removePrefix("@")
            "${selectedPlatform.urlPrefix}$clean"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Full overlay gradient spanning the entire sheet without cutting off midway
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Selected Brand Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SocialPlatformBadge(
                        platform = selectedPlatform,
                        size = 46.dp
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add ${selectedPlatform.displayName} Profile",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Horizontal Platform Selector with Authentic Badges
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SocialPlatform.values().forEach { platform ->
                            val isSelected = platform == selectedPlatform
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 0.5.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.clickable {
                                    haptics.selection()
                                    selectedPlatform = platform
                                    // No prefilling username or phone
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SocialPlatformBadge(platform = platform, size = 24.dp)
                                    Text(
                                        text = platform.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom Platform Name if "OTHER"
                if (selectedPlatform == SocialPlatform.OTHER) {
                    OutlinedTextField(
                        value = customPlatformName,
                        onValueChange = { customPlatformName = it },
                        label = { Text("Platform / Service Name") },
                        placeholder = { Text("e.g. Substack, Discord, Medium, Portfolio") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Username / Handle Input - NOT prefilled
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("Username or Handle") },
                    placeholder = { Text(selectedPlatform.placeholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Direct Profile URL - NOT prefilled
                OutlinedTextField(
                    value = directLink,
                    onValueChange = { directLink = it },
                    label = { Text("Direct Link (optional)") },
                    placeholder = { Text(selectedPlatform.urlPrefix.ifEmpty { "https://..." }) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Password & Vault Storage Section - Clean, no excessive subtitles or hash dumping
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Password & Vault Storage",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                if (it.isNotBlank()) saveToVault = true
                            },
                            label = { Text("Account Password (optional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (passwordInput.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Save to Vault",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Switch(
                                    checked = saveToVault,
                                    onCheckedChange = { saveToVault = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (handle.isNotBlank() || directLink.isNotBlank()) {
                                haptics.success()
                                val effectiveHandle = handle.trim().ifBlank { directLink.trim() }
                                val effectiveUrl = if (directLink.isNotBlank()) directLink.trim() else computedUrl
                                val hashVal = if (passwordInput.isNotBlank()) computedHash else null

                                onAdd(
                                    SocialProfileItem(
                                        platform = selectedPlatform,
                                        handleOrUrl = effectiveHandle,
                                        customPlatformName = if (selectedPlatform == SocialPlatform.OTHER) customPlatformName.trim().takeIf { it.isNotBlank() } else null,
                                        directUrl = effectiveUrl.takeIf { it.isNotBlank() },
                                        passwordHash = hashVal,
                                        hasVaultPassword = saveToVault && passwordInput.isNotBlank()
                                    ),
                                    if (saveToVault && passwordInput.isNotBlank()) passwordInput else null
                                )
                            }
                        },
                        enabled = handle.isNotBlank() || directLink.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
