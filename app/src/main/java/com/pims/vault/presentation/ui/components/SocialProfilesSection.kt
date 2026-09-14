package com.pims.vault.presentation.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

enum class SocialPlatform(
    val displayName: String,
    val iconEmoji: String,
    val urlPrefix: String
) {
    WHATSAPP("WhatsApp", "💬", "https://wa.me/"),
    INSTAGRAM("Instagram", "📸", "https://instagram.com/"),
    FACEBOOK("Facebook", "👥", "https://facebook.com/"),
    TIKTOK("TikTok", "🎵", "https://tiktok.com/@"),
    X_TWITTER("X (Twitter)", "𝕏", "https://x.com/"),
    LINKEDIN("LinkedIn", "💼", "https://linkedin.com/in/"),
    TELEGRAM("Telegram", "✈️", "https://t.me/"),
    YOUTUBE("YouTube", "▶️", "https://youtube.com/@"),
    GITHUB("GitHub", "🐙", "https://github.com/"),
    WEBSITE("Personal Website", "🌐", "https://"),
    BLOG("Blog", "✍️", "https://"),
    OTHER("Custom Link", "🔗", "https://");

    companion object {
        fun fromName(name: String): SocialPlatform {
            return values().firstOrNull { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) } ?: OTHER
        }
    }
}

data class SocialProfileItem(
    val id: String = UUID.randomUUID().toString(),
    val platform: SocialPlatform,
    val handleOrUrl: String,
    val customPlatformName: String? = null,
    val isPublic: Boolean = true
) {
    fun getFullUrl(canonicalPhone: String? = null): String {
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
 * Dedicated Social Profiles & Links management component.
 */
@Composable
fun SocialProfilesSection(
    socialProfiles: List<SocialProfileItem>,
    canonicalPhone: String? = null,
    onAddProfile: (SocialProfileItem) -> Unit,
    onRemoveProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(
                    text = "Social Profiles & Links",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            TextButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Link", fontWeight = FontWeight.SemiBold)
            }
        }

        if (socialProfiles.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("No social profiles yet", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        "Add your WhatsApp, Instagram, LinkedIn, website, or other online profiles to easily share.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                socialProfiles.forEach { item ->
                    val fullUrl = item.getFullUrl(canonicalPhone)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(item.platform.iconEmoji, fontSize = 22.sp)

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.customPlatformName ?: item.platform.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = item.handleOrUrl,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                ) {
                                    Icon(Icons.Default.OpenInNew, "Open link", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }

                                IconButton(onClick = { onRemoveProfile(item.id) }) {
                                    Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
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
            onAdd = { item ->
                onAddProfile(item)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSocialProfileDialog(
    canonicalPhone: String?,
    onAdd: (SocialProfileItem) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPlatform by remember { mutableStateOf(SocialPlatform.WHATSAPP) }
    var handleOrUrl by remember {
        mutableStateOf(if (selectedPlatform == SocialPlatform.WHATSAPP && !canonicalPhone.isNullOrBlank()) canonicalPhone else "")
    }
    var customName by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Social Profile / Link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Platform Selector
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = "${selectedPlatform.iconEmoji} ${selectedPlatform.displayName}",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        SocialPlatform.values().forEach { platform ->
                            DropdownMenuItem(
                                text = { Text("${platform.iconEmoji}  ${platform.displayName}") },
                                onClick = {
                                    selectedPlatform = platform
                                    expandedDropdown = false
                                    if (platform == SocialPlatform.WHATSAPP && !canonicalPhone.isNullOrBlank()) {
                                        handleOrUrl = canonicalPhone
                                    }
                                }
                            )
                        }
                    }
                }

                if (selectedPlatform == SocialPlatform.OTHER) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        placeholder = { Text("Platform Name (e.g. Substack, Portfolio)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = handleOrUrl,
                    onValueChange = { handleOrUrl = it },
                    placeholder = {
                        Text(
                            when (selectedPlatform) {
                                SocialPlatform.WHATSAPP -> "Phone number or +263..."
                                SocialPlatform.INSTAGRAM, SocialPlatform.TIKTOK, SocialPlatform.X_TWITTER, SocialPlatform.GITHUB -> "@username"
                                SocialPlatform.WEBSITE, SocialPlatform.BLOG -> "yourwebsite.com"
                                else -> "Username, handle, or URL"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (handleOrUrl.isNotBlank()) {
                        onAdd(
                            SocialProfileItem(
                                platform = selectedPlatform,
                                handleOrUrl = handleOrUrl.trim(),
                                customPlatformName = customName.trim().takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
