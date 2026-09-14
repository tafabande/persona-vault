package com.pims.vault.presentation.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.model.InformationCategory
import com.pims.vault.core.model.InformationSensitivity
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Unified Library Item representing any stored information object across the vault.
 */
data class LibraryItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String,
    val category: InformationCategory,
    val sensitivity: InformationSensitivity,
    val personName: String = "Me",
    val secretValue: String? = null,
    val isFavorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Central Information Library Screen.
 * Search, filter by category, explore real counts, view vs edit separation, and safe review.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformationLibraryScreen(
    items: List<LibraryItem>,
    onBack: () -> Unit,
    onRequestAuthToReveal: (onSuccess: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<InformationCategory?>(null) }
    var onlyFavorites by remember { mutableStateOf(false) }

    var selectedItemForDetail by remember { mutableStateOf<LibraryItem?>(null) }

    // Dynamic Category Item Counts
    val categoryCounts = remember(items) {
        items.groupBy { it.category }.mapValues { it.value.size }
    }

    // Filter items
    val filteredItems = remember(items, searchQuery, selectedCategory, onlyFavorites) {
        items.filter { item ->
            val matchesCategory = selectedCategory == null || item.category == selectedCategory
            val matchesFavorite = !onlyFavorites || item.isFavorite
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                        item.subtitle.contains(searchQuery, ignoreCase = true) ||
                        item.category.displayName.contains(searchQuery, ignoreCase = true) ||
                        item.personName.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesFavorite && matchesSearch
        }.sortedByDescending { it.updatedAt }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    haptics.light()
                    onBack()
                }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Information Library",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${items.size} total stored records",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Global Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search your personal information...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Pills (Horizontal Scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null && !onlyFavorites,
                    onClick = {
                        haptics.selection()
                        selectedCategory = null
                        onlyFavorites = false
                    },
                    label = { Text("All (${items.size})") }
                )

                FilterChip(
                    selected = onlyFavorites,
                    onClick = {
                        haptics.selection()
                        onlyFavorites = !onlyFavorites
                        if (onlyFavorites) selectedCategory = null
                    },
                    label = { Text("⭐ Favorites (${items.count { it.isFavorite }})") }
                )

                InformationCategory.values().forEach { cat ->
                    val count = categoryCounts[cat] ?: 0
                    if (count > 0 || cat == InformationCategory.PASSWORD || cat == InformationCategory.FINANCIAL) {
                        val isProtected = cat.defaultSensitivity == InformationSensitivity.PROTECTED ||
                                cat.defaultSensitivity == InformationSensitivity.HIGHLY_PROTECTED
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                haptics.selection()
                                selectedCategory = if (selectedCategory == cat) null else cat
                                onlyFavorites = false
                            },
                            label = {
                                Text("${cat.displayName}${if (isProtected) " 🔒" else ""} ($count)")
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Items List
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching records found" else "No information in this category yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Add details from your profile, passwords, or cards to populate your library.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems) { item ->
                        LibraryItemCard(
                            item = item,
                            onClick = {
                                haptics.light()
                                selectedItemForDetail = item
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Detail / Review Sheet (View separated from Edit)
    selectedItemForDetail?.let { item ->
        ItemReviewBottomSheet(
            item = item,
            onRequestAuthToReveal = onRequestAuthToReveal,
            onDismiss = { selectedItemForDetail = null }
        )
    }
}

@Composable
fun LibraryItemCard(
    item: LibraryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isProtected = item.sensitivity == InformationSensitivity.PROTECTED ||
            item.sensitivity == InformationSensitivity.HIGHLY_PROTECTED

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (isProtected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProtected) {
                        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.isFavorite) {
                            Text("⭐", fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = "${item.category.displayName} • ${item.subtitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(item.updatedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemReviewBottomSheet(
    item: LibraryItem,
    onRequestAuthToReveal: (onSuccess: () -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager = LocalClipboardManager.current
    var isRevealed by remember { mutableStateOf(false) }

    val requiresAuth = item.sensitivity == InformationSensitivity.PROTECTED ||
            item.sensitivity == InformationSensitivity.HIGHLY_PROTECTED

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.category.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = item.sensitivity.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Value Box
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Stored Value",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                !requiresAuth -> item.subtitle
                                isRevealed -> item.secretValue ?: item.subtitle
                                else -> "••••••••••••"
                            },
                            fontFamily = if (requiresAuth && !isRevealed) FontFamily.Monospace else FontFamily.Default,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )

                        if (requiresAuth) {
                            IconButton(
                                onClick = {
                                    if (isRevealed) {
                                        isRevealed = false
                                    } else {
                                        onRequestAuthToReveal {
                                            isRevealed = true
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle reveal secret"
                                )
                            }
                        }
                    }
                }
            }

            // Copy Action
            Button(
                onClick = {
                    val textToCopy = if (requiresAuth && !isRevealed) {
                        onRequestAuthToReveal {
                            clipboardManager.setText(AnnotatedString(item.secretValue ?: item.subtitle))
                        }
                        return@Button
                    } else {
                        item.secretValue ?: item.subtitle
                    }
                    clipboardManager.setText(AnnotatedString(textToCopy))
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Text("Copy Value", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
